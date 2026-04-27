package com.lhzkml.jasmine.core.websearch

import com.lhzkml.jasmine.core.websearch.model.DuckDuckGoResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuckDuckGoResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun parse_abstract_response() {
        val responseJson = """
        {
            "AbstractText": "Kotlin is a programming language",
            "AbstractSource": "Wikipedia",
            "AbstractURL": "https://en.wikipedia.org/wiki/Kotlin"
        }
        """.trimIndent()

        val response = json.decodeFromString<DuckDuckGoResponse>(responseJson)
        assertEquals("Kotlin is a programming language", response.AbstractText)
        assertEquals("Wikipedia", response.AbstractSource)
        assertEquals("https://en.wikipedia.org/wiki/Kotlin", response.AbstractURL)
    }

    @Test
    fun parse_results_array() {
        val responseJson = """
        {
            "Results": [
                {
                    "Text": "Result 1",
                    "FirstURL": "https://example.com/1",
                    "Icon": {"URL": "https://example.com/icon.png"}
                },
                {
                    "Text": "Result 2",
                    "FirstURL": "https://example.com/2"
                }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<DuckDuckGoResponse>(responseJson)
        assertEquals(2, response.Results.size)
        assertEquals("Result 1", response.Results[0].Text)
        assertEquals("https://example.com/1", response.Results[0].FirstURL)
        assertNotNull(response.Results[0].Icon)
        assertEquals("https://example.com/icon.png", response.Results[0].Icon!!.URL)
    }

    @Test
    fun parse_empty_response() {
        val response = json.decodeFromString<DuckDuckGoResponse>("{}")
        assertEquals("", response.AbstractText)
        assertEquals("", response.AbstractSource)
        assertTrue(response.Results.isEmpty())
        assertTrue(response.RelatedTopics.isEmpty())
    }

    @Test
    fun parse_related_topics_with_disambiguation() {
        val responseJson = """
        {
            "RelatedTopics": [
                {
                    "Name": "Kotlin",
                    "Topics": [
                        {
                            "Text": "Topic 1",
                            "FirstURL": "https://example.com/topic1"
                        }
                    ]
                },
                {
                    "Text": "Direct topic",
                    "FirstURL": "https://example.com/direct"
                }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<DuckDuckGoResponse>(responseJson)
        assertEquals(2, response.RelatedTopics.size)
        assertEquals("Kotlin", response.RelatedTopics[0].Name)
        assertEquals(1, response.RelatedTopics[0].Topics.size)
        assertEquals("Direct topic", response.RelatedTopics[1].Text)
    }
}
