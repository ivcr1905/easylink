package com.iv.easylink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.settings.SwipeMode
import com.iv.easylink.ui.components.EasyLinkIcons
import com.iv.easylink.ui.theme.EasyLinkDimensions

/** User-facing appearance and interaction choices, kept separate from operational settings. */
@Composable
fun CustomizationScreen(viewModel: EasyLinkViewModel) {
    val settings by viewModel.settings.collectAsState()
    val collections by viewModel.collections.collectAsState()
    var shortcutsDialog by remember { mutableStateOf(false) }
    var swipeMenu by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = EasyLinkDimensions.screenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(EasyLinkDimensions.sectionSpacing),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Appearance")
                AppearanceSelector(settings.appearanceMode, viewModel::setAppearance)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Inbox & gestures")
                SettingsGroup {
                    SettingsRow(
                        EasyLinkIcons.InboxLists,
                        "Inbox lists",
                        "${settings.inboxShortcuts.size} of 5 shown",
                    ) { shortcutsDialog = true }
                    HorizontalDivider()
                    Box {
                        SettingsRow(EasyLinkIcons.Swipe, "Swipe behavior", swipeLabel(settings.swipeMode)) {
                            swipeMenu = true
                        }
                        DropdownMenu(swipeMenu, { swipeMenu = false }) {
                            SwipeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(swipeLabel(mode)) },
                                    onClick = { viewModel.setSwipeMode(mode); swipeMenu = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (shortcutsDialog) {
        InboxShortcutsDialog(
            shortcuts = settings.inboxShortcuts,
            collections = collections,
            onToggle = viewModel::setShortcutVisible,
            onMove = viewModel::moveShortcut,
            onDismiss = { shortcutsDialog = false },
        )
    }
}
