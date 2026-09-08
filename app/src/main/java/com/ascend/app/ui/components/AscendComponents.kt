package com.ascend.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.ascend.app.R
import com.ascend.app.domain.LevelProgress
import com.ascend.app.ui.theme.*
import coil3.compose.AsyncImage
import java.io.File
val AngularShape: Shape = RoundedCornerShape(18.dp)
val CompactShape: Shape = RoundedCornerShape(13.dp)
val PillShape: Shape = RoundedCornerShape(50)

@Composable
fun SystemAvatar(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Image(
        painter = painterResource(R.drawable.system_hooded_avatar),
        contentDescription = "ASCEND SYSTEM hooded avatar",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(size * .28f), ambientColor = EnergyCyan.copy(.22f), spotColor = EnergyViolet.copy(.35f))
            .clip(RoundedCornerShape(size * .28f))
            .border(.75.dp, EnergyCyan.copy(alpha = .58f), RoundedCornerShape(size * .28f)),
    )
}

@Composable
fun PlayerAvatar(imagePath: String?, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = EnergyViolet.copy(.2f), spotColor = EnergyViolet.copy(.25f))
            .clip(CircleShape)
            .background(DeepSurface)
            .border(1.dp, EnergyViolet.copy(alpha = .6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Player profile image",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_ascend_mark),
                error = painterResource(R.drawable.ic_ascend_mark),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_ascend_mark),
                contentDescription = "Default player profile image",
                modifier = Modifier.fillMaxSize(.62f),
            )
        }
    }
}

@Composable
fun AscendCard(
    modifier: Modifier = Modifier,
    accent: Color = EnergyViolet,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (pressed && motion) .985f else 1f, AscendMotion.spatial(), label = "card press")
    val interaction = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = ripple(), onClick = onClick)
    } else Modifier
    Column(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(
                elevation = if (highlighted) 12.dp else 3.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = if (highlighted) .16f else .04f),
                spotColor = accent.copy(alpha = if (highlighted) .18f else .04f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        if (highlighted) RaisedSurface.copy(alpha = .98f) else RaisedSurface.copy(alpha = .84f),
                        DeepSurface.copy(alpha = .96f),
                    ),
                ),
            )
            .drawBehind {
                if (highlighted) {
                    drawRect(Brush.radialGradient(listOf(accent.copy(.12f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * .95f))
                }
            }
            .border(.75.dp, if (highlighted) accent.copy(.3f) else Hairline.copy(.82f), shape)
            .then(interaction)
            .padding(20.dp),
        content = content,
    )
}

@Composable
@SuppressLint("ModifierParameter")
fun SectionHeader(title: String, meta: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.width(8.dp))
        if (meta != null) StatusPill(meta, EnergyCyan)
    }
}

@Composable
fun SystemButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (pressed && motion) .97f else 1f, AscendMotion.spatial(), label = "button press")
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        interactionSource = source,
        enabled = enabled,
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) RaisedSurface else EnergyViolet,
            contentColor = if (secondary) TextPrimary else Void,
            disabledContainerColor = Hairline,
        ),
        border = if (secondary) androidx.compose.foundation.BorderStroke(.75.dp, Hairline) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (secondary) 0.dp else 3.dp, pressedElevation = 0.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(PillShape)
            .background(color.copy(alpha = .11f))
            .border(.75.dp, color.copy(alpha = .24f), PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextPrimary,
    supporting: String? = null,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Void.copy(alpha = .42f))
            .border(.75.dp, Hairline.copy(.72f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        supporting?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun XpProgressBar(progress: LevelProgress, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.fraction, tween(800), label = "xp")
    Column(modifier.semantics { contentDescription = "${progress.xpIntoLevel} of ${progress.xpForNextLevel} experience points" }) {
        Box(Modifier.fillMaxWidth().height(7.dp).clip(PillShape).background(Hairline)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(animated)
                    .clip(PillShape)
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
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape),
            color = color,
            trackColor = Hairline,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
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
