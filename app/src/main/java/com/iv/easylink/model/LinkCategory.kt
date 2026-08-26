package com.iv.easylink.model

enum class LinkCategory(val code: Int, val label: String) {
    OTHER(0, "Other"), VIDEO(1, "Videos"), ARTICLE(2, "Articles"),
    SOCIAL(3, "Social"), SHOPPING(4, "Shopping"), MAPS(5, "Maps"),
    MUSIC(6, "Music"), DOCUMENT(7, "Documents");

    companion object {
        fun fromCode(code: Int): LinkCategory = entries.firstOrNull { it.code == code } ?: OTHER

        val displayEntries = listOf(VIDEO, ARTICLE, SOCIAL, SHOPPING, MAPS, MUSIC, DOCUMENT, OTHER)
    }
}
