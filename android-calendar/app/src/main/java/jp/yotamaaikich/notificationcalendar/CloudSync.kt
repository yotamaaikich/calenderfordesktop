package jp.yotamaaikich.notificationcalendar

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.MessageDigest

object CloudSync {
    fun configured(context: Context) = FirebaseApp.getApps(context).isNotEmpty()

    fun auth(context: Context): FirebaseAuth? = if (configured(context)) FirebaseAuth.getInstance() else null

    fun start(context: Context, onStatus: (String) -> Unit): ListenerRegistration? {
        val user = auth(context)?.currentUser ?: return null
        val events = FirebaseFirestore.getInstance().collection("users").document(user.uid).collection("events")
        val listener = events.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onStatus("同期エラー: ${error.localizedMessage ?: "接続を確認してください"}")
                return@addSnapshotListener
            }
            val remote = snapshot?.documents.orEmpty().mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                val title = doc.getString("title") ?: return@mapNotNull null
                val source = doc.getString("source") ?: "スマホ"
                CalendarEntry(doc.id, date, title, source)
            }
            val remoteIds = snapshot?.documents.orEmpty().map { it.id }.toSet()
            CalendarData.mergeEntries(context, remote)
            CalendarData.entries(context).forEach { entry ->
                val id = documentId(entry)
                if (id !in remoteIds) events.document(id).set(entry.toCloudMap())
            }
            onStatus("同期中 · ${user.email ?: "ログイン済み"}")
        }
        return listener
    }

    fun upload(context: Context, entry: CalendarEntry) {
        val user = auth(context)?.currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .collection("events").document(documentId(entry)).set(entry.toCloudMap())
    }

    private fun documentId(entry: CalendarEntry): String {
        val canonical = "${entry.date}|${entry.title}|${entry.source}"
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun CalendarEntry.toCloudMap() = mapOf(
        "date" to date,
        "title" to title,
        "source" to source
    )
}
