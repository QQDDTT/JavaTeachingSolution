package core;

import java.io.*;
import java.util.function.Consumer;

/**
 * TerminalExecutor 用于执行终端命令并返回实时输出
 */
public class TerminalExecutor {

    private final String shell;

    public TerminalExecutor() {
        // 根据系统选择默认 shell
        this.shell = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd.exe" : "bash";
    }

    /**
     * 执行命令并通过回调逐行返回输出
     * @param command 要执行的命令
     * @param outputHandler 每行输出的处理逻辑（例如 Servlet 输出）
     * @return 进程退出码
     */
    public int execute(String command, Consumer<String> outputHandler) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        if (shell.equals("bash")) {
            builder.command("bash", "-c", command);
        } else {
            builder.command("cmd.exe", "/c", command);
        }
        builder.redirectErrorStream(true); // stderr 合并到 stdout

        Process process = builder.start();

        // 逐行读取并交给调用者处理
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputHandler.accept(line);
            }
        }

        return process.waitFor();
    }

    /**
     * 执行命令并返回完整输出（适合非实时场景）
     */
    public String executeAndGetOutput(String command) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();
        execute(command, line -> output.append(line).append(System.lineSeparator()));
        return output.toString();
    }
}
