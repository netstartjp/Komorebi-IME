package me.zssu.ime.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LostSessionRecoveryTest {
    @Test
    fun `lost response preserves and finishes visible composition`() {
        assertTrue(
            LostSessionRecovery.plan(hadComposition = true).finishEditorComposition
        )
    }

    @Test
    fun `lost idle response does not touch editor text`() {
        assertFalse(
            LostSessionRecovery.plan(hadComposition = false).finishEditorComposition
        )
    }
}
