const { parentPort } = require('node:worker_threads');
const { roInitialize } = require('@microsoft/dynwinrt');
const {
  Application, Button, Grid, HorizontalAlignment, Orientation,
  StackPanel, TextBlock, TextWrapping, VerticalAlignment, Window,
} = require('#winapp/bindings');
const { UserNotificationListener, NotificationKinds } = require('#winapp/bindings');

roInitialize(0);
let app;
let listener;
let status;
let latest;
const known = new Set();

function children(panel, ...items) {
  const vector = panel.children;
  for (const item of items) vector.append(item);
}
function text(value, size, weight = 400) {
  const block = new TextBlock();
  block.text = value;
  block.fontSize = size;
  block.fontWeight = { weight };
  block.textWrapping = TextWrapping.Wrap;
  return block;
}
function setStatus(value) {
  if (status) status.text = value;
}
function reportError(error) {
  parentPort.postMessage({ type: 'error', message: error?.stack || String(error) });
}

function isTeams(appInfo) {
  try {
    const name = appInfo?.displayInfo?.displayName || '';
    const id = appInfo?.appUserModelId || '';
    return /teams/i.test(`${name} ${id}`) || /^Chrome\._crx_ompifglnodjjndh(?:\.|$)/i.test(id);
  } catch { return false; }
}

async function readTeamsNotifications() {
  const list = await listener.getNotificationsAsync(NotificationKinds.Toast);
  for (let i = 0; i < list.size; i += 1) {
    const item = list.getAt(i);
    if (!item || !isTeams(item.appInfo) || known.has(item.id)) continue;
    known.add(item.id);
    const app = item.appInfo.displayInfo.displayName || 'Microsoft Teams';
    const binding = item.notification?.visual?.getBinding('ToastGeneric');
    const textElements = binding?.getTextElements();
    const textParts = [];
    if (textElements) {
      for (let n = 0; n < textElements.size; n += 1) {
        const value = textElements.getAt(n)?.text?.trim();
        if (value) textParts.push(value);
      }
    }
    const body = [...new Set(textParts)].join('\n');
    if (!body) continue;
    parentPort.postMessage({
      type: 'teams-notification',
      notification: { id: String(item.id), app, body, receivedAt: new Date().toISOString() },
    });
    if (latest) latest.text = `${app}\n${body}`;
  }
}

function activateListener() {
  const access = listener.getAccessStatus();
  if (access === 1) {
    setStatus('許可済み · Teamsの通知を監視しています');
    listener.onNotificationChanged(() => {
      readTeamsNotifications().catch(reportError);
    });
    readTeamsNotifications().catch(reportError);
    parentPort.postMessage({ type: 'access', status: 'allowed' });
    return;
  }
  if (access === 2) {
    setStatus('通知へのアクセスが拒否されています。Windowsの設定から許可してください。');
    parentPort.postMessage({ type: 'access', status: 'denied' });
    return;
  }
  setStatus('「通知を許可」を押してTeams通知の読み取りを開始してください');
}

Application.start(() => {
  try {
    app = Application.create(() => {
      try {
        const window = new Window();
        window.title = 'Teams Calendar · 通知連携';
        const root = new Grid();
        root.padding = { left: 28, top: 24, right: 28, bottom: 24 };
        const content = new StackPanel();
        content.orientation = Orientation.Vertical;
        content.spacing = 14;
        content.verticalAlignment = VerticalAlignment.Center;
        const eyebrow = text('TEAMS CALENDAR / LOCAL', 11, 600);
        const heading = text('通知を予定に取り込む', 26, 600);
        const explanation = text('このPC上でTeamsのWindows通知だけを読み取ります。通知本文は外部サーバーに送信しません。', 13);
        status = text('アクセス状態を確認しています…', 12);
        const permission = new Button();
        permission.content = text('通知を許可', 13, 600);
        permission.horizontalAlignment = HorizontalAlignment.Left;
        const openCalendar = new Button();
        openCalendar.content = text('カレンダーを開く', 13, 600);
        openCalendar.horizontalAlignment = HorizontalAlignment.Left;
        latest = text('Teamsの通知がここに表示されます。', 12);
        children(content, eyebrow, heading, explanation, status, permission, openCalendar, latest);
        children(root, content);
        window.content = root;
        permission.onClick(async () => {
          try {
            setStatus('Windowsの許可を待っています…');
            const result = await listener.requestAccessAsync();
            if (result === 1) activateListener();
            else setStatus('通知へのアクセスが許可されませんでした。Windowsの通知設定を確認してください。');
          } catch (error) { reportError(error); setStatus(`通知の許可に失敗しました: ${error.message || error}`); }
        });
        openCalendar.onClick(() => parentPort.postMessage({ type: 'open-calendar' }));
        window.onClosed(() => Application.current?.exit());
        root.onceLoaded(() => {
          const scale = root.xamlRoot?.rasterizationScale ?? 1;
          window.appWindow.resize({ width: Math.round(620 * scale), height: Math.round(430 * scale) });
        });
        listener = UserNotificationListener.current;
        if (!listener) throw new Error('Windows notification listener is unavailable. Start through the packaged setup script.');
        activateListener();
        window.activate();
        parentPort.postMessage({ type: 'ready' });
      } catch (error) { reportError(error); Application.current?.exit(); }
    });
  } catch (error) { reportError(error); Application.current?.exit(); }
});
