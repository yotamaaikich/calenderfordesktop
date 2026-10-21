package jp.yotamaaikich.notificationcalendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CapturedNotice(
    val id: String,
    val app: String,
    val packageName: String,
    val title: String,
    val body: String,
    val receivedAt: String,
    val guessedDate: String?
) {
    companion object {
        fun fromJson(json: JSONObject) = CapturedNotice(
            json.optString("id"), json.optString("app"), json.optString("package"),
            json.optString("title"), json.optString("body"), json.optString("receivedAt"),
            json.optString("date").takeIf(String::isNotBlank)
        )
    }
}

data class CalendarEntry(val id: String, val date: String, val title: String, val source: String) {
    companion object {
        fun fromJson(json: JSONObject) = CalendarEntry(
            json.optString("id"), json.optString("date"), json.optString("title"), json.optString("source")
        )
    }
}

object CalendarData {
    private const val PREFS = "notification_calendar_local"
    private const val NOTICES = "notices"
    private const val ENTRIES = "entries"
    private val noticeDatePatterns = listOf(
        Regex("(?<!\\d)(20\\d{2})[年./-](\\d{1,2})[月./-](\\d{1,2})日?"),
        Regex("(?<!\\d)(\\d{1,2})月(\\d{1,2})日"),
        Regex("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)")
    )

    fun capture(context: Context, notice: CapturedNotice) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readNotices(context).filterNot { it.id == notice.id }.toMutableList()
        items.add(0, notice)
        prefs.edit().putString(NOTICES, JSONArray().apply {
            items.take(300).forEach { put(it.toJson()) }
        }.toString()).apply()
    }

    fun notices(context: Context): List<CapturedNotice> = readNotices(context)

    private fun readNotices(context: Context): List<CapturedNotice> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NOTICES, "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { CapturedNotice.fromJson(array.getJSONObject(it)) }
    } catch (_: Exception) { emptyList() }

    fun entries(context: Context): List<CalendarEntry> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ENTRIES, "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { CalendarEntry.fromJson(array.getJSONObject(it)) }
            .sortedBy { it.date }
    } catch (_: Exception) { emptyList() }

    fun mergeEntries(context: Context, incoming: List<CalendarEntry>) {
        val merged = (entries(context) + incoming)
            .distinctBy { "${it.date}|${it.title}|${it.source}" }
            .sortedBy { it.date }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(
            ENTRIES, JSONArray().apply { merged.forEach { put(it.toJson()) } }.toString()
        ).apply()
    }

    fun addEntry(context: Context, notice: CapturedNotice, date: LocalDate = notice.guessedDate?.let(LocalDate::parse) ?: LocalDate.now()): Boolean {
        val title = listOf(notice.title, notice.body).filter(String::isNotBlank).joinToString(" — ").take(180)
        if (title.isBlank()) return false
        val current = entries(context).toMutableList()
        if (current.any { it.date == date.toString() && it.title == title }) return false
        current.add(CalendarEntry(notice.id, date.toString(), title, notice.app))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(
            ENTRIES, JSONArray().apply { current.forEach { put(it.toJson()) } }.toString()
        ).apply()
        return true
    }

    fun guessDate(text: String, now: LocalDate = LocalDate.now()): LocalDate? {
        val normalized = text.replace('／', '/').replace('－', '-')
        if (normalized.contains("明後日")) return now.plusDays(2)
        if (normalized.contains("明日")) return now.plusDays(1)
        if (normalized.contains("今日")) return now
        for ((index, regex) in noticeDatePatterns.withIndex()) {
            val match = regex.find(normalized) ?: continue
            try {
                val date = if (index == 0) {
                    LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
                } else {
                    var candidate = LocalDate.of(now.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
                    if (candidate.isBefore(now.minusDays(1))) candidate = candidate.plusYears(1)
                    candidate
                }
                return date
            } catch (_: Exception) { }
        }
        return null
    }

    private fun CapturedNotice.toJson() = JSONObject().put("id", id).put("app", app)
        .put("package", packageName).put("title", title).put("body", body)
        .put("receivedAt", receivedAt).put("date", guessedDate)

    private fun CalendarEntry.toJson() = JSONObject().put("id", id).put("date", date)
        .put("title", title).put("source", source)

}
