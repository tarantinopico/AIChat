package com.example

import com.example.domain.util.StreamFileExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamFileExtractorContinuationTest {

    @Test
    fun testDetectUnclosedBlock() {
        val completeText = """
            Zde je kód:
            ```kotlin
            fun hello() { println("Hello") }
            ```
            Dokončeno.
        """.trimIndent()
        assertFalse(StreamFileExtractor.isLastBlockUnclosed(completeText))

        val unclosedText = """
            Zde je kód:
            ```kotlin
            fun hello() {
                println("Ahoj světe")
        """.trimIndent()
        assertTrue(StreamFileExtractor.isLastBlockUnclosed(unclosedText))
    }

    @Test
    fun testMergeRawTextWithoutFences() {
        val part1 = """
            Zde je kód:
            ```kotlin
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
        """.trimIndent()

        val part2 = """
            
                fun multiply(a: Int, b: Int): Int {
                    return a * b
                }
            }
            ```
            To je vše.
        """.trimIndent()

        val merged = StreamFileExtractor.mergeRawText(part1, part2)
        val parsed = StreamFileExtractor.parse(merged)

        assertEquals(1, parsed.files.size)
        assertTrue(parsed.files[0].isComplete)
        assertTrue(parsed.files[0].content.contains("fun add"))
        assertTrue(parsed.files[0].content.contains("fun multiply"))
        assertTrue(parsed.cleanText.contains("Zde je kód:"))
        assertTrue(parsed.cleanText.contains("To je vše."))
    }

    @Test
    fun testMergeRawTextWhenModelRepeatsOpeningFence() {
        val part1 = """
            Zde je kód:
            ```kotlin
            class Service {
                val name: String = "Test"
        """.trimIndent()

        val part2 = """
            ```kotlin
                fun run() {
                    println(name)
                }
            }
            ```
            Konec.
        """.trimIndent()

        val merged = StreamFileExtractor.mergeRawText(part1, part2)
        val parsed = StreamFileExtractor.parse(merged)

        assertEquals(1, parsed.files.size)
        assertTrue(parsed.files[0].isComplete)
        assertTrue(parsed.files[0].content.contains("val name"))
        assertTrue(parsed.files[0].content.contains("fun run"))
    }
}
