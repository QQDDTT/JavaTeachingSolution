document.addEventListener("DOMContentLoaded", () => {
    const tabs = document.querySelectorAll(".tab-btn");
    const contents = document.querySelectorAll(".tab-content");
    const consoleOutput = document.getElementById("consoleOutput");
    const sleepOverlay = document.getElementById("sleepOverlay");
    const projectSelect = document.getElementById("projectSelect");
    const statusBar = document.getElementById("statusBar");
    const fileList = document.getElementById("fileList");


    const editor = document.getElementById("editor");

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

    // --- 状态栏 ---
    function viewStatus(text, color = "white") {
        statusBar.style.color = color;
        statusBar.innerText = text;
    }

    // --- 选项卡 ---
    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            // 设置激活样式
            tabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            // 切换内容
            contents.forEach(c => c.classList.remove("active"));
            const target = document.getElementById(tab.dataset.tab);
            target.classList.add("active");
        });
    });

    // --- 控制台 ---
    function appendConsole(text) {
        const line = document.createElement("div");
        line.textContent = text;
        consoleOutput.appendChild(line);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    document.getElementById("sendConsoleBtn").addEventListener("click", () => {
        const input = document.getElementById("consoleInput");
        if(input.value.trim()) {
            appendConsole("> " + input.value);
            input.value = "";
        }
    });

    // --- 编辑器按钮 ---
    document.getElementById("refreshBtn").addEventListener("click", () => readFile());
    document.getElementById("saveBtn").addEventListener("click", () => writeFile());

    // --- 项目管理 ---
    function refreshProjectList() {
        fetch("/project?action=projects")
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                viewStatus(message, status == 200 ? "green" : "red");
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
                viewStatus(message, status == 200 ? "green" : "red");
                while (fileList.firstChild) fileList.removeChild(fileList.firstChild);
                Object.entries(map).forEach(([key, value]) => {
                    if (value == "File") {
                        const div = document.createElement("div");
                        div.innerHTML = key;
                        div.addEventListener("click", function() {
                            currentFilePath = key;
                            readFile();
                        });
                        fileList.appendChild(div);
                    }
                });
                const add = document.createElement("div");
                add.innerHTML = "+";
                add.addEventListener("click", function () {
                    newFileModal.style.display = "flex";
                });
                fileList.appendChild(add);
            })
            .catch (err => viewStatus(`Get files list failed :${err}`, "red"));
    }
    projectSelect.addEventListener("change", () => listProjectFiles());

    function cleanEditor() {
        editor.innerHTML = "";
    }

    function readFile() {
        const projectName = projectSelect.value;
        fetch(`/project?action=read_file&path=${currentFilePath}&project=${projectName}`)
            .then(res => res.json().then(data => ({ ...data, status: res.status })))
            .then(({ status, message, map }) => {
                viewStatus(`${message}: ${map.file}`, status == 200 ? "green" : "red");
                cleanEditor();
                editor.value = map.content;
            })
            .catch (err => viewStatus(`Read file failed : ${err}`, "red"));
    }

    function writeFile() {
        const projectName = projectSelect.value;
        const content = editor.value;
        fetch(`/project?action=write_file&path=${currentFilePath}&project=${projectName}`, {
            method: "POST",
            headers: {
                "Content-Type": "text/plain; charset=UTF-8"
            },
            body: content
        })
        .then(res => res.json().then(data => ({ ...data, status: res.status })))
        .then(({ status, message }) => {
            viewStatus(message, status === 200 ? "green" : "red");
        })
        .catch(err => viewStatus(`Write file failed: ${err}`, "red"));
    }

    createProjectBtn.addEventListener("click", () => {
        const projectType = document.getElementById("projectType").value;
        const projectName = document.getElementById("projectName").value;
        viewStatus(`Create project : ${projectType} ${projectName}`);
    });
    cancelProjectBtn.addEventListener("click", function(){
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
        editor.contentEditable = "false";
    }

    function exitSleep() {
        sleepOverlay.style.display = "none";
        editor.contentEditable = "true";
    }

    document.getElementById("restartBtn").addEventListener("click", enterSleep);
    document.getElementById("closeBtn").addEventListener("click", () => {
        if(confirm("Confirm closing of this webpage?")) window.close();
    });

    // --- 心跳机制 ---
    function sendHeartbeat() {
        fetch("/heartbeat")
            .then(res => res.text())
            .then(text => {
                if(text.trim() === "OK") {
                    failCount = 0;
                    exitSleep();
                } else {
                    failCount++;
                    if(failCount >= maxFail) enterSleep();
                }
            })
            .catch(err => {
                failCount++;
                if(failCount >= maxFail) enterSleep();
            });
    }

    // 初始化
    exitSleep();
    refreshProjectList();
    cleanEditor();
    setInterval(sendHeartbeat, 3000);
});
