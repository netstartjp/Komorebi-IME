package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceRoutingTest {

    @Test
    fun `last composing character is deleted only by mozc`() {
        assertEquals(
            BackspaceRouting.Target.MOZC_COMPOSITION,
            BackspaceRouting.target(hadComposition = true, hasSelection = false),
        )
    }

    @Test
    fun `editor selection is replaced instead of deleting around it`() {
        assertEquals(
            BackspaceRouting.Target.EDITOR_SELECTION,
            BackspaceRouting.target(hadComposition = false, hasSelection = true),
        )
    }

    @Test
    fun `idle collapsed cursor deletes one editor code point`() {
        assertEquals(
            BackspaceRouting.Target.EDITOR_PREVIOUS_CODE_POINT,
            BackspaceRouting.target(hadComposition = false, hasSelection = false),
        )
    }

    @Test
    fun `composition remains authoritative even if editor reports a selection`() {
        assertEquals(
            BackspaceRouting.Target.MOZC_COMPOSITION,
            BackspaceRouting.target(
                hadComposition = true,
                hasSelection = true,
                rawKeyEvents = true,
            ),
        )
    }

    @Test
    fun `idle terminal receives a raw backspace key event`() {
        assertEquals(
            BackspaceRouting.Target.RAW_KEY_EVENT,
            BackspaceRouting.target(
                hadComposition = false,
                hasSelection = false,
                rawKeyEvents = true,
            ),
        )
    }

    @Test
    fun `raw terminal mode does not query or replace an editor selection`() {
        assertEquals(
            BackspaceRouting.Target.RAW_KEY_EVENT,
            BackspaceRouting.target(
                hadComposition = false,
                hasSelection = true,
                rawKeyEvents = true,
            ),
        )
    }

    @Test
    fun `empty preedit after backspace removes old editor composition`() {
        assertEquals(
            true,
            BackspaceRouting.shouldRemoveOldEditorComposition(
                hadComposition = true,
                nextHasComposition = false,
                committedText = "",
            ),
        )
    }

    @Test
    fun `submitted text replaces old composition without an extra empty commit`() {
        assertEquals(
            false,
            BackspaceRouting.shouldRemoveOldEditorComposition(
                hadComposition = true,
                nextHasComposition = false,
                committedText = "確定",
            ),
        )
    }

    @Test
    fun `idle empty response has no old composition to remove`() {
        assertEquals(
            false,
            BackspaceRouting.shouldRemoveOldEditorComposition(
                hadComposition = false,
                nextHasComposition = false,
                committedText = "",
            ),
        )
    }
}
