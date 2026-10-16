package com.cihatakyol.case_study.data.remote.mapper

import com.cihatakyol.case_study.data.local.database.ContentItemType
import com.cihatakyol.case_study.data.local.database.entity.ContentItemEntity
import com.cihatakyol.case_study.data.local.database.entity.PageEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseSetEntity
import com.cihatakyol.case_study.data.remote.dto.ChoiceDto
import com.cihatakyol.case_study.data.remote.dto.ContentItemDto
import com.cihatakyol.case_study.data.remote.dto.ImageDto
import com.cihatakyol.case_study.data.remote.dto.PageDto
import com.cihatakyol.case_study.data.remote.dto.SectionDto
import com.cihatakyol.case_study.data.remote.dto.TextDto

/** The full content document flattened into its Room tables. */
data class ContentDocumentEntities(
    val pages: List<PageEntity>,
    val items: List<ContentItemEntity>,
    val responseSets: List<ResponseSetEntity>,
    val responses: List<ResponseEntity>,
)

/** Flattens the recursive wire document into rows ready to be persisted. */
fun List<PageDto>.toDocumentEntities(): ContentDocumentEntities {
    val pages = mutableListOf<PageEntity>()
    val items = mutableListOf<ContentItemEntity>()
    val responseSets = mutableListOf<ResponseSetEntity>()
    val responses = mutableListOf<ResponseEntity>()

    fun flatten(pageId: Int, parentId: Int?, dtos: List<ContentItemDto>) {
        dtos.forEachIndexed { index, dto ->
            when (dto) {
                is SectionDto -> {
                    items += ContentItemEntity(
                        id = dto.id,
                        pageId = pageId,
                        parentId = parentId,
                        position = index,
                        type = ContentItemType.SECTION,
                        title = dto.title,
                    )
                    flatten(pageId, dto.id, dto.items)
                }

                is TextDto -> items += ContentItemEntity(
                    id = dto.id,
                    pageId = pageId,
                    parentId = parentId,
                    position = index,
                    type = ContentItemType.TEXT,
                    content = dto.content,
                )

                is ImageDto -> items += ContentItemEntity(
                    id = dto.id,
                    pageId = pageId,
                    parentId = parentId,
                    position = index,
                    type = ContentItemType.IMAGE,
                    src = dto.src,
                    title = dto.title,
                )

                is ChoiceDto -> {
                    items += ContentItemEntity(
                        id = dto.id,
                        pageId = pageId,
                        parentId = parentId,
                        position = index,
                        type = ContentItemType.CHOICE,
                        content = dto.content,
                        responseSetId = dto.responseSet.id,
                    )
                    responseSets += ResponseSetEntity(
                        id = dto.responseSet.id,
                        itemId = dto.id,
                        multipleSelection = dto.responseSet.multipleSelection,
                    )
                    dto.responseSet.responses.forEachIndexed { responseIndex, response ->
                        responses += ResponseEntity(
                            id = response.id,
                            responseSetId = dto.responseSet.id,
                            position = responseIndex,
                            label = response.label,
                            score = response.score,
                        )
                    }
                }
            }
        }
    }

    forEachIndexed { pageIndex, page ->
        pages += PageEntity(id = page.id, title = page.title, position = pageIndex)
        flatten(page.id, null, page.items)
    }

    return ContentDocumentEntities(
        pages = pages,
        items = items,
        responseSets = responseSets,
        responses = responses,
    )
}
