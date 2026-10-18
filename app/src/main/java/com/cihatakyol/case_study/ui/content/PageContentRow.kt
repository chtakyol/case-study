package com.cihatakyol.case_study.ui.content

import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.ContentItem
import com.cihatakyol.case_study.domain.model.Image
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text

/**
 * A single row in the content list. Rows are produced by flattening every page's item tree so the
 * whole document renders in one `LazyColumn`; [depth] records the nesting level for indentation.
 */
sealed interface PageContentRow {
    val id: Int
    val depth: Int

    /** Stable list key. Page and item identifiers come from different spaces, so they are prefixed. */
    val key: String
}

data class PageHeaderRow(override val id: Int, override val depth: Int, val title: String) : PageContentRow {
    override val key: String = "page_$id"
}

data class SectionHeaderRow(override val id: Int, override val depth: Int, val title: String) : PageContentRow {
    override val key: String = "item_$id"
}

data class TextRow(override val id: Int, override val depth: Int, val content: String) : PageContentRow {
    override val key: String = "item_$id"
}

data class ImageRow(override val id: Int, override val depth: Int, val src: String, val title: String) :
    PageContentRow {
    override val key: String = "item_$id"
}

data class ChoiceRow(
    override val id: Int,
    override val depth: Int,
    val content: String,
    val multipleSelection: Boolean,
    val responses: List<Response>,
) : PageContentRow {
    override val key: String = "item_$id"
}

/** Flattens every page, emitting a page header followed by that page's items in depth-first order. */
fun List<Page>.toContentRows(): List<PageContentRow> = flatMap { page ->
    listOf(PageHeaderRow(id = page.id, depth = 0, title = page.title)) +
        page.items.toContentRows(depth = 1)
}

private fun List<ContentItem>.toContentRows(depth: Int): List<PageContentRow> = flatMap { item ->
    when (item) {
        is Section -> listOf(SectionHeaderRow(item.id, depth, item.title)) +
            item.items.toContentRows(depth + 1)

        is Text -> listOf(TextRow(item.id, depth, item.content))

        is Image -> listOf(ImageRow(item.id, depth, item.src, item.title))

        is Choice -> listOf(
            ChoiceRow(
                id = item.id,
                depth = depth,
                content = item.content,
                multipleSelection = item.responseSet.multipleSelection,
                responses = item.responseSet.responses,
            ),
        )
    }
}
