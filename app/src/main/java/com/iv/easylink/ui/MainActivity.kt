package com.iv.easylink.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ── Brand Colors (from DESIGN.md) ────────────────────────────────────────────
private val Canvas = Color(0xFFFCFAFB) // pearl off-white floor
private val Mulberry = Color(0xFF8B1E41) // signature primary
private val MulberryActive = Color(0xFF691430) // pressed/hover variant
private val MulberryMuted = Color(0xFFE8D5DC) // soft selected states
private val SurfaceCard = Color(0xFFF5EFF2) // card background
private val Hairline = Color(0xFFE6DCE0) // 1px borders
private val Ink = Color(0xFF1A1517) // primary text / headlines
private val Body = Color(0xFF4A4045) // running body text
private val Muted = Color(0xFF786B72) // metadata, URLs
private val OnPrimary = Color(0xFFFFFFFF) // text on mulberry buttons

// ── Font Families ────────────────────────────────────────────────────────────
// Using system serif/sans for phase 1. Swap in bundled Lora + Inter TTFs in phase 2.
private val Lora = FontFamily.Serif
private val Inter = FontFamily.SansSerif

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun EasyLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Mulberry,
            onPrimary = OnPrimary,
            primaryContainer = MulberryMuted,
            background = Canvas,
            surface = Canvas,
            onBackground = Ink,
            onSurface = Ink,
            onSurfaceVariant = Body,
            outline = Hairline,
        ),
        typography = Typography(
            // Lora serif for display — weight 400 per DESIGN.md (bold ruins editorial look)
            displayMedium = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
            ),
            // Inter for all UI labels and body
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
        ),
        content = content,
    )
}

// ── Activity ──────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Canvas,
                ) {
                    OnboardingScreen()
                }
            }
        }
    }
}

// ── Onboarding Screen ─────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(checkNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Icon circle ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (hasPermission) MulberryMuted else Mulberry),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.Check else Icons.Default.Link,
                    contentDescription = null,
                    tint = if (hasPermission) Mulberry else OnPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title — Lora serif, weight 400 ────────────────────────────────
            Text(
                text = if (hasPermission) "You're All Set!" else "Welcome to EasyLink",
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Description ───────────────────────────────────────────────────
            Text(
                text = if (hasPermission) {
                    "EasyLink is running silently in the background. Whenever someone sends you a link, you'll see a quick action notification to open it instantly."
                } else {
                    "To open links instantly, EasyLink needs permission to read your incoming notifications."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Body,
                textAlign = TextAlign.Center,
            )

            // ── Privacy note — ai-summary-box style from DESIGN.md ────────────
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Canvas)
                        .border(1.dp, Hairline, RoundedCornerShape(8.dp)),
                ) {
                    // 2px left mulberry bar (ai-summary-box accent from DESIGN.md)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(Mulberry),
                    )
                    Text(
                        text = "All link detection happens 100% on your device. We never store, read, or share your messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Primary CTA — Mulberry, 8dp radius per DESIGN.md ─────────────
            Button(
                onClick = {
                    if (!hasPermission) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                androidx.core.app.ActivityCompat.requestPermissions(
                                    context as android.app.Activity,
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    101,
                                )
                            }
                        }
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mulberry,
                    contentColor = OnPrimary,
                    disabledContainerColor = MulberryMuted,
                    disabledContentColor = Mulberry,
                ),
                enabled = !hasPermission,
            ) {
                Text(
                    text = if (hasPermission) "Permission Granted ✓" else "Grant Permission",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────
fun checkNotificationPermission(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
