package com.ascend.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.EnergyCrimson
import com.ascend.app.ui.theme.EnergyCyan
import com.ascend.app.ui.theme.EnergyViolet
import com.ascend.app.ui.theme.Hairline
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary
import com.ascend.app.ui.theme.Void

@Composable
fun QuestLaunchOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(70)),
        exit = fadeOut(tween(180)),
    ) {
        val transition = rememberInfiniteTransition(label = "quest_glitch")
        val shake by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 380
                    0f at 0
                    -3f at 34
                    2f at 63
                    -1.5f at 101
                    2.5f at 143
                    -1f at 191
                    1.5f at 247
                    0f at 310
                    0f at 380
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "screen_shake",
        )
        val scan by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing)),
            label = "scanline",
        )
        val flicker by transition.animateFloat(
            initialValue = .86f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(220), RepeatMode.Reverse),
            label = "signal_flicker",
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(Void.copy(alpha = .96f))
                .clearAndSetSemantics { contentDescription = "Entering quest. Initializing workout protocol." },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val gap = 14.dp.toPx()
                var y = -gap + scan * gap
                while (y < size.height) {
                    drawLine(EnergyCyan.copy(alpha = .055f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
                    y += gap
                }
                val bandY = size.height * scan
                drawRect(EnergyViolet.copy(alpha = .16f), topLeft = androidx.compose.ui.geometry.Offset(0f, bandY), size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx()))
                drawRect(EnergyCyan.copy(alpha = .13f), topLeft = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .29f), size = androidx.compose.ui.geometry.Size(size.width * (.20f + scan * .25f), 2.dp.toPx()))
                drawRect(EnergyCrimson.copy(alpha = .12f), topLeft = androidx.compose.ui.geometry.Offset(size.width * (.62f - scan * .18f), size.height * .66f), size = androidx.compose.ui.geometry.Size(size.width * .30f, 3.dp.toPx()))
            }

            Column(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .graphicsLayer { translationX = shake },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "> QUEST LINK REQUESTED",
                    style = MaterialTheme.typography.labelLarge,
                    color = EnergyCyan.copy(alpha = flicker),
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.Center) {
                    Text("ENTERING QUEST", Modifier.offset(x = (-3).dp, y = 1.dp), style = MaterialTheme.typography.displayMedium, color = EnergyCrimson.copy(alpha = .58f), textAlign = TextAlign.Center)
                    Text("ENTERING QUEST", Modifier.offset(x = 3.dp, y = (-1).dp), style = MaterialTheme.typography.displayMedium, color = EnergyCyan.copy(alpha = .62f), textAlign = TextAlign.Center)
                    Text("ENTERING QUEST", style = MaterialTheme.typography.displayMedium, color = TextPrimary.copy(alpha = flicker), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    "INJECTING PLAYER PROTOCOL // ${if (scan < .34f) "AUTH" else if (scan < .68f) "MAP" else "READY"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { scan },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = EnergyCyan,
                    trackColor = Hairline,
                )
                Spacer(Modifier.height(11.dp))
                Text("DO NOT BREAK THE SIGNAL", style = MaterialTheme.typography.labelMedium, color = EnergyViolet)
            }
        }
    }
}
