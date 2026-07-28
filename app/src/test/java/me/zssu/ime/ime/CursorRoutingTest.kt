package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class CursorRoutingTest {

    @Test
    fun `composition keeps arrow inside mozc even in terminal`() {
        assertEquals(
            CursorRouting.Target.MOZC_COMPOSITION,
            CursorRouting.target(hasComposition = true, rawKeyEvents = true),
        )
    }

    @Test
    fun `idle terminal receives raw arrow key`() {
        assertEquals(
            CursorRouting.Target.RAW_KEY_EVENT,
            CursorRouting.target(hasComposition = false, rawKeyEvents = true),
        )
    }

    @Test
    fun `rich editor moves its selection`() {
        assertEquals(
            CursorRouting.Target.EDITOR_SELECTION,
            CursorRouting.target(hasComposition = false, rawKeyEvents = false),
        )
    }
}
