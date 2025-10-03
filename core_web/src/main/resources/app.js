const moduleSelect = document.getElementById("moduleSelect");
const consoleDiv = document.getElementById("console");
const editor = document.getElementById("editor");
const sidebar = document.getElementById("sidebar");

function loadModules() {
    fetch('/api/modules')
        .then(res => res.json())
        .then(data => {
            moduleSelect.innerHTML = '';
            data.forEach(mod => {
                const opt = document.createElement('option');
                opt.value = mod;
                opt.textContent = mod;
                moduleSelect.appendChild(opt);
            });
        });
}

function runModule() {
    const module = moduleSelect.value;
    consoleDiv.style.display = 'block';
    editor.style.display = 'none';
    consoleDiv.textContent = '';
    
    fetch(`/api/run?module=${module}`)
        .then(res => res.body.getReader())
        .then(reader => {
            const decoder = new TextDecoder();
            function read() {
                reader.read().then(({done, value}) => {
                    if (done) return;
                    consoleDiv.textContent += decoder.decode(value);
                    consoleDiv.scrollTop = consoleDiv.scrollHeight;
                    read();
                });
            }
            read();
        });
}

function loadFileTree() {
    const module = moduleSelect.value;
    fetch(`/api/filetree?module=${module}`)
        .then(res => res.json())
        .then(data => {
            sidebar.innerHTML = '';
            data.forEach(file => {
                const div = document.createElement('div');
                div.textContent = file.isDirectory ? `[DIR] ${file.name}` : file.name;
                div.style.cursor = 'pointer';
                div.onclick = () => loadFileContent(file.name);
                sidebar.appendChild(div);
            });
        });
}

function loadFileContent(filename) {
    const module = moduleSelect.value;
    fetch(`/api/load?module=${module}&file=${filename}`)
        .then(res => res.text())
        .then(data => {
            editor.value = data;
            editor.style.display = 'block';
            consoleDiv.style.display = 'none';
        });
}

function saveFile() {
    const module = moduleSelect.value;
    const file = prompt("请输入要保存的文件路径", "");
    if (!file) return;

    fetch('/api/save', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: `module=${module}&file=${file}&content=${encodeURIComponent(editor.value)}`
    }).then(res => res.text())
      .then(msg => alert(msg));
}

function newModule() {
    const module = prompt("请输入新模块名", "");
    if (!module) return;
    const type = prompt("类型 console/web", "console");
    fetch('/api/new', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: `module=${module}&type=${type}`
    }).then(res => res.text())
      .then(msg => alert(msg));
}

function restartServer() {
    fetch('/api/restart', {method:'POST'})
        .then(res => res.text())
        .then(msg => alert(msg));
}

document.getElementById("runBtn").onclick = runModule;
document.getElementById("editBtn").onclick = loadFileTree;
document.getElementById("newBtn").onclick = newModule;
document.getElementById("restartBtn").onclick = restartServer;

loadModules();
