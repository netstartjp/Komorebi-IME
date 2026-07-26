package me.zssu.ime.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionUpdateTest {

    @Test
    fun `own composing update keeps native composition`() {
        assertFalse(
            SelectionUpdate.invalidatesComposition(
                isComposing = true,
                newSelectionStart = 5,
                newSelectionEnd = 5,
                composingEnd = 5,
            )
        )
    }

    @Test
    fun `editor clearing composing span resets native composition`() {
        assertTrue(
            SelectionUpdate.invalidatesComposition(
                isComposing = true,
                newSelectionStart = 0,
                newSelectionEnd = 0,
                composingEnd = -1,
            )
        )
    }

    @Test
    fun `caret move or range selection resets native composition`() {
        assertTrue(
            SelectionUpdate.invalidatesComposition(
                isComposing = true,
                newSelectionStart = 3,
                newSelectionEnd = 3,
                composingEnd = 5,
            )
        )
        assertTrue(
            SelectionUpdate.invalidatesComposition(
                isComposing = true,
                newSelectionStart = 2,
                newSelectionEnd = 5,
                composingEnd = 5,
            )
        )
    }

    @Test
    fun `selection changes are irrelevant without a native composition`() {
        assertFalse(
            SelectionUpdate.invalidatesComposition(
                isComposing = false,
                newSelectionStart = 1,
                newSelectionEnd = 4,
                composingEnd = -1,
            )
        )
    }
}
