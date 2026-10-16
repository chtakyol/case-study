package com.cihatakyol.case_study.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageDto(
    val id: Int,
    val title: String,
    @Serializable(with = ContentItemListSerializer::class)
    val items: List<ContentItemDto> = emptyList(),
)
