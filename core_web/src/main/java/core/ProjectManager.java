package core;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectManager {

    private final File parentDir = new File(".");

    // 获取子项目列表
    public List<String> listModules() {
        return Arrays.stream(parentDir.listFiles(File::isDirectory))
                .map(File::getName)
                .filter(name -> !name.equals("target") && !name.startsWith("."))
                .collect(Collectors.toList());
    }

    // 获取子项目文件树（不含 target 和 pom.xml）
    public File[] listProjectFiles(String moduleName) {
        File moduleDir = new File(parentDir, moduleName);
        return moduleDir.listFiles(f -> !f.getName().equals("target") && !f.getName().equals("pom.xml"));
    }
}
