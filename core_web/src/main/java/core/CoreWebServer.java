package core;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

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
        
        // API Servlets
        context.addServlet(ModuleListServlet.class, "/api/modules");
        context.addServlet(RunModuleServlet.class, "/api/run/*");
        context.addServlet(FileTreeServlet.class, "/api/filetree/*");
        context.addServlet(FileSaveServlet.class, "/api/save/*");
        context.addServlet(NewModuleServlet.class, "/api/new/*");
        context.addServlet(RestartServlet.class, "/api/restart");

        

        server.setHandler(context);
        server.start();
        System.out.println("Core Web Console running at http://localhost:" + port);
        server.join();
    }
}
