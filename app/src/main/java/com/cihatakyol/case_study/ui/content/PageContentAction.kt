package com.cihatakyol.case_study.ui.content

/** User actions emitted by the content screen. */
sealed interface PageContentAction {

    /** Requests fresh content from the network. */
    data object Refresh : PageContentAction

    /** The user tapped an image; handled by the screen's navigation callback. */
    data class ImageClicked(val src: String, val title: String) : PageContentAction

    /** The user tapped a response chip of a choice item. */
    data class ChoiceClicked(val choiceId: Int, val responseId: Int) : PageContentAction
}
