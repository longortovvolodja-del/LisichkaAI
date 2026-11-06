package ru.lisichkaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF05050D)
private val Panel = Color(0xFF111020)
private val Violet = Color(0xFF8B35FF)
private val Lavender = Color(0xFFD6BDFF)
private val Muted = Color(0xFF918BAA)
private val ElectricBlue = Color(0xFF287BFF)
private val NeonPink = Color(0xFFE05BFF)
private val PanelBorder = Color(0xFF44306B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LisichkaApp() }
    }
}

data class ChatMessage(val text: String, val fromUser: Boolean)

@Composable
private fun LisichkaApp() {
    var draft by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("Чат") }
    var sending by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Lavender, background = Ink, surface = Panel)) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF18072F), Color(0xFF080813), Color(0xFF06152A), Ink))
            )
        ) {
            TopBar(onSettings = { selectedTab = "Настройки" })

            Box(Modifier.weight(1f).fillMaxWidth()) {
                stateHolder.SaveableStateProvider(selectedTab) {
                    when (selectedTab) {
                        "Чат" -> ChatScreen(
                            messages = messages,
                            draft = draft,
                            onDraftChange = { draft = it },
                            sending = sending,
                            speaking = speaking,
                            voiceError = voiceError,
                            onStopSpeech = { VoicePlayer.stop(); speaking = false },
                            onSend = {
                                val text = draft.trim()
                                if (text.isNotEmpty() && !sending) {
                                    messages.add(ChatMessage(text, true))
                                    draft = ""
                                    sending = true
                                    val history = messages.toList()
                                    scope.launch {
                                        val answer = withContext(Dispatchers.IO) {
                                            runCatching {
                                                AnyModelApi.sendMessage(
                                                    AppSettings.baseUrl(context),
                                                    AppSettings.apiKey(context),
                                                    AppSettings.model(context),
                                                    AppSettings.prompt(context) + MemoryStore.promptContext(context),
                                                    history
                                                )
                                            }.getOrElse {
                                                "Не удалось получить ответ: ${it.message ?: "неизвестная ошибка"}"
                                            }
                                        }
                                        messages.add(ChatMessage(answer, false))
                                        sending = false
                                        if (MemoryStore.autoEnabled(context) && AppSettings.apiKey(context).isNotBlank()) {
                                            val learned = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    AnyModelApi.extractMemories(
                                                        AppSettings.baseUrl(context),
                                                        AppSettings.apiKey(context),
                                                        AppSettings.model(context),
                                                        text,
                                                        answer
                                                    )
                                                }.getOrDefault(emptyList())
                                            }
                                            learned.forEach { MemoryStore.add(context, it) }
                                        }
                                        if (AppSettings.voiceEnabled(context) && answer.isNotBlank()) {
                                            val speech = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    AnyModelApi.synthesizeSpeech(
                                                        AppSettings.baseUrl(context),
                                                        AppSettings.apiKey(context),
                                                        AppSettings.voiceModel(context),
                                                        answer
                                                    )
                                                }
                                            }
                                            speech.onSuccess { audio ->
                                                if (AppSettings.voiceEnabled(context)) {
                                                    runCatching {
                                                        VoicePlayer.play(context, audio) { playing ->
                                                            speaking = playing
                                                            if (playing) voiceError = null
                                                        }
                                                    }.onFailure {
                                                        voiceError = "Не удалось воспроизвести аудио: ${it.message ?: "ошибка плеера"}"
                                                    }
                                                }
                                            }.onFailure {
                                                voiceError = "Не удалось озвучить ответ: ${it.message ?: "ошибка TTS"}"
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        "Память" -> MemoryScreen()
                        "Настройки" -> SettingsScreen(onVoiceDisabled = { speaking = false })
                    }
                }
            }

            NavigationBar(containerColor = Color(0xF5090815), contentColor = Lavender) {
                listOf("Чат", "Память", "Настройки").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Чат" -> Icons.Default.AutoAwesome
                                    "Память" -> Icons.Default.GraphicEq
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = tab
                            )
                        },
                        label = { Text(tab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Lavender,
                            selectedTextColor = Lavender,
                            indicatorColor = Color(0xFF39205F),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    messages: List<ChatMessage>,
    draft: String,
    onDraftChange: (String) -> Unit,
    sending: Boolean,
    speaking: Boolean,
    voiceError: String?,
    onStopSpeech: () -> Unit,
    onSend: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        WelcomeHeader()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { MessageBubble(it) }
        }
        if (sending) {
            Text(
                "Лисичка печатает…",
                color = Lavender,
                modifier = Modifier.padding(start = 22.dp, bottom = 6.dp)
            )
        }
        if (speaking) {
            OutlinedButton(
                onClick = onStopSpeech,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Остановить озвучку")
            }
        }
        voiceError?.let {
            Text(it, color = Color(0xFFFF8D8D), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        Composer(
            value = draft,
            onValueChange = onDraftChange,
            enabled = !sending,
            onSend = onSend
        )
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).background(
                Brush.linearGradient(listOf(NeonPink, Violet, ElectricBlue)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(R.drawable.fox_icon_foreground), contentDescription = "Лисичка", modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Лисичка ИИ", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Твой личный помощник", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Lavender)
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(132.dp).background(
                Brush.radialGradient(listOf(Color(0xFF8D4DFF), Color(0xFF263B88), Color.Transparent)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(R.drawable.fox_icon_foreground), contentDescription = "Лисичка", modifier = Modifier.size(100.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("ТВОЯ ЛИЧНАЯ ИИ-ПОДРУГА", color = Lavender, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Привет! Я Лисичка 💜", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Чем займёмся сегодня?", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("✦ Умная", "♡ Заботливая", "🐾 Рядом").forEach { tag ->
                Surface(color = Color(0x332E1D52), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder)) {
                    Text(tag, color = Lavender, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) Color(0xFF3A1D69) else Color(0xEE151326),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (message.fromUser) Color(0xFF8050D7) else Color(0xFF302747)),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = remember(message.text) { markdownText(message.text) },
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF211633), Color(0xFF101A32))), RoundedCornerShape(26.dp))
            .then(Modifier)
            .padding(start = 17.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Muted)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Lavender),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Напиши что-нибудь...", color = Muted, fontSize = 14.sp)
                inner()
            }
        )
        IconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier.size(42.dp).background(Violet, CircleShape)
        ) {
            Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color.White)
        }
    }
}

@Composable
private fun SettingsScreen(onVoiceDisabled: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var baseUrl by rememberSaveable { mutableStateOf(AppSettings.baseUrl(context)) }
    var apiKey by rememberSaveable { mutableStateOf(AppSettings.apiKey(context)) }
    var model by rememberSaveable { mutableStateOf(AppSettings.model(context)) }
    var prompt by rememberSaveable { mutableStateOf(AppSettings.prompt(context)) }
    var voiceEnabled by rememberSaveable { mutableStateOf(AppSettings.voiceEnabled(context)) }
    var voiceModel by rememberSaveable { mutableStateOf(AppSettings.voiceModel(context)) }
    var models by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable {
        mutableStateOf("Ключ хранится в зашифрованном хранилище телефона.")
    }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройки ✦", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Озвучивание ответов", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Говорить каждый ответ", color = Color.White, fontSize = 15.sp)
                Text("Лисичка автоматически озвучит новые ответы ИИ.", color = Muted, fontSize = 12.sp)
            }
            Switch(
                checked = voiceEnabled,
                onCheckedChange = {
                    voiceEnabled = it
                    AppSettings.putBoolean(context, "voice_enabled", it)
                    if (!it) { VoicePlayer.stop(); onVoiceDisabled() }
                }
            )
        }
        OutlinedTextField(
            value = voiceModel,
            onValueChange = { voiceModel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Модель голоса") },
            supportingText = { Text("Например: edge-tts/ru-RU-SvetlanaNeural") },
            singleLine = true
        )
        Text("Озвучивание использует тот же API-ключ AnyModel. При выключенном переключателе запросы синтеза не отправляются.", color = Muted, fontSize = 12.sp)
        Divider(color = PanelBorder)
        Text("Подключение к AnyModel", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Base URL") },
            singleLine = true
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API-ключ") },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showApiKey) "Скрыть ключ" else "Показать ключ",
                        tint = Lavender
                    )
                }
            }
        )
        Text(
            "Ключ не попадёт в исходный код приложения и сохраняется локально в зашифрованном виде.",
            color = Muted,
            fontSize = 12.sp
        )
        Button(
            enabled = !busy,
            onClick = {
                AppSettings.put(context, "base_url", baseUrl.trim())
                AppSettings.put(context, "api_key", apiKey.trim())
                busy = true
                status = "Проверяю соединение…"
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching { AnyModelApi.loadModels(baseUrl, apiKey) }
                    }
                    result.onSuccess {
                        models = ArrayList(it)
                        status = "Подключение успешно. Найдено моделей: ${it.size}."
                    }.onFailure {
                        status = "Ошибка подключения: ${it.message ?: "неизвестная ошибка"}"
                    }
                    busy = false
                }
            }
        ) {
            Text(if (busy) "Проверка…" else "Проверить и загрузить модели")
        }
        Text(
            status,
            color = if (status.startsWith("Ошибка")) Color(0xFFFF8D8D) else Muted,
            fontSize = 12.sp
        )

        Text("Модель", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ID модели (можно ввести вручную)") },
            singleLine = true
        )
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = models.isNotEmpty()) {
                Text("Выбрать из списка (${models.size})")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                models.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(id) },
                        onClick = {
                            model = id
                            AppSettings.put(context, "model", id)
                            expanded = false
                        }
                    )
                }
            }
        }
        Button(
            onClick = {
                AppSettings.put(context, "base_url", baseUrl.trim())
                AppSettings.put(context, "api_key", apiKey.trim())
                AppSettings.put(context, "model", model.trim())
                AppSettings.put(context, "system_prompt", prompt)
                AppSettings.put(context, "voice_model", voiceModel.trim())
                status = "Настройки сохранены."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить настройки")
        }

        Divider(color = Color(0xFF3A3048))
        Text("Системный промпт", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Выбери готовый профиль — его текст можно изменить ниже.", color = Muted, fontSize = 12.sp)
        PromptProfiles.profiles.forEach { (name, value) ->
            OutlinedButton(
                onClick = { prompt = value },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(name)
            }
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
            label = { Text("Твоя инструкция для Лисички") },
            minLines = 6
        )
        OutlinedButton(
            onClick = { prompt = PromptProfiles.defaultPrompt },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вернуть стандартный промпт")
        }
        Text(
            "Изменения промпта сохраняются кнопкой «Сохранить настройки».",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MemoryScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(MemoryStore.all(context)) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var autoSave by remember { mutableStateOf(MemoryStore.autoEnabled(context)) }
    var confirmClear by remember { mutableStateOf(false) }
    fun refresh() { entries = MemoryStore.all(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Память Лисички 🧠", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Эти записи доступны Лисичке в любом новом разговоре на этом телефоне. Они хранятся локально в зашифрованных настройках приложения.", color = Muted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Автоматически запоминать важное", color = Color.White, fontSize = 15.sp)
                Text("ИИ выделяет устойчивые факты из беседы. Пароли и чувствительные сведения исключаются.", color = Muted, fontSize = 12.sp)
            }
            Switch(checked = autoSave, onCheckedChange = {
                autoSave = it
                MemoryStore.setAutoEnabled(context, it)
            })
        }
        Divider(color = Color(0xFF3A3048))
        Text(if (editingId == null) "Добавить воспоминание" else "Изменить воспоминание", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Например: предпочитаю ответы на русском языке") }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = draft.isNotBlank(), onClick = {
                val id = editingId
                if (id == null) MemoryStore.add(context, draft) else MemoryStore.update(context, id, draft)
                draft = ""
                editingId = null
                refresh()
            }) { Text(if (editingId == null) "Сохранить" else "Обновить") }
            if (editingId != null) OutlinedButton(onClick = { draft = ""; editingId = null }) { Text("Отмена") }
        }
        Divider(color = Color(0xFF3A3048))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сохранённые записи · ${entries.size}", color = Lavender, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirmClear = true }, enabled = entries.isNotEmpty()) { Text("Очистить всё", color = Color(0xFFFF9999)) }
        }
        if (entries.isEmpty()) Text("Пока здесь пусто. Добавь факт вручную или включи автоматическое запоминание.", color = Muted, fontSize = 14.sp)
        else entries.forEach { item ->
            Surface(color = Color(0xEE151326), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.text, color = Color.White, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingId = item.id; draft = item.text }) { Text("Изменить") }
                        TextButton(onClick = { MemoryStore.delete(context, item.id); refresh() }) { Text("Удалить", color = Color(0xFFFF9999)) }
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Очистить всю память?") },
        text = { Text("Все сохранённые воспоминания будут удалены с этого телефона.") },
        confirmButton = { TextButton(onClick = {
            MemoryStore.clear(context); refresh(); confirmClear = false; draft = ""; editingId = null
        }) { Text("Удалить всё", color = Color(0xFFFF9999)) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } }
    )
}

private fun markdownText(source: String) = buildAnnotatedString {
    val token = Regex("""(\*\*.+?\*\*|~~.+?~~|\*[^*]+?\*|`[^`]+`)""")
    source.lines().forEachIndexed { lineIndex, original ->
        val line = original.trimStart()
        val heading = line.startsWith("#")
        val bullet = line.startsWith("- ") || line.startsWith("* ")
        val content = when {
            heading -> line.dropWhile { it == '#' }.trimStart()
            bullet -> "•  " + line.drop(2)
            else -> original
        }
        if (lineIndex > 0) append("\n")
        if (heading) withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Lavender)) { append(content) }
        else {
            var cursor = 0
            token.findAll(content).forEach { match ->
                append(content.substring(cursor, match.range.first))
                val value = match.value
                when {
                    value.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(value.removeSurrounding("**")) }
                    value.startsWith("~~") -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(value.removeSurrounding("~~")) }
                    value.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF30263D), color = Lavender)) { append(value.removeSurrounding("`")) }
                    value.startsWith("*") -> withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(value.removeSurrounding("*")) }
                }
                cursor = match.range.last + 1
            }
            append(content.substring(cursor))
        }
    }
}
