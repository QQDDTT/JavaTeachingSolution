package core;

import jakarta.servlet.http.*;
import java.io.IOException;

public class TerminalServlet extends HttpServlet {

    private final TerminalExecutor terminalExecutor = new TerminalExecutor();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=UTF-8");

        String command = req.getParameter("cmd"); // 客户端传递命令参数
        if (command == null || command.isBlank()) {
            resp.getWriter().println("请提供 cmd 参数");
            return;
        }

        try {
            int exitCode = terminalExecutor.execute(command, line -> {
                try {
                    resp.getWriter().println(line); // 逐行写回客户端
                    resp.getWriter().flush();      // 确保实时推送
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            resp.getWriter().println("\n命令执行完毕，退出码：" + exitCode);
        } catch (Exception e) {
            resp.getWriter().println("执行失败: " + e.getMessage());
        }
    }
}
