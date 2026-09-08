package com.ascend.app.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** One motion vocabulary. Decorative motion follows Android's Remove animations setting. */
object AscendMotion {
    const val Enter = 360
    const val Exit = 180
    const val QuestEntry = 1_160
    fun <T> spatial() = spring<T>(dampingRatio = .86f, stiffness = 420f)
}

val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun rememberSystemMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun enabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var motion by remember(resolver) { mutableStateOf(enabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { motion = enabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return motion
}

/** Entry only, not a scroll effect. Saveable state prevents replaying on tab restoration. */
fun Modifier.reveal(index: Int = 0): Modifier = composed {
    val motion = LocalMotionEnabled.current
    var revealed by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (revealed || !motion) 1f else 0f) }
    LaunchedEffect(motion) {
        if (!motion || revealed) progress.snapTo(1f)
        else {
            revealed = true
            progress.animateTo(1f, tween(AscendMotion.Enter, index.coerceIn(0, 4) * 45, FastOutSlowInEasing))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = 12.dp.toPx() * (1f - progress.value)
    }
}
