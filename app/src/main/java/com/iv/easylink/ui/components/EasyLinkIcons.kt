package com.iv.easylink.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.iv.easylink.model.LinkCategory

/** Semantic icon boundary. Screens depend on product meaning, not vendor icon names. */
object EasyLinkIcons {
    val Add = Icons.Outlined.Add
    val Archive = Icons.Outlined.Archive
    val Automation = Icons.AutoMirrored.Outlined.AltRoute
    val Back = Icons.AutoMirrored.Outlined.ArrowBack
    val Insights = Icons.Outlined.BarChart
    val Bin = Icons.Outlined.DeleteOutline
    val Block = Icons.Outlined.Block
    val Category = Icons.Outlined.Category
    val Collection = Icons.Outlined.Folder
    val CollectionOpen = Icons.Outlined.FolderOpen
    val Copy = Icons.Outlined.ContentCopy
    val Close = Icons.Outlined.Close
    val Check = Icons.Outlined.Check
    val Chevron = Icons.Outlined.ChevronRight
    val CleanUp = Icons.Outlined.DeleteSweep
    val Cleaning = Icons.Outlined.CleaningServices
    val Edit = Icons.Outlined.Edit
    val Favorite = Icons.Outlined.Favorite
    val FavoriteBorder = Icons.Outlined.FavoriteBorder
    val FilterOff = Icons.Outlined.FilterAltOff
    val File = Icons.AutoMirrored.Outlined.InsertDriveFile
    val ImportExport = Icons.Outlined.ImportExport
    val Import = Icons.Outlined.Download
    val Inbox = Icons.Outlined.Inbox
    val Info = Icons.Outlined.Info
    val Library = Icons.AutoMirrored.Outlined.ListAlt
    val Link = Icons.Outlined.Link
    val Menu = Icons.Outlined.Menu
    val More = Icons.Outlined.MoreHoriz
    val MoreVertical = Icons.Outlined.MoreVert
    val MoveDown = Icons.Outlined.KeyboardArrowDown
    val MoveUp = Icons.Outlined.KeyboardArrowUp
    val Notifications = Icons.Outlined.Notifications
    val Privacy = Icons.Outlined.PrivacyTip
    val Customize = Icons.Outlined.Palette
    val Open = Icons.Outlined.OpenInBrowser
    val Pin = Icons.Outlined.PushPin
    val Rate = Icons.Outlined.Star
    val ReadLater = Icons.Outlined.StarOutline
    val Restore = Icons.Outlined.Restore
    val Search = Icons.Outlined.Search
    val Settings = Icons.Outlined.Settings
    val Silence = Icons.AutoMirrored.Outlined.VolumeOff
    val Share = Icons.Outlined.Share
    val Export = Icons.Outlined.Upload
    val Swipe = Icons.Outlined.Tune
    val InboxLists = Icons.Outlined.ViewCarousel
    val Unarchive = Icons.Outlined.Unarchive
    val Warning = Icons.Outlined.WarningAmber
    val Website = Icons.Outlined.Language

    fun category(category: LinkCategory): ImageVector = when (category) {
        LinkCategory.VIDEO -> Icons.Outlined.VideoLibrary
        LinkCategory.ARTICLE -> Icons.AutoMirrored.Outlined.Article
        LinkCategory.SOCIAL -> Icons.Outlined.PeopleAlt
        LinkCategory.SHOPPING -> Icons.Outlined.ShoppingBag
        LinkCategory.MAPS -> Icons.Outlined.Map
        LinkCategory.MUSIC -> Icons.Outlined.Headphones
        LinkCategory.DOCUMENT -> Icons.AutoMirrored.Outlined.Article
        LinkCategory.OTHER -> Icons.Outlined.Language
    }
}
