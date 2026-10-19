package com.cihatakyol.case_study.ui.content

import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.domain.model.ResponseSet
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import com.cihatakyol.case_study.domain.repository.ContentRepository
import com.cihatakyol.case_study.domain.usecase.ObservePagesUseCase
import com.cihatakyol.case_study.domain.usecase.RefreshPagesUseCase
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageContentViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: ContentRepository) = PageContentViewModel(
        observePages = ObservePagesUseCase(repository),
        refreshPages = RefreshPagesUseCase(repository),
    )

    @Test
    fun `initial state is loading`() {
        val viewModel = viewModel(FakeContentRepository(emptyList()))

        assertEquals(PageContentUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `initialize loads every stored page and requests a refresh`() = runTest(dispatcher) {
        val repository = FakeContentRepository(
            pages = listOf(page(id = 1, title = "Main Page"), page(id = 2, title = "Second Page")),
        )
        val viewModel = viewModel(repository)

        viewModel.initialize()
        advanceUntilIdle()

        val state = viewModel.uiState.value.success()
        assertEquals(
            listOf("Main Page", "Second Page"),
            state.rows.filterIsInstance<PageHeaderRow>().map { it.title },
        )
        assertEquals(2, state.rows.filterIsInstance<TextRow>().size)
        assertTrue(repository.refreshCalled)
    }

    @Test
    fun `stored content changes update the list`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(page(id = 1, title = "Main Page")))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()

        repository.emit(listOf(page(id = 1, title = "Renamed Page", text = "Updated body")))
        advanceUntilIdle()

        val state = viewModel.uiState.value.success()
        assertEquals("Renamed Page", state.rows.filterIsInstance<PageHeaderRow>().single().title)
        assertEquals("Updated body", state.rows.filterIsInstance<TextRow>().single().content)
    }

    @Test
    fun `a failed refresh keeps the displayed content and surfaces a message`() = runTest(dispatcher) {
        val repository = FakeContentRepository(
            pages = listOf(page(id = 1, title = "Main Page")),
            initialRefreshResult = Result.Error(IOException("boom")),
        )
        val viewModel = viewModel(repository)

        viewModel.initialize()
        advanceUntilIdle()

        val state = viewModel.uiState.value.success()
        assertEquals(1, state.rows.filterIsInstance<PageHeaderRow>().size)
        assertEquals(PageContentViewModel.REFRESH_ERROR_MESSAGE, state.userMessage)

        viewModel.onUserMessageShown()
        assertNull(viewModel.uiState.value.success().userMessage)
    }

    @Test
    fun `a failed refresh with nothing stored reports an error`() = runTest(dispatcher) {
        val repository = FakeContentRepository(
            pages = emptyList(),
            initialRefreshResult = Result.Error(IOException("boom")),
        )
        val viewModel = viewModel(repository)

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(
            PageContentUiState.Error(PageContentViewModel.REFRESH_ERROR_MESSAGE),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `no stored pages reports empty`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeContentRepository(pages = emptyList()))

        viewModel.initialize()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.success().isEmpty)
    }

    @Test
    fun `single selection replaces the current response and can be cleared`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeContentRepository(listOf(pageWithChoice(multipleSelection = false))))
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.onChoiceClicked(choiceId = 8, responseId = 80)
        assertEquals(setOf(80), viewModel.uiState.value.success().selectedResponseIds(8))

        viewModel.onChoiceClicked(choiceId = 8, responseId = 81)
        assertEquals(setOf(81), viewModel.uiState.value.success().selectedResponseIds(8))

        viewModel.onChoiceClicked(choiceId = 8, responseId = 81)
        assertTrue(viewModel.uiState.value.success().selectedResponseIds(8).isEmpty())
    }

    @Test
    fun `multiple selection toggles responses without clearing the others`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeContentRepository(listOf(pageWithChoice(multipleSelection = true))))
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.onChoiceClicked(choiceId = 8, responseId = 80)
        viewModel.onChoiceClicked(choiceId = 8, responseId = 81)
        assertEquals(setOf(80, 81), viewModel.uiState.value.success().selectedResponseIds(8))

        viewModel.onChoiceClicked(choiceId = 8, responseId = 80)
        assertEquals(setOf(81), viewModel.uiState.value.success().selectedResponseIds(8))
    }

    @Test
    fun `selection survives a stored content re-emission`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(pageWithChoice(multipleSelection = true)))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.onChoiceClicked(choiceId = 8, responseId = 80)

        repository.emit(listOf(pageWithChoice(multipleSelection = true)))
        advanceUntilIdle()

        assertEquals(setOf(80), viewModel.uiState.value.success().selectedResponseIds(8))
    }

    @Test
    fun `clicking an unknown choice is ignored`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeContentRepository(listOf(pageWithChoice(multipleSelection = true))))
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.onChoiceClicked(choiceId = 999, responseId = 80)

        assertTrue(viewModel.uiState.value.success().selections.isEmpty())
    }

    @Test
    fun `a manual refresh requests new content and clears the in-flight flag`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(page(id = 1, title = "Main Page")))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()
        repository.refreshCallCount = 0

        viewModel.onAction(PageContentAction.Refresh)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCallCount)
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `a manual refresh shows progress while in flight`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(page(id = 1, title = "Main Page")))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()

        repository.suspendRefresh = true
        viewModel.onAction(PageContentAction.Refresh)
        runCurrent()

        assertTrue(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `a repeated refresh while one is in flight is ignored`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(page(id = 1, title = "Main Page")))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()
        repository.refreshCallCount = 0
        repository.suspendRefresh = true

        viewModel.onAction(PageContentAction.Refresh)
        runCurrent()
        viewModel.onAction(PageContentAction.Refresh)
        runCurrent()

        assertEquals(1, repository.refreshCallCount)
        repository.releaseRefresh()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `a failed manual refresh with content keeps it and shows a message`() = runTest(dispatcher) {
        val repository = FakeContentRepository(listOf(page(id = 1, title = "Main Page")))
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()

        repository.refreshResult = Result.Error(IOException("boom"))
        viewModel.onAction(PageContentAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value.success()
        assertEquals(1, state.rows.filterIsInstance<PageHeaderRow>().size)
        assertEquals(PageContentViewModel.REFRESH_ERROR_MESSAGE, state.userMessage)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `a failed manual refresh with nothing stored keeps the error state`() = runTest(dispatcher) {
        val repository = FakeContentRepository(pages = emptyList())
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()

        repository.refreshResult = Result.Error(IOException("boom"))
        viewModel.onAction(PageContentAction.Refresh)
        advanceUntilIdle()

        assertEquals(
            PageContentUiState.Error(PageContentViewModel.REFRESH_ERROR_MESSAGE),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `retrying from the error state recovers when the refresh succeeds`() = runTest(dispatcher) {
        val repository = FakeContentRepository(
            pages = emptyList(),
            initialRefreshResult = Result.Error(IOException("boom")),
        )
        val viewModel = viewModel(repository)
        viewModel.initialize()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PageContentUiState.Error)

        repository.refreshResult = Result.Success(Unit)
        viewModel.onAction(PageContentAction.Refresh)
        advanceUntilIdle()

        repository.emit(listOf(page(id = 1, title = "Main Page")))
        advanceUntilIdle()

        val state = viewModel.uiState.value.success()
        assertEquals(listOf("Main Page"), state.rows.filterIsInstance<PageHeaderRow>().map { it.title })
    }

    private fun PageContentUiState.success(): PageContentUiState.Success {
        require(this is PageContentUiState.Success) { "Expected Success but was $this" }
        return this
    }

    private fun page(id: Int, title: String, text: String = "Hello") = Page(
        id = id,
        title = title,
        items = listOf(
            Section(
                id = id * 100,
                title = "Intro",
                items = listOf(Text(id = id * 100 + 1, content = text)),
            ),
        ),
    )

    private fun pageWithChoice(multipleSelection: Boolean) = Page(
        id = 1,
        title = "Main Page",
        items = listOf(
            Choice(
                id = 8,
                content = "Pick one",
                responseSet = ResponseSet(
                    id = 9,
                    multipleSelection = multipleSelection,
                    responses = listOf(
                        Response(id = 80, label = "Yes", score = 1),
                        Response(id = 81, label = "No", score = null),
                    ),
                ),
            ),
        ),
    )
}

private class FakeContentRepository(pages: List<Page>, initialRefreshResult: Result<Unit> = Result.Success(Unit)) :
    ContentRepository {

    private val pagesFlow = MutableStateFlow(pages)
    private var refreshGate: CompletableDeferred<Unit>? = null

    var refreshResult: Result<Unit> = initialRefreshResult

    var refreshCalled = false
        private set

    var refreshCallCount = 0

    /** When true, `refreshPages` suspends until [releaseRefresh] is called. */
    var suspendRefresh = false

    fun emit(pages: List<Page>) {
        pagesFlow.value = pages
    }

    fun releaseRefresh() {
        refreshGate?.complete(Unit)
    }

    override fun observePages(): Flow<List<Page>> = pagesFlow

    override fun observePage(pageId: Int): Flow<Page?> = flowOf(null)

    override suspend fun refreshPages(): Result<Unit> {
        refreshCalled = true
        refreshCallCount++
        if (suspendRefresh) {
            val gate = CompletableDeferred<Unit>()
            refreshGate = gate
            gate.await()
        }
        return refreshResult
    }
}
