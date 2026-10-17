package com.cihatakyol.case_study.domain.usecase

import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.repository.ContentRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentUseCasesTest {

    private val pages = listOf(Page(id = 1, title = "Main Page", items = emptyList()))

    @Test
    fun `observe use case exposes repository stream`() = runTest {
        val useCase = ObservePagesUseCase(FakeContentRepository(pages, Result.Success(Unit)))

        assertEquals(pages, useCase().first())
    }

    @Test
    fun `refresh use case returns repository result`() = runTest {
        val failure = Result.Error(IOException("boom"))
        val useCase = RefreshPagesUseCase(FakeContentRepository(pages, failure))

        assertTrue(useCase() is Result.Error)
    }

    @Test
    fun `observe page use case emits the matching page`() = runTest {
        val useCase = ObservePageUseCase(FakeContentRepository(pages, Result.Success(Unit)))

        assertEquals(pages.first(), useCase(pageId = 1).first())
    }
}

private class FakeContentRepository(private val pages: List<Page>, private val refreshResult: Result<Unit>) :
    ContentRepository {
    override fun observePages(): Flow<List<Page>> = flowOf(pages)

    override fun observePage(pageId: Int): Flow<Page?> = flowOf(pages.firstOrNull { it.id == pageId })

    override suspend fun refreshPages(): Result<Unit> = refreshResult
}
