package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileSaveServlet extends HttpServlet {

    private final ProjectManager projectManager = new ProjectManager();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moduleName = req.getParameter("module");
        String filePath = req.getParameter("file"); // 相对路径，例如 src/Main.java
        String content = req.getParameter("content");

        if (moduleName == null || filePath == null || content == null ||
            moduleName.isEmpty() || filePath.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing parameters");
            return;
        }

        File moduleDir = new File(".", moduleName);
        File targetFile = new File(moduleDir, filePath);

        // 安全检查，防止越级访问
        if (!targetFile.getCanonicalPath().startsWith(moduleDir.getCanonicalPath())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("Forbidden path");
            return;
        }

        // 创建父目录（如果不存在）
        File parent = targetFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Failed to save file: " + e.getMessage());
            return;
        }

        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("File saved successfully: " + filePath);
    }
}
