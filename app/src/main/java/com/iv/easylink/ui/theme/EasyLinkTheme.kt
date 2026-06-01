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
val Canvas         = Color(0xFFFAF9F5)   // tinted cream canvas
val Primary       = Color(0xFFCC785C)   // signature coral
val PrimaryActive  = Color(0xFFA9583E)   // pressed / hover variant
val PrimaryDisabled = Color(0xFFE6DFD8)   // desaturated cream-tinted disabled
val AccentRose     = Color(0xFFC64545)   // using Error color from DESIGN.md for badges/errors

// ── Surface ──────────────────────────────────────────────────────────────────
val SurfaceSoft    = Color(0xFFF5F0E8)   // section dividers, soft bands
val SurfaceCard    = Color(0xFFEFE9DE)   // card backgrounds
val SurfaceDark    = Color(0xFF181715)   // dark mode canvas
val SurfaceDarkElevated = Color(0xFF252320) // elevated dark cards
val Hairline       = Color(0xFFE6DFD8)   // 1px border tone

// ── Text ─────────────────────────────────────────────────────────────────────
val Ink            = Color(0xFF141413)   // headlines, primary text
val BodyStrong     = Color(0xFF252523)   // emphasized paragraphs, link titles
val Body           = Color(0xFF3D3D3A)   // running body text
val Muted          = Color(0xFF6C6A64)   // URLs, metadata, dates
val OnPrimary      = Color(0xFFFFFFFF)   // text on coral buttons
val OnDark         = Color(0xFFFAF9F5)   // text on dark surfaces
val OnDarkMuted    = Color(0xFFA09D96)   // secondary text in dark mode

// ── Font Families ────────────────────────────────────────────────────────────
// System serif/sans as Lora + Inter stand-ins.
// Replace with bundled TTFs via GoogleFont provider when ready.
val Lora  = FontFamily.Serif
val Inter = FontFamily.SansSerif

// ── Color Scheme ─────────────────────────────────────────────────────────────
private val EasyLinkColorScheme = lightColorScheme(
    primary           = Primary,
    onPrimary         = OnPrimary,
    primaryContainer  = PrimaryDisabled,
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
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 33.6.sp,
        letterSpacing = (-0.3).sp,
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
