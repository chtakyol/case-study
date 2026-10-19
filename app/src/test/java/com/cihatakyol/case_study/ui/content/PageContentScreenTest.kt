package com.cihatakyol.case_study.ui.content

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.ui.theme.CasestudyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
                    onAction = {},
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
                    onAction = { action ->
                        if (action is PageContentAction.ImageClicked) {
                            clicked = action.src to action.title
                        }
                    },
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
                    onAction = { action ->
                        if (action is PageContentAction.ChoiceClicked) {
                            clicked = action.choiceId to action.responseId
                        }
                    },
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
                    onAction = { action ->
                        if (action is PageContentAction.ChoiceClicked) {
                            val current = selections[action.choiceId].orEmpty()
                            val updated = if (action.responseId in current) {
                                emptySet()
                            } else {
                                setOf(action.responseId)
                            }
                            selections = selections + (action.choiceId to updated)
                        }
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
                    onAction = {},
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
                    onAction = {},
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
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Unable to refresh content.").assertIsDisplayed()
    }

    @Test
    fun `the app bar shows the refresh action with stored content`() {
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = listOf(TextRow(id = 1, depth = 0, content = "Hello"))),
                    snackbarHostState = SnackbarHostState(),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
    }

    @Test
    fun `tapping refresh reports the refresh action`() {
        var action: PageContentAction? = null
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(rows = listOf(TextRow(id = 1, depth = 0, content = "Hello"))),
                    snackbarHostState = SnackbarHostState(),
                    onAction = { action = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Refresh").performClick()

        assertEquals(PageContentAction.Refresh, action)
    }

    @Test
    fun `progress replaces the refresh action while refreshing`() {
        var action: PageContentAction? = null
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Success(
                        rows = listOf(TextRow(id = 1, depth = 0, content = "Hello")),
                        isRefreshing = true,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onAction = { action = it },
                )
            }
        }

        // The progress indicator carries the same description but is not clickable, so a tap
        // cannot start a second refresh.
        composeRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Refresh").assertHasNoClickAction()
        composeRule.onNodeWithContentDescription("Refresh").performClick()
        assertNull(action)
    }

    @Test
    fun `the refresh action is available in the error state`() {
        composeRule.setContent {
            CasestudyTheme {
                PageContentContent(
                    state = PageContentUiState.Error("Unable to refresh content."),
                    snackbarHostState = SnackbarHostState(),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
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
