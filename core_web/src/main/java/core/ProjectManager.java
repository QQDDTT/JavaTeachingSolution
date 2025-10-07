package core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.ServletInputStream;

/**
 * ProjectService
 * - 管理父项目目录和子项目目录
 * - 提供文件读写、模块管理等功能
 */
public class ProjectManager {

    private static final File PARENT_DIR = new File(".");
    public static final Map<String, String> PROJECTS;

    static {
        PROJECTS = Arrays.stream(PARENT_DIR.listFiles())
                .filter(File::isDirectory)
                .filter(dir -> dir.getName().startsWith("web_") || dir.getName().startsWith("console_"))
                .collect(Collectors.toMap(
                        File::getName,
                        File::getAbsolutePath,
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new
                ));
    }

    /** 当前项目名 */
    private final String project;

    public ProjectManager(String project) {
        if (!PROJECTS.containsKey(project)) {
            throw new IllegalArgumentException("Unknown project: " + project);
        }
        this.project = project;
    }

    public String getProject() {
        return this.project;
    }

    // ========== 文件编辑功能 ==========
    public ResponseData readFile(String relativePath) {
        try {
            Path filePath = Paths.get(PROJECTS.get(this.project), relativePath);
            return ResponseData.success("Read file success", Map.of(
                                        "file", relativePath,
                                        "content", Files.readString(filePath)
                                        ));
        } catch (Exception e) {
            return ResponseData.error("[ProjectManager]" + e.getMessage());
        }
    }

    public ResponseData writeFile(String relativePath, ServletInputStream inputStream) {
        try {
            Path filePath = Paths.get(PROJECTS.get(this.project), relativePath);
            Files.createDirectories(filePath.getParent());

            // 使用 try-with-resources 自动关闭流
            try (OutputStream out = Files.newOutputStream(filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                inputStream.transferTo(out); // 返回写入字节数
            }

            return ResponseData.success("Write file success: " + relativePath, Map.of());
        } catch (Exception e) {
            return ResponseData.error("Write file failed: " + e.getMessage());
        }
    }


    // ========== 文件管理功能 ==========
    public ResponseData createFile(String relativePath) {
        try {
            Path filePath = Paths.get(PROJECTS.get(this.project), relativePath);

            // 确保父目录存在
            Files.createDirectories(filePath.getParent());

            // 如果文件已存在，则返回提示信息
            if (Files.exists(filePath)) {
                return ResponseData.error("File already exists: " + relativePath);
            }

            // 创建新文件
            Files.createFile(filePath);
            String content = this.generateDefaultContent(relativePath);

            return ResponseData.success("File created successfully", Map.of(
                    "file", relativePath,
                    "content", content
            ));
        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    
    public ResponseData createDirectory(String relativePath) {
        try {
            Path dirPath = Paths.get(PROJECTS.get(this.project), relativePath);

            // 如果目录已存在
            if (Files.exists(dirPath)) {
                if (Files.isDirectory(dirPath)) {
                    return ResponseData.error("Directory already exists: " + relativePath);
                } else {
                    return ResponseData.error("A file with the same name already exists: " + relativePath);
                }
            }

            // 创建多级目录
            Files.createDirectories(dirPath);

            return ResponseData.success("Directory created successfully", Map.of(
                    "directory", relativePath
            ));
        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    public ResponseData deletePath(String relativePath) {
        try {
            Path path = Paths.get(PROJECTS.get(this.project), relativePath);

            if (!Files.exists(path)) {
                return ResponseData.error("Path not found: " + relativePath);
            }

            // 递归删除目录或单个文件
            if (Files.isDirectory(path)) {
                Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a)) // 先删除子文件，再删父目录
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + p + " - " + e.getMessage());
                        }
                    });
                return ResponseData.success("Directory deleted successfully", Map.of(
                        "directory", relativePath,
                        "absolutePath", path.toAbsolutePath().toString()
                ));
            } else {
                Files.delete(path);
                return ResponseData.success("File deleted successfully", Map.of(
                        "file", relativePath,
                        "absolutePath", path.toAbsolutePath().toString()
                ));
            }

        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    // ========== 项目管理功能 ==========

    /**
     * 获取子项目列表
     */
    public static ResponseData listModules() {
        return ResponseData.success("Sub projects", PROJECTS);
    }

    /**
     * 获取子项目文件树（不含 target 和 pom.xml）
     */
    public ResponseData listProjectFiles() {
        try {
            Path root = Paths.get(PROJECTS.get(this.project));
            try (Stream<Path> stream = Files.walk(root)) {
                return ResponseData.success("List file success", stream
                        .filter(path -> !path.toString().contains("target"))  
                        .filter(path -> !path.equals(root))
                        .collect(Collectors.toMap(
                            path -> root.relativize(path).toString(),
                            path -> Files.isDirectory(path) ? "Dir" : "File",
                            (oldVal, newVal) -> oldVal, 
                            HashMap::new
                        ))
                    );
            }
        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    // ========== 工具 ==========
    /**
     * 根据文件扩展名生成默认内容
     * - .java：生成 package + class 模板
     * - .html：生成基础 HTML 页面
     * - 其他：返回空字符串
     */
    private String generateDefaultContent(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return "";

        String fileName = Paths.get(relativePath).getFileName().toString();
        String extension = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        switch (extension) {
            case "java":
                // 提取包路径（例如 src/main/java/com/example/Demo.java -> com.example）
                String packageName = extractPackageName(relativePath);
                String className = fileName.substring(0, dotIndex);

                return "package " + packageName + ";\n\n"
                    + "/**\n"
                    + " * Auto-generated class " + className + "\n"
                    + " */\n"
                    + "public class " + className + " {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello from " + className + "!\");\n"
                    + "    }\n"
                    + "}\n";

            case "html":
                return """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>New Page</title>
                        <style>
                            body { font-family: sans-serif; margin: 2em; }
                            h1 { color: #333; }
                        </style>
                    </head>
                    <body>
                        <h1>Hello, world!</h1>
                        <p>This is an auto-generated HTML page.</p>
                    </body>
                    </html>
                    """;

            default:
                return "";
        }
    }

    /**
     * 从路径中提取包名（例如 src/main/java/com/example/Demo.java -> com.example）
     */
    private String extractPackageName(String relativePath) {
        String normalized = relativePath.replace(File.separatorChar, '/');

        int srcIndex = normalized.indexOf("src/main/java/");
        if (srcIndex != -1) {
            String sub = normalized.substring(srcIndex + "src/main/java/".length());
            int lastSlash = sub.lastIndexOf('/');
            if (lastSlash > 0) {
                return sub.substring(0, lastSlash).replace('/', '.');
            }
        }
        return "defaultpkg";
    }

}
