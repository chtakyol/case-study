package com.cihatakyol.case_study.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseSetDto(
    val id: Int,
    @SerialName("multiple_selection")
    val multipleSelection: Boolean = false,
    val responses: List<ResponseDto> = emptyList(),
)

@Serializable
data class ResponseDto(val id: Int, val label: String, val score: Int? = null)
