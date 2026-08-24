package com.iv.easylink

import android.content.Context
import com.iv.easylink.data.local.EasyLinkDatabase
import com.iv.easylink.data.metadata.MetadataClient
import com.iv.easylink.data.metadata.PreviewImageLoader
import com.iv.easylink.data.repository.LinkRepository
import com.iv.easylink.data.repository.LinkRepositoryImpl
import com.iv.easylink.data.settings.AppSettings
import com.iv.easylink.data.settings.SharedPreferencesAppSettings
import com.iv.easylink.ingestion.LinkIngestor

interface AppContainer {
    val linkRepository: LinkRepository
    val settings: AppSettings
    val linkIngestor: LinkIngestor
    val metadataClient: MetadataClient
    val previewImageLoader: PreviewImageLoader
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { EasyLinkDatabase.getInstance(context) }
    override val linkRepository: LinkRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { LinkRepositoryImpl(database.linkDao()) }
    override val settings: AppSettings by lazy(LazyThreadSafetyMode.NONE) { SharedPreferencesAppSettings(context) }
    override val linkIngestor: LinkIngestor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { LinkIngestor(linkRepository) }
    override val metadataClient: MetadataClient by lazy(LazyThreadSafetyMode.NONE) { MetadataClient() }
    override val previewImageLoader: PreviewImageLoader by lazy(LazyThreadSafetyMode.NONE) { PreviewImageLoader() }
}
