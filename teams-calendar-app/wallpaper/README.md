# デスクトップ壁紙として使う

このパッケージは Lively Wallpaper 用です。Windows のデスクトップにカレンダー画面を壁紙として表示し、画面上から予定を操作できます。カレンダーの予定データは既存アプリが `%LOCALAPPDATA%\NotificationCalendar\calendar.json` に保存します。

## 設定

1. Lively Wallpaper をインストールして起動します。
2. Lively の「壁紙を追加」から `NotificationCalendarWallpaper.zip` を読み込みます。
3. 追加された **Notification Calendar** を壁紙に設定します。
4. Lively の設定で壁紙へのキーボード入力を有効にします。クリック操作はそのまま使えます。
5. デスクトップの **Teams Calendar** ショートカットを起動します。アプリが動いている間、壁紙にカレンダーが表示されます。

## 注意

- 壁紙はこの PC 内の `http://127.0.0.1:43187` に接続します。外部サービスへ通知や予定を送信しません。
- Teams Calendar アプリを終了すると壁紙は接続待ち表示になります。アプリを再度起動すると戻ります。
- Lively を終了すると壁紙は表示されません。
- Lively Wallpaper はこの PC にはまだインストールされていません。
