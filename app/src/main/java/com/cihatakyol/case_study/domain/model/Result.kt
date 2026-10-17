package com.cihatakyol.case_study.domain.model

/**
 * Outcome of a domain operation that can fail. Failures cross the domain boundary as values
 * rather than thrown exceptions.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>

    data class Error(val cause: Throwable, val message: String? = null) : Result<Nothing>
}
