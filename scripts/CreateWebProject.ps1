Param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectName
)

$ModuleName = "web_$ProjectName"
# 当前脚本所在目录
$CurrentDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
# 上一级目录
$ParentDir = Split-Path -Parent $CurrentDir
$ParentPom = Join-Path $ParentDir "pom.xml"

# ---------- 计算端口号 ----------
$ExistingCount = (Get-ChildItem -Path $ParentDir -Directory -Name "web_*").Count
$BasePort = 10000
$Port = $BasePort + $ExistingCount
Write-Host "Assigning port $Port to $ModuleName ..."

# ---------- 创建目录结构 ----------
Write-Host "Creating project $ModuleName ..."
$MainDir = Join-Path $ParentDir "$ModuleName\src\main\java\web"
$ResDir  = Join-Path $ParentDir "$ModuleName\src\main\resources"
$TestDir = Join-Path $ParentDir "$ModuleName\src\test\java\web"
New-Item -ItemType Directory -Force -Path $MainDir, $ResDir, $TestDir | Out-Null

# ---------- 创建 WebServer.java ----------
$MainFile = Join-Path $MainDir "WebServer.java"
@"
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
"@ | Set-Content -Encoding UTF8 $MainFile

# ---------- 创建 WebServerImpl.java ----------
$ImplFile = Join-Path $MainDir "WebServerImpl.java"
@"
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
    @ReflectWebServerArgs(port = $Port, sslEnabled = false, sessionTimeout = 3600)
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
        return "$ModuleName";
    }
}
"@ | Set-Content -Encoding UTF8 $ImplFile

# ---------- 创建 home.html ----------
$HtmlFile = Join-Path $ResDir "home.html"
@"
<!DOCTYPE html>
<html>
<head>
    <meta charset='UTF-8'>
    <title>Home - $ModuleName</title>
</head>
<body>
    <h1>Welcome to $ModuleName!</h1>
</body>
</html>
"@ | Set-Content -Encoding UTF8 $HtmlFile

# ---------- 创建 WebServerTest.java ----------
$TestFile = Join-Path $TestDir "WebServerTest.java"
@"
package web;

import org.junit.Test;
import static org.junit.Assert.*;

public class WebServerTest {
    @Test
    public void testDummy() {
        assertTrue(true);
    }
}
"@ | Set-Content -Encoding UTF8 $TestFile

# ---------- 创建 pom.xml ----------
$PomFile = Join-Path $ParentDir "$ModuleName\pom.xml"
@"
<project xmlns='http://maven.apache.org/POM/4.0.0'
         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
         xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd'>

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.javateaching</groupId>
        <artifactId>JavaTeachingSolution</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>$ModuleName</artifactId>
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
                                <transformer implementation='org.apache.maven.plugins.shade.resource.ManifestResourceTransformer'>
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
"@ | Set-Content -Encoding UTF8 $PomFile

# ---------- 注册到父项目 ----------
$PomContent = Get-Content $ParentPom
if ($PomContent -notmatch "<module>$ModuleName</module>") {
    $NewPom = $PomContent -replace "(?=</modules>)", "    <module>$ModuleName</module>`r`n"
    $NewPom | Set-Content -Encoding UTF8 $ParentPom
    Write-Host "Module $ModuleName added to parent pom.xml."
} else {
    Write-Host "Module $ModuleName already exists in parent pom.xml."
}

# ---------- Maven 打包 ----------
Write-Host "Compiling $ModuleName ..."
Set-Location (Join-Path $ParentDir $ModuleName)
mvn clean package

# ---------- 打开 VS Code ----------
Write-Host "Opening VS Code for $ModuleName ..."
code (Join-Path $ParentDir $ModuleName)

Write-Host "Done!"
