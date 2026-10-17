package com.cihatakyol.case_study.domain.usecase

import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.repository.ContentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePagesUseCase @Inject constructor(private val repository: ContentRepository) {
    operator fun invoke(): Flow<List<Page>> = repository.observePages()
}
