package com.android.sandbox.core

import org.junit.Assert.*
import org.junit.Test

class ProotExecutorTest {

    @Test
    fun `smartTruncate returns original string when within limit`() {
        val input = "Hello, World!"
        val result = input.smartTruncate(100)
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `smartTruncate truncates long strings correctly`() {
        val input = "A".repeat(200)
        val result = input.smartTruncate(100)
        assertTrue(result.length <= 100)
        assertTrue(result.contains("[... "))
        assertTrue(result.contains("characters truncated ...]"))
    }

    @Test
    fun `smartTruncate preserves start and end of string`() {
        val input = "START" + "x".repeat(200) + "END"
        val result = input.smartTruncate(100)
        assertTrue(result.startsWith("START"))
        assertTrue(result.endsWith("END"))
    }

    @Test
    fun `smartTruncate handles exact boundary`() {
        val input = "A".repeat(50)
        val result = input.smartTruncate(50)
        assertEquals(50, result.length)
    }

    @Test
    fun `smartTruncate handles empty string`() {
        val result = "".smartTruncate(100)
        assertEquals("", result)
    }

    @Test
    fun `smartTruncate handles single character`() {
        val result = "A".smartTruncate(100)
        assertEquals("A", result)
    }

    private fun String.smartTruncate(maxLength: Int): String {
        if (length <= maxLength) return this
        val keep = (maxLength - 80) / 2
        return take(keep) +
            "\n[... ${length - 2 * keep} characters truncated ...]\n" +
            takeLast(keep)
    }
}
