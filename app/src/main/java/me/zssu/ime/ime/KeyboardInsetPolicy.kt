package me.zssu.ime.ime

/**
 * Insets applied inside the IME content view.
 *
 * Android's IME window already places its content above bottom navigation controls. Applying the
 * reported bottom system-bar inset again grows the input view and leaves an empty strip below the
 * keys. Horizontal insets are still needed for side navigation and display cutouts in landscape.
 */
internal object KeyboardInsetPolicy {
    data class Padding(
        val left: Int,
        val right: Int,
        val bottom: Int,
    )

    fun contentPadding(left: Int, right: Int): Padding = Padding(
        left = left.coerceAtLeast(0),
        right = right.coerceAtLeast(0),
        bottom = 0,
    )
}
