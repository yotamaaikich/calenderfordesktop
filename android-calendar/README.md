# Notification Calendar for Android

An Android companion that reads new notifications through Android's user-approved `NotificationListenerService`, extracts likely dates, and stores notifications and calendar entries on the phone only.

## Current behavior

- Can receive notifications from Teams, Gmail, Google Classroom, Discord, and other apps that post Android notifications.
- Keeps the latest 300 notification texts in the app's private local storage.
- Recognizes a few common Japanese and numeric date formats and displays a date candidate.
- Requires a tap to create an event. If it cannot find a date, a date picker opens.
- Does not upload notification text, use a cloud service, or synchronize with the Windows app yet.

## Build and install

Open this directory in Android Studio (Ladybug or newer), let Gradle sync, then choose **Build > Build APK(s)**. Install the resulting debug APK on the phone. Open the app and tap **通知アクセスを設定**, then allow **通知カレンダー** in Android's Notification access settings. Return to the app; the status should show that access is allowed.

The project targets Android 15 (API 35) and supports Android 8.0 (API 26) or newer. A Java 17 JDK and Android SDK platform 35 are required to build. The Gradle wrapper is included. A GitHub Actions workflow can also build an APK for this project.

## Limits and privacy

Android requires the user to grant special notification access. The listener receives notifications while the phone is on and the operating system has it connected; force-stopping the app, revoking access, or manufacturer battery controls can interrupt it. It cannot read old messages that never appeared as notifications.

Android does not let a normal notification listener read notifications from a managed work profile in all configurations. If Teams is installed in a school/work profile, the profile's administrator or device policy may block access. Test with one new Teams notification before relying on it.

Notification text may contain private information. This version stores it on the device and intentionally has no account login or cloud sync. Account login and synchronization with the Windows calendar require a separately configured backend and are not included in this first mobile prototype.
