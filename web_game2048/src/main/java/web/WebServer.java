package web;

/**
 * WebServer 启动入口
 * - 简化，只用于 CoreRunner 反射调用
 */
public class WebServer {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        boolean sslEnabled = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
        int sessionTimeout = args.length > 2 ? Integer.parseInt(args[2]) : 600;

        WebServerImpl serverImpl = new WebServerImpl();
        serverImpl.start(port, sslEnabled, sessionTimeout);

        // 阻塞线程，保证服务存活
        Thread.currentThread().join();
    }
}
