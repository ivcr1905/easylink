package com.iv.easylink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.iv.easylink.ingestion.LinkSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Toast.makeText(this, "No link found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val ingestor = (application as EasyLinkApplication).container.linkIngestor
            val prepared = try {
                ingestor.prepare(text)
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Unable to clean shared link", error)
                emptyList()
            }
            if (prepared.isEmpty()) {
                Toast.makeText(this@ShareReceiverActivity, R.string.no_link_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val copied = try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clean_link_clip_label), prepared.first().url))
                true
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Unable to copy cleaned link", error)
                false
            }

            val savedCount = try {
                ingestor.ingestPrepared(
                    prepared,
                    LinkSource(packageName = null, appName = getString(R.string.source_shared)),
                ).size
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
            Toast.makeText(
                this@ShareReceiverActivity,
                message,
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    private companion object {
        const val LOG_TAG = "EasyLink"
    }
}
