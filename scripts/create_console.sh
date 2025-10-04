#!/bin/bash

# -------------------------
# 脚本功能：
# 1. 创建新的控制台子项目
# 2. 将子项目加入父项目 pom.xml <modules>
# 3. 执行 mvn clean package
# 4. 打开 VS Code 子项目
# -------------------------

# 参数检查
if [ $# -ne 1 ]; then
    echo "Usage: $0 <ProjectName>"
    echo "Example: $0 Example"
    exit 1
fi

PROJECT_NAME=$1
MODULE_NAME="console_${PROJECT_NAME}"

# 父项目路径（假设脚本放在父项目 scripts/ 目录下）
PARENT_DIR=$(dirname "$(realpath "$0")")
PARENT_POM="$PARENT_DIR/pom.xml"

# ---------- 1. 创建目录结构 ----------
echo "Creating project ${MODULE_NAME} ..."
mkdir -p "${MODULE_NAME}/src/main/java/console"
mkdir -p "${MODULE_NAME}/src/test/java/console"

# 创建 Main.java
MAIN_FILE="${MODULE_NAME}/src/main/java/console/Main.java"
cat > "$MAIN_FILE" <<EOL
package console;

import common.annotations.ReflectConsoleArgs;

public class Main {

    @ReflectConsoleArgs({"core-${PROJECT_NAME}"})
    public static void main(String[] args) {
        String message = "Hello from ${MODULE_NAME}!";
        if (args != null && args.length > 0) {
            message += " <" + args[0] + ">";
        }
        System.out.println(message);
    }
}
EOL

# 创建 MainTest.java
TEST_FILE="${MODULE_NAME}/src/test/java/console/MainTest.java"
cat > "$TEST_FILE" <<EOL
package console;

import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
    @Test
    public void testMainOutput() {
        assertTrue(true); // 占位测试
    }
}
EOL

# 创建 pom.xml
POM_FILE="${MODULE_NAME}/pom.xml"
cat > "$POM_FILE" <<EOL
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.javateaching</groupId>
        <artifactId>JavaTeachingSolution</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>${MODULE_NAME}</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 公共注解 -->
        <dependency>
            <groupId>com.javateaching</groupId>
            <artifactId>common</artifactId>
        </dependency>

        <!-- JUnit -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>console.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOL

echo "Project ${MODULE_NAME} created successfully!"

# ---------- 2. 将子项目加入父项目 pom.xml ----------
echo "Adding ${MODULE_NAME} to parent pom.xml ..."
if ! grep -q "<module>${MODULE_NAME}</module>" "$PARENT_POM"; then
    # 在 </modules> 前插入模块
    sed -i "/<\/modules>/i \ \ \ \ <module>${MODULE_NAME}<\/module>" "$PARENT_POM"
    echo "Module ${MODULE_NAME} added to parent pom.xml."
else
    echo "Module ${MODULE_NAME} already exists in parent pom.xml."
fi

# ---------- 3. 执行 Maven 打包 ----------
echo "Compiling parent project ..."
cd "$PARENT_DIR"
mvn clean package

# ---------- 4. 用 VS Code 打开子项目 ----------
echo "Opening VS Code for ${MODULE_NAME} ..."
code "${PARENT_DIR}/${MODULE_NAME}"

echo "Done!"
