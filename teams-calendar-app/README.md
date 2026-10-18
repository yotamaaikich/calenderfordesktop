# Notification Calendar

A Windows desktop companion that collects Microsoft Teams and Chrome toast notifications locally and turns their text into calendar candidates. Gmail website notifications arrive through Chrome when Gmail and Chrome site notifications are enabled. Notification text stays on this PC; the app does not send it to Google Cloud or another remote service.

## First-time setup

Requirements: Windows 10 2004 (build 19041) or later, Node.js 20+, Windows Developer Mode, and Teams / Chrome notifications enabled in Windows.

In PowerShell, run:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\prepare-local.ps1
```

The script installs the Microsoft Windows App SDK metadata and Node dependencies, registers a local development package identity for notification access, creates a **Teams Calendar** desktop shortcut, and starts the app. Allow notification access when Windows asks.

For Gmail, sign in to Gmail in Chrome and enable site notifications in Gmail and Chrome. Browser notifications are identified as Chrome; this can include notifications from other Chrome sites too.

## What it can read

The listener reads notifications that Windows exposes to `UserNotificationListener`. It can only process notifications that Teams / Chrome actually sends to Windows and keeps available to the listener. Muted chats, disabled notifications, or messages already dismissed from Windows may not be available. It does not currently search old Teams channel history.

The calendar runs at `http://127.0.0.1:43187`; its API and UI bind to loopback only. Calendar entries stay in browser local storage and are copied to `%LOCALAPPDATA%\NotificationCalendar\calendar.json` so the desktop widget can display them. This file stays on the PC and is not committed to Git. The widget is a small always-on-top window; close it with the app, then reopen the app from the desktop shortcut. Dates are extracted from Japanese relative dates, weekdays, and common numeric date formats. Review each candidate before adding it as a calendar entry; candidates without a date remain pending for manual date selection.

## Teams channel history

Reading existing channel posts requires a separate Microsoft Graph connection using a work or school account. Microsoft Graph lists `ChannelMessage.Read.All` as the least-privileged delegated permission for channel messages, and does not support this endpoint for personal Microsoft accounts. School tenant consent or administrator policy may also be required. The current notification-only app does not request this permission or access channel history.

## Restart / remove

Use the desktop shortcut to start the calendar and listener. Closing the **Teams Calendar** native window stops the app. To remove the desktop shortcut, delete `Teams Calendar.lnk`; to unregister the local development identity, run `npx winapp unregister` from this directory.

This is a source-based local install, not a signed public installer. No Google Cloud account or billing setup is used.
