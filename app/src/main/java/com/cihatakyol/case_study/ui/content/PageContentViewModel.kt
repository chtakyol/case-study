package com.cihatakyol.case_study.ui.content

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.usecase.ObservePagesUseCase
import com.cihatakyol.case_study.domain.usecase.RefreshPagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class PageContentViewModel @Inject constructor(
    private val observePages: ObservePagesUseCase,
    private val refreshPages: RefreshPagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PageContentUiState>(PageContentUiState.Loading)
    val uiState: StateFlow<PageContentUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var refreshJob: Job? = null

    /** Seeds the state pipeline once: observe the stored pages, then refresh them from the network. */
    @MainThread
    fun initialize() {
        if (initialized) return
        initialized = true

        observePages()
            .onEach(::onPagesChanged)
            .launchIn(viewModelScope)

        refresh()
    }

    /** Single entry point for the screen's user actions. */
    fun onAction(action: PageContentAction) {
        when (action) {
            PageContentAction.Refresh -> refresh()

            is PageContentAction.ChoiceClicked -> onChoiceClicked(action.choiceId, action.responseId)

            // Image taps navigate; the screen handles them through its navigation callback.
            is PageContentAction.ImageClicked -> Unit
        }
    }

    private fun onPagesChanged(pages: List<Page>) {
        val current = _uiState.value as? PageContentUiState.Success
        _uiState.value = PageContentUiState.Success(
            rows = pages.toContentRows(),
            selections = current?.selections.orEmpty(),
            isRefreshing = current?.isRefreshing ?: false,
        )
    }

    /**
     * Selects or deselects a response for a choice item, honoring the item's multiple-selection flag:
     * a single-selection item replaces its selection, a multiple-selection item toggles the response.
     */
    fun onChoiceClicked(choiceId: Int, responseId: Int) {
        val current = _uiState.value as? PageContentUiState.Success ?: return
        val multipleSelection = current.rows
            .filterIsInstance<ChoiceRow>()
            .firstOrNull { it.id == choiceId }
            ?.multipleSelection
            ?: return

        val selected = current.selectedResponseIds(choiceId)
        val updated = if (multipleSelection) {
            if (responseId in selected) selected - responseId else selected + responseId
        } else {
            if (responseId in selected) emptySet() else setOf(responseId)
        }
        _uiState.value = current.copy(selections = current.selections + (choiceId to updated))
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            setRefreshing(true)
            when (val result = refreshPages()) {
                is Result.Success -> onRefreshSucceeded()
                is Result.Error -> onRefreshFailed(result.message ?: REFRESH_ERROR_MESSAGE)
            }
            setRefreshing(false)
        }
    }

    /** Flips the in-flight flag on the states that can show a refresh, leaving `Loading` alone. */
    private fun setRefreshing(refreshing: Boolean) {
        _uiState.value = when (val current = _uiState.value) {
            PageContentUiState.Loading -> current
            is PageContentUiState.Success -> current.copy(isRefreshing = refreshing)
            is PageContentUiState.Error -> current.copy(isRefreshing = refreshing)
        }
    }

    /**
     * Recovers from an error state: the SSOT flow replaces this empty success with the refreshed
     * document as soon as it is stored.
     */
    private fun onRefreshSucceeded() {
        if (_uiState.value is PageContentUiState.Error) {
            _uiState.value = PageContentUiState.Success(rows = emptyList(), isRefreshing = true)
        }
    }

    /**
     * Keeps displayed content on a failed refresh, surfacing a transient message; falls back to the
     * error state only when nothing is stored, so a failure never discards content.
     */
    private fun onRefreshFailed(message: String) {
        val current = _uiState.value
        _uiState.value = if (current is PageContentUiState.Success && current.rows.isNotEmpty()) {
            current.copy(userMessage = message)
        } else {
            PageContentUiState.Error(message)
        }
    }

    /** Acknowledges the transient message so it is not shown again. */
    fun onUserMessageShown() {
        val current = _uiState.value
        if (current is PageContentUiState.Success && current.userMessage != null) {
            _uiState.value = current.copy(userMessage = null)
        }
    }

    companion object {
        const val REFRESH_ERROR_MESSAGE = "Unable to refresh content."
    }
}
