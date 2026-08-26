package com.iv.easylink.actions

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.data.settings.MuteBehavior
import com.iv.easylink.data.settings.SenderRuleBehavior
import com.iv.easylink.ui.MainActivity
import com.iv.easylink.utils.LinkExtractor

class LinkActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COPY_LINK, ACTION_OPEN_LINK -> {
                val rawUrl = intent.getStringExtra(EXTRA_URL) ?: return
                val url = LinkExtractor.process(rawUrl)?.url ?: return
                if (intent.action == ACTION_COPY_LINK) copy(context, url) else runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show() }
            }
            ACTION_MUTE_SENDER -> muteSender(context, intent)
        }
    }

    private fun muteSender(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_SOURCE_PACKAGE)?.trim().orEmpty()
        val appName = intent.getStringExtra(EXTRA_SOURCE_APP)?.trim().orEmpty()
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME)?.trim().orEmpty()
        if (packageName.isEmpty() || senderName.isEmpty()) return
        val app = context.applicationContext as? EasyLinkApplication ?: return
        val snapshot = app.container.settings.state.value
        val behavior = snapshot.muteBehavior
        if (behavior == MuteBehavior.ASK_EVERY_TIME || behavior == MuteBehavior.IGNORE_FUTURE_LINKS && snapshot.moveExistingOnIgnore) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_CHOOSE_MUTE
                    putExtra(EXTRA_SOURCE_PACKAGE, packageName)
                    putExtra(EXTRA_SOURCE_APP, appName)
                    putExtra(EXTRA_SENDER_NAME, senderName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
            return
        }
        when (behavior) {
            MuteBehavior.ASK_EVERY_TIME -> Unit
            MuteBehavior.SILENCE_ALERTS -> {
                app.container.settings.muteSender(packageName, appName, senderName, SenderRuleBehavior.SILENCE_ALERTS)
                Toast.makeText(context, "Muted $senderName · links will still be saved", Toast.LENGTH_LONG).show()
            }
            MuteBehavior.IGNORE_FUTURE_LINKS -> {
                app.container.settings.muteSender(packageName, appName, senderName, SenderRuleBehavior.IGNORE_FUTURE_LINKS)
                Toast.makeText(context, "Ignoring $senderName · existing links kept", Toast.LENGTH_LONG).show()
            }
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
        const val ACTION_MUTE_SENDER = "ACTION_MUTE_SENDER"
        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_SOURCE_PACKAGE = "EXTRA_SOURCE_PACKAGE"
        const val EXTRA_SOURCE_APP = "EXTRA_SOURCE_APP"
        const val EXTRA_SENDER_NAME = "EXTRA_SENDER_NAME"
    }
}
