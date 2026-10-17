package com.cihatakyol.case_study.data.remote.dto

import com.cihatakyol.case_study.testJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnknownItemTypeTest {

    @Test
    fun `skips an unrecognized item type and still deserializes the rest`() {
        val payload = """
            [
              {
                "id": 1,
                "type": "page",
                "title": "Page",
                "items": [
                  { "id": 2, "type": "text", "content": "keep me" },
                  { "id": 3, "type": "video", "content": "drop me" },
                  { "id": 4, "type": "image", "src": "https://example.com/a.png", "title": "me too" }
                ]
              }
            ]
        """.trimIndent()

        val pages = testJson.decodeFromString<List<PageDto>>(payload)

        val items = pages.single().items
        assertEquals("the unknown item type should be dropped", 2, items.size)
        assertTrue(items[0] is TextDto)
        assertTrue(items[1] is ImageDto)
    }

    @Test
    fun `skips an unrecognized nested item type`() {
        val payload = """
            [
              {
                "id": 1,
                "type": "page",
                "title": "Page",
                "items": [
                  {
                    "id": 2,
                    "type": "section",
                    "title": "Section",
                    "items": [
                      { "id": 3, "type": "audio", "src": "drop me" },
                      { "id": 4, "type": "text", "content": "keep me" }
                    ]
                  }
                ]
              }
            ]
        """.trimIndent()

        val pages = testJson.decodeFromString<List<PageDto>>(payload)

        val section = pages.single().items.filterIsInstance<SectionDto>().single()
        assertEquals(1, section.items.size)
        assertTrue(section.items.single() is TextDto)
    }
}
