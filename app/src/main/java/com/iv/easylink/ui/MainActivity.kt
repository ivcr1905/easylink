package com.iv.easylink.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.actions.LinkActionReceiver
import com.iv.easylink.data.settings.SenderRuleBehavior
import com.iv.easylink.ui.components.EasyLinkIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingMuteRequest = MutableStateFlow<MuteRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingMuteRequest.value = intent.toMuteRequest()
        setContent { EasyLinkRoot(pendingMuteRequest, clearMuteRequest = { pendingMuteRequest.value = null }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMuteRequest.value = intent.toMuteRequest()
    }

    private fun Intent.toMuteRequest(): MuteRequest? {
        if (action != ACTION_CHOOSE_MUTE) return null
        val sourcePackage = getStringExtra(LinkActionReceiver.EXTRA_SOURCE_PACKAGE)?.trim().orEmpty()
        val sourceApp = getStringExtra(LinkActionReceiver.EXTRA_SOURCE_APP)?.trim().orEmpty()
        val senderName = getStringExtra(LinkActionReceiver.EXTRA_SENDER_NAME)?.trim().orEmpty()
        return if (sourcePackage.isEmpty() || senderName.isEmpty()) null else MuteRequest(sourcePackage, sourceApp, senderName)
    }

    companion object {
        const val ACTION_CHOOSE_MUTE = "com.iv.easylink.action.CHOOSE_MUTE"
    }
}

private data class MuteRequest(val sourcePackage: String, val sourceApp: String, val senderName: String)

private object Routes {
    const val INBOX = "inbox"
    const val LIBRARY = "library"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val CUSTOMIZE = "customize"
    const val ARCHIVE = "archive"
    const val BIN = "bin"
    const val TRANSFER = "transfer"
    const val ABOUT = "about"
    const val FAVORITES = "favorites"
    const val WARNINGS = "warnings"
    const val COLLECTION = "collection/{id}/{name}"
    const val CATEGORY = "category/{code}"
}

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val mainDestinations = listOf(
    Destination(Routes.INBOX, "Inbox", EasyLinkIcons.Inbox),
    Destination(Routes.LIBRARY, "Library", EasyLinkIcons.Library),
)

private const val MAX_PINNED_MENU_COLLECTIONS = 3

@Composable
private fun EasyLinkRoot(
    pendingMuteRequest: StateFlow<MuteRequest?>,
    clearMuteRequest: () -> Unit,
    viewModel: EasyLinkViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateCompat()
    val muteRequest by pendingMuteRequest.collectAsStateCompat()
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
        muteRequest?.let { request ->
            var matchingLinkCount by remember(
                request.sourcePackage,
                request.senderName,
                settings.moveExistingOnIgnore,
            ) { mutableStateOf<Int?>(if (settings.moveExistingOnIgnore) null else 0) }
            LaunchedEffect(request.sourcePackage, request.senderName, settings.moveExistingOnIgnore) {
                if (settings.moveExistingOnIgnore) {
                    viewModel.countActiveSenderLinks(request.sourcePackage, request.senderName) { matchingLinkCount = it }
                }
            }
            AlertDialog(
                onDismissRequest = clearMuteRequest,
                title = { Text("Mute ${request.senderName}?") },
                text = {
                    Text(
                        buildString {
                            append("Choose how EasyLink handles future links from this exact sender in ${request.sourceApp.ifBlank { "this app" }}.")
                            if (settings.moveExistingOnIgnore) {
                                append(
                                    when (val count = matchingLinkCount) {
                                        null -> " EasyLink is checking for existing links."
                                        0 -> " There are no existing Inbox links to move."
                                        1 -> " Ignoring also moves 1 existing Inbox link to Bin, where it remains recoverable for 30 days."
                                        else -> " Ignoring also moves $count existing Inbox links to Bin, where they remain recoverable for 30 days."
                                    },
                                )
                            }
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.applyMute(request.sourcePackage, request.sourceApp, request.senderName, SenderRuleBehavior.SILENCE_ALERTS)
                        clearMuteRequest()
                    }) { Text("Mute alerts") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = clearMuteRequest) { Text("Cancel") }
                        TextButton(onClick = {
                            viewModel.applyMute(request.sourcePackage, request.sourceApp, request.senderName, SenderRuleBehavior.IGNORE_FUTURE_LINKS)
                            clearMuteRequest()
                        }, enabled = !settings.moveExistingOnIgnore || matchingLinkCount != null) {
                            Text("Ignore links", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: EasyLinkViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val collections by viewModel.collections.collectAsState()
    val query by viewModel.query.collectAsState()
    val route = backStack?.destination?.route ?: Routes.INBOX
    val mainRoute = mainDestinations.any { it.route == route }
    val menuRoute = mainRoute || route == Routes.INSIGHTS || route == Routes.ARCHIVE || route == Routes.BIN
    var menuExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember(route) { mutableStateOf(false) }
    var categoryActionsExpanded by remember(route) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val categoryCode = backStack?.arguments?.getString("code")?.toIntOrNull() ?: LinkCategory.OTHER.code
    BackHandler(enabled = route == Routes.INBOX && searchExpanded) {
        if (query.text.isNotEmpty()) viewModel.setSearch("") else searchExpanded = false
    }
    val title = when (route) {
        Routes.INBOX -> "Inbox"
        Routes.LIBRARY -> "Library"
        Routes.INSIGHTS -> "Insights"
        Routes.SETTINGS -> "Settings"
        Routes.CUSTOMIZE -> "Customize"
        Routes.ARCHIVE -> "Archive"
        Routes.BIN -> "Bin"
        Routes.TRANSFER -> "Import & export"
        Routes.ABOUT -> "About EasyLink"
        Routes.FAVORITES -> "Favorites"
        Routes.WARNINGS -> "Warnings"
        Routes.CATEGORY -> LinkCategory.fromCode(categoryCode).label
        else -> backStack?.arguments?.getString("name") ?: "Collection"
    }
    val navigateFromMenu: (String) -> Unit = { destination ->
        menuExpanded = false
        navController.navigate(destination) { launchSingleTop = true }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(64.dp),
                title = {
                    AnimatedContent(
                        modifier = Modifier.offset(y = 4.dp),
                        targetState = route == Routes.INBOX && searchExpanded,
                        transitionSpec = {
                            (fadeIn(tween(200)) + expandHorizontally(tween(200), expandFrom = Alignment.End)) togetherWith
                                (fadeOut(tween(140)) + shrinkHorizontally(tween(180), shrinkTowards = Alignment.End))
                        },
                        label = "Inbox search",
                    ) { searching ->
                        if (searching) TopBarSearchField(query.text, viewModel::setSearch)
                        else Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (menuRoute) Box(Modifier.offset(y = 4.dp)) {
                        IconButton(onClick = { menuExpanded = true }) { Icon(EasyLinkIcons.Menu, "Open menu") }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.width(252.dp).heightIn(max = 360.dp),
                        ) {
                            Text("EasyLink", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            CompactMenuItem("Insights", EasyLinkIcons.Insights, route == Routes.INSIGHTS) { navigateFromMenu(Routes.INSIGHTS) }
                            CompactMenuItem("Archive", EasyLinkIcons.Archive, route == Routes.ARCHIVE) { navigateFromMenu(Routes.ARCHIVE) }
                            CompactMenuItem("Bin", EasyLinkIcons.Bin, route == Routes.BIN, "30 days") { navigateFromMenu(Routes.BIN) }
                            collections.asSequence().filter { it.isPinned }.take(MAX_PINNED_MENU_COLLECTIONS).forEach { collection ->
                                CompactMenuItem(collection.name, EasyLinkIcons.Collection, false) {
                                    navigateFromMenu("collection/${collection.id}/${android.net.Uri.encode(collection.name)}")
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            CompactMenuItem("Import & export", EasyLinkIcons.ImportExport, route == Routes.TRANSFER) { navigateFromMenu(Routes.TRANSFER) }
                            CompactMenuItem("Customize", EasyLinkIcons.Customize, route == Routes.CUSTOMIZE) { navigateFromMenu(Routes.CUSTOMIZE) }
                            CompactMenuItem("Settings", EasyLinkIcons.Settings, route == Routes.SETTINGS) { navigateFromMenu(Routes.SETTINGS) }
                            CompactMenuItem("About", EasyLinkIcons.Info, route == Routes.ABOUT) { navigateFromMenu(Routes.ABOUT) }
                            CompactMenuItem("Rate EasyLink", EasyLinkIcons.Rate, false) { menuExpanded = false; openRatePage(context) }
                        }
                    } else IconButton(onClick = navController::popBackStack, modifier = Modifier.offset(y = 4.dp)) {
                        Icon(EasyLinkIcons.Back, "Back")
                    }
                },
                actions = {
                    when (route) {
                        Routes.INBOX -> IconButton(
                            modifier = Modifier.offset(y = 4.dp),
                            onClick = {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) viewModel.setSearch("")
                            },
                        ) {
                            Icon(if (searchExpanded) EasyLinkIcons.Close else EasyLinkIcons.Search, if (searchExpanded) "Close search" else "Search links")
                        }
                        Routes.CATEGORY -> Box(Modifier.offset(y = 4.dp)) {
                            IconButton(onClick = { categoryActionsExpanded = true }) {
                                Icon(EasyLinkIcons.MoreVertical, "Category sharing options")
                            }
                            DropdownMenu(categoryActionsExpanded, { categoryActionsExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Copy links") },
                                    leadingIcon = { Icon(EasyLinkIcons.Copy, null) },
                                    onClick = {
                                        categoryActionsExpanded = false
                                        viewModel.categoryBackup(categoryCode) { backup ->
                                            val copied = backup != null && copyBackup(context, backup)
                                            Toast.makeText(context, if (copied) "Category copied" else "Category is too large; share it as a file", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share links") },
                                    leadingIcon = { Icon(EasyLinkIcons.Share, null) },
                                    onClick = {
                                        categoryActionsExpanded = false
                                        viewModel.categoryBackup(categoryCode) { backup ->
                                            val shared = backup != null && shareBackupText(context, backup)
                                            if (!shared) Toast.makeText(context, "Category is too large; share it as a file", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share EasyLink file") },
                                    leadingIcon = { Icon(EasyLinkIcons.File, null) },
                                    onClick = {
                                        categoryActionsExpanded = false
                                        viewModel.categoryBackup(categoryCode) { backup ->
                                            backup?.let {
                                                scope.launch {
                                                    try {
                                                        shareBackupFile(context, it)
                                                    } catch (cancelled: CancellationException) {
                                                        throw cancelled
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        else -> Unit
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (mainRoute) NavigationBar(modifier = Modifier.height(64.dp), containerColor = MaterialTheme.colorScheme.surface) {
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
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(navController, Routes.INBOX, Modifier.padding(padding)) {
            composable(Routes.INBOX) {
                LaunchedEffect(Unit) { viewModel.showActiveLinks() }
                InboxScreen(
                    viewModel = viewModel,
                    showShortcuts = true,
                    onOpenMenu = { menuExpanded = true },
                    onOpenLibrary = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenCategory = { navController.navigate("category/$it") },
                    onOpenCollection = { id, name -> navController.navigate("collection/$id/${android.net.Uri.encode(name)}") },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(
                    viewModel,
                    onReviewFavorites = { navController.navigate(Routes.FAVORITES) },
                    onReviewWarnings = { navController.navigate(Routes.WARNINGS) },
                    onOpenCategory = { navController.navigate("category/$it") },
                )
            }
            composable(Routes.ARCHIVE) {
                LaunchedEffect(Unit) { viewModel.showArchivedLinks() }
                InboxScreen(viewModel)
            }
            composable(Routes.BIN) {
                LaunchedEffect(Unit) { viewModel.showBin() }
                InboxScreen(viewModel)
            }
            composable(Routes.FAVORITES) {
                LaunchedEffect(Unit) { viewModel.showFavorites() }
                InboxScreen(viewModel)
            }
            composable(Routes.WARNINGS) {
                LaunchedEffect(Unit) { viewModel.showWarnings() }
                InboxScreen(viewModel)
            }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel) }
            composable(Routes.CUSTOMIZE) { CustomizationScreen(viewModel) }
            composable(Routes.TRANSFER) { TransferScreen(viewModel) }
            composable(Routes.ABOUT) { AboutScreen() }
            composable(Routes.CATEGORY) { entry ->
                val code = entry.arguments?.getString("code")?.toIntOrNull() ?: 0
                LaunchedEffect(code) { viewModel.showActiveLinks(); viewModel.setCategory(code) }
                InboxScreen(viewModel)
            }
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
private fun CompactMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = trailing?.let { value -> ({ Text(value, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }) },
        modifier = Modifier.height(42.dp),
    )
}

@Composable
private fun TopBarSearchField(value: String, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.medium,
        color = EasyLinkTheme.extraColors.softSurface,
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(EasyLinkIcons.Search, null, Modifier.size(19.dp), tint = EasyLinkTheme.extraColors.mutedText)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.padding(start = 9.dp).weight(1f).focusRequester(focusRequester),
                decorationBox = { field ->
                    if (value.isEmpty()) Text("Search links", style = MaterialTheme.typography.bodyMedium, color = EasyLinkTheme.extraColors.mutedText)
                    field()
                },
            )
            if (value.isNotEmpty()) IconButton(onClick = { onValueChange("") }, Modifier.size(36.dp)) { Icon(EasyLinkIcons.Close, "Clear search", Modifier.size(18.dp)) }
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
        ) { Icon(EasyLinkIcons.Link, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp)) }
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
                Icon(EasyLinkIcons.Privacy, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Before you continue", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Notification access allows EasyLink to read notification text from other apps. Processing happens on this device. EasyLink stores detected links, their source app, and a sender name when the messaging app provides one. Message bodies are not stored. Rich previews are off by default because enabling them contacts linked websites.",
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
        ) { Icon(if (disclosureAccepted) EasyLinkIcons.Check else EasyLinkIcons.Privacy, null); Text(if (disclosureAccepted) "  Disclosure accepted" else "  I understand") }
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

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("EasyLink", style = MaterialTheme.typography.displayMedium)
        Text("Private, on-device link organization without accounts or advertising.", style = MaterialTheme.typography.bodyLarge)
        Text("Version 1.0", color = EasyLinkTheme.extraColors.mutedText)
        Text("Privacy policy and Play Store rating links will be connected to the EasyLink website and listing before production launch.", style = MaterialTheme.typography.bodyMedium, color = EasyLinkTheme.extraColors.mutedText)
    }
}

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun openRatePage(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
    runCatching { context.startActivity(market) }.onFailure { runCatching { context.startActivity(web) } }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    collectAsState()
