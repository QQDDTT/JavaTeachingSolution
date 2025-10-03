# JavaTeachingSolution

## 📘 项目简介

JavaTeachingSolution 是一个 多模块 Java 教学项目，用于演示 控制台程序 与 JavaEE Web 程序 的开发、运行与统一管理。

- 父项目：负责依赖管理、工具类、子项目运行控制。
- 子项目：可以独立运行，也可以通过父项目提供的反射机制启动。
 - 控制台子项目：console_xxx
 - Web 子项目：web_xxx
- 核心子项目：
 - core：注解与反射工具
 - core_web：Web 管理控制台（统一入口，支持运行/管理所有子项目）

## 📂 目录结构

```bash
JavaTeachingSolution
├── create_console.sh                       # 创建控制台项目脚本
├── create_web.sh                           # 创建 Web 项目脚本
├── pom.xml                                 # 父级 Maven 配置文件
├── README.md
│
├── common                                  # 通用模块：提供公共类和注解
│   ├── pom.xml                             # Maven 配置文件
│   └── src/main/java/common
│       ├── annotations                     # 自定义注解，用于反射方法调用
│       │   ├── ReflectConsoleArgs.java     # 标记控制台方法的注解
│       │   └── ReflectWebServerArgs.java   # 标记 WebServer 方法的注解
│       └── server
│           └── LifecycleServer.java        # 服务生命周期接口，定义启动/停止等方法
│
├── core                                    # 核心模块：负责加载控制台或 Web 子项目
│   ├── pom.xml                             # Maven 配置文件
│   ├── src/main/java/core
│   │   ├── ConsoleLoader.java              # 控制台模块加载器
│   │   ├── CoreRunner.java                 # 核心启动入口，根据输入选择加载模块
│   │   └── WebLoader.java                  # Web 模块加载器
│   └── src/test/java/core
│       └── CoreRunnerTest.java             # 核心启动模块单元测试
│
├── core_web                                # 核心 Web 管理模块：提供 Web 管理界面和模块操作
│   ├── pom.xml                             # Maven 配置文件
│   ├── src/main/java/core
│   │   ├── ConsoleExecutor.java            # 执行控制台命令
│   │   ├── CoreWebServer.java              # WebServer 主入口
│   │   ├── FileSaveServlet.java            # 处理文件保存请求
│   │   ├── FileTreeServlet.java            # 处理文件目录结构请求
│   │   ├── ModuleListServlet.java          # 返回模块列表
│   │   ├── NewModuleServlet.java           # 创建新模块
│   │   ├── ProjectManager.java             # 项目管理工具类
│   │   ├── RestartServlet.java             # 重启模块
│   │   ├── RunModuleServlet.java           # 执行模块
│   │   └── WebExecutor.java                # 执行 Web 相关命令
│   └── src/main/resources
│       ├── app.js                          # 前端 JS 脚本
│       ├── index.html                      # Web 管理界面主页
│       └── style.css                       # Web 页面样式表
│
├── console_demo                            # 控制台演示模块
│   ├── pom.xml
│   ├── src/main/java/console
│   │   └── Main.java
│   └── src/test/java/console
│       └── MainTest.java
│
├── web_demo                                # Web 演示模块
│   ├── pom.xml
│   ├── src/main/java/web
│   │   ├── WebServerImpl.java
│   │   └── WebServer.java
│   └── src/main/resources
│       └── home.html
│   └── src/test/java/web
│       └── WebServerTest.java
```


## ⚙️ 构建与运行

### 1. 构建整个项目

```bash
mvn clean install
```


### 2. 运行控制台子项目

- 进入控制台子项目目录，例如 Console.Example：

```bash
cd console_example
java -jar target/console_example-1.0-SNAPSHOT.jar arg1 arg2
```


### 3. 运行 Web 子项目
- 进入 Web 子项目目录，例如 web_example：

```bash
cd web_example
java -jar target/web_example-1.0-SNAPSHOT.jar arg1 arg2
```


### 4. core 项目

core 模块是 控制台核心库，提供：

- 核心启动器：统一管理和启动子项目
- 控制台子项目执行器：通过扫描 target/*.jar 运行控制台项目
- Web 子项目启动接口：为 core_web 提供调用 Web 项目的方法

作用：简化各子项目启动流程，使父项目可以统一通过反射或交互式输入启动子项目。

```bash
# 进入 core 模块目录
cd core
# 编译
mvn clean package
# 运行核心启动器
java -jar target/core-1.0-SNAPSHOT.jar
```
- 输入模块名运行对应子项目
- 支持控制台和 Web 子项目
- 可扩展新子项目，无需改动 CoreRunner


### 5. Core Web 模块说明（core_web）

⚠️ **警告**：该模块开发中，相关功能尚未完善

core_web 模块是 Web 管理控制台，目标：
- 统一展示所有已注册的子项目（控制台/Web）
- 提供 Web 页面操作入口：
 - 启动/停止子项目
 - 创建/修改子项目
 - 在线监控运行状态

```bash
# 进入 core_web 模块目录
cd core_web
# 编译打包
mvn clean package
# 运行 Web 控制台
java -jar target/core_web-1.0-SNAPSHOT.jar
```

### 6. 创建新的子项目

Linux 使用方法（Bash 脚本）
- 脚本需有可执行权限：chmod +x CreateWebProject.sh
- 系统需已安装 Maven 和 JDK
- code 命令需在 PATH 中可用

```bash
./CreateWebProject.sh Demo
```

Windows 使用方法（PowerShell / BAT）

- BAT 文件会自动使用 ExecutionPolicy Bypass，无需修改系统策略
- 系统需已安装 Maven 和 JDK
- code 命令需在 PATH 中可用

- 通过 BAT 文件启动（推荐）

```bat
 CreateWebProject.bat Demo
```

- 直接在 PowerShell 中运行

```powershell
.\CreateWebProject.ps1 -ProjectName Demo
```

## 📦 依赖管理

父项目统一管理各模块常用依赖，方便子模块继承，减少版本冲突：

| 依赖名称         | GroupId                       | ArtifactId                | Version    | Scope    | 用途说明                             |
|-----------------|-------------------------------|---------------------------|-----------|---------|------------------------------------|
| Jetty Server     | org.eclipse.jetty             | jetty-server              | 11.0.17  | 默认    | 提供 HTTP/Web 服务器功能            |
| Jetty Servlet 支持 | org.eclipse.jetty           | jetty-servlet             | 11.0.17  | 默认    | 支持 Servlet 功能                   |
| Jetty 扩展 Servlets | org.eclipse.jetty          | jetty-servlets            | 11.0.17  | 默认    | 提供附加 Servlet 功能（如 DefaultServlet） |
| Jetty WebSocket  | org.eclipse.jetty.websocket   | websocket-jetty-server    | 11.0.17  | 默认    | WebSocket 支持                      |
| Servlet API      | jakarta.servlet              | jakarta.servlet-api       | 6.0.0    | provided | Web 模块使用的 Servlet 接口          |
| JSON 支持        | com.fasterxml.jackson.core   | jackson-databind          | 2.16.2   | 默认    | JSON 序列化与反序列化               |
| JUnit 测试       | junit                        | junit                     | 4.13.2   | test    | 单元测试框架                         |
| Common 模块      | com.javateaching             | common                    | 1.0-SNAPSHOT | 默认 | 父模块下公共类库                     |


## 🛠️ 构建与环境配置

- Maven Compiler 插件：3.11.0
- Java 编译版本：21