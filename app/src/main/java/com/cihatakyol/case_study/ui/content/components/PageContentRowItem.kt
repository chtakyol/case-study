package com.cihatakyol.case_study.ui.content.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.cihatakyol.case_study.R
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.ui.content.ChoiceRow
import com.cihatakyol.case_study.ui.content.ImageRow
import com.cihatakyol.case_study.ui.content.PageContentRow
import com.cihatakyol.case_study.ui.content.PageHeaderRow
import com.cihatakyol.case_study.ui.content.SectionHeaderRow
import com.cihatakyol.case_study.ui.content.TextRow
import com.cihatakyol.case_study.ui.theme.CasestudyTheme

/** Renders one [PageContentRow], indenting by the row's nesting depth. */
@Composable
internal fun PageContentRowItem(
    row: PageContentRow,
    selectedResponseIds: Set<Int>,
    onImageClick: (src: String, title: String) -> Unit,
    onChoiceClick: (choiceId: Int, responseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val depthPadding = (row.depth * 16).dp
    when (row) {
        is PageHeaderRow -> PageHeader(row, modifier.padding(top = depthPadding))

        is SectionHeaderRow -> SectionHeader(row, modifier.padding(start = depthPadding))

        is TextRow -> TextContent(row, modifier.padding(start = depthPadding))

        is ImageRow -> ImageContent(row, onImageClick, modifier.padding(start = depthPadding))

        is ChoiceRow -> ChoiceContent(
            row = row,
            selectedResponseIds = selectedResponseIds,
            onChoiceClick = onChoiceClick,
            modifier = modifier.padding(start = depthPadding),
        )
    }
}

@Composable
private fun PageHeader(row: PageHeaderRow, modifier: Modifier = Modifier) {
    Text(
        text = row.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun SectionHeader(row: SectionHeaderRow, modifier: Modifier = Modifier) {
    Text(
        text = row.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun TextContent(row: TextRow, modifier: Modifier = Modifier) {
    Text(
        text = row.content,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun ImageContent(
    row: ImageRow,
    onImageClick: (src: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = dropUnlessResumed { onImageClick(row.src, row.title) })
            .semantics { contentDescription = row.title },
    ) {
        AsyncImage(
            model = row.src,
            contentDescription = null,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Fit,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceContent(
    row: ChoiceRow,
    selectedResponseIds: Set<Int>,
    onChoiceClick: (choiceId: Int, responseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = row.content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                if (row.multipleSelection) R.string.choice_mode_multiple else R.string.choice_mode_single,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            row.responses.forEach { response ->
                FilterChip(
                    selected = response.id in selectedResponseIds,
                    onClick = { onChoiceClick(row.id, response.id) },
                    label = { Text(response.label) },
                )
            }
        }
        val selectedLabels = row.responses
            .filter { it.id in selectedResponseIds }
            .map { it.label }
        if (selectedLabels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.choice_selected, selectedLabels.joinToString(", ")),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PageHeaderRowPreview() {
    CasestudyTheme {
        PageHeader(
            row = PageHeaderRow(id = 1, depth = 0, title = "Main Page"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SectionHeaderRowPreview() {
    CasestudyTheme {
        SectionHeader(
            row = SectionHeaderRow(id = 1, depth = 1, title = "Introduction"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun TextRowPreview() {
    CasestudyTheme {
        TextContent(
            row = TextRow(id = 2, depth = 1, content = "Welcome to the main page!"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ImageRowPreview() {
    CasestudyTheme {
        ImageContent(
            row = ImageRow(
                id = 3,
                depth = 1,
                src = "https://raw.githubusercontent.com/github/explore/main/topics/android/android.png",
                title = "Welcome Image",
            ),
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(16.dp).width(320.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ChoiceRowPreview() {
    CasestudyTheme {
        ChoiceContent(
            row = ChoiceRow(
                id = 4,
                depth = 1,
                content = "How did you find this page?",
                multipleSelection = true,
                responses = listOf(
                    Response(id = 5, label = "Great", score = 2),
                    Response(id = 6, label = "Okay", score = 1),
                ),
            ),
            selectedResponseIds = setOf(5),
            onChoiceClick = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}
