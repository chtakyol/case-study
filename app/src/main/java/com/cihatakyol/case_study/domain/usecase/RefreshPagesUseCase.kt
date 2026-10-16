package com.cihatakyol.case_study.domain.usecase

import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.repository.ContentRepository
import javax.inject.Inject

class RefreshPagesUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshPages()
}
