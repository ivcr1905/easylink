package com.iv.easylink

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.iv.easylink.utils.LinkExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// We use Activity instead of ComponentActivity/AppCompatActivity because we don't need a UI at all.
class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check if the intent that started this activity is a "Share" intent with text
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                // 2. Extract the URL using your existing singleton
                val links = LinkExtractor.extractLinks(sharedText)
                if (links.isNotEmpty()) {
                    val url = links.first()
                    // 3. Copy to clipboard
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Link", url)
                    clipboard.setPrimaryClip(clip)
                    // 4. Show a quick confirmation Toast
                    Toast.makeText(this, "Link copied by EasyLink!", Toast.LENGTH_SHORT).show()

                    // 5. Phase 2: persist the shared link to the database
                    val repository = (application as EasyLinkApplication).linkRepository
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.saveLink(
                                url = url,
                                sourceApp = "Shared",
                                senderName = "Shared via Share Sheet",
                                isSuspicious = com.iv.easylink.utils.SecurityScanner.isSuspiciousUrl(url),
                            )
                        } catch (_: Exception) {
                            // Best-effort save — don't crash the share flow
                        }
                    }
                } else {
                    Toast.makeText(this, "No link found to copy.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // 6. CRITICAL: Instantly kill the activity so the user stays in Instagram/WhatsApp seamlessly
        finish()
    }
}