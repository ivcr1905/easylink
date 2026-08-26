package com.iv.easylink

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class EasyLinkApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DefaultAppContainer(this) }
    val linkRepository get() = container.linkRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val retentionDays = container.settings.state.value.retentionDays
        applicationScope.launch {
            try {
                container.linkRepository.purgeExpiredBin(System.currentTimeMillis() - BIN_RETENTION_DAYS * MILLIS_PER_DAY)
                if (retentionDays > 0) {
                    container.linkRepository.clearOlderThan(System.currentTimeMillis() - retentionDays * MILLIS_PER_DAY)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("EasyLink", "Automatic cleanup could not run", error)
            }
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
        const val BIN_RETENTION_DAYS = 30L
    }
}
