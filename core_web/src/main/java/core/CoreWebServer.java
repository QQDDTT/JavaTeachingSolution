package core;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.http.HttpServlet;

public class CoreWebServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        Server server = new Server(port);

        // 创建上下文
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // 设置静态资源目录
        Resource base = Resource.newClassPathResource("/META-INF/resources");
        context.setBaseResource(base);

        // 设置欢迎文件为 home.html
        context.setWelcomeFiles(new String[]{"home.html"});

        // ----------------------------
        // 静态资源 Servlet
        // ----------------------------
        ServletHolder staticHolder = new ServletHolder("default", DefaultServlet.class);
        staticHolder.setInitParameter("dirAllowed", "true"); // 可选：显示目录结构
        context.addServlet(staticHolder, "/"); // 根路径映射静态资源，欢迎文件会自动生效

        // ----------------------------
        // 心跳 Servlet
        // ----------------------------
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                                 jakarta.servlet.http.HttpServletResponse resp)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("OK");
            }
        }), "/heartbeat");

        // ----------------------------
        // API Servlets
        // ----------------------------
        context.addServlet(SystemServlet.class, "/system");
        context.addServlet(ProjectServlet.class, "/project");
        context.addServlet(TerminalServlet.class, "/terminal");

        // 启动 Jetty
        server.setHandler(context);
        server.start();
        System.out.println("Core Web Console running at http://localhost:" + port);
        server.join();
    }
}
