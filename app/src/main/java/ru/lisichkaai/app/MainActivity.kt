package ru.lisichkaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B0912)
private val Panel = Color(0xFF171321)
private val Violet = Color(0xFF9B72F2)
private val Lavender = Color(0xFFC7B2FF)
private val Muted = Color(0xFF9B93AA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LisichkaApp() }
    }
}

private data class ChatMessage(val text: String, val fromUser: Boolean)

@Composable
private fun LisichkaApp() {
    var draft by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Чат") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Lavender,
            background = Ink,
            surface = Panel
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF171024), Ink, Color(0xFF09080D)))
            )
        ) {
            TopBar(onSettings = { selectedTab = "Настройки" })
            if (selectedTab == "Чат") {
                WelcomeHeader()
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message -> MessageBubble(message) }
                }
                Composer(
                    value = draft,
                    onValueChange = { draft = it },
                    onSend = {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(draft.trim(), true))
                            draft = ""
                        }
                    }
                )
            } else {
                SettingsScreen()
                Spacer(Modifier.weight(1f))
            }
            NavigationBar(
                containerColor = Color(0xF014101D),
                contentColor = Lavender
            ) {
                listOf("Чат", "Память", "Настройки").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
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
                            indicatorColor = Color(0xFF35264F),
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
private fun TopBar(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(
                Brush.linearGradient(listOf(Color(0xFF8D5CE6), Color(0xFF39235E))),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) { Text("🦊", fontSize = 23.sp) }
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
            Modifier.size(116.dp).background(
                Brush.radialGradient(listOf(Color(0xFF6746A0), Color(0xFF241A36), Color.Transparent)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) { Text("🦊", fontSize = 62.sp) }
        Spacer(Modifier.height(8.dp))
        Text("Привет! Я Лисичка 💜", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Text("Чем займёмся сегодня?", color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (message.fromUser) Color(0xFF49316D) else Panel,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(message.text, Modifier.padding(horizontal = 15.dp, vertical = 12.dp), color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Composer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp)
            .background(Color(0xFF211A2D), RoundedCornerShape(26.dp))
            .padding(start = 17.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Muted)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Lavender),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Напиши что-нибудь...", color = Muted, fontSize = 14.sp)
                inner()
            }
        )
        IconButton(onClick = onSend, modifier = Modifier.size(42.dp).background(Violet, CircleShape)) {
            Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color.White)
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Настройки", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Подключение искусственного интеллекта", color = Muted, fontSize = 13.sp)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("AnyModel", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Text("Подключение API появится в следующем этапе разработки.", color = Muted, fontSize = 13.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Внешний вид", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Тёмно-фиолетовая тема", color = Muted, fontSize = 13.sp)
            }
        }
    }
}
