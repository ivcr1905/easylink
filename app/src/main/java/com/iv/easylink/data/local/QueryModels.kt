package com.iv.easylink.data.local

data class CountByKey(val key: String, val count: Int)

data class CollectionSummary(
    val id: Long,
    val name: String,
    val colorToken: Int,
    val linkCount: Int,
    val isPinned: Boolean,
)

data class SourceAppSummary(
    val packageName: String,
    val appName: String,
    val linkCount: Int,
)

data class SenderSummary(
    val senderName: String,
    val packageName: String,
    val appName: String,
    val linkCount: Int,
)
