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

1. 构建整个项目

```bash
mvn clean install
```

2. 运行控制台子项目

- 进入控制台子项目目录，例如 Console.Example：

```bash
cd console_example
java -jar target/console_example-1.0-SNAPSHOT.jar arg1 arg2
```

3. 运行 Web 子项目
- 进入 Web 子项目目录，例如 web_example：

```bash
cd web_example
java -jar target/web_example-1.0-SNAPSHOT.jar arg1 arg2
```

4. core 项目

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

5. Core Web 模块说明（core_web）

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

## 📦 依赖管理

- 本项目依赖 Maven 中央仓库，由父项目统一管理：
- javax.servlet-api → Web 项目 Servlet 支持
junit → 单元测试
其他依赖可在 pom.xml 中集中配置
