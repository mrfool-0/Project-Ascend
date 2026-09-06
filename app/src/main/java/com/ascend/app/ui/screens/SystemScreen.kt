package com.ascend.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.SystemMessageEntity
import com.ascend.app.domain.MotivationLibrary
import com.ascend.app.domain.SystemTone
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(
    state: DashboardState,
    messages: List<SystemMessageEntity>,
    onSend: (String, SystemTone) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var toneName by rememberSaveable { mutableStateOf(SystemTone.DIRECT.name) }
    var showInfo by remember { mutableStateOf(false) }
    val tone = SystemTone.valueOf(toneName)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isThinking = messages.lastOrNull()?.role == "PLAYER"
    val newestSystemId = messages.lastOrNull { it.role != "PLAYER" }?.id
    var lastRenderedSystemId by rememberSaveable { mutableStateOf(newestSystemId) }

    fun submit(text: String = input) {
        val message = text.trim()
        if (message.isNotEmpty() && !isThinking) {
            onSend(message, tone)
            input = ""
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex + if (isThinking) 1 else 0)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SystemAvatar(size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("SYSTEM", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(3.dp))
                StatusPill(if (isThinking) "Thinking" else "Context online", if (isThinking) EnergyAmber else EnergyEmerald)
            }
            IconButton(onClick = { showInfo = true }) { Icon(Icons.Outlined.Info, "About SYSTEM", tint = TextSecondary) }
            IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteOutline, "Clear conversation", tint = TextSecondary) }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            SystemTone.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = tone == option,
                    onClick = { toneName = option.name },
                    shape = SegmentedButtonDefaults.itemShape(index, SystemTone.entries.size),
                    icon = {},
                    label = { Text(option.displayName.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Scan my day",
                "What should I train?",
                "Motivate me",
                "Create a hydration quest",
                "Add a daily reading habit",
            ).forEach { prompt ->
                SuggestionChip(prompt, !isThinking) { submit(prompt) }
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (messages.isEmpty()) item {
                AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan, highlighted = true) {
                    Text("Tactical system, ready", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Ask about your plan, training, food, recovery or consistency. SYSTEM can also create a habit or daily quest when you explicitly ask.",
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("“${MotivationLibrary.quoteFor(LocalDate.now())}”", style = MaterialTheme.typography.bodyLarge, color = EnergyCyan)
                }
            }
            items(messages, key = { it.id }) { message ->
                val player = message.role == "PLAYER"
                val cleaned = message.message.toDisplayText()
                val shouldAnimate = !player && message.id == newestSystemId && message.id != lastRenderedSystemId
                MessageBubble(message.role, cleaned, player, shouldAnimate) { lastRenderedSystemId = message.id }
            }
            if (isThinking) item(key = "system_processing") { SystemProcessingBubble() }
        }

        Surface(color = GlassSurface, shadowElevation = 12.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(500) },
                    modifier = Modifier.weight(1f),
                    enabled = !isThinking,
                    maxLines = 4,
                    placeholder = { Text(if (isThinking) "SYSTEM is thinking…" else "Message SYSTEM") },
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EnergyViolet,
                        unfocusedBorderColor = Hairline,
                        focusedContainerColor = DeepSurface,
                        unfocusedContainerColor = DeepSurface,
                    ),
                )
                Spacer(Modifier.width(9.dp))
                FilledIconButton(
                    onClick = { submit() },
                    enabled = input.isNotBlank() && !isThinking,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send message") }
            }
        }
    }

    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }, containerColor = DeepSurface) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SystemAvatar(size = 50.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("About SYSTEM", style = MaterialTheme.typography.headlineMedium)
                        Text("Fitness, nutrition and behavior support", color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "SYSTEM adapts its coaching tone to the behavior you log. It uses direct, transparent techniques such as reflective questions, implementation intentions and small next actions—never hidden coercion.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "When Firebase AI Logic is available, your message and compact ASCEND context are sent for processing. A private on-device response engine remains available as fallback. SYSTEM is guidance, not medical care.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(18.dp))
                SystemButton("Got it", { showInfo = false }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(text, style = MaterialTheme.typography.bodySmall) },
        shape = RoundedCornerShape(14.dp),
        colors = AssistChipDefaults.assistChipColors(containerColor = RaisedSurface.copy(.72f)),
        border = AssistChipDefaults.assistChipBorder(enabled = enabled, borderColor = Hairline),
    )
}

@Composable
private fun MessageBubble(role: String, text: String, player: Boolean, animate: Boolean, onComplete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (player) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!player) {
            SystemAvatar(size = 30.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier
                .fillMaxWidth(if (player) .84f else .88f)
                .clip(
                    if (player) RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp)
                    else RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
                )
                .background(if (player) EnergyViolet.copy(.2f) else RaisedSurface.copy(.88f))
                .padding(horizontal = 15.dp, vertical = 13.dp),
        ) {
            Text(
                when (role) {
                    "PLAYER" -> "YOU"
                    "SYSTEM_AI" -> "SYSTEM · AI"
                    "SYSTEM_LOCAL" -> "SYSTEM · ON DEVICE"
                    "SYSTEM_SAFETY" -> "SYSTEM · SAFETY"
                    else -> "SYSTEM"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (player) EnergyViolet else EnergyCyan,
            )
            Spacer(Modifier.height(5.dp))
            if (player) Text(text, style = MaterialTheme.typography.bodyLarge)
            else RobotTypewriterText(text, animate, onComplete)
        }
    }
}

@Composable
private fun RobotTypewriterText(text: String, animate: Boolean, onComplete: () -> Unit) {
    var visibleCharacters by remember(text, animate) { mutableIntStateOf(if (animate) 0 else text.length) }
    val currentOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(text, animate) {
        if (!animate) return@LaunchedEffect
        delay(160)
        while (visibleCharacters < text.length) {
            visibleCharacters = (visibleCharacters + if (text.length > 360) 3 else 2).coerceAtMost(text.length)
            delay(if (text.getOrNull(visibleCharacters - 1) in listOf('.', '!', '?', '\n')) 45 else 12)
        }
        currentOnComplete()
    }

    Row(Modifier.semantics { contentDescription = text }) {
        Text(text.take(visibleCharacters), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
        if (animate && visibleCharacters < text.length) Text("▍", style = MaterialTheme.typography.bodyLarge, color = EnergyCyan)
    }
}

@Composable
private fun SystemProcessingBubble() {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(320)
            frame = (frame + 1) % 4
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SystemAvatar(size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp)).background(RaisedSurface).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Thinking", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = frame,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                label = "thinking dots",
            ) { dots -> Text("•".repeat(dots + 1), style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
        }
    }
}

private fun String.toDisplayText(): String =
    replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .lineSequence()
        .joinToString("\n") { line -> line.replace(Regex("^#{1,4}\\s*"), "").replace(Regex("^[-*]\\s+"), "• ") }
        .trim()
