package com.iv.easylink.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.R
import com.iv.easylink.actions.LinkActionReceiver
import com.iv.easylink.ingestion.LinkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LinkNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentEvents = ArrayDeque<Int>(MAX_RECENT_EVENTS)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            handleNotification(sbn)
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unexpected notification format", error)
        }
    }

    private fun handleNotification(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName || sbn.isOngoing) return
        val app = application as EasyLinkApplication
        if (sbn.packageName in app.container.settings.state.value.ignoredPackages) return
        val notification = sbn.notification ?: return
        val category = notification.category
        if (category != null && category !in ACCEPTED_CATEGORIES) return
        val text = latestNotificationText(notification) ?: return
        val fingerprint = 31 * sbn.key.hashCode() + text.hashCode()
        synchronized(recentEvents) {
            if (fingerprint in recentEvents) return
            if (recentEvents.size == MAX_RECENT_EVENTS) recentEvents.removeFirst()
            recentEvents.addLast(fingerprint)
        }
        val sourceName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(getString(R.string.unknown_app))
        val sender = notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.take(80)

        serviceScope.launch {
            runCatching {
                app.container.linkIngestor.ingest(text, LinkSource(sbn.packageName, sourceName))
            }.onSuccess { links ->
                if (links.isNotEmpty()) showDetectedNotification(links.first().url, links.first().suspicious, links.size, sender, sourceName)
            }.onFailure { error ->
                Log.e(LOG_TAG, "Could not process notification", error)
            }
        }
    }

    private fun latestNotificationText(notification: android.app.Notification): CharSequence? {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        style?.messages?.lastOrNull()?.text?.takeIf { it.isNotBlank() }?.let { return it }
        val extras = notification.extras
        return extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)?.lastOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun showDetectedNotification(url: String, suspicious: Boolean, count: Int, sender: String?, sourceName: String) {
        val openIntent = actionIntent(LinkActionReceiver.ACTION_OPEN_LINK, url, url.hashCode())
        val copyIntent = actionIntent(LinkActionReceiver.ACTION_COPY_LINK, url, url.hashCode() + 1)
        val title = when {
            suspicious -> getString(R.string.suspicious_link_title)
            !sender.isNullOrBlank() -> sender
            else -> getString(R.string.link_detected)
        }
        val body = if (count > 1) resources.getQuantityString(R.plurals.links_detected, count, count) else url
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bolt)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(sourceName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_action_copy, getString(R.string.copy), copyIntent)
        if (!suspicious) {
            builder.setContentIntent(openIntent)
                .addAction(R.drawable.ic_action_open, getString(R.string.open), openIntent)
        }
        try {
            NotificationManagerCompat.from(this).notify(url.hashCode(), builder.build())
        } catch (error: SecurityException) {
            Log.w(LOG_TAG, "Notification permission is unavailable", error)
        }
    }

    private fun actionIntent(action: String, url: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, LinkActionReceiver::class.java).apply {
            this.action = action
            putExtra(LinkActionReceiver.EXTRA_URL, url)
        }
        return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.channel_detected_links), NotificationManager.IMPORTANCE_HIGH).apply {
            description = getString(R.string.channel_detected_links_description)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val LOG_TAG = "EasyLink"
        const val CHANNEL_ID = "easyLink_detected_links"
        const val MAX_RECENT_EVENTS = 64
        val ACCEPTED_CATEGORIES = setOf(
            NotificationCompat.CATEGORY_MESSAGE,
            NotificationCompat.CATEGORY_SOCIAL,
            NotificationCompat.CATEGORY_EMAIL,
        )
    }
}
