package com.cihatakyol.case_study.ui.content

import com.cihatakyol.case_study.domain.model.Response

/** Screen state for the content screen. */
sealed interface PageContentUiState {

    /** True while a refresh is in flight, so the app bar can show progress. */
    val isRefreshing: Boolean get() = false

    /** Before the stored content has been observed for the first time. */
    data object Loading : PageContentUiState

    /** The stored document as flattened rows, with the current choice selections. */
    data class Success(
        val rows: List<PageContentRow>,
        val selections: Map<Int, Set<Int>> = emptyMap(),
        val userMessage: String? = null,
        override val isRefreshing: Boolean = false,
    ) : PageContentUiState {

        /** True when nothing is stored, so the empty state is shown. */
        val isEmpty: Boolean get() = rows.isEmpty()

        /** The responses selected for a choice item, empty when nothing is selected. */
        fun selectedResponseIds(choiceId: Int): Set<Int> = selections[choiceId].orEmpty()
    }

    /** A refresh failed and nothing is stored. */
    data class Error(val message: String, override val isRefreshing: Boolean = false) : PageContentUiState

    companion object {
        val Preview = Success(
            rows = listOf(
                PageHeaderRow(id = 1, depth = 0, title = "Main Page"),
                SectionHeaderRow(id = 2, depth = 1, title = "Introduction"),
                TextRow(id = 3, depth = 2, content = "Welcome to the main page!"),
                ImageRow(
                    id = 4,
                    depth = 2,
                    src = "https://raw.githubusercontent.com/github/explore/main/topics/android/android.png",
                    title = "Welcome Image",
                ),
                ChoiceRow(
                    id = 5,
                    depth = 2,
                    content = "How did you find this page?",
                    multipleSelection = true,
                    responses = listOf(
                        Response(id = 6, label = "Great", score = 2),
                        Response(id = 7, label = "Okay", score = 1),
                    ),
                ),
                PageHeaderRow(id = 10, depth = 0, title = "Second Page"),
                TextRow(id = 11, depth = 1, content = "This is the second page."),
            ),
            selections = mapOf(5 to setOf(6)),
        )

        val PreviewEmpty = Success(rows = emptyList())

        val PreviewError = Error("Unable to refresh content.")
    }
}
