package me.zssu.ime.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSimilarityTest {
    @Test
    fun normalizesKatakanaToHiragana() {
        assertTrue(ReadingSimilarity.normalize("マンション") == "まんしょん")
    }

    @Test
    fun acceptsSingleEditAndAdjacentTransposition() {
        assertTrue(ReadingSimilarity.isNear("まんしょん", "やんしょん"))
        assertTrue(ReadingSimilarity.isNear("ありがとう", "ありがうと"))
    }

    @Test
    fun rejectsShortOrDistantReadings() {
        assertFalse(ReadingSimilarity.isNear("ねこ", "ねご"))
        assertFalse(ReadingSimilarity.isNear("まんしょん", "ありがとう"))
    }
}
