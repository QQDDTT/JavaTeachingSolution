package web;

import common.server.LifecycleServer;
import common.annotations.ReflectWebServerArgs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Web 子项目 Server 实现
 */
public class WebServerImpl implements LifecycleServer {

    private Server server;

    @Override
    @ReflectWebServerArgs(port = 10002, sslEnabled = false, sessionTimeout = 3600)
    public void start(int port, boolean sslEnabled, int sessionTimeout) throws Exception {
        server = new Server(port);

        // 启用 Session 支持
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // 设置 Session 超时
        context.getSessionHandler().setMaxInactiveInterval(sessionTimeout);

        // 静态资源路径
        Resource base = Resource.newClassPathResource("/META-INF/resources");
        if (base == null || !base.exists()) {
            throw new RuntimeException("无法加载资源文件，请确保 resources 放在 META-INF/resources 内并打包到 JAR！");
        }
        context.setBaseResource(base);

        context.setWelcomeFiles(new String[]{"home.html"});
        context.addServlet(DefaultServlet.class, "/").setInitParameter("dirAllowed", "true");

        // ✅ 添加 GameServlet
        context.addServlet(GameServlet.class, "/game/*");

        // ✅ 添加 Session 测试 Servlet（可选）
        context.addServlet(SessionInfoServlet.class, "/session");

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
        return "web_gobang";
    }

    /**
     * ✅ 一个简单的 Servlet 示例：演示 sessionId 的生成与读取
     */
    public static class SessionInfoServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json; charset=utf-8");

            // 获取 session（没有则创建）
            HttpSession session = req.getSession(true);
            String sessionId = session.getId();

            // 记录日志
            System.out.println("[SESSION] ID = " + sessionId +
                    " | isNew = " + session.isNew() +
                    " | Created = " + session.getCreationTime());

            // 输出 JSON 响应
            resp.getWriter().write("""
                {
                  "sessionId": "%s",
                  "isNew": %s
                }
                """.formatted(sessionId, session.isNew()));
        }
    }
}
