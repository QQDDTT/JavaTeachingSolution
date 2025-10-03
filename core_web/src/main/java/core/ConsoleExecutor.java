package core;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleExecutor {

    public interface OutputListener {
        void onOutput(String line);
    }

    // 异步执行 console 子项目
    public static void runConsole(String jarPath, OutputListener listener) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath);
        Process process = pb.start();

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    listener.onOutput(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
