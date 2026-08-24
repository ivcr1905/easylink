package com.iv.easylink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.iv.easylink.ingestion.LinkSource
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
            val result = runCatching {
                (application as EasyLinkApplication).container.linkIngestor.ingest(
                    text,
                    LinkSource(packageName = null, appName = getString(com.iv.easylink.R.string.source_shared)),
                )
            }.getOrDefault(emptyList())
            Toast.makeText(
                this@ShareReceiverActivity,
                if (result.isEmpty()) getString(com.iv.easylink.R.string.no_link_found) else resources.getQuantityString(com.iv.easylink.R.plurals.links_saved, result.size, result.size),
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }
}
