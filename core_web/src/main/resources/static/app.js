document.addEventListener("DOMContentLoaded", () => {
    const consoleTab = document.getElementById("consoleTab");
    const editorTab = document.getElementById("editorTab");
    const consoleInput = document.getElementById("consoleInput");
    const consoleOutput = document.getElementById("consoleOutput");
    const sleepOverlay = document.getElementById("sleepOverlay");
    const projectSelect = document.getElementById("projectSelect");
    const statusBar = document.getElementById("statusBar");
    const fileList = document.getElementById("fileList");

    const monacoContainer = document.getElementById("monacoEditor");
    const compileBtn = document.getElementById("compileBtn");
    const renderBtn = document.getElementById("renderBtn");

    const newProjectModal = document.getElementById("newProjectModal");
    const createProjectBtn = document.getElementById("createProjectBtn");
    const cancelProjectBtn = document.getElementById("cancelProjectBtn");

    const newFileModal = document.getElementById("newFileModal");
    const createFileBtn = document.getElementById("createFileBtn");
    const cancelFileBtn = document.getElementById("cancelFileBtn");

    let isSleeping = false;
    let failCount = 0;
    const maxFail = 5;
    let currentFilePath = "";
    let currentProjectName = "";

    // Monaco Editor 实例
    let editor = null;
    // 记录展开的文件夹路径
    const expandedFolders = new Set();

    // --- 系统操作 ---
    async function systemAction(action) {
        if (!['restart', 'close'].includes(action)) {
            viewStatus("Unknow action", "red");
            return;
        }

        const res = await fetch(`/system?action=${encodeURIComponent(action)}`);

        const data = await res.json().then(data => ({ ...data, status: res.status }));

        if (data.status === 200) {
            viewStatus(data.message, "orange");

            // 再次确认执行
            if (confirm(`Are you sure you want to ${action === 'restart' ? 'restart' : 'shut down'} the system now?`)) {
                await fetch(`/system?action=${encodeURIComponent(action)}&code=${encodeURIComponent(data.code)}`, {
                    method: 'POST',
                    headers: { "Content-Type": "text/plain; charset=UTF-8" }
                });

                if (action === "close") {
                    setTimeout(() => window.close(), 500);
                }
            }
        } else {
            viewStatus(data.message, "red");
        }
    }
    document.getElementById("restartBtn").addEventListener("click", function () { systemAction("restart") });
    document.getElementById("closeBtn").addEventListener("click", function () { systemAction("close") });


    // --- 状态栏 ---
    function viewStatus(text, color = "white") {
        statusBar.style.color = color;
        statusBar.innerText = text;
    }
    document.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            if (consoleTab.classList.contains("active"))
                executeCommand();
        }
    });

    // --- 选项卡 ---
    const tabs = [
        { btn: document.getElementById("consoleBtn"), content: consoleTab },
        { btn: document.getElementById("editorBtn"), content: editorTab }
    ];

    tabs.forEach(t => {
        t.btn.addEventListener("click", () => {
            tabs.forEach(x => {
                x.btn.classList.remove("active");
                x.content.classList.remove("active");
            });
            t.btn.classList.add("active");
            t.content.classList.add("active");
        });
    });

    function viewStatus(text, color = "white") {
        statusBar.style.color = color;
        statusBar.innerText = text;
    }

    viewStatus("Welcome", "green");

    // --- 控制台 ---
    document.getElementById("startTerminalBtn").addEventListener("click", startExecutor);
    document.getElementById("closeTerminalBtn").addEventListener("click", closeExecutor);

    let current_path = ".";

    function appendConsole(text, type = "out") {
        const div = document.createElement("div");
        div.textContent = text;
        div.style.color = type === "err" ? "red" : "#0f0";
        consoleOutput.appendChild(div);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    document.getElementById("sendConsoleBtn").addEventListener("click", () => {
        executeCommand();
    });

    const terminalPrompt = document.getElementById("terminalPrompt");
    const terminalInputBar = document.getElementById("consoleInputBar");
    function updateTerminalPrompt(path, project) {
        let displayPath = path;
        if (project && displayPath.startsWith(".")) {
            displayPath = path.replace(".", project);
        }
        terminalPrompt.innerText = `~/` + displayPath + ` $`;
    }

    consoleInput.addEventListener("input", () => {
        terminalInputBar.setAttribute("data-value", consoleInput.value);
    });

    let polling = false;

    function startExecutor() {
        fetch(`/terminal?action=start`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                viewStatus(message, status == 200 ? "green" : "red");
            })
            .catch(err => viewStatus(`Start executor failed : ${err}`));
    }

    function closeExecutor() {
        fetch(`/terminal?action=close`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                viewStatus(message, status == 200 ? "green" : "red");
            })
            .catch(err => viewStatus(`Close executor failed : ${err}`));
    }

    function executeCommand() {
        const cmd = consoleInput.value.trim();
        if (!cmd) return;
        appendConsole(`~${current_path} > ${cmd}`);
        fetch(`/terminal?action=execute&cmd=${encodeURIComponent(cmd)}`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                if (status != 200) {
                    viewStatus(message, "red");
                    return;
                }
                viewStatus(message, "green");
                consoleInput.value = "";
                if (map.path) {
                    current_path = map.path;
                    updateTerminalPrompt(current_path, currentProjectName);
                }
                if (map.running == 1) {
                    polling = true;
                    pollOutput();
                }
            })
            .catch(err => `Execute command failed : ${err}`);
    }


    function pollOutput() {
        if (!polling) return;

        fetch(`/terminal?action=poll`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                if (status === 200) {
                    if (map.path) {
                        current_path = map.path;
                        updateTerminalPrompt(current_path, currentProjectName);
                    }
                    if (map.out) map.out.split("\n").forEach(line => line && appendConsole(line, "out"));
                    if (map.err) map.err.split("\n").forEach(line => line && appendConsole(line, "err"));

                    if (map.running === "1") {
                        viewStatus("Terminal is running...", "orange");
                        setTimeout(pollOutput, 100); // 0.1 秒轮询
                    } else {
                        viewStatus("Terminal is waiting...", "green");
                        polling = false;
                    }
                } else {
                    viewStatus(message, "red");
                }
            })
            .catch(err => {
                // 遇到错误也停止轮询，避免无限报错
                polling = false;
                viewStatus("Poll failed", "red");
            });
    }

    // --- 编辑器按钮 ---
    document.getElementById("refreshBtn").addEventListener("click", () => readFile());
    document.getElementById("saveBtn").addEventListener("click", () => writeFile());

    compileBtn.addEventListener("click", () => {
        if (!currentFilePath) {
            viewStatus("Please select a file to compile", "red");
            return;
        }
        let command = "";
        if (currentFilePath.endsWith(".java")) {
            command = `javac ${currentFilePath} && java ${currentFilePath.replace(".java", "")}`;
        } else {
            command = prompt("Enter compile/run command:", "make run");
        }
        if (command) {
            tabs[0].btn.click(); // Switch to Terminal
            consoleInput.value = command;
            terminalInputBar.setAttribute("data-value", command);
            executeCommand();
        }
    });

    // --- Monaco Editor 初始化 ---
    require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' } });
    require(['vs/editor/editor.main'], function () {
        editor = monaco.editor.create(monacoContainer, {
            value: "// Select a file to start editing\n",
            language: 'java',
            theme: 'vs-dark',
            automaticLayout: true,
            fontSize: 14,
            minimap: { enabled: false },
            scrollbar: {
                vertical: 'visible',
                horizontal: 'visible',
                useShadows: false,
                verticalScrollbarSize: 10,
                horizontalScrollbarSize: 10
            }
        });
        viewStatus("Monaco Editor loaded", "green");
    });

    /**
     * 设置编辑器波浪线辅助函数
     * @param {Array} markers 格式: [{ message, severity, startLineNumber, endLineNumber, startColumn, endColumn }]
     * severity: 8 (Error), 4 (Warning), 2 (Info)
     */
    window.setEditorMarkers = function(markers) {
        if (!editor || !monaco) return;
        const model = editor.getModel();
        monaco.editor.setModelMarkers(model, "owner", markers);
    };

    renderBtn.addEventListener("click", () => {
        if (!currentFilePath) {
            viewStatus("Please select a HTML file to render", "red");
            return;
        }
        if (!editor) return;
        const content = editor.getValue();
        const win = window.open("", "_blank");
        win.document.write(content);
        win.document.close();
        viewStatus("Rendered in new tab", "green");
    });

    // --- 项目管理 ---
    function refreshProjectList() {
        fetch("/project?action=projects")
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                if (status != 200) {
                    viewStatus(message, "resd");
                    return
                }
                viewStatus(message, "green");
                while (projectSelect.firstChild) projectSelect.removeChild(projectSelect.firstChild);
                // 添加项目
                Object.keys(map).forEach(item => {
                    const option = document.createElement("option");
                    option.textContent = item;
                    option.value = item;
                    projectSelect.appendChild(option);
                });
                const add = document.createElement("option");
                add.textContent = "+";
                add.value = "create_project";
                projectSelect.appendChild(add);
                listProjectFiles();
            })
            .catch(err => viewStatus(`Get project list failed :${err}`, "red"));
    }

    function buildTree(files) {
        const root = {};
        Object.entries(files).forEach(([path, type]) => {
            const parts = path.split('/');
            let current = root;
            parts.forEach((part, i) => {
                if (!current[part]) {
                    current[part] = {
                        name: part,
                        type: (i === parts.length - 1) ? type : "Dir",
                        fullPath: parts.slice(0, i + 1).join('/'),
                        children: {}
                    };
                }
                current = current[part].children;
            });
        });
        return root;
    }

    function renderTree(container, nodes, indent = 0) {
        // 先排序：文件夹在前，文件在后
        const sortedNodes = Object.values(nodes).sort((a, b) => {
            if (a.type !== b.type) return a.type === "Dir" ? -1 : 1;
            return a.name.localeCompare(b.name);
        });

        sortedNodes.forEach(node => {
            const item = document.createElement("div");
            item.className = "tree-item";
            if (node.fullPath === currentFilePath) item.classList.add("active");

            // 缩进
            for (let i = 0; i < indent; i++) {
                const space = document.createElement("span");
                space.className = "tree-indent";
                item.appendChild(space);
            }

            if (node.type === "Dir") {
                // Chevron
                const chevron = document.createElement("span");
                chevron.className = "tree-chevron";
                chevron.innerHTML = "▼";
                if (!expandedFolders.has(node.fullPath)) chevron.classList.add("collapsed");
                item.appendChild(chevron);

                // Folder Icon
                const icon = document.createElement("span");
                icon.className = "tree-icon icon-folder";
                icon.innerHTML = "📁";
                item.appendChild(icon);

                // Name
                const name = document.createElement("span");
                name.className = "folder-header";
                name.textContent = node.name;
                item.appendChild(name);

                item.addEventListener("click", (e) => {
                    e.stopPropagation();
                    if (expandedFolders.has(node.fullPath)) {
                        expandedFolders.delete(node.fullPath);
                    } else {
                        expandedFolders.add(node.fullPath);
                    }
                    renderFileList(lastFileData);
                });

                container.appendChild(item);

                // 递归渲染子节点
                if (expandedFolders.has(node.fullPath)) {
                    renderTree(container, node.children, indent + 1);
                }
            } else {
                // File Spacer (代替 Chevron)
                const spacer = document.createElement("span");
                spacer.className = "tree-chevron"; // 仅占位
                item.appendChild(spacer);

                // File Icon
                const icon = document.createElement("span");
                icon.className = "tree-icon icon-file";
                let ext = node.name.split('.').pop().toLowerCase();
                if (ext === "java") { icon.innerHTML = "☕"; icon.classList.add("icon-java"); }
                else if (ext === "html") { icon.innerHTML = "🌐"; icon.classList.add("icon-html"); }
                else { icon.innerHTML = "📄"; }
                item.appendChild(icon);

                // Name
                const name = document.createElement("span");
                name.textContent = node.name;
                item.appendChild(name);

                item.addEventListener("click", (e) => {
                    e.stopPropagation();
                    currentFilePath = node.fullPath;
                    // 重新强调 active 状态
                    document.querySelectorAll(".tree-item").forEach(el => el.classList.remove("active"));
                    item.classList.add("active");
                    readFile();
                });
                container.appendChild(item);
            }
        });
    }

    let lastFileData = {};

    function listProjectFiles() {
        const projectName = projectSelect.value;
        if (projectName == "create_project") {
            newProjectModal.style.display = "flex";
            return
        }
        cleanEditor();
        fetch(`/project?action=list&project=${projectName}`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                if (status != 200) {
                    viewStatus(message, "red");
                    return;
                }
                viewStatus(message, "green");
                lastFileData = map;
                renderFileList(map);
            })
            .catch(err => viewStatus(`Get files list failed :${err}`, "red"));
    }

    function renderFileList(map) {
        while (fileList.firstChild) fileList.removeChild(fileList.firstChild);
        const tree = buildTree(map);
        renderTree(fileList, tree);

        // 添加新增按钮
        const add = document.createElement("div");
        add.className = "tree-item";
        add.style.marginTop = "10px";
        add.style.justifyContent = "center";
        add.style.border = "1px dashed #444";
        add.innerHTML = "<span>+ New File / Dir</span>";
        add.addEventListener("click", function () {
            newFileModal.style.display = "flex";
        });
        fileList.appendChild(add);
    }

    projectSelect.addEventListener("change", () => {
        currentProjectName = projectSelect.value;
        listProjectFiles();
        updateTerminalPrompt(current_path, currentProjectName);
    });

    function cleanEditor() { 
        if (editor) editor.setValue(""); 
    }

    function readFile() {
        const projectName = projectSelect.value;
        fetch(`/project?action=read_file&path=${currentFilePath}&project=${projectName}`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                if (status != 200) {
                    viewStatus(message, "red");
                    return;
                }
                viewStatus(`${message}: ${map.file}`, "green");
                if (editor) {
                    const ext = map.file.split('.').pop().toLowerCase();
                    let lang = 'plaintext';
                    if (ext === 'java') lang = 'java';
                    else if (ext === 'js') lang = 'javascript';
                    else if (ext === 'html') lang = 'html';
                    else if (ext === 'css') lang = 'css';
                    
                    const model = monaco.editor.createModel(map.content, lang);
                    editor.setModel(model);
                }
            })
            .catch(err => viewStatus(`Read file failed : ${err}`, "red"));
    }

    function writeFile() {
        if (!editor) return;
        const projectName = projectSelect.value;
        const content = editor.getValue();
        fetch(`/project?action=write_file&path=${currentFilePath}&project=${projectName}`, {
            method: "POST",
            headers: {
                "Content-Type": "text/plain; charset=UTF-8"
            },
            body: content
        })
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message }) => {
                viewStatus(message, status == 200 ? "green" : "red");
            })
            .catch(err => viewStatus(`Write file failed: ${err}`, "red"));
    }

    createProjectBtn.addEventListener("click", () => {
        const projectType = document.getElementById("projectType").value;
        const projectName = document.getElementById("projectName").value;
        viewStatus(`Create project : ${projectType} ${projectName}`);
    });
    cancelProjectBtn.addEventListener("click", function () {
        newProjectModal.style.display = "none";
        document.getElementById("projectName").value = "";
    });


    createFileBtn.addEventListener("click", () => {
        const fileType = document.getElementById("fileType").value;
        const filePath = document.getElementById("filePath").value;
        viewStatus(`Create file : ${fileType} ${filePath}`);
    });
    cancelFileBtn.addEventListener("click", function () {
        newFileModal.style.display = "none";
        document.getElementById("filePath").value = "";
    });

    // --- 休眠控制 ---
    function enterSleep() {
        sleepOverlay.style.display = "flex";
        if (editor) editor.updateOptions({ readOnly: true });
    }

    function exitSleep() {
        sleepOverlay.style.display = "none";
        if (editor) editor.updateOptions({ readOnly: false });
    }

    // --- 心跳机制 ---
    function sendHeartbeat() {
        fetch("/heartbeat")
            .then(res => {
                const status = res.status;
                return res.text().then(text => ({ status, text }));
            })
            .then(({ status, text }) => {
                if (status === 200 && text.trim() === "OK") {
                    failCount = 0;
                    exitSleep();
                } else {
                    failCount++;
                    console.warn(`Heartbeat failed (${failCount}/${maxFail}): Status ${status}, Body: ${text}`);
                    if (failCount >= maxFail) enterSleep();
                }
            })
            .catch(err => {
                failCount++;
                console.error(`Heartbeat error (${failCount}/${maxFail}):`, err);
                if (failCount >= maxFail) enterSleep();
            });
    }

    // 初始化
    refreshProjectList();
    viewStatus("Welcome", "green");
    exitSleep();
    setInterval(sendHeartbeat, 3000);
});
