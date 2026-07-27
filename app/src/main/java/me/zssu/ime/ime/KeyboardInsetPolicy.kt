package me.zssu.ime.ime

/**
 * Insets reserved inside the IME panel so keys never overlap system navigation controls.
 */
internal data class KeyboardSystemInsets(
    val left: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val isKnown: Boolean get() = left > 0 || right > 0 || bottom > 0
}

/**
 * Chooses a safe value before the input view's first layout.
 *
 * The IME window may not have dispatched WindowInsets when Android asks for its input view. A
 * framework navigation-bar dimension is therefore used only for the first, otherwise-unknown
 * frame; real window insets replace it as soon as the view is attached.
 */
internal object KeyboardInsetPolicy {
    fun initial(
        window: KeyboardSystemInsets?,
        previous: KeyboardSystemInsets,
        navigationBarFallback: Int,
    ): KeyboardSystemInsets = when {
        window != null -> window
        previous.isKnown -> previous
        else -> KeyboardSystemInsets(bottom = navigationBarFallback.coerceAtLeast(0))
    }
}
