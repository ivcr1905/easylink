package com.iv.easylink.actions

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import com.iv.easylink.utils.LinkExtractor

class LinkActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rawUrl = intent.getStringExtra(EXTRA_URL) ?: return
        val url = LinkExtractor.process(rawUrl)?.url ?: return
        when (intent.action) {
            ACTION_COPY_LINK -> copy(context, url)
            ACTION_OPEN_LINK -> runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun copy(context: Context, url: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("EasyLink URL", url))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_COPY_LINK = "ACTION_COPY_LINK"
        const val ACTION_OPEN_LINK = "ACTION_OPEN_LINK"
        const val EXTRA_URL = "EXTRA_URL"
    }
}
