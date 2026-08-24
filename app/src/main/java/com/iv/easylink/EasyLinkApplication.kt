package com.iv.easylink

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EasyLinkApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DefaultAppContainer(this) }
    val linkRepository get() = container.linkRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val retentionDays = container.settings.state.value.retentionDays
        if (retentionDays > 0) {
            applicationScope.launch {
                container.linkRepository.clearOlderThan(System.currentTimeMillis() - retentionDays * MILLIS_PER_DAY)
            }
        }
    }

    private companion object { const val MILLIS_PER_DAY = 86_400_000L }
}
