package com.cihatakyol.case_study.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cihatakyol.case_study.data.remote.dto.PageDto
import com.cihatakyol.case_study.data.remote.mapper.toDocumentEntities
import com.cihatakyol.case_study.sampleContentJson
import com.cihatakyol.case_study.testJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ContentDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.contentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `persists a document preserving nesting and sibling order`() = runTest {
        val document = testJson
            .decodeFromString<List<PageDto>>(sampleContentJson())
            .toDocumentEntities()

        dao.replaceAll(document.pages, document.items, document.responseSets, document.responses)

        assertEquals(2, dao.observePages().first().size)
        assertEquals(2, dao.observeResponseSets().first().size)
        assertEquals(6, dao.observeResponses().first().size)

        val items = dao.observeItems().first()
        assertEquals(12, items.size)
        assertEquals(
            listOf(3, 4),
            items.filter { it.parentId == 2 }.sortedBy { it.position }.map { it.id },
        )
        assertEquals(
            listOf(8, 9),
            items.filter { it.parentId == 7 }.sortedBy { it.position }.map { it.id },
        )
    }

    @Test
    fun `page-scoped reads return one page with its full tree`() = runTest {
        val document = testJson
            .decodeFromString<List<PageDto>>(sampleContentJson())
            .toDocumentEntities()
        dao.replaceAll(document.pages, document.items, document.responseSets, document.responses)

        assertEquals("Second Page", dao.observePage(10).first()?.title)
        assertNull(dao.observePage(999).first())

        val pageOneItems = dao.observeItemsForPage(1).first()
        assertTrue(pageOneItems.all { it.pageId == 1 })
        assertEquals(setOf(2, 3, 4, 5, 6, 7, 8, 9), pageOneItems.map { it.id }.toSet())
        assertTrue(dao.observeResponseSetsForPage(1).first().isEmpty())
        assertTrue(dao.observeResponsesForPage(1).first().isEmpty())

        assertEquals(listOf(101, 102), dao.observeResponseSetsForPage(10).first().map { it.id }.sorted())
        assertEquals(6, dao.observeResponsesForPage(10).first().size)
    }

    @Test
    fun `replacing clears the previous document`() = runTest {
        val document = testJson
            .decodeFromString<List<PageDto>>(sampleContentJson())
            .toDocumentEntities()
        dao.replaceAll(document.pages, document.items, document.responseSets, document.responses)

        val replacement = testJson
            .decodeFromString<List<PageDto>>(
                """
                [
                  {
                    "id": 99,
                    "type": "page",
                    "title": "Only Page",
                    "items": [ { "id": 100, "type": "text", "content": "hi" } ]
                  }
                ]
                """.trimIndent(),
            )
            .toDocumentEntities()
        dao.replaceAll(
            replacement.pages,
            replacement.items,
            replacement.responseSets,
            replacement.responses,
        )

        assertEquals(1, dao.observePages().first().size)
        assertEquals(1, dao.observeItems().first().size)
        assertTrue(dao.observeResponseSets().first().isEmpty())
        assertTrue(dao.observeResponses().first().isEmpty())
    }
}
