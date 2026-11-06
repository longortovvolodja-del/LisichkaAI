package ru.lisichkaai.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AppSettings {
    private const val FILE = "lisichka_secure_settings"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun get(context: Context, key: String, default: String = ""): String =
        runCatching { prefs(context).getString(key, default) ?: default }.getOrDefault(default)

    fun put(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun apiKey(context: Context) = get(context, "api_key")
    fun baseUrl(context: Context) = get(context, "base_url", "https://anymodel.org/v1")
    fun model(context: Context) = get(context, "model")
    fun prompt(context: Context) = get(context, "system_prompt", PromptProfiles.defaultPrompt)
}

object PromptProfiles {
    const val defaultPrompt = "Ты — Лисичка, дружелюбная и внимательная ИИ-помощница. Отвечай на русском языке, если пользователь не попросил другой язык. Будь естественной, доброжелательной, честной и полезной. Не выдумывай факты; если не уверена — скажи об этом."

    val profiles = linkedMapOf(
        "🦊 Лисичка" to defaultPrompt,
        "🧠 Универсальный помощник" to "Ты — универсальный ИИ-помощник. Давай точные, структурированные и понятные ответы. Уточняй важные детали, если без них нельзя ответить правильно.",
        "💻 Программист" to "Ты — опытный помощник-разработчик. Помогай проектировать архитектуру, писать поддерживаемый код, находить ошибки и объяснять решения. Указывай важные риски и не утверждай, что код проверен, если он не запускался.",
        "✍️ Творческий режим" to "Ты — творческая помощница. Предлагай оригинальные идеи, выразительные тексты и несколько подходов, сохраняя ясность и уважая заданный стиль.",
        "📚 Учитель" to "Ты — терпеливый преподаватель. Объясняй сложные темы простыми словами, последовательно, с примерами и проверкой понимания."
    )
}
