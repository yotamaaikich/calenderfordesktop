# 通知カレンダー

Microsoft TeamsなどのWindows通知から日付を読み取り、課題や予定の候補を月間カレンダーにまとめるWindows向けアプリです。Teamsのほか、Chromeに届くGmailなどのサイト通知も対象にできます。候補は内容を確認してから登録できます。

通知本文と予定データはこのPC内に保存されます。通知の過去ログをTeamsから検索する機能や、PCの電源が切れている間にWindows通知を受け取る機能はありません。

## 使い方

1. [最新のリリース](https://github.com/yotamaaikich/teams-calenderforPC/raw/refs/heads/master/outputs/teams-calendar-app.zip)から `teams-calendar-app.zip` をダウンロードし、任意のフォルダーに展開します。
2. Windows 10（ビルド19041以降）またはWindows 11、Node.js 20以降を用意し、Windowsの「開発者モード」を有効にします。
3. 展開した `teams-calendar-app` フォルダーでPowerShellを開き、次を実行します。

   ```powershell
   Set-ExecutionPolicy -Scope Process Bypass
   .\prepare-local.ps1
   ```

4. Windowsから通知アクセスを求められたら許可します。デスクトップに作成された **Teams Calendar** ショートカットから起動できます。

通知候補はアプリ上で確認して登録します。日付が読み取れない通知は、日付を選んで追加できます。Gmailの通知を使う場合は、ChromeでGmailのサイト通知とWindowsのChrome通知を有効にしてください。

## 開発者向け

ソースから起動する場合は、`teams-calendar-app` フォルダーで以下を実行します。

```powershell
npm run prepare-local
```

準備後にアプリを起動する場合は、同じフォルダーで `npm start` を実行します。Node.js 20以降とWindows Developer Modeが必要です。

## 保存先とプライバシー

- 通知本文はWindows上のアプリ内で処理し、通知履歴をクラウドへ送信しません。
- 予定はブラウザーのローカルストレージと `%LOCALAPPDATA%\NotificationCalendar\calendar.json` に保存されます。
- このmasterブランチ版にはクラウド同期機能はありません。予定・通知ともこのPC内に保存されます。
- 個人の通知内容や予定データはGitHubへ登録しないでください。

## 仕様上の制限

このアプリが読めるのは、TeamsやChromeがWindowsに通知として表示し、Windowsの通知機能から取得できる内容です。通知が無効・ミュートの場合や、既に消去された通知、通知として表示されない投稿は取得できません。過去のTeamsチャネル投稿や返信スレッドを遡って読む機能はありません。PCの電源が切れている間は通知を取得できません。

## リポジトリ構成

- `teams-calendar-app/` — Windowsアプリのソースコードとセットアップ手順

- `outputs/` — Windowsアプリとデスクトップ壁紙向け配布ファイル

