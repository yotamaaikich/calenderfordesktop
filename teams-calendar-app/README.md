# Teams Task Calendar

A Windows desktop companion that reads Microsoft Teams' Windows toast notifications locally and forwards their text to the calendar page served on `127.0.0.1`. It uses the Windows User Notification Listener API and never sends notification content to Google Cloud or any remote service.

## First-time setup

Requirements: Windows 10 2004 (build 19041) or later, Node.js 20+, Windows Developer Mode, and Microsoft Teams notifications enabled in Windows.

In PowerShell, run:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\prepare-local.ps1
```

The script installs the Microsoft Windows App SDK metadata and Node dependencies, registers a local development package identity for notification access, creates a **Teams Calendar** desktop shortcut, and starts the app. If Windows prompts for notification access, allow it. The companion window displays whether the listener is active; close it to stop listening.

## Data flow

`UserNotificationListener` reads active Windows toast notifications. The app accepts notifications whose Windows app name contains `Teams`, plus the Microsoft Teams Chrome PWA AppUserModelId (`Chrome._crx_ompifglnodjjndh`). This covers the PWA installed in the Chrome profile used on the development PC. The app serves the calendar at `http://127.0.0.1:43187`; both the API and UI bind to loopback only. Notification messages are kept in memory until the calendar page receives them. Calendar entries are stored in the browser's local storage.

Notifications without a date stay as candidates with a date picker. Date extraction supports Japanese relative dates, weekdays, and common numeric date formats; a human still reviews every candidate before it becomes a calendar entry. The listener sees notifications that Windows and Teams actually generate; it cannot read every message in a muted chat or bypass Teams/Windows notification settings.

## Restart / remove

Use the desktop shortcut to start the calendar and listener. Closing the **Teams Calendar** native window stops the app. To remove the desktop shortcut, delete `Teams Calendar.lnk`; to unregister the local development identity, run `npx winapp unregister` from this directory.

This is a source-based local install, not a signed public installer. No Google Cloud account or billing setup is used.
