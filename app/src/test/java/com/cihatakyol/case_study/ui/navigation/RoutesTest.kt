package com.cihatakyol.case_study.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var navController: NavHostController? = null

    private fun setGraphContent() {
        composeRule.setContent {
            val controller = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            navController = controller
            NavHost(navController = controller, startDestination = PageContentRoute) {
                composable<PageContentRoute> { }
                composable<ImageViewerRoute> { }
            }
        }
    }

    @Test
    fun `type-safe image route round-trips a source url containing a query string`() {
        val src = "https://example.com/a.png?x=1&y=2"
        val title = "A & B"
        setGraphContent()

        composeRule.runOnIdle { navController?.navigate(ImageViewerRoute(src = src, title = title)) }
        composeRule.waitForIdle()

        val route = checkNotNull(navController?.currentBackStackEntry).toRoute<ImageViewerRoute>()
        assertEquals(src, route.src)
        assertEquals(title, route.title)
    }

    @Test
    fun `start destination is the page content route`() {
        setGraphContent()
        composeRule.waitForIdle()

        val current = checkNotNull(navController?.currentBackStackEntry)
        assertEquals(PageContentRoute, current.toRoute<PageContentRoute>())
    }
}
