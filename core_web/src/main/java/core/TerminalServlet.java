package core;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import common.utils.ResponseData;

public class TerminalServlet extends HttpServlet {

    // sessionID -> TerminalExecutor 映射表（线程安全）
    private static final Map<String, TerminalExecutor> EXECUTOR_MAP = new ConcurrentHashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String action = req.getParameter("action");
        String sessionId = req.getSession(true).getId();
        TerminalExecutor executor = EXECUTOR_MAP.get(sessionId);

        try {
            switch (action) {
                case "start"  -> {
                    if (executor == null) {
                        executor = new TerminalExecutor();
                        EXECUTOR_MAP.put(sessionId, executor);
                    }
                    ResponseData.success("Terminal executor is started.", Map.of("SID", sessionId)).sendJson(resp);
                }
                case "execute" -> {
                    if (executor == null) {
                        ResponseData.error("Terminal executor is not started").sendJson(resp);
                    } else {
                        executor.executeCommand(req.getParameter("cmd")).sendJson(resp);
                    }
                }
                case "poll" -> {
                    if (executor == null) {
                        ResponseData.error("Terminal executor is not started").sendJson(resp);
                    } else {
                        executor.pollOutput().sendJson(resp);
                    }
                }
                case "close" -> {
                    if (executor != null) {
                        EXECUTOR_MAP.remove(sessionId);
                    }
                    ResponseData.success("Terminal executor is closed.", Map.of("SID", sessionId)).sendJson(resp);
                }
                default -> {
                    ResponseData.error("Unknown action: " + action).sendJson(resp);
                }
            }
        } catch (Exception e) {
            ResponseData.error("Terminal servlet error cause : " + e.getMessage());
        }
    }
}
