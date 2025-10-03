package web;

import common.server.LifecycleServer;
import common.annotations.ReflectWebServerArgs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;

import java.io.IOException;


/**
 * Web 子项目 Server 实现
 */
public class WebServerImpl implements LifecycleServer {

    private Server server;

    @Override
    @ReflectWebServerArgs(port = 10001, sslEnabled = false, sessionTimeout = 3600)
    public void start(int port, boolean sslEnabled, int sessionTimeout) throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.getSessionHandler().setMaxInactiveInterval(sessionTimeout);

        Resource base = Resource.newClassPathResource("/META-INF/resources");
        if (base == null || !base.exists()) {
            throw new RuntimeException("无法加载资源文件，请确保 resources 放在 META-INF/resources 内并打包到 JAR！");
        }
        context.setBaseResource(base);

        // 默认首页文件
        context.setWelcomeFiles(new String[]{"home.html"});
        context.addServlet(DefaultServlet.class, "/").setInitParameter("dirAllowed", "true");

        // 添加 API Servlet
        context.addServlet(GameServlet.class, "/api/*");

        server.setHandler(context);
        server.start();

        System.out.println(getName() + " started at port " + port +
                ", SSL=" + sslEnabled +
                ", sessionTimeout=" + sessionTimeout + "s");
    }

    @Override
    public void stop() throws Exception {
        if (server != null && server.isStarted()) {
            server.stop();
            System.out.println(getName() + " stopped.");
        }
    }

    @Override
    public String getName() {
        return "web_game2048";
    }

}
