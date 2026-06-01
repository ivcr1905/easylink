package com.iv.easylink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand & Accent (from DESIGN.md) ─────────────────────────────────────────
val Canvas         = Color(0xFFFCFAFB)   // pearl off-white floor
val Mulberry       = Color(0xFF8B1E41)   // signature primary
val MulberryActive = Color(0xFF691430)   // pressed / hover variant
val MulberryMuted  = Color(0xFFE8D5DC)   // soft selected states
val AccentRose     = Color(0xFFD96C8A)   // badges, spark icons

// ── Surface ──────────────────────────────────────────────────────────────────
val SurfaceSoft    = Color(0xFFF7F3F5)   // section dividers, soft bands
val SurfaceCard    = Color(0xFFF5EFF2)   // card backgrounds
val SurfaceDark    = Color(0xFF1A1316)   // dark mode canvas
val SurfaceDarkElevated = Color(0xFF261D22) // elevated dark cards
val Hairline       = Color(0xFFE6DCE0)   // 1px border tone

// ── Text ─────────────────────────────────────────────────────────────────────
val Ink            = Color(0xFF1A1517)   // headlines, primary text
val BodyStrong     = Color(0xFF2E262A)   // emphasized paragraphs, link titles
val Body           = Color(0xFF4A4045)   // running body text
val Muted          = Color(0xFF786B72)   // URLs, metadata, dates
val OnPrimary      = Color(0xFFFFFFFF)   // text on mulberry buttons
val OnDark         = Color(0xFFFCFAFB)   // text on dark surfaces
val OnDarkMuted    = Color(0xFFA69B9F)   // secondary text in dark mode

// ── Font Families ────────────────────────────────────────────────────────────
// System serif/sans as Lora + Inter stand-ins.
// Replace with bundled TTFs via GoogleFont provider when ready.
val Lora  = FontFamily.Serif
val Inter = FontFamily.SansSerif

// ── Color Scheme ─────────────────────────────────────────────────────────────
private val EasyLinkColorScheme = lightColorScheme(
    primary           = Mulberry,
    onPrimary         = OnPrimary,
    primaryContainer  = MulberryMuted,
    background        = Canvas,
    surface           = Canvas,
    onBackground      = Ink,
    onSurface         = Ink,
    onSurfaceVariant  = Body,
    outline           = Hairline,
)

// ── Typography ───────────────────────────────────────────────────────────────
private val EasyLinkTypography = Typography(
    // Lora serif for display — weight 400 per DESIGN.md (bold ruins editorial look)
    displayLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    // Inter for all UI labels and body
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 14.sp,
    ),
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun EasyLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EasyLinkColorScheme,
        typography = EasyLinkTypography,
        content = content,
    )
}
