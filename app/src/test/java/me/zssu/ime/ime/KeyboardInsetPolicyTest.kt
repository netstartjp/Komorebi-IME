package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardInsetPolicyTest {

    @Test
    fun `bottom OS control area is reserved from keyboard content`() {
        assertEquals(
            KeyboardInsetPolicy.Padding(left = 0, right = 0, bottom = 84),
            KeyboardInsetPolicy.contentPadding(left = 0, right = 0, bottom = 84),
        )
    }

    @Test
    fun `horizontal safety insets remain in landscape`() {
        assertEquals(
            KeyboardInsetPolicy.Padding(left = 48, right = 12, bottom = 20),
            KeyboardInsetPolicy.contentPadding(left = 48, right = 12, bottom = 20),
        )
    }

    @Test
    fun `invalid negative insets cannot create negative padding`() {
        assertEquals(
            KeyboardInsetPolicy.Padding(left = 0, right = 0, bottom = 0),
            KeyboardInsetPolicy.contentPadding(left = -1, right = -20, bottom = -3),
        )
    }
}
