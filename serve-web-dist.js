// 本地预览网页版产物的最小静态服务器：node serve-web-dist.js
// 仅用于开发验证（如中文渲染），不用于生产部署
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, 'app', 'webApp', 'build', 'dist', 'js', 'productionExecutable');
const mime = {
    '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript',
    '.css': 'text/css', '.json': 'application/json', '.wasm': 'application/wasm',
    '.xml': 'text/xml', '.md': 'text/markdown; charset=utf-8', '.png': 'image/png',
};

http.createServer((req, res) => {
    let urlPath = decodeURIComponent(req.url.split('?')[0]);
    if (urlPath === '/') urlPath = '/index.html';
    const file = path.join(root, urlPath);
    // 防目录穿越
    if (!file.startsWith(root)) { res.writeHead(403); return res.end(); }
    fs.readFile(file, (err, data) => {
        if (err) { res.writeHead(404); return res.end('Not Found: ' + urlPath); }
        res.writeHead(200, { 'Content-Type': mime[path.extname(file)] || 'application/octet-stream' });
        res.end(data);
    });
}).listen(5230, () => console.log('Serving web dist at http://localhost:5230'));
