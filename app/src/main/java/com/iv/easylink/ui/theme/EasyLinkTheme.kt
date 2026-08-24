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

private val Coral = Color(0xFFCC785C)
private val CoralDark = Color(0xFFE49A7D)
private val Ink = Color(0xFF171613)
private val Canvas = Color(0xFFFAF9F5)
private val CreamCard = Color(0xFFF0EAE0)
private val DarkCanvas = Color(0xFF151412)
private val DarkCard = Color(0xFF24221F)

object ClaudeThemeStyle : ThemeStyle {
    override val id = "claude"
    override val lightColors = lightColorScheme(
        primary = Coral, onPrimary = Color.White, primaryContainer = Color(0xFFF5DED4),
        onPrimaryContainer = Color(0xFF542012), background = Canvas, onBackground = Ink,
        surface = Canvas, onSurface = Ink, surfaceVariant = CreamCard,
        onSurfaceVariant = Color(0xFF4C4943), outline = Color(0xFFD8D1C7), error = Color(0xFFB83C3C),
    )
    override val darkColors = darkColorScheme(
        primary = CoralDark, onPrimary = Color(0xFF3B150B), primaryContainer = Color(0xFF713A29),
        onPrimaryContainer = Color(0xFFFFDBCD), background = DarkCanvas, onBackground = Color(0xFFF8F5EF),
        surface = DarkCanvas, onSurface = Color(0xFFF8F5EF), surfaceVariant = DarkCard,
        onSurfaceVariant = Color(0xFFCBC5BC), outline = Color(0xFF55514A), error = Color(0xFFFFB4AB),
    )
    override val typography = Typography(
        displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 39.sp, letterSpacing = (-0.4).sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 25.sp, lineHeight = 31.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    )
    override val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    )
    override val extras = EasyLinkExtraColors(
        card = CreamCard, softSurface = Color(0xFFF6F1E9), mutedText = Color(0xFF6D6962),
        success = Color(0xFF397A4B), warning = Color(0xFFB7791F),
        categoryColors = listOf(Coral, Color(0xFF657B9A), Color(0xFF7C6B9A), Color(0xFF477E73), Color(0xFFAA713A), Color(0xFF5C7488), Color(0xFF926675), Color(0xFF65714B), Color(0xFF6B6963)),
    )
    override val darkExtras = EasyLinkExtraColors(
        card = DarkCard, softSurface = Color(0xFF1C1B18), mutedText = Color(0xFFA8A29A),
        success = Color(0xFF84D69A), warning = Color(0xFFFFC875),
        categoryColors = listOf(CoralDark, Color(0xFF92ADD0), Color(0xFFB19BDC), Color(0xFF75B9A9), Color(0xFFDFA467), Color(0xFF91B2C9), Color(0xFFD297AA), Color(0xFFA8B781), Color(0xFFAAA69D)),
    )
}

val LocalEasyLinkColors = staticCompositionLocalOf { ClaudeThemeStyle.extras }

object EasyLinkTheme {
    val extraColors: EasyLinkExtraColors @Composable get() = LocalEasyLinkColors.current
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
