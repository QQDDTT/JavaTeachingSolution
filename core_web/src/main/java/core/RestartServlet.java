package core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RestartServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=UTF-8");

        // 立即返回响应给前端，告诉客户端进入休眠
        resp.getWriter().write("Core WebServer restarting... Please wait.");
        resp.getWriter().flush();

        // 异步后台执行重启脚本
        new Thread(() -> {
            try {
                // 修改这里为你的实际重启脚本路径
                String scriptPath = "./restart_core.sh";

                ProcessBuilder pb = new ProcessBuilder("bash", scriptPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[RestartScript] " + line);
                    }
                }

                int exitCode = process.waitFor();
                System.out.println("Restart script finished with exit code: " + exitCode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
