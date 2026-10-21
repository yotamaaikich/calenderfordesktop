const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { spawn } = require('node:child_process');
const { Worker } = require('node:worker_threads');

const architecture = { arm64: 'arm64', x64: 'x64' }[process.arch];
if (!architecture) throw new Error(`Unsupported Node.js architecture: ${process.arch}`);
const bootstrapDll = path.join(__dirname, '.winapp', 'bin', architecture, 'Microsoft.WindowsAppRuntime.Bootstrap.dll');
if (!fs.existsSync(bootstrapDll)) throw new Error('Windows App SDK missing. Run npm run restore first.');
process.env.WINAPPSDK_BOOTSTRAP_DLL_PATH = bootstrapDll;
const dynwinrt = require('@microsoft/dynwinrt');
if (!dynwinrt.hasPackageIdentity()) dynwinrt.initWinappsdk(2, 2);

const port = Number(process.env.PORT || 43187);
const page = path.join(__dirname, 'public', 'index.html');
const calendarFile = path.join(process.env.LOCALAPPDATA || __dirname, 'NotificationCalendar', 'calendar.json');
const firebaseConfigFile = path.join(process.env.LOCALAPPDATA || __dirname, 'NotificationCalendar', 'firebase-web-config.json');
const notifications = [];
let worker;

function readCalendar() {
  try { return JSON.parse(fs.readFileSync(calendarFile, 'utf8')); }
  catch { return []; }
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${port}`);
  if (req.method === 'GET' && url.pathname === '/') {
    res.writeHead(200, { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' });
    fs.createReadStream(page).pipe(res);
    return;
  }
  if (req.method === 'GET' && url.pathname === '/api/firebase-config') {
    try {
      const config = JSON.parse(fs.readFileSync(firebaseConfigFile, 'utf8'));
      const safe = { apiKey: config.apiKey, projectId: config.projectId };
      if (typeof safe.apiKey !== 'string' || typeof safe.projectId !== 'string') throw new Error('Invalid Firebase config');
      res.writeHead(200, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
      res.end(JSON.stringify(safe));
    } catch {
      res.writeHead(200, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
      res.end('{}');
    }
    return;
  }
  if (req.method === 'GET' && url.pathname === '/api/notifications') {
    const after = Number(url.searchParams.get('after') || 0);
    const items = notifications.filter((item) => item.sequence > after);
    res.writeHead(200, { 'content-type': 'application/json; charset=utf-8', 'access-control-allow-origin': `http://127.0.0.1:${port}`, 'cache-control': 'no-store' });
    res.end(JSON.stringify({ items, latest: notifications.at(-1)?.sequence || after }));
    return;
  }
  if (req.method === 'POST' && url.pathname === '/api/open-calendar') {
    openCalendar();
    res.writeHead(204, { 'cache-control': 'no-store' });
    res.end();
    return;
  }
  if (url.pathname === '/api/calendar' && req.method === 'GET') {
    res.writeHead(200, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
    res.end(JSON.stringify(readCalendar()));
    return;
  }
  if (url.pathname === '/api/calendar' && req.method === 'POST') {
    let body = '';
    req.on('data', (chunk) => { body += chunk; if (body.length > 1_000_000) req.destroy(); });
    req.on('end', () => {
      try {
        const submittedItems = JSON.parse(body);
        if (!Array.isArray(submittedItems) || submittedItems.length > 500 || submittedItems.some((x) => !/^\d{4}-\d{2}-\d{2}$/.test(x.date) || typeof x.title !== 'string' || typeof x.type !== 'string')) throw new Error('Invalid calendar data');
        const items = [...new Map(submittedItems.map((x) => [`${x.date}|${x.type}|${x.title}`, x])).values()];
        fs.mkdirSync(path.dirname(calendarFile), { recursive: true });
        fs.writeFileSync(calendarFile, JSON.stringify(items, null, 2), 'utf8');
        res.writeHead(200, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
        res.end(JSON.stringify(items));
      } catch {
        res.writeHead(400, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('Invalid calendar data');
      }
    });
    return;
  }
  res.writeHead(404);
  res.end('Not found');
});

worker = new Worker(path.join(__dirname, 'winui-worker.js'));
worker.on('message', (message) => {
  if (message?.type === 'calendar-notification') {
    notifications.push({ ...message.notification, sequence: (notifications.at(-1)?.sequence || 0) + 1 });
    if (notifications.length > 200) notifications.shift();
    console.log(`${message.notification.source} notification received: ${message.notification.app}`);
  } else if (message?.type === 'ready') {
    console.log('Calendar is ready.');
  } else if (message?.type === 'open-calendar') {
    openCalendar();
  } else if (message?.type === 'access') {
    console.log(`Notification access: ${message.status}`);
  } else if (message?.type === 'error') {
    console.error(message.message);
    server.close();
  }
});

function openCalendar() {
  spawn('rundll32.exe', ['url.dll,FileProtocolHandler', `http://127.0.0.1:${port}`], { detached: true, stdio: 'ignore' }).unref();
}

server.listen(port, '127.0.0.1', () => {
  openCalendar();
  console.log(`Calendar opened at http://127.0.0.1:${port}`);
});

function shutdown() {
  worker.terminate();
  server.close();
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
worker.on('exit', () => server.close());
worker.on('error', (error) => {
  console.error(error);
  server.close();
});
