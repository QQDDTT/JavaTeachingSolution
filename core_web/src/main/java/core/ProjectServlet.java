package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectServlet
 * ----------------------------------------
 * 提供项目管理的 JSON 接口。
 * 响应均为 application/json
 */
public class ProjectServlet extends HttpServlet {

    /** 每个 session 的独立 ProjectManager */
    private static final Map<String, ProjectManager> MANAGER_MAP = new ConcurrentHashMap<>();
    
    
    @Override
    public void init() throws ServletException {
        super.init();
        log("[INIT] ProjectServlet initialized");
    }


    /**
     * 处理 GET 请求
     * -------------------------------
     * 支持的 endpoint：
     * - /project?action=list     → 获取项目列表
     * - /project?action=delete&id=<项目ID> → 删除项目
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String action = req.getParameter("action");
        HttpSession session = req.getSession(true);
        String sessionId = session.getId();
        switch (action) {
            case "projects" -> ProjectManager.listModules().sendJson(resp);
            case "list" -> {
                String project = req.getParameter("project");
                ProjectManager pm = MANAGER_MAP.get(sessionId);
                if (pm == null) {
                    pm = new ProjectManager(project);
                    MANAGER_MAP.put(sessionId, pm);
                }
                pm.listProjectFiles(project).sendJson(resp);
            }
            case "read_file" -> {
                String project = req.getParameter(sessionId);
                ProjectManager pm = MANAGER_MAP.get(sessionId);
                if (pm == null) {
                    ResponseData.error("Project does not match sessionID").sendJson(resp);
                    return; 
                }
                pm.readFile(project, req.getParameter("path")).sendJson(resp);
            }
            default -> ResponseData.error("Unknown GET action: " + action).sendJson(resp);
        }
    }

    /**
     * 处理 POST 请求
     * -------------------------------
     * 支持的 endpoint：
     * - /project?action=add&id=<项目ID>&name=<项目名称>
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String action = req.getParameter("action");
        HttpSession session = req.getSession(true);
        String sessionId = session.getId();
        switch (action) {
            case "write_file" -> {
                String project = req.getParameter("project");
                ProjectManager pm = MANAGER_MAP.get(sessionId);
                if (pm == null) {
                    ResponseData.error("Project does not match sessionID").sendJson(resp);
                    return;
                }
                pm.writeFile(project, req.getParameter("path"), req.getInputStream()).sendJson(resp);
            }
            default -> ResponseData.error("Unknown POST action: " + action).sendJson(resp);
        }
    }
}
