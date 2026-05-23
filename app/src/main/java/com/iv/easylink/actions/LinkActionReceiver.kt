package com.iv.easylink.actions

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

class LinkActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_COPY_LINK") {
            val url = intent.getStringExtra("EXTRA_URL") ?: return

            // 1. Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Link", url)
            clipboard.setPrimaryClip(clip)

            // 2. SMART TOAST: Only show our Toast on Android 12 and below.
            // Android 13+ automatically shows a slick clipboard UI at the bottom.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}