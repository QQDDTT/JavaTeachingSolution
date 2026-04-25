package core;

import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import common.utils.ResponseData;

/**
 * 系统控制 Servlet
 * 用于执行 restart / close 动作
 * 不依赖 TerminalExecutor
 */
public class SystemServlet extends HttpServlet {
    
    private static final String MODULE_NAME = "core_web";
    private static final String RESTART_WIN = "restart.bat";
    private static final String CLOSE_WIN = "close.bat";
    private static final String RESTART_LINUX = "restart.sh";
    private static final String CLOSE_LINUX = "close.sh";

    // 短期有效的校验码
    private final Map<String, Long> validCodes = new ConcurrentHashMap<>();

    // ---------- GET ----------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        String action = req.getParameter("action");

        if (action == null) {
            ResponseData.error("missing acrion").sendJson(resp);
            return;
        }

        // 检查脚本存在
        if (!checkScriptExists(action)) {
            ResponseData.error("script not found for action: " + action).sendJson(resp);
            return;
        }

        // 生成校验码（5分钟有效）
        String code = UUID.randomUUID().toString();
        validCodes.put(code, System.currentTimeMillis());

        ResponseData.success("Verify " + action, Map.of("code", code)).sendJson(resp);
    }

        // ---------- POST ----------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String action = req.getParameter("action");
        String code = req.getParameter("code");

        if (action == null || code == null) {
            ResponseData.error("missing parameters").sendJson(resp);
            return;
        }

        Long createTime = validCodes.get(code);
        if (createTime == null || System.currentTimeMillis() - createTime > 5 * 60_000) {
            ResponseData.error("invalid or expired code").sendJson(resp);
            return;
        }

        // 获取脚本文件名
        String scriptName = getScriptName(action);

        // 异步执行
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            executeScript(scriptName);
        }, 5, TimeUnit.SECONDS);

        // 执行成功 -> 无响应
    }

    // ---------- 工具函数 ----------

    /**
     * 根据动作名返回脚本文件名
     */
    private String getScriptName(String action) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String scriptFile = switch (action.toLowerCase()) {
            case "restart" -> isWindows ? RESTART_WIN : RESTART_LINUX;
            case "close"   -> isWindows ? CLOSE_WIN : CLOSE_LINUX;
            default -> null;
        };

        if (scriptFile == null) return null;

        return "scripts/" + scriptFile; // 拼接文件夹路径
    }


    /**
     * 检查脚本是否存在于当前工作目录
     */
    private boolean checkScriptExists(String action) {
        String scriptPath = getScriptName(action);
        if (scriptPath == null) return false;

        return Files.exists(Paths.get(scriptPath));
    }

    /**
     * 执行脚本（异步）
     */
    private void executeScript(String scriptName) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path scriptPath = Paths.get(".", scriptName).toAbsolutePath();

        try {
            ProcessBuilder pb;
            if (isWindows) {
                pb = new ProcessBuilder("cmd", "/c", scriptPath.toString(), MODULE_NAME);
            } else {
                pb = new ProcessBuilder("bash", scriptPath.toString(), MODULE_NAME);
            }
            pb.inheritIO().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
