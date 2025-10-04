package core;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TerminalServlet extends HttpServlet {

    private final TerminalExecutor executor = new TerminalExecutor();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String action = req.getParameter("action");
        switch (action) {
            case "start" -> {
                String cmd = req.getParameter("cmd");
                if (cmd == null || cmd.isBlank()) ResponseData.error("Command is blank").sendJson(resp);
                else executor.startCommand(cmd).sendJson(resp);
            }
            case "poll" -> executor.pollOutput().sendJson(resp);
            default -> ResponseData.error("Unknown action: " + action).sendJson(resp);
        }
    }
}
