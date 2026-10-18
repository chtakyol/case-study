package com.cihatakyol.case_study.ui.content

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.ui.theme.CasestudyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageContentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders page headers and items in document order`() {
        val state = PageContentUiState.Success(
            rows = listOf(
                PageHeaderRow(id = 1, depth = 0, title = "Main Page"),
                SectionHeaderRow(id = 2, depth = 1, title = "Section A"),
                TextRow(id = 3, depth = 2, content = "First"),
                TextRow(id = 4, depth = 2, content = "Second"),
            ),
        )
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Main Page").assertIsDisplayed()
        composeRule.onNodeWithText("Section A").assertIsDisplayed()
        composeRule.onNodeWithText("First").assertIsDisplayed()
        composeRule.onNodeWithText("Second").assertIsDisplayed()

        val pageTop = composeRule.onNodeWithText("Main Page").getBoundsInRoot().top
        val sectionTop = composeRule.onNodeWithText("Section A").getBoundsInRoot().top
        val firstTop = composeRule.onNodeWithText("First").getBoundsInRoot().top
        val secondTop = composeRule.onNodeWithText("Second").getBoundsInRoot().top
        assertTrue(pageTop < sectionTop)
        assertTrue(sectionTop < firstTop)
        assertTrue(firstTop < secondTop)
    }

    @Test
    fun `tapping an image reports its source and title`() {
        var clicked: Pair<String, String>? = null
        val imageRow = ImageRow(
            id = 4,
            depth = 2,
            src = "https://example.com/a.png",
            title = "Welcome Image",
        )
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = listOf(imageRow)),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { src, title -> clicked = src to title },
                    onChoiceClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Welcome Image").performClick()

        assertEquals("https://example.com/a.png" to "Welcome Image", clicked)
    }

    @Test
    fun `tapping a chip reports the choice and response`() {
        var clicked: Pair<Int, Int>? = null
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = listOf(choiceRow())),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { choiceId, responseId -> clicked = choiceId to responseId },
                )
            }
        }

        composeRule.onNodeWithText("Great").performClick()

        assertEquals(7 to 101, clicked)
    }

    @Test
    fun `the list reflects the selection and shows the selected line`() {
        composeRule.setContent {
            CasestudyTheme {
                var selections by remember { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
                PageContentContent(
                    state = PageContentUiState.Success(
                        rows = listOf(choiceRow()),
                        selections = selections,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { choiceId, responseId ->
                        val current = selections[choiceId].orEmpty()
                        val updated = if (responseId in current) emptySet() else setOf(responseId)
                        selections = selections + (choiceId to updated)
                    },
                )
            }
        }

        composeRule.onNodeWithText("Great").performClick()

        composeRule.onNodeWithText("Selected: Great").assertIsDisplayed()
    }

    @Test
    fun `shows the selection mode`() {
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = listOf(choiceRow(multipleSelection = true))),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Multi select").assertIsDisplayed()
    }

    @Test
    fun `shows the empty state when nothing is stored`() {
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = emptyList()),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("No content yet.").assertIsDisplayed()
    }

    @Test
    fun `shows the error state when a load fails with nothing stored`() {
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Error("Unable to refresh content."),
                    snackbarHostState = SnackbarHostState(),
                    onImageClick = { _, _ -> },
                    onChoiceClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Unable to refresh content.").assertIsDisplayed()
    }

    @Test
    fun `viewer shows the image title and back invokes the callback`() {
        var backPressed = false
        composeRule.setContent {
            CasestudyTheme {
                ImageViewerScreen(
                    src = "https://example.com/a.png",
                    title = "Welcome Image",
                    onBack = { backPressed = true },
                )
            }
        }

        composeRule.onNodeWithText("Welcome Image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backPressed)
    }

    private fun choiceRow(multipleSelection: Boolean = false) = ChoiceRow(
        id = 7,
        depth = 1,
        content = "How did you find this page?",
        multipleSelection = multipleSelection,
        responses = listOf(
            Response(id = 101, label = "Great", score = 2),
            Response(id = 102, label = "Okay", score = 1),
        ),
    )
}
