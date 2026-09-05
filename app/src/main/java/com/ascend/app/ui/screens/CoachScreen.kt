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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.CoachMessageEntity
import com.ascend.app.domain.MotivationLibrary
import com.ascend.app.ui.components.AngularShape
import com.ascend.app.ui.components.AscendCard
import com.ascend.app.ui.theme.*
import java.time.LocalDate

@Composable
fun CoachScreen(
    state: DashboardState,
    messages: List<CoachMessageEntity>,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    fun submit() {
        val message = input.trim()
        if (message.isNotEmpty()) { onSend(message); input = "" }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SYSTEM COACH", style = MaterialTheme.typography.headlineMedium)
                Text("LEVEL ${state.level.level} • LOCAL ADAPTIVE GUIDANCE CORE", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
            }
            IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteSweep, "Clear coach conversation", tint = TextSecondary) }
        }
        AscendCard(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), accent = EnergyAmber) {
            Text("COACHING, NOT MEDICAL CARE", style = MaterialTheme.typography.labelMedium, color = EnergyAmber)
            Text("ASCEND uses your logged plan and nutrition data to suggest next actions. It cannot diagnose, prescribe, or replace a qualified professional.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf("TODAY'S PLAN", "PROTEIN", "MOTIVATE ME").forEach { prompt ->
                AssistChip(onClick = { onSend(prompt) }, label = { Text(prompt, style = MaterialTheme.typography.labelMedium) }, modifier = Modifier.weight(1f), shape = AngularShape)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) item {
                AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                    Text("COACH CORE ONLINE", style = MaterialTheme.typography.titleLarge, color = EnergyCyan)
                    Spacer(Modifier.height(7.dp))
                    Text("Ask about your workout, calories, protein, hydration, progress, pain, recovery, or motivation.", color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text("“${MotivationLibrary.quoteFor(LocalDate.now())}”", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(messages, key = { it.id }) { message ->
                val player = message.role == "PLAYER"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (player) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.fillMaxWidth(if (player) .84f else .94f)
                            .background(if (player) EnergyViolet.copy(.24f) else RaisedSurface, AngularShape)
                            .padding(14.dp),
                    ) {
                        Text(if (player) "PLAYER" else "ASCEND SYSTEM", style = MaterialTheme.typography.labelMedium, color = if (player) EnergyViolet else EnergyCyan)
                        Spacer(Modifier.height(5.dp)); Text(message.message, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Surface(color = DeepSurface, tonalElevation = 0.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    input, { input = it.take(500) }, Modifier.weight(1f), maxLines = 4,
                    placeholder = { Text("Ask the system…") }, shape = AngularShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { submit() }),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.size(54.dp), shape = RoundedCornerShape(4.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, "Send message")
                }
            }
        }
    }
}
