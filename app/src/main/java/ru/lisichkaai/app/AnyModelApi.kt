package ru.lisichkaai.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AnyModelApi {
    data class Result(val text: String, val models: List<String> = emptyList())

    private fun request(url: String, apiKey: String, method: String = "GET", body: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 60000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "HTTP $status: ${response.take(240)}")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    fun loadModels(baseUrl: String, apiKey: String): List<String> {
        require(apiKey.isNotBlank()) { "Сначала укажи API-ключ." }
        val json = JSONObject(request("${baseUrl.trimEnd('/')}/models", apiKey))
        val data = json.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) add(id)
            }
        }.distinct().sorted()
    }

    fun synthesizeSpeech(baseUrl: String, apiKey: String, voiceModel: String, text: String): ByteArray {
        require(apiKey.isNotBlank()) { "Не указан API-ключ AnyModel." }
        require(voiceModel.isNotBlank()) { "Не указана модель озвучивания." }
        require(text.isNotBlank()) { "Нечего озвучивать." }

        val connection = (URL("${baseUrl.trimEnd('/')}/audio/speech").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 120000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg")
            doOutput = true
        }
        try {
            val body = JSONObject()
                .put("model", voiceModel)
                .put("input", text)
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = runCatching {
                    JSONObject(error).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "HTTP $status: ${error.take(240)}")
            }
            val audio = connection.inputStream.use { it.readBytes() }
            require(audio.isNotEmpty()) { "Сервис озвучивания вернул пустой аудиофайл." }
            return audio
        } finally {
            connection.disconnect()
        }
    }

    fun sendMessage(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): String {
        require(apiKey.isNotBlank()) { "Не указан API-ключ AnyModel." }
        require(model.isNotBlank()) { "Выбери модель или введи её ID." }
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        history.forEach {
            messages.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text))
        }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", false)
        val response = JSONObject(
            request("${baseUrl.trimEnd('/')}/chat/completions", apiKey, "POST", body.toString())
        )
        return response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
            ?: "Модель вернула пустой ответ."
    }
}
