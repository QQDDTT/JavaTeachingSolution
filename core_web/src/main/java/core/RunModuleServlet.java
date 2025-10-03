package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RunModuleServlet extends HttpServlet {

    private final ProjectManager projectManager = new ProjectManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moduleName = req.getParameter("module");
        if (moduleName == null || moduleName.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing module parameter");
            return;
        }

        File moduleDir = new File(".", moduleName);
        File targetDir = new File(moduleDir, "target");

        File[] jars = targetDir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("No jar found for module: " + moduleName);
            return;
        }

        File jarFile = jars[0];

        if (moduleName.startsWith("console_")) {
            // Console 子项目，实时输出
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter out = resp.getWriter();

            try {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.println(line);
                        out.flush();
                    }
                }

                process.waitFor();

            } catch (Exception e) {
                out.println("Error: " + e.getMessage());
                e.printStackTrace(out);
            }

        } else if (moduleName.startsWith("web_")) {
            // Web 子项目，直接启动
            try {
                new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath()).start();
                resp.getWriter().write("Web module started: " + moduleName);
            } catch (Exception e) {
                resp.getWriter().write("Failed to start web module: " + e.getMessage());
            }
        } else {
            resp.getWriter().write("Unknown module type: " + moduleName);
        }
    }
}
