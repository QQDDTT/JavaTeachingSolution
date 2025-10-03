Param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectName
)

$ModuleName = "console_$ProjectName"

# 父项目路径（假设脚本放在父项目 scripts/ 目录下）
$ParentDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ParentPom = Join-Path $ParentDir "pom.xml"

# ---------- 1. 创建目录结构 ----------
Write-Host "Creating project $ModuleName ..."
$MainDir = Join-Path $ModuleName "src\main\java\console"
$TestDir = Join-Path $ModuleName "src\test\java\console"
New-Item -ItemType Directory -Force -Path $MainDir, $TestDir | Out-Null

# 创建 Main.java
$MainFile = Join-Path $MainDir "Main.java"
@"
package console;

import common.annotations.ReflectConsoleArgs;

public class Main {

    @ReflectConsoleArgs({"core-$ProjectName"})
    public static void main(String[] args) {
        String message = "Hello from $ModuleName!";
        if (args != null && args.length > 0) {
            message += " <" + args[0] + ">";
        }
        System.out.println(message);
    }
}
"@ | Set-Content -Encoding UTF8 $MainFile

# 创建 MainTest.java
$TestFile = Join-Path $TestDir "MainTest.java"
@"
package console;

import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
    @Test
    public void testMainOutput() {
        assertTrue(true); // 占位测试
    }
}
"@ | Set-Content -Encoding UTF8 $TestFile

# 创建 pom.xml
$PomFile = Join-Path $ModuleName "pom.xml"
@"
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

    <artifactId>$ModuleName</artifactId>
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
</project>
"@ | Set-Content -Encoding UTF8 $PomFile

Write-Host "Project $ModuleName created successfully!"

# ---------- 2. 将子项目加入父项目 pom.xml ----------
Write-Host "Adding $ModuleName to parent pom.xml ..."
$PomContent = Get-Content $ParentPom
if ($PomContent -notmatch "<module>$ModuleName</module>") {
    $NewPom = $PomContent -replace "(?=</modules>)", "    <module>$ModuleName</module>`r`n"
    $NewPom | Set-Content -Encoding UTF8 $ParentPom
    Write-Host "Module $ModuleName added to parent pom.xml."
} else {
    Write-Host "Module $ModuleName already exists in parent pom.xml."
}

# ---------- 3. 执行 Maven 打包 ----------
Write-Host "Compiling parent project ..."
Set-Location $ParentDir
mvn clean package

# ---------- 4. 用 VS Code 打开子项目 ----------
Write-Host "Opening VS Code for $ModuleName ..."
code (Join-Path $ParentDir $ModuleName)

Write-Host "Done!"
