package com.cihatakyol.case_study.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cihatakyol.case_study.R
import com.cihatakyol.case_study.ui.content.components.PageContentRowItem
import com.cihatakyol.case_study.ui.theme.CasestudyTheme

@Composable
fun PageContentScreen(
    viewModel: PageContentViewModel,
    onImageClick: (src: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.initialize() }

    val userMessage = (uiState as? PageContentUiState.Success)?.userMessage
    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            viewModel.onUserMessageShown()
        }
    }

    PageContentContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onImageClick = onImageClick,
        onChoiceClick = viewModel::onChoiceClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageContentContent(
    state: PageContentUiState,
    snackbarHostState: SnackbarHostState,
    onImageClick: (src: String, title: String) -> Unit,
    onChoiceClick: (choiceId: Int, responseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                PageContentUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                is PageContentUiState.Error -> EmptyContent(
                    message = state.message,
                    modifier = Modifier.align(Alignment.Center),
                )

                is PageContentUiState.Success -> if (state.isEmpty) {
                    EmptyContent(
                        message = stringResource(R.string.content_empty_page),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.rows, key = { it.key }) { row ->
                            PageContentRowItem(
                                row = row,
                                selectedResponseIds = state.selectedResponseIds(row.id),
                                onImageClick = onImageClick,
                                onChoiceClick = onChoiceClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(24.dp),
    )
}

@PreviewLightDark
@Composable
private fun PageContentContentPreview() {
    CasestudyTheme {
        PageContentContent(
            state = PageContentUiState.Preview,
            snackbarHostState = remember { SnackbarHostState() },
            onImageClick = { _, _ -> },
            onChoiceClick = { _, _ -> },
        )
    }
}

@PreviewLightDark
@Composable
private fun PageContentContentEmptyPreview() {
    CasestudyTheme {
        PageContentContent(
            state = PageContentUiState.PreviewEmpty,
            snackbarHostState = remember { SnackbarHostState() },
            onImageClick = { _, _ -> },
            onChoiceClick = { _, _ -> },
        )
    }
}

@PreviewLightDark
@Composable
private fun PageContentContentErrorPreview() {
    CasestudyTheme {
        PageContentContent(
            state = PageContentUiState.PreviewError,
            snackbarHostState = remember { SnackbarHostState() },
            onImageClick = { _, _ -> },
            onChoiceClick = { _, _ -> },
        )
    }
}
