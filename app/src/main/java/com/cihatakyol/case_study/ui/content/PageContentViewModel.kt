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

    private fun onPagesChanged(pages: List<Page>) {
        val selections = (_uiState.value as? PageContentUiState.Success)?.selections.orEmpty()
        _uiState.value = PageContentUiState.Success(
            rows = pages.toContentRows(),
            selections = selections,
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
        viewModelScope.launch {
            when (val result = refreshPages()) {
                is Result.Success -> Unit
                is Result.Error -> onRefreshFailed(result.message ?: REFRESH_ERROR_MESSAGE)
            }
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
