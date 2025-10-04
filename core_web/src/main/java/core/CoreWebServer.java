package core;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CoreWebServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Resource base = Resource.newClassPathResource("/META-INF/resources");
        context.setBaseResource(base);
        context.setWelcomeFiles(new String[]{"index.html"});

        // 映射 DefaultServlet 用于提供静态资源
        ServletHolder staticHolder = new ServletHolder("default", DefaultServlet.class);
        staticHolder.setInitParameter("dirAllowed", "true"); // 可选：显示目录结构
        context.addServlet(staticHolder, "/"); // 根路径映射静态资源
        
        // ----------------------------
        // 匿名类实现心跳 Servlet
        // ----------------------------
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("OK");  // 返回心跳成功
            }
        }), "/heartbeat");

        // API Servlets
        context.addServlet(ProjectServlet.class, "/project");


        server.setHandler(context);
        server.start();
        System.out.println("Core Web Console running at http://localhost:" + port);
        server.join();
    }
}
