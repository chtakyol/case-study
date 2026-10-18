package com.cihatakyol.case_study.ui.content

import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.Image
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Response
import com.cihatakyol.case_study.domain.model.ResponseSet
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageContentMapperTest {

    @Test
    fun `flattens every page with a header followed by its items in depth-first order`() {
        val pages = listOf(
            Page(
                id = 1,
                title = "Main Page",
                items = listOf(
                    Section(
                        id = 2,
                        title = "Introduction",
                        items = listOf(
                            Text(id = 3, content = "Welcome"),
                            Image(id = 4, src = "https://example.com/a.png", title = "A"),
                        ),
                    ),
                    Section(
                        id = 5,
                        title = "Chapter 1",
                        items = listOf(
                            Text(id = 6, content = "First"),
                            Section(
                                id = 7,
                                title = "Subsection",
                                items = listOf(
                                    Choice(
                                        id = 8,
                                        content = "Pick one",
                                        responseSet = ResponseSet(
                                            id = 9,
                                            multipleSelection = false,
                                            responses = listOf(Response(id = 10, label = "Yes", score = 1)),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            Page(id = 11, title = "Second Page", items = listOf(Text(id = 12, content = "Body"))),
        )

        val rows = pages.toContentRows()

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 11, 12), rows.map { it.id })
        assertEquals(listOf(0, 1, 2, 2, 1, 2, 2, 3, 0, 1), rows.map { it.depth })
        assertTrue(rows[0] is PageHeaderRow)
        assertEquals("Main Page", (rows[0] as PageHeaderRow).title)
        assertTrue(rows[1] is SectionHeaderRow)
        assertTrue(rows[2] is TextRow)
        assertTrue(rows[7] is ChoiceRow)
        assertTrue(rows[8] is PageHeaderRow)
        assertEquals("Second Page", (rows[8] as PageHeaderRow).title)
    }

    @Test
    fun `row keys are unique across pages and items`() {
        val pages = listOf(
            Page(id = 1, title = "Main Page", items = listOf(Text(id = 1, content = "Same id as page"))),
            Page(id = 2, title = "Second Page", items = listOf(Text(id = 2, content = "Body"))),
        )

        val rows = pages.toContentRows()

        assertEquals(rows.size, rows.map { it.key }.distinct().size)
    }

    @Test
    fun `carries the choice responses and its single-selection flag`() {
        val pages = listOf(
            Page(
                id = 1,
                title = "Main Page",
                items = listOf(
                    Choice(
                        id = 2,
                        content = "Pick one",
                        responseSet = ResponseSet(
                            id = 3,
                            multipleSelection = false,
                            responses = listOf(
                                Response(id = 4, label = "Yes", score = 1),
                                Response(id = 5, label = "No", score = null),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val choice = pages.toContentRows().filterIsInstance<ChoiceRow>().single()

        assertEquals(listOf("Yes", "No"), choice.responses.map { it.label })
        assertFalse(choice.multipleSelection)
    }

    @Test
    fun `carries the choice multiple-selection flag`() {
        val pages = listOf(
            Page(
                id = 1,
                title = "Main Page",
                items = listOf(
                    Choice(
                        id = 2,
                        content = "Pick any",
                        responseSet = ResponseSet(
                            id = 3,
                            multipleSelection = true,
                            responses = listOf(Response(id = 4, label = "Yes", score = 1)),
                        ),
                    ),
                ),
            ),
        )

        val choice = pages.toContentRows().filterIsInstance<ChoiceRow>().single()

        assertTrue(choice.multipleSelection)
    }

    @Test
    fun `no pages produces no rows`() {
        assertTrue(emptyList<Page>().toContentRows().isEmpty())
    }
}
