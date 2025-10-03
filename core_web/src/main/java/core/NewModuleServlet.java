package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class NewModuleServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moduleName = req.getParameter("module");
        String type = req.getParameter("type"); // console 或 web

        if (moduleName == null || type == null || moduleName.isEmpty() || type.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing parameters");
            return;
        }

        String script;
        if (type.equals("console")) {
            script = "./create_console.sh " + moduleName;
        } else if (type.equals("web")) {
            script = "./create_web.sh " + moduleName;
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid type: " + type);
            return;
        }

        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("Creating module: " + moduleName + " ...\n");
        resp.getWriter().flush();

        // 异步执行脚本
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", script);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[NewModuleScript] " + line);
                    }
                }

                int exitCode = process.waitFor();
                System.out.println("Module creation script finished with exit code: " + exitCode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
