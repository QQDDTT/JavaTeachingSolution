package core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

/**
 * TerminalExecutor
 * 管理命令队列和输出队列，同时区分 stdout 和 stderr
 */
public class TerminalExecutor {

    private final ConcurrentLinkedQueue<Map<String, String>> outputQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = false;
    private final long commandTimeoutMillis = 30_000; // 超时30秒
    /**
     * 执行命令，并将 stdout 和 stderr 输出存入队列
     */
    public ResponseData startCommand(String command) {
        if (running) {
            return ResponseData.error("Command is running");
        }

        running = true;
        outputQueue.clear();

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                Process process = pb.start();

                // stdout
                Thread stdoutThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("type", "out");
                            map.put("text", line);
                            outputQueue.offer(map);
                        }
                    } catch (Exception e) {
                        Map<String, String> map = new HashMap<>();
                        map.put("type", "err");
                        map.put("text", "Read stdout faild: " + e.getMessage());
                        outputQueue.offer(map);
                    }
                });

                // stderr
                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("type", "err");
                            map.put("text", line);
                            outputQueue.offer(map);
                        }
                    } catch (Exception e) {
                        Map<String, String> map = new HashMap<>();
                        map.put("type", "err");
                        map.put("text", "Read stderr faild: " + e.getMessage());
                        outputQueue.offer(map);
                    }
                });

                stdoutThread.start();
                stderrThread.start();

                // 等待进程结束或超时
                boolean finished = process.waitFor(commandTimeoutMillis, TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly(); // 超时强制终止
                    outputQueue.offer(Map.of("type", "err", "text", "Command timed out after " + (commandTimeoutMillis / 1000) + " seconds"));
                }

                stdoutThread.join();
                stderrThread.join();

            } catch (Exception e) {
                Map<String, String> map = new HashMap<>();
                map.put("type", "err");
                map.put("text", "Run command faild: " + e.getMessage());
                outputQueue.offer(map);
                
            } finally {
                running = false;
            }
        }).start();

        return ResponseData.success("Command started", Map.of("running", "1"));
    }

    /**
     * 获取队列输出，返回 ResponseData
     */
    public ResponseData pollOutput() {
        Map<String, String> map = new HashMap<>();
        StringBuilder outSb = new StringBuilder();
        StringBuilder errSb = new StringBuilder();

        Map<String, String> entry;
        while ((entry = outputQueue.poll()) != null) {
            String type = entry.get("type");
            String text = entry.get("text");
            if ("err".equals(type)) {
                errSb.append(text).append("\n");
            } else {
                outSb.append(text).append("\n");
            }
        }

        map.put("out", outSb.toString());
        map.put("err", errSb.toString());
        map.put("running", running ? "1" : "0");

        return ResponseData.success("Get output", map);
    }
}
