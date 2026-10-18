package com.cihatakyol.case_study.ui.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.cihatakyol.case_study.R
import com.cihatakyol.case_study.ui.theme.CasestudyTheme

@Composable
fun ImageViewerScreen(src: String, title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ImageViewerContent(src = src, title = title, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageViewerContent(src: String, title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title.ifBlank { stringResource(R.string.image_viewer_title) },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
        AsyncImage(
            model = src,
            contentDescription = title,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ImageViewerContentPreview() {
    CasestudyTheme {
        ImageViewerContent(
            src = "https://raw.githubusercontent.com/github/explore/main/topics/android/android.png",
            title = "Welcome Image",
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ImageViewerContentMissingSourcePreview() {
    CasestudyTheme {
        ImageViewerContent(src = "", title = "", onBack = {})
    }
}
