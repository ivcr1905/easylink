package com.iv.easylink.data.settings

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode { SYSTEM, LIGHT, DARK }
enum class SwipeMode { LINK_ACTIONS, BROWSE_LISTS, NO_SWIPE }
enum class MuteBehavior { SILENCE_ALERTS, IGNORE_FUTURE_LINKS, ASK_EVERY_TIME }
enum class SenderRuleBehavior { SILENCE_ALERTS, IGNORE_FUTURE_LINKS }
enum class InboxShortcutType { ALL, RECENT, FAVORITES, READ_LATER, CATEGORY, COLLECTION }

data class InboxShortcut(val type: InboxShortcutType, val targetId: Long = 0) {
    val storageKey: String get() = "${type.name}:$targetId"

    companion object {
        val defaults = listOf(
            InboxShortcut(InboxShortcutType.ALL),
            InboxShortcut(InboxShortcutType.RECENT),
            InboxShortcut(InboxShortcutType.FAVORITES),
            InboxShortcut(InboxShortcutType.READ_LATER),
        )

        fun parse(value: String): InboxShortcut? {
            val separator = value.indexOf(':')
            if (separator <= 0) return null
            val type = runCatching { InboxShortcutType.valueOf(value.substring(0, separator)) }.getOrNull() ?: return null
            val target = value.substring(separator + 1).toLongOrNull() ?: return null
            if (target < 0 || (type != InboxShortcutType.CATEGORY && type != InboxShortcutType.COLLECTION && target != 0L)) return null
            return InboxShortcut(type, target)
        }
    }
}

data class SenderRuleKey(val sourcePackage: String, val senderName: String)

data class MutedSenderRule(
    val sourcePackage: String,
    val sourceApp: String,
    val senderName: String,
    val behavior: SenderRuleBehavior,
) {
    val key: SenderRuleKey get() = SenderRuleKey(sourcePackage, senderName)
}

data class SettingsSnapshot(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val richPreviews: Boolean = false,
    val retentionDays: Int = 0,
    val ignoredPackages: Set<String> = emptySet(),
    val onboardingSeen: Boolean = false,
    val inboxShortcuts: List<InboxShortcut> = InboxShortcut.defaults,
    val swipeMode: SwipeMode = SwipeMode.LINK_ACTIONS,
    val muteBehavior: MuteBehavior = MuteBehavior.SILENCE_ALERTS,
    val moveExistingOnIgnore: Boolean = false,
    val mutedSenders: Map<SenderRuleKey, MutedSenderRule> = emptyMap(),
)

interface AppSettings {
    val state: StateFlow<SettingsSnapshot>
    fun setAppearanceMode(value: AppearanceMode)
    fun setRichPreviews(value: Boolean)
    fun setRetentionDays(value: Int)
    fun setPackageIgnored(packageName: String, ignored: Boolean)
    fun setOnboardingSeen()
    fun setInboxShortcuts(value: List<InboxShortcut>)
    fun setSwipeMode(value: SwipeMode)
    fun setMuteBehavior(value: MuteBehavior)
    fun setMoveExistingOnIgnore(value: Boolean)
    fun muteSender(sourcePackage: String, sourceApp: String, senderName: String, behavior: SenderRuleBehavior)
    fun unmuteSender(sourcePackage: String, senderName: String)
}

class SharedPreferencesAppSettings(context: Context) : AppSettings {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    override val state: StateFlow<SettingsSnapshot> = mutableState.asStateFlow()

    override fun setAppearanceMode(value: AppearanceMode) = update { copy(appearanceMode = value) }
    override fun setRichPreviews(value: Boolean) = update { copy(richPreviews = value) }
    override fun setRetentionDays(value: Int) = update { copy(retentionDays = value.coerceIn(0, 3650)) }
    override fun setPackageIgnored(packageName: String, ignored: Boolean) = update {
        val next = ignoredPackages.toMutableSet()
        if (ignored) next.add(packageName) else next.remove(packageName)
        copy(ignoredPackages = next.toSet())
    }
    override fun setOnboardingSeen() = update { copy(onboardingSeen = true) }
    override fun setInboxShortcuts(value: List<InboxShortcut>) = update { copy(inboxShortcuts = sanitizeShortcuts(value)) }
    override fun setSwipeMode(value: SwipeMode) = update { copy(swipeMode = value) }
    override fun setMuteBehavior(value: MuteBehavior) = update { copy(muteBehavior = value) }
    override fun setMoveExistingOnIgnore(value: Boolean) = update { copy(moveExistingOnIgnore = value) }

    override fun muteSender(sourcePackage: String, sourceApp: String, senderName: String, behavior: SenderRuleBehavior) {
        val packageName = sourcePackage.trim().take(MAX_PACKAGE_LENGTH)
        val name = senderName.trim().take(MAX_SENDER_LENGTH)
        if (packageName.isEmpty() || name.isEmpty()) return
        val rule = MutedSenderRule(packageName, sourceApp.trim().take(MAX_APP_LENGTH), name, behavior)
        update { copy(mutedSenders = mutedSenders + (rule.key to rule)) }
    }

    override fun unmuteSender(sourcePackage: String, senderName: String) = update {
        copy(mutedSenders = mutedSenders - SenderRuleKey(sourcePackage.trim(), senderName.trim()))
    }

    @Synchronized
    private fun update(transform: SettingsSnapshot.() -> SettingsSnapshot) {
        val next = mutableState.value.transform()
        preferences.edit {
            putString(KEY_APPEARANCE, next.appearanceMode.name)
            putBoolean(KEY_PREVIEWS, next.richPreviews)
            putInt(KEY_RETENTION, next.retentionDays)
            putStringSet(KEY_IGNORED, next.ignoredPackages)
            putBoolean(KEY_ONBOARDING, next.onboardingSeen)
            putString(KEY_SHORTCUTS, next.inboxShortcuts.joinToString(SHORTCUT_SEPARATOR.toString()) { it.storageKey })
            putString(KEY_SWIPE_MODE, next.swipeMode.name)
            putString(KEY_MUTE_BEHAVIOR, next.muteBehavior.name)
            putBoolean(KEY_MOVE_EXISTING, next.moveExistingOnIgnore)
            putStringSet(KEY_MUTED_SENDERS, next.mutedSenders.values.mapTo(HashSet(next.mutedSenders.size), ::encodeRule))
        }
        mutableState.value = next
    }

    private fun read(): SettingsSnapshot {
        val shortcuts = preferences.getString(KEY_SHORTCUTS, null)
            ?.split(SHORTCUT_SEPARATOR)
            ?.mapNotNull(InboxShortcut::parse)
            .orEmpty()
        val muted = preferences.getStringSet(KEY_MUTED_SENDERS, emptySet()).orEmpty()
            .mapNotNull(::decodeRule)
            .associateBy(MutedSenderRule::key)
        return SettingsSnapshot(
            appearanceMode = preferences.enumValue(KEY_APPEARANCE, AppearanceMode.SYSTEM),
            richPreviews = preferences.getBoolean(KEY_PREVIEWS, false),
            retentionDays = preferences.getInt(KEY_RETENTION, 0),
            ignoredPackages = preferences.getStringSet(KEY_IGNORED, emptySet())?.toSet().orEmpty(),
            onboardingSeen = preferences.getBoolean(KEY_ONBOARDING, false),
            inboxShortcuts = sanitizeShortcuts(shortcuts),
            swipeMode = preferences.enumValue(KEY_SWIPE_MODE, SwipeMode.LINK_ACTIONS),
            muteBehavior = preferences.enumValue(KEY_MUTE_BEHAVIOR, MuteBehavior.SILENCE_ALERTS),
            moveExistingOnIgnore = preferences.getBoolean(KEY_MOVE_EXISTING, false),
            mutedSenders = muted,
        )
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumValue(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(getString(key, null) ?: fallback.name) }.getOrDefault(fallback)

    private companion object {
        const val FILE_NAME = "easylink_settings"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_PREVIEWS = "rich_previews"
        const val KEY_RETENTION = "retention_days"
        const val KEY_IGNORED = "ignored_packages"
        const val KEY_ONBOARDING = "onboarding_seen"
        const val KEY_SHORTCUTS = "inbox_shortcuts"
        const val KEY_SWIPE_MODE = "swipe_mode"
        const val KEY_MUTE_BEHAVIOR = "mute_behavior"
        const val KEY_MOVE_EXISTING = "move_existing_on_ignore"
        const val KEY_MUTED_SENDERS = "muted_senders"
        const val SHORTCUT_SEPARATOR = ';'
        const val RULE_SEPARATOR = '|'
        const val MAX_PACKAGE_LENGTH = 200
        const val MAX_APP_LENGTH = 80
        const val MAX_SENDER_LENGTH = 80

        fun sanitizeShortcuts(value: List<InboxShortcut>): List<InboxShortcut> {
            if (value.isEmpty()) return InboxShortcut.defaults
            val result = ArrayList<InboxShortcut>(5)
            result.add(InboxShortcut(InboxShortcutType.ALL))
            value.forEach { shortcut ->
                if (result.size < 5 && shortcut.type != InboxShortcutType.ALL && shortcut !in result) result.add(shortcut)
            }
            return result
        }

        fun encodeRule(rule: MutedSenderRule): String = listOf(
            rule.behavior.name,
            encode(rule.sourcePackage),
            encode(rule.sourceApp),
            encode(rule.senderName),
        ).joinToString(RULE_SEPARATOR.toString())

        fun decodeRule(value: String): MutedSenderRule? {
            val parts = value.split(RULE_SEPARATOR, limit = 4)
            if (parts.size != 4) return null
            val behavior = runCatching { SenderRuleBehavior.valueOf(parts[0]) }.getOrNull() ?: return null
            val sourcePackage = decode(parts[1])?.take(MAX_PACKAGE_LENGTH)?.takeIf(String::isNotBlank) ?: return null
            val sourceApp = decode(parts[2])?.take(MAX_APP_LENGTH).orEmpty()
            val sender = decode(parts[3])?.take(MAX_SENDER_LENGTH)?.takeIf(String::isNotBlank) ?: return null
            return MutedSenderRule(sourcePackage, sourceApp, sender, behavior)
        }

        fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        fun decode(value: String): String? = runCatching {
            Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8)
        }.getOrNull()
    }
}
