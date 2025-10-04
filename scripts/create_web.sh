#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <ProjectName>"
    exit 1
fi

PROJECT_NAME=$1
MODULE_NAME="web_${PROJECT_NAME}"

PARENT_DIR=$(dirname "$(dirname "$(realpath "$0")")")
PARENT_POM="$PARENT_DIR/pom.xml"

# ---------- 计算端口号 ----------
EXISTING_COUNT=$(find "$PARENT_DIR" -maxdepth 1 -type d -name "web_*" | wc -l)
BASE_PORT=10000
PORT=$((BASE_PORT + EXISTING_COUNT))
echo "Assigning port $PORT to ${MODULE_NAME} ..."

# ---------- 创建目录结构 ----------
echo "Creating project ${MODULE_NAME} ..."
mkdir -p "${PARENT_DIR}/${MODULE_NAME}/src/main/java/web"
mkdir -p "${PARENT_DIR}/${MODULE_NAME}/src/main/resources"
mkdir -p "${PARENT_DIR}/${MODULE_NAME}/src/test/java/web"

# ---------- 创建 WebServer.java ----------
MAIN_FILE="${PARENT_DIR}/${MODULE_NAME}/src/main/java/web/WebServer.java"
cat > "$MAIN_FILE" <<EOL
package web;

/**
 * WebServer 启动入口
 * - 简化，只用于 CoreRunner 反射调用
 */
public class WebServer {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        boolean sslEnabled = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
        int sessionTimeout = args.length > 2 ? Integer.parseInt(args[2]) : 600;

        WebServerImpl serverImpl = new WebServerImpl();
        serverImpl.start(port, sslEnabled, sessionTimeout);

        // 阻塞线程，保证服务存活
        Thread.currentThread().join();
    }
}
EOL

# ---------- 创建 WebServerImpl.java ----------
IMPL_FILE="${PARENT_DIR}/${MODULE_NAME}/src/main/java/web/WebServerImpl.java"
cat > "$IMPL_FILE" <<EOL
package web;

import common.server.LifecycleServer;
import common.annotations.ReflectWebServerArgs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Web 子项目 Server 实现
 */
public class WebServerImpl implements LifecycleServer {

    private Server server;

    @Override
    @ReflectWebServerArgs(port = $PORT, sslEnabled = false, sessionTimeout = 3600)
    public void start(int port, boolean sslEnabled, int sessionTimeout) throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.getSessionHandler().setMaxInactiveInterval(sessionTimeout);

        Resource base = Resource.newClassPathResource("/META-INF/resources");
        if (base == null || !base.exists()) {
            throw new RuntimeException("无法加载资源文件，请确保 resources 放在 META-INF/resources 内并打包到 JAR！");
        }
        context.setBaseResource(base);

        context.setWelcomeFiles(new String[]{"home.html"});
        context.addServlet(DefaultServlet.class, "/").setInitParameter("dirAllowed", "true");

        server.setHandler(context);
        server.start();

        System.out.println(getName() + " started at port " + port +
                ", SSL=" + sslEnabled +
                ", sessionTimeout=" + sessionTimeout + "s");
    }

    @Override
    public void stop() throws Exception {
        if (server != null && server.isStarted()) {
            server.stop();
            System.out.println(getName() + " stopped.");
        }
    }

    @Override
    public String getName() {
        return "${MODULE_NAME}";
    }
}
EOL

# ---------- 创建 home.html ----------
HTML_FILE="${PARENT_DIR}/${MODULE_NAME}/src/main/resources/home.html"
cat > "$HTML_FILE" <<EOL
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Home - ${MODULE_NAME}</title>
</head>
<body>
    <h1>Welcome to ${MODULE_NAME}!</h1>
</body>
</html>
EOL

# ---------- 创建测试 ----------
TEST_FILE="${PARENT_DIR}/${MODULE_NAME}/src/test/java/web/WebServerTest.java"
cat > "$TEST_FILE" <<EOL
package web;

import org.junit.Test;
import static org.junit.Assert.*;

public class WebServerTest {
    @Test
    public void testDummy() {
        assertTrue(true);
    }
}
EOL

# ---------- 创建 pom.xml ----------
POM_FILE="${PARENT_DIR}/${MODULE_NAME}/pom.xml"
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
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlets</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.javateaching</groupId>
            <artifactId>common</artifactId>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <targetPath>META-INF/resources</targetPath>
                <includes>
                    <include>**/*</include>
                </includes>
            </resource>
        </resources>

        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>web.WebServer</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOL

# ---------- 注册到父项目 ----------
echo "Adding ${MODULE_NAME} to parent pom.xml ..."
if ! grep -q "<module>${MODULE_NAME}</module>" "$PARENT_POM"; then
    sed -i "/<\/modules>/i \ \ \ \ <module>${MODULE_NAME}<\/module>" "$PARENT_POM"
    echo "Module ${MODULE_NAME} added to parent pom.xml."
else
    echo "Module ${MODULE_NAME} already exists in parent pom.xml."
fi

# ---------- Maven 打包 ----------
echo "Compiling ${MODULE_NAME} ..."
cd "${PARENT_DIR}/${MODULE_NAME}"
mvn clean package

# ---------- 打开 VS Code ----------
echo "Opening VS Code for ${MODULE_NAME} ..."
code "${PARENT_DIR}/${MODULE_NAME}"

echo "Done!"
