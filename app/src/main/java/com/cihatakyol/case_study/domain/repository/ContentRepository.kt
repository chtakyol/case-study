package com.cihatakyol.case_study.domain.repository

import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    /** Observes the locally stored content document as domain models. */
    fun observePages(): Flow<List<Page>>

    /** Observes a single locally stored page as a domain model, or emits `null` when absent. */
    fun observePage(pageId: Int): Flow<Page?>

    /** Refreshes the local content document from the network. */
    suspend fun refreshPages(): Result<Unit>
}
