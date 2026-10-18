package com.cihatakyol.case_study.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
data object PageContentRoute

@Serializable
data class ImageViewerRoute(val src: String, val title: String)
