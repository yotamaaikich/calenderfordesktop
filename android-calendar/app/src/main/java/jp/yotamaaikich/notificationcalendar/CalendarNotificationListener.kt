package jp.yotamaaikich.notificationcalendar

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach(::capture)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = capture(sbn)

    private fun capture(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val parts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(parts::add)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
            line?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(parts::add)
        }
        val body = parts.distinct().joinToString("\n").take(4000)
        if (title.isBlank() && body.isBlank()) return
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) { sbn.packageName }
        val received = Instant.ofEpochMilli(sbn.postTime).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
        val notificationId = "${sbn.packageName}:${sbn.key}"
        val text = "$title\n$body"
        val guessed = CalendarData.guessDate(text, LocalDate.now())?.toString()
        CalendarData.capture(this, CapturedNotice(notificationId, appName, sbn.packageName, title, body, received, guessed))
    }
}
