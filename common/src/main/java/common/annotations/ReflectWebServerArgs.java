package common.annotations;

import java.lang.annotation.*;

/**
 * Web 项目注解
 * - 反射读取 server 启动参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReflectWebServerArgs {
    int port() default 8080;
    boolean sslEnabled() default false;
    int sessionTimeout() default 30; // 单位：分钟
}
