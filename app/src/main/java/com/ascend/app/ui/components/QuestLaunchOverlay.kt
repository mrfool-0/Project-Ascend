package com.ascend.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.app.ui.theme.*

/** A finite signal-lock transition. Only small text offsets glitch; the screen never strobes. */
@Composable
fun QuestLaunchOverlay(title: String, onAnimationComplete: () -> Unit) {
    val motion = LocalMotionEnabled.current
    val timeline = remember { Animatable(0f) }
    val complete by rememberUpdatedState(onAnimationComplete)
    LaunchedEffect(motion) {
        if (motion) timeline.animateTo(1f, tween(AscendMotion.QuestEntry, easing = LinearEasing))
        else timeline.snapTo(1f)
        complete()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier.fillMaxSize().background(Void)
                .clearAndSetSemantics { contentDescription = "Entering quest. $title. Preparing your session." },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(EnergyViolet.copy(.16f), Color.Transparent), center, size.width * .9f))
                // Sparse, low-contrast static grid. No random noise or infinite loops.
                val grid = 48.dp.toPx()
                for (x in 0..(size.width / grid).toInt()) drawLine(Hairline.copy(.24f), Offset(x * grid, 0f), Offset(x * grid, size.height))
                for (y in 0..(size.height / grid).toInt()) drawLine(Hairline.copy(.24f), Offset(0f, y * grid), Offset(size.width, y * grid))
                if (motion && timeline.value < 1f) {
                    val y = size.height * timeline.value
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, EnergyCyan.copy(.08f), Color.Transparent), y - 32.dp.toPx(), y + 32.dp.toPx()), Offset(0f, y - 32.dp.toPx()), Size(size.width, 64.dp.toPx()))
                }
            }
            Column(
                Modifier.safeDrawingPadding().padding(28.dp).widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusPill("Ascend / quest link", EnergyCyan)
                Spacer(Modifier.height(32.dp))
                SystemAvatar(size = 80.dp)
                Spacer(Modifier.height(28.dp))
                Box(contentAlignment = Alignment.Center) {
                    fun offset(): Float {
                        if (!motion) return 0f
                        val t = timeline.value
                        // Two isolated chromatic displacements, then a clean, stable lock.
                        return when {
                            t in .12f.. .16f -> -3f
                            t in .16f.. .20f -> 4f
                            t in .20f.. .24f -> -1f
                            t in .48f.. .52f -> 2f
                            t in .52f.. .56f -> -2f
                            t in .56f.. .60f -> 1f
                            else -> 0f
                        }
                    }
                    listOf(EnergyCyan to 1f, EnergyViolet to -1f, TextPrimary to 0f).forEach { (color, direction) ->
                        Text(
                            "ENTERING\nQUEST",
                            Modifier.graphicsLayer {
                                translationX = offset().dp.toPx() * if (direction == 0f) .7f else direction * 1.6f
                                alpha = if (direction == 0f) 1f else .55f
                            },
                            style = MaterialTheme.typography.displayMedium,
                            color = color,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(30.dp))
                Canvas(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50))) {
                    drawRect(Hairline)
                    drawRect(Brush.horizontalGradient(listOf(EnergyViolet, EnergyCyan)), size = Size(size.width * timeline.value, size.height))
                }
                Spacer(Modifier.height(12.dp))
                Text("FOCUS IN. SHOW UP.", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
            }
        }
    }
}
