package com.cihatakyol.case_study.data.local.database.mapper

import com.cihatakyol.case_study.data.local.database.ContentItemType
import com.cihatakyol.case_study.data.remote.dto.PageDto
import com.cihatakyol.case_study.data.remote.mapper.toDocumentEntities
import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.Image
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import com.cihatakyol.case_study.sampleContentJson
import com.cihatakyol.case_study.testJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentMapperTest {

    private val document = testJson
        .decodeFromString<List<PageDto>>(sampleContentJson())
        .toDocumentEntities()

    @Test
    fun `flattens the document preserving pages and nesting`() {
        assertEquals(2, document.pages.size)
        assertEquals(listOf("Main Page", "Second Page"), document.pages.sortedBy { it.position }.map { it.title })

        val introduction = document.items.single { it.id == 2 }
        assertEquals(ContentItemType.SECTION, introduction.type)
        assertNull(introduction.parentId)
        assertEquals(
            listOf(3, 4),
            document.items.filter { it.parentId == 2 }.sortedBy { it.position }.map { it.id },
        )

        val subsection = document.items.single { it.id == 7 }
        assertEquals(ContentItemType.SECTION, subsection.type)
        assertEquals(5, subsection.parentId)
        assertEquals(
            listOf(8, 9),
            document.items.filter { it.parentId == 7 }.sortedBy { it.position }.map { it.id },
        )
    }

    @Test
    fun `captures choice response sets and null scores`() {
        val choice = document.items.single { it.id == 13 }
        assertEquals(ContentItemType.CHOICE, choice.type)
        assertEquals(101, choice.responseSetId)

        val responseSet = document.responseSets.single { it.id == 101 }
        assertEquals(13, responseSet.itemId)
        assertFalse(responseSet.multipleSelection)

        val responses = document.responses
            .filter { it.responseSetId == 101 }
            .sortedBy { it.position }
        assertEquals(listOf(1011, 1012, 1013), responses.map { it.id })
        assertNull(responses[2].score)

        assertTrue(document.responseSets.single { it.id == 102 }.multipleSelection)
    }

    @Test
    fun `rebuilds the recursive domain document`() {
        val pages = buildPages(
            pages = document.pages,
            items = document.items,
            responseSets = document.responseSets,
            responses = document.responses,
        )

        assertEquals(listOf("Main Page", "Second Page"), pages.map { it.title })

        val main = pages[0]
        val introduction = main.items.filterIsInstance<Section>().first { it.title == "Introduction" }
        assertTrue(introduction.items.any { it is Text })
        assertTrue(introduction.items.any { it is Image })

        val chapter1 = main.items.filterIsInstance<Section>().first { it.title == "Chapter 1" }
        val subsection = chapter1.items.filterIsInstance<Section>().single()
        assertEquals("Subsection 1.1", subsection.title)
        assertEquals(listOf(8, 9), subsection.items.map { it.id })

        val chapter2 = pages[1].items.filterIsInstance<Section>().single()
        val choices = chapter2.items.filterIsInstance<Choice>()
        assertEquals(2, choices.size)
        assertEquals(3, choices[0].responseSet.responses.size)
        assertNull(choices[0].responseSet.responses[2].score)
        assertTrue(choices[1].responseSet.multipleSelection)
        assertTrue(choices[1].responseSet.responses.all { it.score == null })
    }

    @Test
    fun `rebuilds a single page by id`() {
        val main = buildPage(
            page = document.pages.single { it.id == 1 },
            items = document.items,
            responseSets = document.responseSets,
            responses = document.responses,
        )

        assertEquals("Main Page", main?.title)
        assertTrue(main!!.items.any { it is Section })

        val missing = buildPage(
            page = document.pages.firstOrNull { it.id == 999 },
            items = document.items,
            responseSets = document.responseSets,
            responses = document.responses,
        )
        assertNull(missing)
    }
}
