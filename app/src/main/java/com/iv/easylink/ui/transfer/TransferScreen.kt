package com.iv.easylink.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.BackupKind
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.ui.theme.EasyLinkTheme
import com.iv.easylink.ui.components.EasyLinkIcons
import java.time.LocalDate

@Composable
fun TransferScreen(viewModel: EasyLinkViewModel) {
    var pendingImport by remember { mutableStateOf<EasyLinkBackup?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupCodec.MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportAll(it) { success -> status = if (success) "Backup exported" else "Backup could not be exported" } }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.readBackup(it) { result ->
                result.onSuccess { backup -> pendingImport = backup }
                    .onFailure { error -> status = error.message ?: "This file could not be imported" }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Move your library safely", style = MaterialTheme.typography.headlineLarge)
        Text("Backups stay under your control and are never uploaded by EasyLink.", color = EasyLinkTheme.extraColors.mutedText)
        Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Export everything", style = MaterialTheme.typography.titleLarge)
                Text("Links, collections, favorites, archive, Bin, sender labels, and automation rules.", color = EasyLinkTheme.extraColors.mutedText)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { export.launch("easylink-backup-${LocalDate.now()}${BackupCodec.FILE_SUFFIX}") }) {
                    Icon(EasyLinkIcons.Export, null); Text("  Choose backup location")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Import an EasyLink file", style = MaterialTheme.typography.titleLarge)
                Text("The complete file is validated before anything is changed.", color = EasyLinkTheme.extraColors.mutedText)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { import.launch(arrayOf(BackupCodec.MIME_TYPE, "application/octet-stream")) }) {
                    Icon(EasyLinkIcons.Import, null); Text("  Choose file")
                }
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
    }
    pendingImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import ${backup.links.size} link${if (backup.links.size == 1) "" else "s"}?") },
            text = {
                Text(
                    if (backup.kind == BackupKind.FULL) "Merge keeps existing data. Replace removes the current library only after this backup has passed validation."
                    else "This will add the shared ${backup.kind.wireName} to your library without removing existing data.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importBackup(backup, false) { result ->
                        status = result.fold(
                            { "Imported ${it.importedLinks} new link${if (it.importedLinks == 1) "" else "s"}" },
                            { it.message ?: "Import failed" },
                        )
                    }
                    pendingImport = null
                }) { Text("Merge") }
            },
            dismissButton = {
                if (backup.kind == BackupKind.FULL) {
                    TextButton(onClick = {
                        viewModel.importBackup(backup, true) { result ->
                            status = result.fold(
                                { "Restored ${it.importedLinks} links" },
                                { it.message ?: "Restore failed" },
                            )
                        }
                        pendingImport = null
                    }) { Text("Replace", color = MaterialTheme.colorScheme.error) }
                } else {
                    TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
                }
            },
        )
    }
}
