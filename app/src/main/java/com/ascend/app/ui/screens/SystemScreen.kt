package com.ascend.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.SystemMessageEntity
import com.ascend.app.domain.MotivationLibrary
import com.ascend.app.domain.SystemTone
import com.ascend.app.ui.components.AngularShape
import com.ascend.app.ui.components.AscendCard
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun SystemScreen(
    state: DashboardState,
    messages: List<SystemMessageEntity>,
    onSend: (String, SystemTone) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var toneName by rememberSaveable { mutableStateOf(SystemTone.DIRECT.name) }
    val tone = SystemTone.valueOf(toneName)
    val listState = rememberLazyListState()
    val isThinking = messages.lastOrNull()?.role == "PLAYER"
    val newestSystemId = messages.lastOrNull { it.role == "SYSTEM" }?.id
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
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.SmartToy, null, tint = EnergyCyan, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("ASCEND SYSTEM", style = MaterialTheme.typography.headlineMedium)
                Text("LEVEL ${state.level.level} • CONTEXT ENGINE ONLINE", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
            }
            IconButton(onClick = onClear, enabled = !isThinking) {
                Icon(Icons.Outlined.DeleteSweep, "Clear system conversation", tint = TextSecondary)
            }
        }

        AscendCard(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), accent = EnergyAmber) {
            Text("GUIDANCE CORE • NOT MEDICAL CARE", style = MaterialTheme.typography.labelMedium, color = EnergyAmber)
            Text(
                "SYSTEM uses Firebase AI Logic when configured and a private on-device fallback otherwise. AI mode sends your message and compact ASCEND context to Firebase. It can challenge you, but cannot replace a qualified professional.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text("VOICE PROTOCOL", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SystemTone.entries.forEach { option ->
                    FilterChip(
                        selected = tone == option,
                        onClick = { toneName = option.name },
                        label = { Text(option.displayName, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f),
                        shape = AngularShape,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf("SCAN MY DAY", "NEXT WORKOUT", "MOTIVATE ME").forEach { prompt ->
                AssistChip(
                    onClick = { submit(prompt) },
                    enabled = !isThinking,
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.weight(1f),
                    shape = AngularShape,
                )
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) item {
                AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                    Text("SYSTEM ONLINE", style = MaterialTheme.typography.titleLarge, color = EnergyCyan)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Transmit a goal, obstacle, feeling, or question. I understand workouts, schedules, nutrition, recovery, progress, plateaus, app actions, and motivation.",
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("“${MotivationLibrary.quoteFor(LocalDate.now())}”", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(messages, key = { it.id }) { message ->
                val player = message.role == "PLAYER"
                val shouldAnimate = !player && message.id == newestSystemId && message.id != lastRenderedSystemId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (player) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.fillMaxWidth(if (player) .84f else .95f)
                            .background(if (player) EnergyViolet.copy(.24f) else RaisedSurface, AngularShape)
                            .padding(14.dp),
                    ) {
                        Text(
                            if (player) "PLAYER // TRANSMISSION" else "SYSTEM // RESPONSE STREAM",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (player) EnergyViolet else EnergyCyan,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (player) {
                            Text(message.message, style = MaterialTheme.typography.bodyLarge)
                        } else {
                            RobotTypewriterText(message.message, shouldAnimate) { lastRenderedSystemId = message.id }
                        }
                    }
                }
            }
            if (isThinking) item(key = "system_processing") { SystemProcessingBubble() }
        }

        Surface(color = DeepSurface, tonalElevation = 0.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(500) },
                    modifier = Modifier.weight(1f),
                    enabled = !isThinking,
                    maxLines = 4,
                    placeholder = { Text(if (isThinking) "SYSTEM is calculating…" else "Transmit to SYSTEM…") },
                    shape = AngularShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { submit() },
                    enabled = input.isNotBlank() && !isThinking,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(4.dp),
                ) { Icon(Icons.AutoMirrored.Outlined.Send, "Transmit message") }
            }
        }
    }
}

@Composable
private fun RobotTypewriterText(text: String, animate: Boolean, onComplete: () -> Unit) {
    var visibleCharacters by remember(text, animate) { mutableIntStateOf(if (animate) 0 else text.length) }
    val currentOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(text, animate) {
        if (!animate) return@LaunchedEffect
        delay(220)
        while (visibleCharacters < text.length) {
            val current = text.getOrNull(visibleCharacters)
            visibleCharacters = (visibleCharacters + if (text.length > 280) 2 else 1).coerceAtMost(text.length)
            delay(if (current in listOf('.', '!', '?', '\n')) 65 else 15)
        }
        currentOnComplete()
    }

    Text(
        text = text.take(visibleCharacters) + if (animate && visibleCharacters < text.length) " ▋" else "",
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    )
}

@Composable
private fun SystemProcessingBubble() {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(180)
            frame = (frame + 1) % 4
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(Modifier.fillMaxWidth(.72f).background(RaisedSurface, AngularShape).padding(14.dp)) {
            Text("SYSTEM // ANALYZING SIGNAL", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
            Spacer(Modifier.height(7.dp))
            Text("DECRYPTING" + "█".repeat(frame) + "░".repeat(3 - frame), fontFamily = FontFamily.Monospace, color = TextSecondary)
        }
    }
}
