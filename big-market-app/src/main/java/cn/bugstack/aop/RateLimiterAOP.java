package cn.bugstack.aop;


import cn.bugstack.types.annotations.DCCValue;
import cn.bugstack.types.annotations.RateLimiterAccseeIntertceptor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimiterAOP {
    @DCCValue("rateLimiterSwitch:closed")
    private String rateLimiterSwitch;
    // 个人限频记录1分钟
    private final Cache<String, RateLimiter> loginRecord = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    // 个人限频黑名单24h - 分布式业务场景，可以记录到 Redis 中
    private final Cache<String, Long> blacklist = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();


    @Pointcut("@annotation(cn.bugstack.types.annotations.RateLimiterAccseeIntertceptor)")
    public void aopPoint() {

    }
    @Around("aopPoint() && @annotation(rateLimiterAccseeIntertceptor)")
    public Object doRouter(ProceedingJoinPoint jp, RateLimiterAccseeIntertceptor rateLimiterAccseeIntertceptor) throws Throwable {
        if(StringUtils.isBlank(rateLimiterSwitch)||"closed".equals(rateLimiterSwitch)) {
            return jp.proceed();
        }
        String key= rateLimiterAccseeIntertceptor.key();
        if(StringUtils.isEmpty(key)){
            throw new RuntimeException("userId is null");
        }
        String keyAttr=getAttrValue(key,jp.getArgs());
        log.info("aop attrr:{}",keyAttr);
        // 黑名单拦截
        if (!"all".equals(keyAttr) && rateLimiterAccseeIntertceptor.blackListCount() != 0 && null != blacklist.getIfPresent(keyAttr) && blacklist.getIfPresent(keyAttr) > rateLimiterAccseeIntertceptor.blackListCount()) {
            log.info("限流-黑名单拦截(24h)：{}", keyAttr);
            return fallbackMethodResult(jp, rateLimiterAccseeIntertceptor.fallbackMethod());
        }

        RateLimiter rateLimiter = loginRecord.getIfPresent(keyAttr);
        if(rateLimiter == null){
            rateLimiter = RateLimiter.create(rateLimiterAccseeIntertceptor.permitsPerSecond());
            loginRecord.put(keyAttr,rateLimiter);
        }
        if(!rateLimiter.tryAcquire()){
            if(rateLimiterAccseeIntertceptor.blackListCount()!=0){
                if(null==blacklist.getIfPresent(keyAttr)){
                    blacklist.put(keyAttr,1L);
                }else{
                    blacklist.put(keyAttr,blacklist.getIfPresent(keyAttr)+1L);
                }
            }
            log.info("限流超频次拦截：{}",keyAttr);
            return fallbackMethodResult(jp, rateLimiterAccseeIntertceptor.fallbackMethod());
        }
        return jp.proceed();
    }
    private Object fallbackMethodResult(JoinPoint jp, String fallbackMethod) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Signature sig = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        Method method = jp.getTarget().getClass().getMethod(fallbackMethod, methodSignature.getParameterTypes());
        return method.invoke(jp.getThis(), jp.getArgs());
    }


    public String getAttrValue(String attr, Object[] args) {
        if (args[0] instanceof String) {
            return args[0].toString();
        }
        String filedValue = null;
        for (Object arg : args) {
            try {
                if (StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                // filedValue = BeanUtils.getProperty(arg, attr);
                // fix: 使用lombok时，uId这种字段的get方法与idea生成的get方法不同，会导致获取不到属性值，改成反射获取解决
                filedValue = String.valueOf(this.getValueByName(arg, attr));
            } catch (Exception e) {
                log.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return filedValue;
    }

    /**
     * 获取对象的特定属性值
     *
     * @param item 对象
     * @param name 属性名
     * @return 属性值
     * @author tang
     */
    private Object getValueByName(Object item, String name) {
        try {
            Field field = getFieldByName(item, name);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object o = field.get(item);
            field.setAccessible(false);
            return o;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 根据名称获取方法，该方法同时兼顾继承类获取父类的属性
     *
     * @param item 对象
     * @param name 属性名
     * @return 该属性对应方法
     * @author tang
     */
    private Field getFieldByName(Object item, String name) {
        try {
            Field field;
            try {
                field = item.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                field = item.getClass().getSuperclass().getDeclaredField(name);
            }
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }


}
