package com.cihatakyol.case_study.domain.model

/**
 * A single item in the content document. Items nest: a [Section] holds more [ContentItem]s.
 */
sealed interface ContentItem {
    val id: Int
}

data class Section(override val id: Int, val title: String, val items: List<ContentItem>) : ContentItem

data class Text(override val id: Int, val content: String) : ContentItem

data class Image(override val id: Int, val src: String, val title: String) : ContentItem

data class Choice(override val id: Int, val content: String, val responseSet: ResponseSet) : ContentItem
