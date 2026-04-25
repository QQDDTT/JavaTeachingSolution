package core;

import common.annotations.ReflectConsoleArgs;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 控制台子项目加载器
 * - 通过子项目 jar 加载 main 类
 * - 支持 @MainArgs 注解
 */
public class ConsoleLoader {

    public static void runConsole(File[] jars, String moduleName) throws Exception {

        URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            urls[i] = jars[i].toURI().toURL();
        }

        try (URLClassLoader loader = new URLClassLoader(urls, CoreRunner.class.getClassLoader())) {
            Class<?> clazz = loader.loadClass("console.Main");
            Method mainMethod = clazz.getMethod("main", String[].class);

            ReflectConsoleArgs ann = mainMethod.getAnnotation(ReflectConsoleArgs.class);
            String[] finalArgs = (ann != null) ? ann.value() : new String[]{};

            System.out.println("ConsoleLoader: Running " + moduleName);
            mainMethod.invoke(null, (Object) finalArgs);
        }
    }
}
