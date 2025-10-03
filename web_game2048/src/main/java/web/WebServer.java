package web;

/**
 * WebServer 启动入口
 * - 简化，只用于 CoreRunner 反射调用
 */
public class WebServer {

    public static void main(String[] args) throws Exception {

        WebServerImpl serverImpl = new WebServerImpl();
        serverImpl.start(8080, false, 3600);

        // 阻塞线程，保证服务存活
        Thread.currentThread().join();
    }
}
