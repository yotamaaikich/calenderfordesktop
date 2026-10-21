package jp.yotamaaikich.notificationcalendar

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var notices: LinearLayout
    private lateinit var agenda: LinearLayout
    private val refresh = object : Runnable {
        override fun run() { render(); root.postDelayed(this, 2000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(18))
            setBackgroundColor(0xFFF5F7F6.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        val header = TextView(this).apply {
            text = "通知カレンダー\nNOTIFICATION CALENDAR · ANDROID"
            textSize = 23f
            setTextColor(0xFF17352C.toInt())
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(header)
        val intro = label("通知をこの端末内で読み取り、日付がある文章を予定候補にします。通知本文は外部へ送信しません。", 14f)
        content.addView(intro)
        status = label("通知アクセスを確認しています…", 14f)
        status.setPadding(0, dp(16), 0, dp(8))
        content.addView(status)
        val accessButton = Button(this).apply {
            text = "通知アクセスを設定"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        content.addView(accessButton)
        val privacy = label("通知アクセスを許可すると、端末に届く通知を読み取れます。Teamsが仕事用プロファイルにある場合、Androidや学校の管理設定で読み取れないことがあります。", 12f)
        content.addView(privacy)
        content.addView(section("予定カレンダー"))
        agenda = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(agenda)
        content.addView(section("受け取った通知 · 最新100件"))
        notices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(notices)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        root.removeCallbacks(refresh)
        root.post(refresh)
    }

    override fun onPause() {
        root.removeCallbacks(refresh)
        super.onPause()
    }

    private fun render() {
        val component = ComponentName(this, CalendarNotificationListener::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.split(':')?.contains(component) == true
        status.text = if (enabled) "● 通知アクセス許可済み · Teams / Gmail / Classroom / Discord など" else "● 通知アクセスはまだ許可されていません"
        status.setTextColor(if (enabled) 0xFF16834A.toInt() else 0xFFAF5B00.toInt())

        agenda.removeAllViews()
        val events = CalendarData.entries(this).sortedBy { it.date }
        if (events.isEmpty()) agenda.addView(label("まだ予定がありません。通知から「予定に追加」を押してください。", 14f))
        events.forEach { event ->
            agenda.addView(card("${formatDate(event.date)}  ·  ${event.source}\n${event.title}"))
        }

        notices.removeAllViews()
        val recent = CalendarData.notices(this).take(100)
        if (recent.isEmpty()) notices.addView(label("通知待ちです。Teamsなどから新しい通知が届くとここに表示します。", 14f))
        recent.forEach { notice ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(10))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val display = listOf(notice.app, notice.title, notice.body, notice.guessedDate?.let { "日付候補: ${formatDate(it)}" })
                .filterNotNull().filter(String::isNotBlank).joinToString("\n")
            val text = label(display, 14f)
            card.addView(text)
            val button = Button(this).apply {
                text = if (notice.guessedDate == null) "日付を選んで予定に追加" else "${formatDate(notice.guessedDate)} の予定に追加"
                setOnClickListener {
                    val initial = notice.guessedDate?.let(LocalDate::parse) ?: LocalDate.now()
                    DatePickerDialog(this@MainActivity, { _, year, month, day ->
                        CalendarData.addEntry(this@MainActivity, notice, LocalDate.of(year, month + 1, day))
                        render()
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }
            }
            card.addView(button)
            notices.addView(card, spacedParams())
        }
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(0xFF17352C.toInt())
        setPadding(0, dp(22), 0, dp(10))
    }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(0xFF35443E.toInt())
        setPadding(0, dp(4), 0, dp(6))
    }

    private fun card(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(0xFF25332D.toInt())
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundColor(0xFFFFFFFF.toInt())
        layoutParams = spacedParams()
    }

    private fun spacedParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        bottomMargin = dp(8)
    }

    private fun formatDate(raw: String) = try {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPAN))
    } catch (_: Exception) { raw }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
