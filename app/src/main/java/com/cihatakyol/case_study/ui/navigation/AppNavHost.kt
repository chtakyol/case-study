package com.cihatakyol.case_study.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cihatakyol.case_study.ui.content.ImageViewerScreen
import com.cihatakyol.case_study.ui.content.PageContentScreen

/** Single navigation host for the app's content screens, wired with type-safe routes. */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PageContentRoute,
        modifier = modifier,
    ) {
        composable<PageContentRoute> {
            PageContentScreen(
                viewModel = hiltViewModel(),
                onImageClick = { src, title -> navController.navigate(ImageViewerRoute(src, title)) },
            )
        }

        composable<ImageViewerRoute> { entry ->
            val route = entry.toRoute<ImageViewerRoute>()
            ImageViewerScreen(
                src = route.src,
                title = route.title,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
