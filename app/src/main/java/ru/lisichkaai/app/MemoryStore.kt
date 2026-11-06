package ru.lisichkaai.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(val id: Long, val text: String)

object MemoryStore {
    private const val KEY = "long_term_memory"

    fun all(context: Context): List<MemoryItem> {
        val array = runCatching { JSONArray(AppSettings.get(context, KEY, "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val text = item.optString("text").trim()
                if (text.isNotBlank()) add(MemoryItem(item.optLong("id", i.toLong()), text))
            }
        }
    }

    private fun save(context: Context, items: List<MemoryItem>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("text", it.text)) }
        AppSettings.put(context, KEY, array.toString())
    }

    fun add(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val current = all(context)
        if (current.any { it.text.equals(clean, ignoreCase = true) }) return
        save(context, current + MemoryItem(System.currentTimeMillis(), clean))
    }

    fun update(context: Context, id: Long, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        save(context, all(context).map { if (it.id == id) it.copy(text = clean) else it })
    }

    fun delete(context: Context, id: Long) {
        save(context, all(context).filterNot { it.id == id })
    }

    fun clear(context: Context) = save(context, emptyList())

    fun promptContext(context: Context): String {
        val memories = all(context)
        if (memories.isEmpty()) return ""
        return "\n\nДОЛГОВРЕМЕННАЯ ПАМЯТЬ О ПОЛЬЗОВАТЕЛЕ:\n" +
            memories.joinToString("\n") { "- ${it.text}" } +
            "\nИспользуй эти сведения только когда они уместны. Если пользователь сообщает, что факт изменился, не считай старую запись актуальной."
    }
}
