package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * ProjectServlet
 * ----------------------------------------
 * 提供项目管理的 JSON 接口。
 * 响应均为 application/json
 */
public class ProjectServlet extends HttpServlet {

    private ProjectManager projectManager;
    // -------------------------------
    // 模拟项目存储（线程安全）
    // -------------------------------
    @Override
    public void init() throws ServletException {
        super.init();
        this.projectManager = new ProjectManager();
        log("[INIT] ProjectServlet initialized with default projects");
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

        switch (action) {
            case "projects" -> projectManager.listModules().sendJson(resp);
            case "list" -> projectManager.listProjectFiles(req.getParameter("project")).sendJson(resp);
            case "read_file" -> projectManager.readFile(req.getParameter("project"), req.getParameter("path")).sendJson(resp);
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
        switch (action) {
            case "write_file" -> projectManager.writeFile(req.getParameter("project"), req.getParameter("path"), req.getInputStream()).sendJson(resp);
            default -> ResponseData.error("Unknown POST action: " + action).sendJson(resp);
        }
    }
}
