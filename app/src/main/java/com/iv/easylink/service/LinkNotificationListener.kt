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
import com.iv.easylink.data.settings.SenderRuleBehavior
import com.iv.easylink.data.settings.SenderRuleKey
import com.iv.easylink.ingestion.LinkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
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
        val settings = app.container.settings.state.value
        if (sbn.packageName in settings.ignoredPackages) return
        val notification = sbn.notification ?: return
        val category = notification.category
        if (category != null && category !in ACCEPTED_CATEGORIES) return
        val content = notificationContent(notification) ?: return
        val senderName = content.senderName?.trim()?.take(80)?.takeIf(String::isNotEmpty)
        val senderRule = senderName?.let { settings.mutedSenders[SenderRuleKey(sbn.packageName, it)] }
        if (senderRule?.behavior == SenderRuleBehavior.IGNORE_FUTURE_LINKS) return
        val text = content.text
        val fingerprint = 31 * sbn.key.hashCode() + text.hashCode()
        synchronized(recentEvents) {
            if (fingerprint in recentEvents) return
            if (recentEvents.size == MAX_RECENT_EVENTS) recentEvents.removeFirst()
            recentEvents.addLast(fingerprint)
        }
        val sourceName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(getString(R.string.unknown_app))
        serviceScope.launch {
            try {
                val links = app.container.linkIngestor.ingest(
                    text,
                    LinkSource(
                        packageName = sbn.packageName,
                        appName = sourceName,
                        senderName = senderName,
                        conversationName = content.conversationName,
                    ),
                )
                if (links.isNotEmpty() && senderRule?.behavior != SenderRuleBehavior.SILENCE_ALERTS) {
                    showDetectedNotification(
                        url = links.first().url,
                        suspicious = links.first().suspicious,
                        count = links.size,
                        sender = senderName,
                        sourcePackage = sbn.packageName,
                        sourceName = sourceName,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Could not process notification", error)
            }
        }
    }

    private fun notificationContent(notification: android.app.Notification): NotificationContent? {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val message = style?.messages?.lastOrNull { !it.text.isNullOrBlank() }
        val messageText = message?.text
        if (!messageText.isNullOrBlank()) {
            return NotificationContent(
                text = messageText,
                senderName = message.person?.name?.toString()?.take(80),
                conversationName = style.conversationTitle?.toString()?.take(80),
            )
        }
        val extras = notification.extras
        val text = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)?.lastOrNull()?.takeIf { it.isNotBlank() }
            ?: return null
        return NotificationContent(
            text = text,
            senderName = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.take(80),
            conversationName = extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)?.toString()?.take(80),
        )
    }

    private fun showDetectedNotification(
        url: String,
        suspicious: Boolean,
        count: Int,
        sender: String?,
        sourcePackage: String,
        sourceName: String,
    ) {
        val openIntent = actionIntent(LinkActionReceiver.ACTION_OPEN_LINK, url, url.hashCode())
        val copyIntent = actionIntent(LinkActionReceiver.ACTION_COPY_LINK, url, url.hashCode() + 1)
        val muteIntent = sender?.let {
            muteIntent(sourcePackage, sourceName, it, 31 * url.hashCode() + 2)
        }
        val title = if (sender == null) {
            if (count == 1) "Link from $sourceName" else "$count links from $sourceName"
        } else if (count == 1) {
            "$sender shared a link"
        } else {
            "$sender shared $count links"
        }
        val body = if (suspicious) "Potentially unsafe link · $url" else url
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bolt)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(sourceName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        if (!suspicious) {
            builder.setContentIntent(openIntent)
                .addAction(R.drawable.ic_action_open, getString(R.string.open), openIntent)
        }
        builder.addAction(R.drawable.ic_action_copy, getString(R.string.copy), copyIntent)
        muteIntent?.let { builder.addAction(R.drawable.ic_action_mute, "Mute", it) }
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

    private fun muteIntent(sourcePackage: String, sourceApp: String, senderName: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, LinkActionReceiver::class.java).apply {
            action = LinkActionReceiver.ACTION_MUTE_SENDER
            putExtra(LinkActionReceiver.EXTRA_SOURCE_PACKAGE, sourcePackage)
            putExtra(LinkActionReceiver.EXTRA_SOURCE_APP, sourceApp)
            putExtra(LinkActionReceiver.EXTRA_SENDER_NAME, senderName)
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

    private data class NotificationContent(
        val text: CharSequence,
        val senderName: String?,
        val conversationName: String?,
    )
}
