package com.cihatakyol.case_study.data.remote.dto

import com.cihatakyol.case_study.sampleContentJson
import com.cihatakyol.case_study.testJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentItemSerializationTest {

    @Test
    fun `deserializes the sample payload with every item variant`() {
        val pages = testJson.decodeFromString<List<PageDto>>(sampleContentJson())

        assertEquals(2, pages.size)
        assertEquals("Main Page", pages[0].title)
        assertEquals("Second Page", pages[1].title)

        val introduction = pages[0].section("Introduction")
        assertTrue(
            "Introduction should contain a text item",
            introduction.items.any { it is TextDto },
        )
        assertTrue(
            "Introduction should contain an image item",
            introduction.items.any { it is ImageDto },
        )

        val chapter1 = pages[0].section("Chapter 1")
        val subsection = chapter1.items.filterIsInstance<SectionDto>().single()
        assertEquals("Subsection 1.1", subsection.title)
        assertTrue(subsection.items.any { it is TextDto })
        assertTrue(subsection.items.any { it is ImageDto })

        val choices = pages[1].items.filterIsInstance<SectionDto>().single()
            .items.filterIsInstance<ChoiceDto>()
        assertEquals(2, choices.size)

        val singleChoice = choices[0]
        assertEquals("What is the main topic of Chapter 2?", singleChoice.content)
        assertEquals(101, singleChoice.responseSet.id)
        assertFalse(singleChoice.responseSet.multipleSelection)
        assertEquals(3, singleChoice.responseSet.responses.size)
        assertEquals(1, singleChoice.responseSet.responses[0].score)
        assertEquals(2, singleChoice.responseSet.responses[1].score)
        assertNull(singleChoice.responseSet.responses[2].score)

        val multiChoice = choices[1]
        assertEquals(102, multiChoice.responseSet.id)
        assertTrue(multiChoice.responseSet.multipleSelection)
        assertTrue(multiChoice.responseSet.responses.all { it.score == null })
    }

    private fun PageDto.section(title: String): SectionDto =
        items.filterIsInstance<SectionDto>().first { it.title == title }
}
