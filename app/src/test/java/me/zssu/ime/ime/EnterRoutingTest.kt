package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class EnterRoutingTest {

    @Test
    fun `composition is submitted before any editor action`() {
        assertEquals(
            EnterRouting.Target.SUBMIT_COMPOSITION,
            EnterRouting.target(
                hadComposition = true,
                rawKeyEvents = true,
                hasEnabledEditorAction = true,
                multiline = true,
            ),
        )
    }

    @Test
    fun `explicit send action beats multiline newline`() {
        assertEquals(
            EnterRouting.Target.EDITOR_ACTION,
            EnterRouting.target(
                hadComposition = false,
                rawKeyEvents = false,
                hasEnabledEditorAction = true,
                multiline = true,
            ),
        )
    }

    @Test
    fun `multiline field without action inserts newline`() {
        assertEquals(
            EnterRouting.Target.NEWLINE,
            EnterRouting.target(
                hadComposition = false,
                rawKeyEvents = false,
                hasEnabledEditorAction = false,
                multiline = true,
            ),
        )
    }

    @Test
    fun `terminal field receives raw enter`() {
        assertEquals(
            EnterRouting.Target.RAW_KEY_EVENT,
            EnterRouting.target(
                hadComposition = false,
                rawKeyEvents = true,
                hasEnabledEditorAction = true,
                multiline = true,
            ),
        )
    }

    @Test
    fun `single line field without action receives raw enter`() {
        assertEquals(
            EnterRouting.Target.RAW_KEY_EVENT,
            EnterRouting.target(
                hadComposition = false,
                rawKeyEvents = false,
                hasEnabledEditorAction = false,
                multiline = false,
            ),
        )
    }
}
