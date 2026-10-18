package com.cihatakyol.case_study.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp

// App typography. Titles use the headline/title styles; content uses the body/label styles. Sizes
// and weights are set here so call sites read `MaterialTheme.typography.<style>` only.
val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)

@PreviewLightDark
@Composable
private fun TypographyPreview() {
    CasestudyTheme {
        androidx.compose.foundation.layout.Column {
            Text("headlineSmall", style = MaterialTheme.typography.headlineSmall)
            Text("titleLarge", style = MaterialTheme.typography.titleLarge)
            Text("titleMedium", style = MaterialTheme.typography.titleMedium)
            Text("bodyLarge", style = MaterialTheme.typography.bodyLarge)
            Text("labelLarge", style = MaterialTheme.typography.labelLarge)
        }
    }
}
