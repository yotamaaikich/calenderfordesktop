# Notification Calendar for Android

An Android companion that reads new notifications through Android's user-approved `NotificationListenerService`, extracts likely dates, and stores notifications on the phone. Accepted calendar events can optionally sync through Firebase to the Windows calendar.

## Current behavior

- Can receive notifications from Teams, Gmail, Google Classroom, Discord, and other apps that post Android notifications.
- Keeps the latest 300 notification texts in the app's private local storage.
- Recognizes a few common Japanese and numeric date formats and displays a date candidate.
- Requires a tap to create an event. If it cannot find a date, a date picker opens.
- Notification bodies remain on the phone. Only an event the user adds to the calendar (date, title, source app) is synchronized when Firebase is configured and the user signs in.
- Without Firebase configuration, all features continue to work locally and the login controls stay unavailable.

## Build and install

Open this directory in Android Studio (Ladybug or newer), let Gradle sync, then choose **Build > Build APK(s)**. Install the resulting debug APK on the phone. Open the app and tap **通知アクセスを設定**, then allow **通知カレンダー** in Android's Notification access settings. Return to the app; the status should show that access is allowed.

## Optional free account and sync setup

The Firebase Spark plan does not require billing details. Create a Firebase project without starting the Google Cloud free trial, enable **Authentication > Email/Password**, and create a **Cloud Firestore** database. Register an Android app with package name `jp.yotamaaikich.notificationcalendar` and download `google-services.json`. For GitHub Actions builds, base64-encode that file and add the result as the repository Actions secret `GOOGLE_SERVICES_JSON_BASE64`; the workflow writes it only during the build. Register a Web app in the same project and save its `apiKey` and `projectId` as `%LOCALAPPDATA%\NotificationCalendar\firebase-web-config.json` on Windows:

```json
{"apiKey":"YOUR_WEB_API_KEY","projectId":"YOUR_FIREBASE_PROJECT_ID"}
```

Set the Firestore rules to the contents of the repository's `firestore.rules`. Create the same email/password account in either app, then use it on both. Only events added to the calendar are synchronized; notification history is never uploaded. Keep the Android config file and local Windows config out of public Git repositories. Firebase documents the no-payment-information Spark plan and its free quotas at https://firebase.google.com/docs/projects/billing/firebase-pricing-plans.

The project targets Android 15 (API 35) and supports Android 8.0 (API 26) or newer. A Java 17 JDK and Android SDK platform 35 are required to build. The Gradle wrapper is included. A GitHub Actions workflow can also build an APK for this project.

## Limits and privacy

Android requires the user to grant special notification access. The listener receives notifications while the phone is on and the operating system has it connected; force-stopping the app, revoking access, or manufacturer battery controls can interrupt it. It cannot read old messages that never appeared as notifications.

Android does not let a normal notification listener read notifications from a managed work profile in all configurations. If Teams is installed in a school/work profile, the profile's administrator or device policy may block access. Test with one new Teams notification before relying on it.

Notification text may contain private information. This app keeps notification history on the device. Calendar sync is optional and sends only the fields described above to the signed-in user's private Firestore path; owner-only access rules are provided in `firestore.rules`.
