package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardInsetPolicyTest {

    @Test
    fun `current window insets win before first layout`() {
        assertEquals(
            KeyboardSystemInsets(left = 20, bottom = 12),
            KeyboardInsetPolicy.initial(
                window = KeyboardSystemInsets(left = 20, bottom = 12),
                previous = KeyboardSystemInsets(bottom = 48),
                navigationBarFallback = 60,
            ),
        )
    }

    @Test
    fun `known previous insets seed a rebuilt view`() {
        assertEquals(
            KeyboardSystemInsets(bottom = 48),
            KeyboardInsetPolicy.initial(
                window = null,
                previous = KeyboardSystemInsets(bottom = 48),
                navigationBarFallback = 60,
            ),
        )
    }

    @Test
    fun `first launch reserves navigation bar before insets arrive`() {
        assertEquals(
            KeyboardSystemInsets(bottom = 60),
            KeyboardInsetPolicy.initial(
                window = null,
                previous = KeyboardSystemInsets(),
                navigationBarFallback = 60,
            ),
        )
    }

    @Test
    fun `fallback cannot create negative padding`() {
        assertEquals(
            KeyboardSystemInsets(),
            KeyboardInsetPolicy.initial(
                window = null,
                previous = KeyboardSystemInsets(),
                navigationBarFallback = -1,
            ),
        )
    }
}
