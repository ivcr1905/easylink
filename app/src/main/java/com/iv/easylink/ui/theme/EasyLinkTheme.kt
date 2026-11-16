package com.iv.easylink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iv.easylink.data.settings.AppearanceMode

interface ThemeStyle {
    val id: String
    val lightColors: ColorScheme
    val darkColors: ColorScheme
    val typography: Typography
    val shapes: Shapes
    val extras: EasyLinkExtraColors
    val darkExtras: EasyLinkExtraColors
}

@Immutable
data class EasyLinkExtraColors(
    val card: Color,
    val softSurface: Color,
    val mutedText: Color,
    val success: Color,
    val warning: Color,
    val categoryColors: List<Color>,
)

private val Terracotta = Color(0xFFC35638)
private val TerracottaDark = Color(0xFFE87A5D)
private val Charcoal = Color(0xFF141413)
private val Canvas = Color(0xFFFBFBFA)
private val CardWhite = Color(0xFFFFFFFF)
private val DarkCanvas = Color(0xFF0F0F10)
private val DarkCard = Color(0xFF18181A)
private val SoftSurfaceLight = Color(0xFFF3F3F1)
private val SoftSurfaceDark = Color(0xFF222225)

object ClaudeThemeStyle : ThemeStyle {
    override val id = "claude"
    override val lightColors = lightColorScheme(
        primary = Terracotta,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFBECE6),
        onPrimaryContainer = Color(0xFF431608),
        background = Canvas,
        onBackground = Charcoal,
        surface = CardWhite,
        onSurface = Charcoal,
        surfaceVariant = SoftSurfaceLight,
        onSurfaceVariant = Color(0xFF52514D),
        outline = Color(0xFFE2E2DF),
        outlineVariant = Color(0xFFEAEAEA),
        error = Color(0xFFBA2D2D),
    )
    override val darkColors = darkColorScheme(
        primary = TerracottaDark,
        onPrimary = Color(0xFF381409),
        primaryContainer = Color(0xFF522315),
        onPrimaryContainer = Color(0xFFFFDDD2),
        background = DarkCanvas,
        onBackground = Color(0xFFF3F3F2),
        surface = DarkCard,
        onSurface = Color(0xFFF3F3F2),
        surfaceVariant = SoftSurfaceDark,
        onSurfaceVariant = Color(0xFFA1A09A),
        outline = Color(0xFF2E2E32),
        outlineVariant = Color(0xFF252528),
        error = Color(0xFFFFB4AB),
    )
    override val typography = Typography(
        displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
    override val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    )
    override val extras = EasyLinkExtraColors(
        card = CardWhite,
        softSurface = SoftSurfaceLight,
        mutedText = Color(0xFF6B6A65),
        success = Color(0xFF2E7D32),
        warning = Color(0xFFB45309),
        categoryColors = listOf(
            Terracotta,
            Color(0xFF4A6B82),
            Color(0xFF6A5A82),
            Color(0xFF3B7A6A),
            Color(0xFF8C5E35),
            Color(0xFF4E6375),
            Color(0xFF7E5262),
            Color(0xFF56663E),
            Color(0xFF5A5852),
        ),
    )
    override val darkExtras = EasyLinkExtraColors(
        card = DarkCard,
        softSurface = SoftSurfaceDark,
        mutedText = Color(0xFF8E8D88),
        success = Color(0xFF66BB6A),
        warning = Color(0xFFFBBF24),
        categoryColors = listOf(
            TerracottaDark,
            Color(0xFF8BA5BA),
            Color(0xFFA597BA),
            Color(0xFF70B3A2),
            Color(0xFFC7986E),
            Color(0xFF8E9FA9),
            Color(0xFFB88E9E),
            Color(0xFF9CA984),
            Color(0xFF9E9B95),
        ),
    )
}

val LocalEasyLinkColors = staticCompositionLocalOf { ClaudeThemeStyle.extras }

object EasyLinkTheme {
    val extraColors: EasyLinkExtraColors @Composable get() = LocalEasyLinkColors.current
}

/** Shared geometry keeps new screens visually compatible without duplicating magic values. */
object EasyLinkDimensions {
    val screenPadding = 16.dp
    val sectionSpacing = 24.dp
    val itemSpacing = 8.dp
}

@Composable
fun EasyLinkAppTheme(appearanceMode: AppearanceMode, style: ThemeStyle = ClaudeThemeStyle, content: @Composable () -> Unit) {
    val dark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalEasyLinkColors provides if (dark) style.darkExtras else style.extras) {
        MaterialTheme(
            colorScheme = if (dark) style.darkColors else style.lightColors,
            typography = style.typography,
            shapes = style.shapes,
            content = content,
        )
    }
}
