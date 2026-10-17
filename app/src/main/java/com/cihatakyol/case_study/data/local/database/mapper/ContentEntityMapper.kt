package com.cihatakyol.case_study.data.local.database.mapper

import com.cihatakyol.case_study.data.local.database.ContentItemType
import com.cihatakyol.case_study.data.local.database.entity.ContentItemEntity
import com.cihatakyol.case_study.data.local.database.entity.PageEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseSetEntity
import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.ContentItem
import com.cihatakyol.case_study.domain.model.Image
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.domain.model.ResponseSet
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text

/** Reassembles the flat Room rows into the recursive domain document. */
fun buildPages(
    pages: List<PageEntity>,
    items: List<ContentItemEntity>,
    responseSets: List<ResponseSetEntity>,
    responses: List<ResponseEntity>,
): List<Page> {
    val responsesBySet = responses
        .groupBy { it.responseSetId }
        .mapValues { (_, values) ->
            values.sortedBy { it.position }.map { Response(it.id, it.label, it.score) }
        }
    val responseSetsById = responseSets.associateBy { it.id }
    val itemsByParent = items.groupBy { it.pageId to it.parentId }

    fun buildItems(pageId: Int, parentId: Int?): List<ContentItem> = itemsByParent[pageId to parentId]
        .orEmpty()
        .sortedBy { it.position }
        .mapNotNull { entity ->
            when (entity.type) {
                ContentItemType.SECTION -> Section(
                    id = entity.id,
                    title = entity.title.orEmpty(),
                    items = buildItems(pageId, entity.id),
                )

                ContentItemType.TEXT -> Text(
                    id = entity.id,
                    content = entity.content.orEmpty(),
                )

                ContentItemType.IMAGE -> Image(
                    id = entity.id,
                    src = entity.src.orEmpty(),
                    title = entity.title.orEmpty(),
                )

                ContentItemType.CHOICE -> {
                    val responseSet = entity.responseSetId?.let(responseSetsById::get)
                    Choice(
                        id = entity.id,
                        content = entity.content.orEmpty(),
                        responseSet = ResponseSet(
                            id = responseSet?.id ?: entity.responseSetId ?: 0,
                            multipleSelection = responseSet?.multipleSelection ?: false,
                            responses = responseSet?.let { responsesBySet[it.id] }.orEmpty(),
                        ),
                    )
                }

                else -> null
            }
        }

    return pages
        .sortedBy { it.position }
        .map { page -> Page(id = page.id, title = page.title, items = buildItems(page.id, null)) }
}

/** Reassembles a single page's tree, reusing the whole-document reassembly in [buildPages]. */
fun buildPage(
    page: PageEntity?,
    items: List<ContentItemEntity>,
    responseSets: List<ResponseSetEntity>,
    responses: List<ResponseEntity>,
): Page? = page?.let { buildPages(listOf(it), items, responseSets, responses).firstOrNull() }
