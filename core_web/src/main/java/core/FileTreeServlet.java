package core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileTreeServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectManager projectManager = new ProjectManager();

    // 文件信息类
    public static class FileInfo {
        public String name;
        public boolean isDirectory;

        public FileInfo(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moduleName = req.getParameter("module");
        if (moduleName == null || moduleName.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing module parameter");
            return;
        }

        File[] files = projectManager.listProjectFiles(moduleName);
        if (files == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Module not found: " + moduleName);
            return;
        }

        List<FileInfo> fileList = Arrays.stream(files)
                .map(f -> new FileInfo(f.getName(), f.isDirectory()))
                .collect(Collectors.toList());

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(mapper.writeValueAsString(fileList));
    }
}
