package core;

import common.annotations.ReflectWebServerArgs;
import common.server.LifecycleServer;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Web 子项目加载器
 * - 独立类加载器
 * - 优先通过固定类名加载实现
 * - 失败则通过 SPI 加载 LifecycleServer
 * - 启动后主线程等待固定时间
 * - 优先使用 start(port, sslEnabled, sessionTimeout)，回退执行 main(String[] args)
 */
public class WebLoader {

    public static void runWeb(File[] jars, String moduleName) throws Exception {

        // 构建 URLClassLoader
        URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            urls[i] = jars[i].toURI().toURL();
        }

        URLClassLoader loader = new URLClassLoader(urls, CoreRunner.class.getClassLoader()) {
            @Override
            public URL getResource(String name) {
                return findResource(name); // 仅从子项目查找资源
            }

            @Override
            public java.util.Enumeration<URL> getResources(String name) {
                try {
                    return findResources(name);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    return java.util.Collections.emptyEnumeration();
                }
            }
        };

        final LifecycleServer[] serverRef = new LifecycleServer[1];

        // 尝试固定类名加载
        try {
            Class<?> clazz = loader.loadClass("web.WebServerImpl");
            serverRef[0] = (LifecycleServer) clazz.getDeclaredConstructor().newInstance();
            System.out.println("WebLoader: 使用固定类名加载 WebServerImpl 成功");
        } catch (ClassNotFoundException e) {
            // SPI 加载
            ServiceLoader<LifecycleServer> spiLoader = ServiceLoader.load(LifecycleServer.class, loader);
            for (LifecycleServer s : spiLoader) {
                serverRef[0] = s;
                System.out.println("WebLoader: 使用 SPI 加载 LifecycleServer 成功 -> " + s.getClass().getName());
                break;
            }
        }

        if (serverRef[0] == null) {
            throw new RuntimeException("未找到 LifecycleServer 实现，请确认子项目已实现固定类名或 SPI");
        }

        final LifecycleServer server = serverRef[0];

        // 启动子线程
        Thread webThread = new Thread(() -> {
            try {
                boolean startWithArgs = false;
                // 尝试使用 start 方法 + 注解参数
                try {
                    Method startMethod = server.getClass()
                            .getMethod("start", int.class, boolean.class, int.class);
                    ReflectWebServerArgs ann = startMethod.getAnnotation(ReflectWebServerArgs.class);
                    if (ann != null) {
                        int port = ann.port();
                        boolean sslEnabled = ann.sslEnabled();
                        int sessionTimeout = ann.sessionTimeout();
                        System.out.println("WebLoader: 启动 " + moduleName + " 使用 start 注解参数: port=" 
                                + port + ", ssl=" + sslEnabled + ", timeout=" + sessionTimeout);
                        startMethod.invoke(server, port, sslEnabled, sessionTimeout);
                        startWithArgs = true;
                    }
                } catch (NoSuchMethodException e) {
                    // 方法不存在，回退执行 main
                }

                // 回退执行 main(String[] args)
                if (!startWithArgs) {
                    try {
                        Method mainMethod = server.getClass().getMethod("main", String[].class);
                        System.out.println("WebLoader: 注解不存在或参数不匹配，执行 main 方法启动 " + moduleName);
                        mainMethod.invoke(null, (Object) new String[]{});
                    } catch (NoSuchMethodException ex) {
                        System.err.println("WebLoader: main 方法不存在，无法启动服务 " + moduleName);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "WebLoader-" + moduleName);

        webThread.setDaemon(true);
        webThread.start();

        System.out.println("WebLoader: 等待 10 秒，确保服务启动...");
        TimeUnit.SECONDS.sleep(10);
        System.out.println("WebLoader: " + moduleName + " 继续执行主线程逻辑");
    }
}
