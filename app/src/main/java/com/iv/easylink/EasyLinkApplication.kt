package com.iv.easylink

import android.app.Application
import com.iv.easylink.data.local.EasyLinkDatabase
import com.iv.easylink.data.repository.LinkRepository
import com.iv.easylink.data.repository.LinkRepositoryImpl

/**
 * Application-level singleton holder.
 * Lazily initialises the Room database and the [LinkRepository] so every component
 * (activities, services, ViewModels) can access them without manual context passing.
 */
class EasyLinkApplication : Application() {

    /** The Room database — created on first access. */
    val database: EasyLinkDatabase by lazy { EasyLinkDatabase.getInstance(this) }

    /** App-wide link repository. */
    val linkRepository: LinkRepository by lazy { LinkRepositoryImpl(database.linkDao()) }
}
