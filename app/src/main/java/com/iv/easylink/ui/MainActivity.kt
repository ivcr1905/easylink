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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iv.easylink.ui.inbox.LinkInboxScreen
import com.iv.easylink.ui.theme.*

// ── Navigation route constants ───────────────────────────────────────────────
private object Routes {
    const val ONBOARDING = "onboarding"
    const val INBOX = "inbox"
}

// ── Activity ─────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyLinkTheme {
                val context = LocalContext.current
                val hasPermission = checkNotificationPermission(context)

                // Start destination: inbox if permission already granted, onboarding otherwise
                val startDestination = if (hasPermission) Routes.INBOX else Routes.ONBOARDING

                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Canvas,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                    ) {
                        composable(Routes.ONBOARDING) {
                            OnboardingScreen(
                                onContinue = {
                                    navController.navigate(Routes.INBOX) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.INBOX) {
                            LinkInboxScreen()
                        }
                    }
                }
            }
        }
    }
}

// ── Onboarding Screen ─────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onContinue: () -> Unit = {}) {
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
                    .background(if (hasPermission) PrimaryDisabled else Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.Check else Icons.Default.Link,
                    contentDescription = null,
                    tint = if (hasPermission) Primary else OnPrimary,
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

            // ── Privacy note ──────────────────────────────────────────────────
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
                    Text(
                        text = "All link detection happens 100% on your device. We never store, read, or share your messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Primary CTA — 8dp radius, 40dp height per DESIGN.md ──────────
            Button(
                onClick = {
                    if (hasPermission) {
                        // Permission already granted — proceed to inbox
                        onContinue()
                    } else {
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
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = PrimaryDisabled,
                    disabledContentColor = Muted,
                ),
            ) {
                Text(
                    text = if (hasPermission) "Continue to Link Inbox →" else "Grant Permission",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────
fun checkNotificationPermission(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
