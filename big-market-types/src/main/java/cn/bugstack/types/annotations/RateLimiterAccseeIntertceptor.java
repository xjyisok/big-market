package cn.bugstack.types.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RateLimiterAccseeIntertceptor {
    String key() default "";
    double permitsPerSecond();
    double blackListCount() default 0;
    String fallbackMethod();
}
