package com.iv.easylink.data.transfer

data class EasyLinkBackup(
    val kind: BackupKind,
    val exportedAt: Long,
    val name: String? = null,
    val collections: List<BackupCollection> = emptyList(),
    val links: List<BackupLink> = emptyList(),
    val rules: List<BackupRule> = emptyList(),
)

enum class BackupKind(val wireName: String) {
    FULL("backup"), COLLECTION("collection"), CATEGORY("category");

    companion object {
        fun fromWireName(value: String): BackupKind? = entries.firstOrNull { it.wireName == value }
    }
}

data class BackupCollection(
    val ref: String,
    val name: String,
    val colorToken: Int,
    val createdAt: Long,
    val isPinned: Boolean,
)

data class BackupLink(
    val ref: String,
    val url: String,
    val sourcePackage: String?,
    val sourceApp: String,
    val senderName: String?,
    val conversationName: String?,
    val timestamp: Long,
    val category: Int,
    val isSuspicious: Boolean,
    val isFavorite: Boolean,
    val wasCleaned: Boolean,
    val title: String?,
    val siteName: String?,
    val archivedAt: Long?,
    val deletedAt: Long?,
    val collectionRefs: List<String>,
    val originalUrl: String? = null,
    val isReadLater: Boolean = false,
)

data class BackupRule(
    val name: String,
    val hostPattern: String,
    val pathContains: String?,
    val category: Int?,
    val collectionRef: String?,
    val enabled: Boolean,
    val priority: Int,
)

data class ImportResult(val importedLinks: Int, val importedCollections: Int)

class InvalidBackupException(message: String) : Exception(message)
