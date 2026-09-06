package com.ascend.app.ui.components

import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.ascend.app.R
import com.ascend.app.ui.theme.DeepSurface
import com.ascend.app.ui.theme.EnergyCyan
import com.ascend.app.ui.theme.EnergyEmerald
import com.ascend.app.ui.theme.EnergyViolet
import com.ascend.app.ui.theme.Hairline
import com.ascend.app.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.sin

private enum class MotionPattern { SQUAT, PUSH, PULL, HINGE, CORE, CURL, RAISE, CARDIO, NEUTRAL }

private data class MotionSpec(val pattern: MotionPattern, val cue: String)

@Composable
fun ExerciseMotionDemo(exerciseName: String, muscleGroup: String, modifier: Modifier = Modifier) {
    val spec = remember(exerciseName, muscleGroup) { motionSpec(exerciseName, muscleGroup) }
    if (spec.pattern == MotionPattern.SQUAT) {
        PrebakedExerciseMotion(exerciseName, spec.cue, modifier)
        return
    }
    val transition = rememberInfiniteTransition(label = "${exerciseName}_motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "form_phase",
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(DeepSurface.copy(alpha = .82f), AngularShape)
            .border(1.dp, Hairline, AngularShape)
            .semantics { contentDescription = "Animated movement cue for $exerciseName. ${spec.cue}" },
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
            val cyan = EnergyCyan
            val violet = EnergyViolet.copy(alpha = .78f)
            val ghost = EnergyCyan.copy(alpha = .16f)
            val stroke = 4.dp.toPx()
            fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
            fun limb(a: Offset, b: Offset, color: androidx.compose.ui.graphics.Color = cyan, width: Float = stroke) =
                drawLine(color, a, b, width, StrokeCap.Round)
            fun joint(at: Offset, color: androidx.compose.ui.graphics.Color = violet) =
                drawCircle(color, stroke * .72f, at)
            fun head(at: Offset, color: androidx.compose.ui.graphics.Color = cyan) =
                drawCircle(color, stroke * 2.15f, at)

            drawLine(Hairline, point(.05f, .86f), point(.95f, .86f), 1.dp.toPx())
            for (index in 0..5) {
                val x = .10f + index * .16f
                drawLine(ghost, point(x, .12f), point(x, .82f), 1.dp.toPx())
            }

            when (spec.pattern) {
                MotionPattern.SQUAT -> {
                    val drop = phase * .16f
                    val head = point(.50f, .19f + drop)
                    val shoulder = point(.50f, .31f + drop)
                    val hip = point(.50f, .51f + drop)
                    val leftKnee = point(.38f - phase * .04f, .68f + drop * .55f)
                    val rightKnee = point(.62f + phase * .04f, .68f + drop * .55f)
                    head(head); limb(shoulder, hip); limb(shoulder, point(.31f, .43f + drop)); limb(shoulder, point(.69f, .43f + drop))
                    limb(hip, leftKnee); limb(leftKnee, point(.35f, .84f)); limb(hip, rightKnee); limb(rightKnee, point(.65f, .84f))
                    joint(hip); joint(leftKnee); joint(rightKnee)
                }
                MotionPattern.PUSH -> {
                    val chestY = .43f + phase * .12f
                    val shoulder = point(.34f, chestY)
                    val hip = point(.58f, .52f + phase * .08f)
                    val heel = point(.82f, .70f)
                    val elbow = point(.30f, .58f + phase * .07f)
                    val hand = point(.22f, .75f)
                    head(point(.25f, chestY - .08f)); limb(shoulder, hip); limb(hip, heel)
                    limb(shoulder, elbow); limb(elbow, hand); limb(hand, point(.18f, .82f), violet)
                    limb(heel, point(.86f, .82f), violet); joint(shoulder); joint(elbow)
                }
                MotionPattern.PULL -> {
                    val handsY = .25f + phase * .25f
                    val shoulder = point(.50f, .34f)
                    val hip = point(.50f, .60f)
                    val leftHand = point(.29f + phase * .11f, handsY)
                    val rightHand = point(.71f - phase * .11f, handsY)
                    head(point(.50f, .19f)); limb(shoulder, hip)
                    limb(shoulder, point(.38f, .41f)); limb(point(.38f, .41f), leftHand)
                    limb(shoulder, point(.62f, .41f)); limb(point(.62f, .41f), rightHand)
                    limb(hip, point(.40f, .84f)); limb(hip, point(.60f, .84f))
                    limb(point(.22f, .12f), leftHand, violet, 2.dp.toPx()); limb(point(.78f, .12f), rightHand, violet, 2.dp.toPx())
                }
                MotionPattern.HINGE -> {
                    val shoulder = point(.50f - phase * .15f, .34f + phase * .17f)
                    val hip = point(.53f, .57f)
                    val knee = point(.50f, .70f)
                    head(point(shoulder.x / size.width - .03f, shoulder.y / size.height - .12f))
                    limb(shoulder, hip); limb(hip, knee); limb(knee, point(.45f, .84f))
                    limb(hip, point(.62f, .84f)); limb(shoulder, point(.34f, .71f + phase * .07f)); limb(shoulder, point(.48f, .71f + phase * .07f))
                    joint(hip); joint(knee)
                }
                MotionPattern.CORE -> {
                    val shoulder = point(.36f, .61f)
                    val hip = point(.55f, .65f)
                    val hand = point(.21f + phase * .13f, .27f + phase * .08f)
                    val knee = point(.68f, .44f + phase * .12f)
                    head(point(.27f, .55f)); limb(shoulder, hip); limb(shoulder, hand)
                    limb(hip, knee); limb(knee, point(.80f, .66f + phase * .10f)); limb(hip, point(.72f, .78f), violet)
                    joint(shoulder); joint(hip); joint(knee)
                }
                MotionPattern.CURL -> {
                    val shoulder = point(.50f, .34f)
                    val elbowLeft = point(.41f, .52f)
                    val elbowRight = point(.59f, .52f)
                    val handY = .72f - phase * .34f
                    head(point(.50f, .19f)); limb(shoulder, point(.50f, .61f)); limb(point(.50f, .61f), point(.40f, .84f)); limb(point(.50f, .61f), point(.60f, .84f))
                    limb(shoulder, elbowLeft); limb(elbowLeft, point(.34f + phase * .08f, handY), violet)
                    limb(shoulder, elbowRight); limb(elbowRight, point(.66f - phase * .08f, handY), violet)
                    joint(elbowLeft); joint(elbowRight)
                }
                MotionPattern.RAISE -> {
                    val shoulder = point(.50f, .35f)
                    val handY = .68f - phase * .48f
                    val handOffset = .12f + phase * .23f
                    head(point(.50f, .19f)); limb(shoulder, point(.50f, .62f)); limb(point(.50f, .62f), point(.40f, .84f)); limb(point(.50f, .62f), point(.60f, .84f))
                    limb(shoulder, point(.50f - handOffset * .55f, (.35f + handY) / 2)); limb(point(.50f - handOffset * .55f, (.35f + handY) / 2), point(.50f - handOffset, handY), violet)
                    limb(shoulder, point(.50f + handOffset * .55f, (.35f + handY) / 2)); limb(point(.50f + handOffset * .55f, (.35f + handY) / 2), point(.50f + handOffset, handY), violet)
                }
                MotionPattern.CARDIO -> {
                    val wave = sin(phase * PI.toFloat())
                    val center = point(.50f, .37f - wave * .04f)
                    head(point(.50f, .20f - wave * .04f)); limb(center, point(.50f, .59f - wave * .03f))
                    limb(center, point(.34f + phase * .08f, .51f)); limb(center, point(.66f - phase * .08f, .47f))
                    limb(point(.50f, .59f - wave * .03f), point(.36f + phase * .16f, .82f)); limb(point(.50f, .59f - wave * .03f), point(.64f - phase * .16f, .82f))
                }
                MotionPattern.NEUTRAL -> {
                    val pulse = sin(phase * PI.toFloat()) * .04f
                    val shoulder = point(.50f, .34f - pulse)
                    head(point(.50f, .18f - pulse)); limb(shoulder, point(.50f, .60f)); limb(shoulder, point(.33f, .54f - pulse)); limb(shoulder, point(.67f, .54f - pulse)); limb(point(.50f, .60f), point(.40f, .84f)); limb(point(.50f, .60f), point(.60f, .84f))
                }
            }
        }
        Text(
            text = "MOVEMENT CUE · ${spec.cue}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.BottomStart).background(DeepSurface.copy(alpha = .90f)).padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun PrebakedExerciseMotion(exerciseName: String, cue: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(AngularShape)
            .background(DeepSurface)
            .border(1.dp, Hairline, AngularShape)
            .semantics { contentDescription = "Looping rendered movement cue for $exerciseName. $cue" },
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI("android.resource://${context.packageName}/${R.raw.exercise_squat_pattern}".toUri())
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { video -> if (!video.isPlaying) video.start() },
            onRelease = VideoView::stopPlayback,
        )
        Text(
            text = "PRE-RENDERED FORM LOOP · $cue",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.BottomStart).background(DeepSurface.copy(alpha = .90f)).padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

private fun motionSpec(exerciseName: String, muscleGroup: String): MotionSpec {
    val name = exerciseName.lowercase()
    return when {
        listOf("squat", "lunge", "leg press", "leg extension", "calf").any(name::contains) -> MotionSpec(MotionPattern.SQUAT, "KNEES TRACK • CONTROL DEPTH")
        listOf("deadlift", "hinge", "hamstring curl").any(name::contains) -> MotionSpec(MotionPattern.HINGE, "BRACE • HIPS BACK • LONG SPINE")
        listOf("bench", "push-up", "push up", "floor press", "chest press", "triceps extension").any(name::contains) -> MotionSpec(MotionPattern.PUSH, "BRACE • CONTROL THE LOWERING")
        listOf("row", "pulldown", "pull-up", "pull up", "face pull", "pullover", "snow angel").any(name::contains) -> MotionSpec(MotionPattern.PULL, "LEAD WITH ELBOWS • NO SWING")
        listOf("curl", "isometric").any(name::contains) -> MotionSpec(MotionPattern.CURL, "ELBOWS QUIET • FULL CONTROL")
        listOf("overhead", "lateral", "shoulder press", "scapular", "y-t-w", "pike").any(name::contains) -> MotionSpec(MotionPattern.RAISE, "RIBS DOWN • PAIN-FREE RANGE")
        listOf("dead bug", "plank", "bridge", "hip thrust", "core").any(name::contains) -> MotionSpec(MotionPattern.CORE, "BRACE • BREATHE • MOVE SLOWLY")
        listOf("zone 2", "mobility", "walking", "finisher").any(name::contains) -> MotionSpec(MotionPattern.CARDIO, "SMOOTH RHYTHM • STEADY BREATH")
        muscleGroup.equals("Chest", true) -> MotionSpec(MotionPattern.PUSH, "BRACE • CONTROL THE LOWERING")
        muscleGroup.equals("Back", true) -> MotionSpec(MotionPattern.PULL, "LEAD WITH ELBOWS • NO SWING")
        muscleGroup.equals("Legs", true) || muscleGroup.equals("Glutes", true) -> MotionSpec(MotionPattern.SQUAT, "CONTROL THE RANGE • STAY BALANCED")
        muscleGroup.equals("Shoulders", true) -> MotionSpec(MotionPattern.RAISE, "RIBS DOWN • PAIN-FREE RANGE")
        muscleGroup.equals("Core", true) -> MotionSpec(MotionPattern.CORE, "BRACE • BREATHE • MOVE SLOWLY")
        else -> MotionSpec(MotionPattern.NEUTRAL, "CONTROL EVERY REP • STOP FOR PAIN")
    }
}
