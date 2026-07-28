package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class InputViewRefreshPolicyTest {
    @Test
    fun `background-only change updates attached panel without replacing input view`() {
        assertEquals(
            InputViewRefreshPolicy.Target.UPDATE_PANEL,
            InputViewRefreshPolicy.target(
                structureChanged = false,
                revisionChanged = true,
            ),
        )
    }

    @Test
    fun `geometry or layout change rebuilds the input view`() {
        assertEquals(
            InputViewRefreshPolicy.Target.REBUILD,
            InputViewRefreshPolicy.target(
                structureChanged = true,
                revisionChanged = true,
            ),
        )
    }

    @Test
    fun `unchanged settings leave view and bitmap untouched`() {
        assertEquals(
            InputViewRefreshPolicy.Target.NONE,
            InputViewRefreshPolicy.target(
                structureChanged = false,
                revisionChanged = false,
            ),
        )
    }
}
