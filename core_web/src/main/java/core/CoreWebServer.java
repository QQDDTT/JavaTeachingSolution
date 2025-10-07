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
/**
 * 核心 Web 服务器启动器
 */
public class CoreWebServer {
    /** 服务器端口号 */
    private static final int PORT = 8080;

    /** 最大允许用户连接数（示例） */
    private static final int MAX_USER = 100;

    /** 会话超时时间（分钟）*/
    private static final int SESSION_TIMEOUT = -1;

    /** 静态资源路径（相对于项目根目录） */
    private static final String STATIC_RESOURCE_PATH = "/META-INF/resources";
    public static void main(String[] args) throws Exception {

        QueuedThreadPool threadPool = new QueuedThreadPool(10, 1, 30000);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(PORT);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Resource base = Resource.newClassPathResource(STATIC_RESOURCE_PATH);
        context.setBaseResource(base);
        context.setWelcomeFiles(new String[]{"home.html"});

        // 全局用户访问限制 Filter
        addGlobalUserLimit(context);

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
        System.out.println("Core Web Console running at http://localhost:" + PORT);
        server.join();
    }

    /**
     * 全局用户访问限制（永久 Session 版本）
     * <p>
     * 使用 Semaphore 控制最大并发用户数。
     * 每个用户首次访问时分配 Session，并且 Session 将永久有效（除非服务器关闭或手动销毁）。
     * </p>
     * @param context ServletContextHandler
     */
    private static void addGlobalUserLimit(ServletContextHandler context) {
        
        // 并发访问许可（最大用户数）
        Semaphore userSemaphore = new Semaphore(MAX_USER);

        // 活跃 Session 记录表
        ConcurrentHashMap<String, Boolean> activeUsers = new ConcurrentHashMap<>();

        // 用户访问过滤器
        Filter userLimitFilter = new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {}

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse resp = (HttpServletResponse) response;

                // 获取或创建 Session
                HttpSession session = req.getSession(true);
                String sessionId = session.getId();

                // 永久有效 Session（不自动过期）
                session.setMaxInactiveInterval(SESSION_TIMEOUT);

                boolean isNewUser = !activeUsers.containsKey(sessionId);

                if (isNewUser) {
                    // 若为新用户则尝试获取访问许可
                    if (!userSemaphore.tryAcquire()) {
                        resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        resp.setContentType("text/plain;charset=UTF-8");
                        resp.getWriter().write("Server busy, maximum users reached.");
                        return;
                    }

                    // 在 session 中保存用户信息
                    session.setAttribute("connectedAt", System.currentTimeMillis());
                    session.setAttribute("remoteAddr", req.getRemoteAddr());
                    activeUsers.put(sessionId, Boolean.TRUE);

                    System.out.println("[SESSION] New user connected: " + sessionId + " from " + req.getRemoteAddr());
                }

                try {
                    chain.doFilter(request, response);
                } finally {
                    // 不在此处释放信号量，由 sessionDestroyed 控制
                }
            }

            @Override
            public void destroy() {}
        };

        // 注册过滤器
        context.addFilter(new FilterHolder(userLimitFilter), "/*", null);

        // 注册 Session 监听器（用户断开或服务器关闭时释放许可）
        context.getSessionHandler().addEventListener(new HttpSessionListener() {
            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                String sessionId = se.getSession().getId();
                if (activeUsers.remove(sessionId) != null) {
                    userSemaphore.release();
                    System.out.println("[SESSION] User disconnected: " + sessionId);
                }
            }
        });
    }

}