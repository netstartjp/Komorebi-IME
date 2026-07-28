package me.zssu.ime.ime

/**
 * Insets applied inside the IME content view.
 *
 * Modern Android and vendor IME windows disagree on whether the input view is pre-positioned above
 * the navigation/keyboard-switcher area. Insets are the only per-device authority: keep the panel
 * background edge-to-edge, but pad interactive keyboard content away from every reported edge.
 */
internal object KeyboardInsetPolicy {
    data class Padding(
        val left: Int,
        val right: Int,
        val bottom: Int,
    )

    fun contentPadding(left: Int, right: Int, bottom: Int): Padding = Padding(
        left = left.coerceAtLeast(0),
        right = right.coerceAtLeast(0),
        bottom = bottom.coerceAtLeast(0),
    )
}
