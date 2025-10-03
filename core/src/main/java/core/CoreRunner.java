package core;

import java.util.Scanner;
import java.io.File;

/**
 * 核心启动器
 * 输入子项目名称，根据类型调用对应 Loader
 */
public class CoreRunner {

    public static void main(String[] args) throws Exception {
        File parentDir = new File(".");

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.println("请输入要运行的模块名(console_demo 或 web_demo),输入 quit 退出:");
                String moduleName = scanner.nextLine().trim();

                if ("quit".equalsIgnoreCase(moduleName)) {
                    System.out.println("退出 CoreRunner");
                    break;
                }

                File moduleDir = new File(parentDir, moduleName);
                File targetDir = new File(moduleDir, "target");

                File[] jars = targetDir.listFiles((d, n) -> n.endsWith(".jar"));
                if (jars == null || jars.length == 0) {
                    System.err.println("未找到子项目 jar: " + moduleName);
                    continue;
                }
                
                if (moduleName.startsWith("console_")) {
                    try {
                        ConsoleLoader.runConsole(jars, moduleName);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                } else if (moduleName.startsWith("web_")) {
                    try {
                        WebLoader.runWeb(jars, moduleName);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                } else {
                    System.out.println("未知模块类型: " + moduleName);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
}
