package com.iv.easylink.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iv.easylink.ui.theme.EasyLinkAppTheme
import com.iv.easylink.ui.theme.EasyLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EasyLinkRoot() }
    }
}

private object Routes {
    const val INBOX = "inbox"
    const val LIBRARY = "library"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection/{id}/{name}"
}

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val mainDestinations = listOf(
    Destination(Routes.INBOX, "Inbox", Icons.Default.Inbox),
    Destination(Routes.LIBRARY, "Library", Icons.Default.Folder),
    Destination(Routes.INSIGHTS, "Insights", Icons.Default.BarChart),
)

@Composable
private fun EasyLinkRoot(viewModel: EasyLinkViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateCompat()
    EasyLinkAppTheme(settings.appearanceMode) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var listenerEnabled by remember { mutableStateOf(hasNotificationAccess(context)) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) listenerEnabled = hasNotificationAccess(context)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (settings.onboardingSeen) AppShell(viewModel) else OnboardingScreen(listenerEnabled, viewModel::setOnboardingSeen)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: EasyLinkViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.INBOX
    val mainRoute = mainDestinations.any { it.route == route }
    val title = when (route) {
        Routes.INBOX -> "EasyLink"
        Routes.LIBRARY -> "Library"
        Routes.INSIGHTS -> "Insights"
        Routes.SETTINGS -> "Settings"
        else -> backStack?.arguments?.getString("name") ?: "Collection"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    if (!mainRoute) IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (route != Routes.SETTINGS) {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (mainRoute) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                mainDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(navController, Routes.INBOX, Modifier.padding(padding)) {
            composable(Routes.INBOX) { InboxScreen(viewModel) }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenCategory = { viewModel.setCategory(it); navController.navigate(Routes.INBOX) },
                    onOpenCollection = { id, name -> navController.navigate("collection/$id/${android.net.Uri.encode(name)}") },
                )
            }
            composable(Routes.INSIGHTS) { InsightsScreen(viewModel) }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel) }
            composable(Routes.COLLECTION) { entry ->
                CollectionScreen(
                    viewModel,
                    entry.arguments?.getString("id")?.toLongOrNull() ?: 0,
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(listenerEnabled: Boolean, onComplete: () -> Unit) {
    val context = LocalContext.current
    var disclosureAccepted by remember { mutableStateOf(false) }
    val openNotificationAccess = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { openNotificationAccess() }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Links, ready when you are.", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "EasyLink finds web links in incoming notifications, cleans common tracking parameters, and saves them into your private on-device inbox.",
            style = MaterialTheme.typography.bodyLarge,
            color = EasyLinkTheme.extraColors.mutedText,
        )
        Spacer(Modifier.height(24.dp))
        Surface(color = EasyLinkTheme.extraColors.card, shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Outlined.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Before you continue", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Notification access allows EasyLink to read notification text from other apps. Processing happens on this device. EasyLink stores detected links and their source app, but does not store message bodies or sender names. Rich previews contact the linked website and can be disabled.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { disclosureAccepted = true },
            enabled = !disclosureAccepted,
            colors = ButtonDefaults.buttonColors(containerColor = if (disclosureAccepted) EasyLinkTheme.extraColors.success else MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) { Icon(if (disclosureAccepted) Icons.Default.Check else Icons.Outlined.PrivacyTip, null); Text(if (disclosureAccepted) "  Disclosure accepted" else "  I understand") }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (listenerEnabled) {
                    onComplete()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationAccess()
                }
            },
            enabled = disclosureAccepted,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (listenerEnabled) "Continue to EasyLink" else "Open notification access") }
        TextButton(onClick = onComplete, enabled = disclosureAccepted, modifier = Modifier.fillMaxWidth()) { Text("Use share sheet and manual entry only") }
        Spacer(Modifier.height(12.dp))
        Text("You can revoke access or delete all saved data at any time in Settings.", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    collectAsState()
