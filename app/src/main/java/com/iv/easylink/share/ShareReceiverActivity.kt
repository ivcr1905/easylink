package com.iv.easylink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.BackupKind
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.data.transfer.BackupCollection
import com.iv.easylink.ingestion.LinkSource
import com.iv.easylink.ingestion.PreparedLink
import com.iv.easylink.ui.theme.EasyLinkAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareReceiverActivity : ComponentActivity() {
    private val app by lazy(LazyThreadSafetyMode.NONE) { application as EasyLinkApplication }
    private val source by lazy(LazyThreadSafetyMode.NONE) {
        LinkSource(packageName = null, appName = getString(R.string.source_shared))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_SEND && intent.type == BackupCodec.MIME_TYPE) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri == null || uri.scheme != "content") {
                Toast.makeText(this, "This EasyLink file cannot be opened", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                readSharedFile(uri)
            }
            return
        }
        val text = intent.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank() || text.length > MAX_SHARED_TEXT_LENGTH) {
            Toast.makeText(this, if (text.isNullOrBlank()) "No link found" else "Shared text is too large", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val prepared = try {
            app.container.linkIngestor.prepare(text)
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to clean shared links", error)
            emptyList()
        }
        when {
            prepared.isEmpty() -> {
                Toast.makeText(this, R.string.no_link_found, Toast.LENGTH_SHORT).show()
                finish()
            }
            prepared.size == 1 -> saveSingle(prepared)
            else -> showBulkImport(prepared)
        }
    }

    private fun readSharedFile(uri: Uri) {
        lifecycleScope.launch {
            val backup = try {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use(BackupCodec::read)
                        ?: error("The shared file could not be opened")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Unable to read shared EasyLink file", error)
                Toast.makeText(this@ShareReceiverActivity, error.message ?: "Invalid EasyLink file", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            showFileImport(backup)
        }
    }

    private fun showFileImport(backup: EasyLinkBackup) {
        setContent {
            val settings by app.container.settings.state.collectAsState()
            val collections by app.container.linkRepository.observeCollections().collectAsState(emptyList())
            EasyLinkAppTheme(settings.appearanceMode) {
                FileImportDialog(
                    backup = backup,
                    collections = collections,
                    onImport = { replace, targetCollection ->
                        lifecycleScope.launch {
                            try {
                                val value = targetCollection?.let { backup.targetCollection(it) } ?: backup
                                val result = app.container.linkRepository.importBackup(value, replace)
                                Toast.makeText(this@ShareReceiverActivity, "Imported ${result.importedLinks} new links", Toast.LENGTH_SHORT).show()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Log.e(LOG_TAG, "Unable to import shared file", error)
                                Toast.makeText(this@ShareReceiverActivity, "Import failed", Toast.LENGTH_SHORT).show()
                            } finally {
                                finish()
                            }
                        }
                    },
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun saveSingle(prepared: List<PreparedLink>) {
        lifecycleScope.launch {
            val copied = copy(prepared.first().url)
            val savedCount = try {
                app.container.linkIngestor.ingestPrepared(prepared, source).size
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Unable to save shared link", error)
                0
            }
            val message = when {
                copied && savedCount > 0 -> getString(R.string.clean_link_copied_and_saved)
                copied -> getString(R.string.clean_link_copied)
                savedCount > 0 -> resources.getQuantityString(R.plurals.links_saved, savedCount, savedCount)
                else -> getString(R.string.share_failed)
            }
            Toast.makeText(this@ShareReceiverActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showBulkImport(prepared: List<PreparedLink>) {
        setContent {
            val settings by app.container.settings.state.collectAsState()
            val collections by app.container.linkRepository.observeCollections().collectAsState(emptyList())
            EasyLinkAppTheme(settings.appearanceMode) {
                BulkImportDialog(
                    linkCount = prepared.size,
                    collections = collections,
                    onInbox = { saveBulk(prepared, null, null) },
                    onExisting = { saveBulk(prepared, it, null) },
                    onNew = { saveBulk(prepared, null, it) },
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun saveBulk(prepared: List<PreparedLink>, collectionId: Long?, newCollectionName: String?) {
        lifecycleScope.launch {
            try {
                val targetId = collectionId ?: newCollectionName?.let {
                    app.container.linkRepository.createCollection(it, 0)
                }
                val count = if (targetId == null) {
                    app.container.linkIngestor.ingestPrepared(prepared, source).size
                } else {
                    app.container.linkIngestor.ingestPreparedToCollection(prepared, source, targetId)
                }
                Toast.makeText(this@ShareReceiverActivity, "$count links imported", Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Unable to import shared links", error)
                Toast.makeText(this@ShareReceiverActivity, "Links could not be imported", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    private fun copy(url: String): Boolean = try {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clean_link_clip_label), url))
        true
    } catch (error: Exception) {
        Log.e(LOG_TAG, "Unable to copy cleaned link", error)
        false
    }

    private companion object {
        const val LOG_TAG = "EasyLink"
        const val MAX_SHARED_TEXT_LENGTH = 250_000
    }
}

@Composable
private fun FileImportDialog(
    backup: EasyLinkBackup,
    collections: List<CollectionSummary>,
    onImport: (Boolean, CollectionSummary?) -> Unit,
    onCancel: () -> Unit,
) {
    var collectionMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Import ${backup.links.size} links?") },
        text = { Text("This ${backup.kind.wireName} was shared as an EasyLink file. It has passed format and URL validation.") },
        confirmButton = {
            Column {
                TextButton(onClick = { onImport(false, null) }) { Text(if (backup.kind == BackupKind.FULL) "Merge" else "Create or merge") }
                if (backup.kind != BackupKind.FULL && collections.isNotEmpty()) {
                    TextButton(onClick = { collectionMenu = true }) { Text("Add to existing collection") }
                    DropdownMenu(collectionMenu, { collectionMenu = false }) {
                        collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { collectionMenu = false; onImport(false, item) }) }
                    }
                }
            }
        },
        dismissButton = {
            if (backup.kind == BackupKind.FULL) {
                TextButton(onClick = { onImport(true, null) }) { Text("Replace", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
    )
}

private fun EasyLinkBackup.targetCollection(collection: CollectionSummary): EasyLinkBackup {
    val ref = "target"
    return copy(
        collections = listOf(BackupCollection(ref, collection.name, collection.colorToken, System.currentTimeMillis(), collection.isPinned)),
        links = links.map { it.copy(collectionRefs = listOf(ref)) },
        rules = emptyList(),
    )
}

@Composable
private fun BulkImportDialog(
    linkCount: Int,
    collections: List<CollectionSummary>,
    onInbox: () -> Unit,
    onExisting: (Long) -> Unit,
    onNew: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var collectionMenu by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Import $linkCount links") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose where these tracker-free links should be saved.")
                Button(onClick = onInbox, modifier = Modifier.fillMaxWidth()) { Text("Save to Inbox") }
                if (collections.isNotEmpty()) {
                    Column {
                        Button(onClick = { collectionMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Add to existing collection") }
                        DropdownMenu(collectionMenu, { collectionMenu = false }) {
                            collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { collectionMenu = false; onExisting(item.id) }) }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(newName, { newName = it.take(40) }, label = { Text("New collection name") }, singleLine = true)
                Button(onClick = { onNew(newName.trim()) }, enabled = newName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create collection") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
