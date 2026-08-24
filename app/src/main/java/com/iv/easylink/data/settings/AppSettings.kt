package com.iv.easylink.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

data class SettingsSnapshot(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val richPreviews: Boolean = true,
    val retentionDays: Int = 0,
    val ignoredPackages: Set<String> = emptySet(),
    val onboardingSeen: Boolean = false,
)

interface AppSettings {
    val state: StateFlow<SettingsSnapshot>
    fun setAppearanceMode(value: AppearanceMode)
    fun setRichPreviews(value: Boolean)
    fun setRetentionDays(value: Int)
    fun setPackageIgnored(packageName: String, ignored: Boolean)
    fun setOnboardingSeen()
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
        copy(ignoredPackages = next)
    }
    override fun setOnboardingSeen() = update { copy(onboardingSeen = true) }

    private inline fun update(transform: SettingsSnapshot.() -> SettingsSnapshot) {
        val next = mutableState.value.transform()
        preferences.edit {
            putString(KEY_APPEARANCE, next.appearanceMode.name)
            putBoolean(KEY_PREVIEWS, next.richPreviews)
            putInt(KEY_RETENTION, next.retentionDays)
            putStringSet(KEY_IGNORED, next.ignoredPackages)
            putBoolean(KEY_ONBOARDING, next.onboardingSeen)
        }
        mutableState.value = next
    }

    private fun read(): SettingsSnapshot {
        val appearance = runCatching {
            AppearanceMode.valueOf(preferences.getString(KEY_APPEARANCE, null) ?: AppearanceMode.SYSTEM.name)
        }.getOrDefault(AppearanceMode.SYSTEM)
        return SettingsSnapshot(
            appearanceMode = appearance,
            richPreviews = preferences.getBoolean(KEY_PREVIEWS, true),
            retentionDays = preferences.getInt(KEY_RETENTION, 0),
            ignoredPackages = preferences.getStringSet(KEY_IGNORED, emptySet())?.toSet().orEmpty(),
            onboardingSeen = preferences.getBoolean(KEY_ONBOARDING, false),
        )
    }

    private companion object {
        const val FILE_NAME = "easylink_settings"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_PREVIEWS = "rich_previews"
        const val KEY_RETENTION = "retention_days"
        const val KEY_IGNORED = "ignored_packages"
        const val KEY_ONBOARDING = "onboarding_seen"
    }
}
