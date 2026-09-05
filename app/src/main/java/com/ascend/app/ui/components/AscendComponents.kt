package com.ascend.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.LevelProgress
import com.ascend.app.ui.theme.*
import kotlin.math.min

val AngularShape = GenericShape { size, _ ->
    val cut = min(size.width, size.height) * .10f
    moveTo(cut, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height); lineTo(0f, size.height); lineTo(0f, cut); close()
}

@Composable
fun AscendCard(
    modifier: Modifier = Modifier,
    accent: Color = EnergyViolet,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .then(interaction)
            .drawBehind {
                if (highlighted) drawCircle(accent.copy(alpha = .10f), radius = size.maxDimension * .7f, center = Offset(size.width * .8f, 0f))
            }
            .background(
                Brush.linearGradient(listOf(RaisedSurface.copy(alpha = .96f), DeepSurface.copy(alpha = .94f))),
                AngularShape,
            )
            .border(1.dp, if (highlighted) accent.copy(.65f) else Hairline, AngularShape)
            .padding(18.dp)
            .animateContentSize(),
        content = content,
    )
}

@Composable
@SuppressLint("ModifierParameter")
fun SectionHeader(title: String, meta: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).background(EnergyViolet))
        Spacer(Modifier.width(9.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (meta != null) Text(meta.uppercase(), style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
    }
}

@Composable
fun SystemButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = AngularShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) RaisedSurface else EnergyViolet,
            contentColor = TextPrimary,
            disabledContainerColor = Hairline,
        ),
    ) { Text(text.uppercase(), style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun XpProgressBar(progress: LevelProgress, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.fraction, tween(800), label = "xp")
    Column(modifier.semantics { contentDescription = "${progress.xpIntoLevel} of ${progress.xpForNextLevel} experience points" }) {
        Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(2.dp)).background(Hairline)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(animated)
                    .background(Brush.horizontalGradient(listOf(EnergyViolet, EnergyCyan)))
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            if (progress.isMaxLevel) "MAX LEVEL" else "${progress.xpIntoLevel.format()} / ${progress.xpForNextLevel.format()} XP",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
fun RankBadge(level: Int, rank: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(36.dp).semantics { contentDescription = "$rank rank badge" }) {
            val p = Path().apply {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height * .32f)
                lineTo(size.width * .82f, size.height); lineTo(size.width * .18f, size.height)
                lineTo(0f, size.height * .32f); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(EnergyCyan, EnergyViolet)), style = Stroke(width = 2.5f))
            drawCircle(EnergyViolet.copy(.45f), radius = size.minDimension * (.12f + level / 1000f))
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text("RANK", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(rank, style = MaterialTheme.typography.titleMedium, color = EnergyCyan)
        }
    }
}

@Composable
fun MacroProgressBar(label: String, current: Int, target: Int, unit: String, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, tween(650), label = label)
    Column(modifier.semantics { contentDescription = "$label $current of $target $unit" }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Text("$current", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(" / $target $unit", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Hairline,
            strokeCap = StrokeCap.Square,
        )
    }
}

@Composable
fun MacroRing(label: String, current: Int, target: Int, color: Color, modifier: Modifier = Modifier, unit: String = "G") {
    val fraction = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, tween(800), label = label)
    Box(modifier.aspectRatio(1f).semantics { contentDescription = "$label $current of $target $unit" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * .075f
            val inset = stroke / 2
            drawArc(Hairline, -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            drawArc(color, -90f, animated * 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(current.toString(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StreakIndicator(current: Int, longest: Int, modifier: Modifier = Modifier) {
    val color = when {
        current >= 30 -> EnergyViolet
        current >= 7 -> EnergyAmber
        else -> EnergyCyan
    }
    Row(modifier.semantics { contentDescription = "$current day current streak, $longest day longest streak" }, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(34.dp)) {
            val p = Path().apply {
                moveTo(size.width * .5f, 0f); cubicTo(size.width * .9f, size.height * .35f, size.width, size.height * .7f, size.width * .5f, size.height)
                cubicTo(0f, size.height * .7f, size.width * .15f, size.height * .35f, size.width * .44f, size.height * .16f); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(color, EnergyViolet)))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("$current DAY STREAK", style = MaterialTheme.typography.titleMedium, color = color)
            Text("LONGEST $longest DAYS", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    AscendCard(modifier.fillMaxWidth()) {
        Text(title.uppercase(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        SystemButton(action, onAction, Modifier.fillMaxWidth(), secondary = true)
    }
}

private fun Int.format() = "% ,d".format(this).trim()
