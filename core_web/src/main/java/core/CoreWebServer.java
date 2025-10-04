package core;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CoreWebServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        int maxUsers = 1; // 同时允许的最大用户数

        QueuedThreadPool threadPool = new QueuedThreadPool(10, 1, 30000);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Resource base = Resource.newClassPathResource("/META-INF/resources");
        context.setBaseResource(base);
        context.setWelcomeFiles(new String[]{"home.html"});

        // 全局用户访问限制 Filter
        addGlobalUserLimit(context, maxUsers);

        // 静态资源 Servlet
        ServletHolder staticHolder = new ServletHolder("default", org.eclipse.jetty.servlet.DefaultServlet.class);
        staticHolder.setInitParameter("dirAllowed", "true");
        context.addServlet(staticHolder, "/");

        // 心跳 Servlet
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("OK");
            }
        }), "/heartbeat");

        // 其他 API Servlets
        context.addServlet(SystemServlet.class, "/system");
        context.addServlet(ProjectServlet.class, "/project");
        context.addServlet(TerminalServlet.class, "/terminal");

        server.setHandler(context);
        server.start();
        System.out.println("Core Web Console running at http://localhost:" + port);
        server.join();
    }

    /**
     * 全局用户访问限制
     *
     * @param context ServletContextHandler
     * @param maxUsers 最大同时访问用户数
     */
    private static void addGlobalUserLimit(ServletContextHandler context, int maxUsers) {
        // 信号量控制最大用户数
        Semaphore userSemaphore = new Semaphore(maxUsers);

        // Map 存储活跃 Session
        ConcurrentHashMap<String, Boolean> activeUsers = new ConcurrentHashMap<>();

        Filter userLimitFilter = new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {}

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse resp = (HttpServletResponse) response;
                HttpSession session = req.getSession(true);
                String sessionId = session.getId();

                boolean isNewUser = !activeUsers.containsKey(sessionId);

                if (isNewUser) {
                    // 尝试获取 Semaphore
                    if (!userSemaphore.tryAcquire()) {
                        resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        resp.getWriter().write("Server busy, maximum users reached.");
                        return;
                    }
                    activeUsers.put(sessionId, Boolean.TRUE);
                }

                try {
                    chain.doFilter(request, response);
                } finally {
                    // 请求结束后检查 Session 是否无更多请求，如果是新用户且请求结束释放 Semaphore
                    // 这里简单处理：每个请求结束释放一次 Semaphore，不考虑同一用户多个并发请求
                    // 更精细实现可以统计每个 Session 内请求数
                    if (isNewUser) {
                        activeUsers.remove(sessionId);
                        userSemaphore.release();
                    }
                }
            }

            @Override
            public void destroy() {}
        };

        context.addFilter(new FilterHolder(userLimitFilter), "/*", null);
    }
}