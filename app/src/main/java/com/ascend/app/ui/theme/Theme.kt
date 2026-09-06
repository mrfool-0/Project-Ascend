package com.ascend.app.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.R

// Obsidian-first palette: high contrast where it matters, quiet everywhere else.
val Void = Color(0xFF07090D)
val DeepSurface = Color(0xFF0D1118)
val RaisedSurface = Color(0xFF141A24)
val GlassSurface = Color(0xE6111720)
val EnergyViolet = Color(0xFF8C7CFF)
val EnergyCyan = Color(0xFF52D9F5)
val EnergyEmerald = Color(0xFF48DDA5)
val EnergyAmber = Color(0xFFFFC35A)
val EnergyCrimson = Color(0xFFFF6584)
val TextPrimary = Color(0xFFF4F6FA)
val TextSecondary = Color(0xFF9AA6B8)
val TextTertiary = Color(0xFF68758A)
val Hairline = Color(0xFF252C39)

val Manrope = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold),
    Font(R.font.manrope_variable, FontWeight.ExtraBold),
)

val SystemMono = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, FontWeight.Medium),
    Font(R.font.jetbrains_mono_variable, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_variable, FontWeight.Bold),
)

private val AscendColors = darkColorScheme(
    primary = EnergyViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF292344),
    onPrimaryContainer = Color(0xFFE8E3FF),
    secondary = EnergyCyan,
    onSecondary = Color(0xFF002B34),
    secondaryContainer = Color(0xFF12313B),
    onSecondaryContainer = Color(0xFFC7F5FF),
    tertiary = EnergyEmerald,
    background = Void,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = TextSecondary,
    outline = Hairline,
    outlineVariant = Hairline.copy(alpha = .7f),
    error = EnergyCrimson,
)

private val AscendTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 35.sp, lineHeight = 40.sp, letterSpacing = (-.8).sp),
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-.5).sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-.35).sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-.2).sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-.1).sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = .15.sp),
    labelMedium = TextStyle(fontFamily = SystemMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = .65.sp),
    labelSmall = TextStyle(fontFamily = SystemMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = .5.sp),
)

private val AscendShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AscendColors,
        typography = AscendTypography,
        shapes = AscendShapes,
    ) {
        Surface(Modifier.fillMaxSize(), color = Void, contentColor = TextPrimary, content = content)
    }
}
