@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cihatakyol.case_study.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cihatakyol.case_study.data.local.database.AppDatabase
import com.cihatakyol.case_study.data.local.database.ContentDao
import com.cihatakyol.case_study.data.remote.ContentApi
import com.cihatakyol.case_study.data.remote.dto.PageDto
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.sampleContentJson
import com.cihatakyol.case_study.testJson
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ContentRepositoryImplTest {

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
    fun `refresh writes the document and observe exposes domain models`() = runTest {
        val repository = repository(FakeContentApi { samplePages() })

        val result = repository.refreshPages()

        assertTrue(result is Result.Success)
        val pages = repository.observePages().first()
        assertEquals(listOf("Main Page", "Second Page"), pages.map { it.title })
    }

    @Test
    fun `failed refresh keeps existing content and returns an error`() = runTest {
        repository(FakeContentApi { samplePages() }).refreshPages()

        val failing = repository(FakeContentApi { throw IOException("boom") })
        val result = failing.refreshPages()

        assertTrue(result is Result.Error)
        assertEquals(listOf("Main Page", "Second Page"), failing.observePages().first().map { it.title })
    }

    @Test
    fun `observePage emits one page, null for an unknown id, and reflects changes`() = runTest {
        val repository = repository(FakeContentApi { samplePages() })
        repository.refreshPages()

        assertEquals("Main Page", repository.observePage(pageId = 1).first()?.title)
        assertNull(repository.observePage(pageId = 999).first())

        repository(FakeContentApi { renamedPages() }).refreshPages()
        assertEquals("Renamed Page", repository.observePage(pageId = 1).first()?.title)
    }

    private fun repository(api: ContentApi): ContentRepositoryImpl =
        ContentRepositoryImpl(dao, api, UnconfinedTestDispatcher())

    private fun samplePages(): List<PageDto> = testJson.decodeFromString(sampleContentJson())

    private fun renamedPages(): List<PageDto> =
        samplePages().map { if (it.id == 1) it.copy(title = "Renamed Page") else it }
}

private class FakeContentApi(private val response: () -> List<PageDto>) : ContentApi {
    override suspend fun getPages(): List<PageDto> = response()
}
