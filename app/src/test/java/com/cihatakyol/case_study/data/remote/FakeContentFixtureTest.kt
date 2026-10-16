package com.cihatakyol.case_study.data.remote

import com.cihatakyol.case_study.data.local.database.mapper.buildPages
import com.cihatakyol.case_study.data.remote.dto.PageDto
import com.cihatakyol.case_study.data.remote.mapper.toDocumentEntities
import com.cihatakyol.case_study.debugContentFixtureJson
import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import com.cihatakyol.case_study.testJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeContentFixtureTest {

    @Test
    fun `debug fixture deserializes through the DTOs into the expected domain tree`() {
        val document = testJson
            .decodeFromString<List<PageDto>>(debugContentFixtureJson())
            .toDocumentEntities()

        val pages = buildPages(
            pages = document.pages,
            items = document.items,
            responseSets = document.responseSets,
            responses = document.responses,
        )

        assertEquals(listOf("Main Page", "Second Page"), pages.map { it.title })

        val chapter1 = pages[0].items.filterIsInstance<Section>().first { it.title == "Chapter 1" }
        val subsection = chapter1.items.filterIsInstance<Section>().single()
        assertEquals("Subsection 1.1", subsection.title)
        assertTrue(subsection.items.any { it is Text })

        val chapter2 = pages[1].items.filterIsInstance<Section>().single()
        val choices = chapter2.items.filterIsInstance<Choice>()
        assertEquals(2, choices.size)
        assertEquals(3, choices[0].responseSet.responses.size)
        assertTrue(choices[1].responseSet.multipleSelection)
    }
}
