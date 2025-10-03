package common.annotations;

import java.lang.annotation.*;

/**
 * 控制台项目注解
 * - 反射读取 main 方法参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReflectConsoleArgs {
    String[] value() default {};
}
