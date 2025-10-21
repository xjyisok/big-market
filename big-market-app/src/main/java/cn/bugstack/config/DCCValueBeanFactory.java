package cn.bugstack.config;

import cn.bugstack.types.annotations.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {
    private static final String BASE_CONFIG_PATH="/big-market-dcc";
    private static final String BASE_CONFIG_PATH_CONFIG=BASE_CONFIG_PATH+"/config";
    private final CuratorFramework client;
    private final Map<String,Object> dccObjGroup=new HashMap<String,Object>();

    public DCCValueBeanFactory(CuratorFramework client)throws Exception {
        this.client = client;
        if(null==client.checkExists().forPath(BASE_CONFIG_PATH_CONFIG)){
            client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH_CONFIG);
        }
        CuratorCache curatorCache=CuratorCache.build(client,BASE_CONFIG_PATH_CONFIG);
        curatorCache.start();
        curatorCache.listenable().addListener((type, oldData, data) ->{
            switch(type){
                case NODE_CHANGED:
                    String dccValuePath=data.getPath();
                    Object objBean=dccObjGroup.get(dccValuePath);
                    if(null==objBean){return;}
                    try {
                        Class<?> objBeanClass = objBean.getClass();
                        // 检查 objBean 是否是代理对象
                        if (AopUtils.isAopProxy(objBean)) {
                            // 获取代理对象的目标对象
                            objBeanClass = AopUtils.getTargetClass(objBean);
//                            objBeanClass = AopProxyUtils.ultimateTargetClass(objBean);
                        }

                        Field field=objBeanClass.getDeclaredField(dccValuePath.substring(dccValuePath.lastIndexOf("/")+1));
                        field.setAccessible(true);
                        field.set(objBean,new String(data.getData()));
                        field.setAccessible(false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                default:break;
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 注意；增加 AOP 代理后，获得类的方式要通过 AopProxyUtils.getTargetClass(bean);
        // 不能直接 bean.class 因为代理后类的结构发生变化，这样不能获得到自己的自定义注解了。
        Class<?>targetBeanClass= bean.getClass();
        Object targetBeanObject=bean;
        if(AopUtils.isAopProxy(bean)){
            targetBeanClass=AopUtils.getTargetClass(bean);
            targetBeanObject= AopProxyUtils.getSingletonTarget(bean);
        }
        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAnnotationPresent(DCCValue.class)){
                continue;
            }
            DCCValue dcc = field.getAnnotation(DCCValue.class);
            String value = dcc.value();
            if(value.isEmpty()){
                throw new RuntimeException("dcc value is not configured");
            }
            String[] values = value.split(":");
            String key = values[0];
            String defaultValue = values.length ==2 ? values[1] : null;
            String keyPath = BASE_CONFIG_PATH_CONFIG+"/"+key;
            try{
                if(null==client.checkExists().forPath(keyPath)){
                    client.create().creatingParentsIfNeeded().forPath(keyPath);
                    if(StringUtils.isNotBlank(defaultValue)){
                        field.setAccessible(true);
                        field.set(targetBeanObject, defaultValue);
                        field.setAccessible(false);
                    }
                }else{
                    String configValue=new String(client.getData().forPath(keyPath));
                    if(StringUtils.isNotBlank(defaultValue)){
                        field.setAccessible(true);
                        field.set(targetBeanObject, configValue);
                        field.setAccessible(false);
                    }
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            dccObjGroup.put(keyPath,targetBeanObject);
        }
        return bean;
    }
}
