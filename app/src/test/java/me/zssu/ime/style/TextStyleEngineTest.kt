package me.zssu.ime.style

import org.junit.Assert.assertEquals
import org.junit.Test

class TextStyleEngineTest {

    @Test
    fun originalDoesNotChangeText() {
        assertEquals(
            "了解です",
            TextStyleEngine.apply("了解です", TextStyle.ORIGINAL),
        )
    }

    @Test
    fun plainAndCasualStylesChangeSentenceEnding() {
        assertEquals("了解だ", TextStyleEngine.apply("了解です", TextStyle.PLAIN))
        assertEquals("了解だよ", TextStyleEngine.apply("了解です", TextStyle.CASUAL))
    }

    @Test
    fun threePoliteLevelsRemainDistinct() {
        assertEquals(
            "了解です",
            TextStyleEngine.apply("了解だ", TextStyle.NORMAL_KEIGO),
        )
        assertEquals(
            "了解ですね",
            TextStyleEngine.apply("了解だ", TextStyle.SOFT_KEIGO),
        )
        assertEquals(
            "了解でございます",
            TextStyleEngine.apply("了解だ", TextStyle.HARD_KEIGO),
        )
    }

    @Test
    fun fixedPoliteExpressionNormalizesBeforeGenericEnding() {
        assertEquals(
            "ありがとう",
            TextStyleEngine.apply("ありがとうございます", TextStyle.PLAIN),
        )
    }

    @Test
    fun longHonorificVerbIsNotPartiallyNormalized() {
        assertEquals(
            "来た",
            TextStyleEngine.apply("いらっしゃいました", TextStyle.PLAIN),
        )
    }
}
