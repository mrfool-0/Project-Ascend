package com.ascend.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Void = Color(0xFF05070D)
val DeepSurface = Color(0xFF0B1020)
val RaisedSurface = Color(0xFF10182C)
val EnergyViolet = Color(0xFF7667FF)
val EnergyCyan = Color(0xFF35D9FF)
val EnergyEmerald = Color(0xFF32E6A1)
val EnergyAmber = Color(0xFFFFBF47)
val EnergyCrimson = Color(0xFFFF4D6D)
val TextPrimary = Color(0xFFF3F6FF)
val TextSecondary = Color(0xFF92A0BC)
val Hairline = Color(0xFF26304A)

private val AscendColors = darkColorScheme(
    primary = EnergyViolet,
    onPrimary = Color.White,
    secondary = EnergyCyan,
    tertiary = EnergyEmerald,
    background = Void,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = TextSecondary,
    error = EnergyCrimson,
)

private val AscendTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = .5.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = .4.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = .8.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = .6.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = AscendColors, typography = AscendTypography) {
        Surface(Modifier.fillMaxSize(), color = Void, contentColor = TextPrimary, content = content)
    }
}
