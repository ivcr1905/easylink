package com.iv.easylink.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.EasyLinkBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CollectionShareFormatter {
    const val MAX_MESSAGE_CHARS = 240_000

    fun format(backup: EasyLinkBackup): String? {
        val text = StringBuilder(minOf(MAX_MESSAGE_CHARS, 4_096))
        fun append(value: String): Boolean {
            if (text.length + value.length > MAX_MESSAGE_CHARS) return false
            text.append(value)
            return true
        }
        if (!append("EasyLink ${backup.kind.wireName}: ${backup.name?.cleanLine() ?: "Shared links"}\n")) return null
        if (!append("${backup.links.size} link${if (backup.links.size == 1) "" else "s"}\n\n")) return null
        backup.links.forEachIndexed { index, link ->
            link.title?.cleanLine()?.takeIf { it.isNotBlank() }?.let { if (!append("$it\n")) return null }
            if (!append(link.url)) return null
            if (index != backup.links.lastIndex && !append("\n\n")) return null
        }
        return text.toString()
    }

    private fun String.cleanLine(): String = replace('\n', ' ').replace('\r', ' ').trim()
}

fun copyBackup(context: Context, backup: EasyLinkBackup): Boolean {
    val text = CollectionShareFormatter.format(backup) ?: return false
    return try {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("EasyLink ${backup.kind.wireName}", text))
            ?: return false
        true
    } catch (_: Exception) {
        false
    }
}

fun shareBackupText(context: Context, backup: EasyLinkBackup): Boolean {
    val text = CollectionShareFormatter.format(backup) ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, backup.name ?: "EasyLink links")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Share links"))
        true
    } catch (_: Exception) {
        false
    }
}

suspend fun shareBackupFile(context: Context, backup: EasyLinkBackup) {
    val uri = withContext(Dispatchers.IO) { createBackupFile(context, backup) }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = BackupCodec.MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("EasyLink file", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share EasyLink file"))
}

private fun createBackupFile(context: Context, backup: EasyLinkBackup): Uri {
    val directory = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val cutoff = System.currentTimeMillis() - 86_400_000L
    directory.listFiles()?.forEach { if (it.isFile && it.lastModified() < cutoff) it.delete() }
    val safeName = backup.name.orEmpty().lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').take(48)
        .ifBlank { backup.kind.wireName }
    val file = File(directory, "$safeName-${System.currentTimeMillis()}${BackupCodec.FILE_SUFFIX}")
    file.outputStream().buffered().use { BackupCodec.write(backup, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
