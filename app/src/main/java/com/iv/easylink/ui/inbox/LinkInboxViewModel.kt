package com.iv.easylink.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.repository.LinkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Link Inbox screen.
 *
 * Uses [AndroidViewModel] so we can access the [Application] to obtain the repository
 * without needing a DI framework. The repository instance comes from [EasyLinkApplication].
 */
class LinkInboxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LinkRepository =
        (application as EasyLinkApplication).linkRepository

    /** All saved links, newest first. Emits an empty list initially while the DB loads. */
    val links: StateFlow<List<LinkEntity>> = repository.getAllLinks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Delete a single link from history. */
    fun deleteLink(link: LinkEntity) {
        viewModelScope.launch { repository.deleteLink(link) }
    }

    /** Clear the entire link history. */
    fun deleteAllLinks() {
        viewModelScope.launch { repository.deleteAllLinks() }
    }
}
