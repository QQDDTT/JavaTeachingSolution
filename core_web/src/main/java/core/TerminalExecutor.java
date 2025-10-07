package core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TerminalExecutor
 * 所有命令都在独立终端中执行，startCommand + pollOutput 与终端通信
 * pollOutput 增加空输出计数，连续空输出达到上限后自动将 running 置为 false
 */
public class TerminalExecutor {

    private static final int EMPTY_POLL_LIMIT = 10; // 连续空输出次数上限

    private final String rootPath;
    private String currentPath;
    private final ConcurrentLinkedQueue<Map<String, String>> outputQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = false;
    private int emptyPollCount = 0;

    private Process terminalProcess;
    private BufferedWriter terminalWriter;
    private Thread stdoutThread;
    private Thread stderrThread;

    public TerminalExecutor() throws IOException {
        this.rootPath = new File("").getAbsolutePath();
        this.currentPath = ".";

        // 根据操作系统选择终端命令
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            // Windows 使用 cmd /K 保持终端打开，或 powershell
            pb = new ProcessBuilder("cmd.exe", "/K");
        } else {
            // Linux / Mac 使用 bash
            pb = new ProcessBuilder("bash");
        }

        pb.directory(new File(rootPath));
        terminalProcess = pb.start();

        // 与终端通信的 stdin/stdout
        terminalWriter = new BufferedWriter(new OutputStreamWriter(terminalProcess.getOutputStream()));

        // 读取终端 stdout 和 stderr
        stdoutThread = new Thread(() -> readStreamToQueue(terminalProcess.getInputStream(), "out"));
        stderrThread = new Thread(() -> readStreamToQueue(terminalProcess.getErrorStream(), "err"));
        stdoutThread.start();
        stderrThread.start();
    }

    /** 在终端中执行命令 */
    public ResponseData executeCommand(String command) {
        if (running) return ResponseData.error("Command is running");
        running = true;
        emptyPollCount = 0; // 重置空轮询计数

        try {
            terminalWriter.write(command);
            terminalWriter.newLine();
            terminalWriter.flush();
        } catch (IOException e) {
            outputQueue.offer(Map.of("type", "err", "text", "Failed to send command: " + e.getMessage()));
            running = false;
        }

        return ResponseData.success("Command sent", Map.of("running", "1", "path", currentPath));
    }

    /** 获取输出 */
    public ResponseData pollOutput() {
        Map<String, String> map = new HashMap<>();
        StringBuilder outSb = new StringBuilder();
        StringBuilder errSb = new StringBuilder();

        Map<String, String> entry;
        boolean hasOutput = false;

        while ((entry = outputQueue.poll()) != null) {
            hasOutput = true;
            String type = entry.get("type");
            String text = entry.get("text");
            if ("err".equals(type)) errSb.append(text).append("\n");
            else outSb.append(text).append("\n");
        }

        // 连续空输出检测
        if (hasOutput) {
            emptyPollCount = 0; // 有输出就重置计数
        } else {
            emptyPollCount++;
            if (emptyPollCount >= EMPTY_POLL_LIMIT) {
                running = false; // 超过空输出次数 → 自动解除运行状态
            }
        }

        map.put("out", outSb.toString());
        map.put("err", errSb.toString());
        map.put("running", running ? "1" : "0");
        map.put("path", currentPath);

        return ResponseData.success("Get output", map);
    }

    private void readStreamToQueue(InputStream is, String type) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputQueue.offer(Map.of("type", type, "text", line));
            }
        } catch (IOException e) {
            outputQueue.offer(Map.of("type", "err", "text", "Read " + type + " failed: " + e.getMessage()));
        }
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public boolean isRunning() {
        return running;
    }
}
