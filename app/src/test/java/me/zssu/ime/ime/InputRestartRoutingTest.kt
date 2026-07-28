package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class InputRestartRoutingTest {

    @Test
    fun `same editor restart preserves a live composition`() {
        assertEquals(
            InputRestartRouting.Target.PRESERVE_COMPOSITION,
            InputRestartRouting.target(restarting = true, hasComposition = true),
        )
    }

    @Test
    fun `restart while idle clears any invisible Mozc state`() {
        assertEquals(
            InputRestartRouting.Target.RESET_SESSION,
            InputRestartRouting.target(restarting = true, hasComposition = false),
        )
    }

    @Test
    fun `new input always starts with a clean Mozc session`() {
        assertEquals(
            InputRestartRouting.Target.RESET_SESSION,
            InputRestartRouting.target(restarting = false, hasComposition = false),
        )
        assertEquals(
            InputRestartRouting.Target.RESET_SESSION,
            InputRestartRouting.target(restarting = false, hasComposition = true),
        )
    }
}
