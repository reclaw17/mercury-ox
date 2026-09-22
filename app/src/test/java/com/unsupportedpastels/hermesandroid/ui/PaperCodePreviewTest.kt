package com.unsupportedpastels.hermesandroid.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperCodePreviewTest {
    @Test fun twelveLinesStayWhole() {
        val code = lines(12)
        val preview = paperCodePreview(code, expanded = false)
        assertEquals(code, preview.text)
        assertFalse(preview.canExpand)
    }

    @Test fun thirteenthLineWaitsForExpand() {
        val code = lines(13)
        val collapsed = paperCodePreview(code, expanded = false)
        assertEquals(lines(12), collapsed.text)
        assertTrue(collapsed.canExpand)
        assertFalse(collapsed.text.contains("line-13"))
        val expanded = paperCodePreview(code, expanded = true)
        assertEquals(code, expanded.text)
        assertFalse(expanded.canExpand)
    }

    @Test fun trailingFenceNewlineDoesNotCountAsAnExtraLine() {
        val exact = lines(12) + "\n"
        val exactPreview = paperCodePreview(exact, expanded = false)
        assertEquals(exact, exactPreview.text)
        assertFalse(exactPreview.canExpand)

        val longer = lines(13) + "\n"
        val collapsed = paperCodePreview(longer, expanded = false)
        assertEquals(lines(12), collapsed.text)
        assertTrue(collapsed.canExpand)
    }

    private fun lines(count: Int): String =
        (1..count).joinToString(separator = "\n") { "line-$it" }
}
