package com.iv.easylink.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iv.easylink.actions.LinkActionReceiver
import com.iv.easylink.utils.LinkExtractor
import androidx.core.net.toUri
import com.iv.easylink.R
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Bitmap
import com.iv.easylink.utils.SecurityScanner.isSuspiciousUrl

class LinkNotificationListener : NotificationListenerService() {
    private var lastProcessedKey: String? = null
    private var lastProcessedUrl: String? = null

    companion object {
        private const val CHANNEL_ID = "easyLink_detected_links"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        try {
            if(sbn==null){
                return
            }
            if(sbn.packageName == applicationContext.packageName || sbn.isOngoing) {
                return
            }
            val notificationCategory = sbn.notification?.category
            val categoryList = listOf(
                NotificationCompat.CATEGORY_MESSAGE,
                NotificationCompat.CATEGORY_SOCIAL,
                NotificationCompat.CATEGORY_EMAIL)
            if (notificationCategory != null && !categoryList.contains(notificationCategory)){
                return
            }
            var textToParse: String?
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
            if (messagingStyle != null) {
                textToParse = messagingStyle.messages.lastOrNull()?.text?.toString()
            } else {
                // The fallback for basic SMS apps
                textToParse = sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
            }
            val extractedLinks = LinkExtractor.extractLinks(textToParse)
            if (extractedLinks.isEmpty()) {
                return
            }
            val primaryLink = extractedLinks[0]
            if (sbn.key == lastProcessedKey && primaryLink == lastProcessedUrl) return
            lastProcessedKey = sbn.key
            lastProcessedUrl = primaryLink
            val isSuspicious = isSuspiciousUrl(primaryLink)
            val pm = applicationContext.packageManager
            val extras = sbn.notification.extras
            val senderName = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: "Unknown Sender"
            var appName = "Unknown App"
            var appIconBitmap : Bitmap? = null
            try {
                appName = pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                // Fetch the actual app icon (like the WhatsApp logo) and convert it to a Bitmap
                appIconBitmap = pm.getApplicationIcon(sbn.packageName).toBitmap()
            } catch (e: Exception) {
                // Failsafe in case the app icon can't be found
            }
            showLinkNotification(primaryLink, isSuspicious, senderName, appName, appIconBitmap)
        } catch (e : Exception){
            Log.e("EasyLink", "Failed to process notification safely", e)
        }
    }

    private fun showLinkNotification(url : String, isSuspicious : Boolean, senderName : String, appName: String, appIcon: Bitmap?) {
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        val openPendingIntent = PendingIntent.getActivity(this,
            url.hashCode(),
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val copyIntent = Intent(this, LinkActionReceiver::class.java).apply {
            action = "ACTION_COPY_LINK"
            putExtra("EXTRA_URL", url)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            this,
            url.hashCode()+1,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this,CHANNEL_ID )
            .setSmallIcon(R.drawable.ic_notification_bolt)
            .setSubText(appName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
        if(isSuspicious){
            builder.setContentTitle("⚠️ $senderName sent a Link that may be unsafe")
            builder.setContentText("This link may be unsafe. Proceed with caution.")
            builder.addAction(R.drawable.ic_action_copy, "Copy Link", copyPendingIntent)
        } else {
            builder.setContentTitle(senderName)
            builder.setContentText(url)
            builder.setContentIntent(openPendingIntent)
            builder.addAction(R.drawable.ic_action_open,"Open",openPendingIntent)
            builder.addAction(R.drawable.ic_action_copy,"Copy",copyPendingIntent)
        }
        if(appIcon != null) builder.setLargeIcon(appIcon)
        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(url.hashCode(),builder.build())
        } catch (e : SecurityException) {
            Log.e("EasyLink", "Failed to show notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Detected Links"
            val descriptionText = "Shows actions for links found in your messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


}