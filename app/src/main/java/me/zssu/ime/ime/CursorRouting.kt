package me.zssu.ime.ime

/** Chooses whether a horizontal arrow belongs to Mozc, a rich editor, or a raw terminal. */
internal object CursorRouting {

    enum class Target {
        MOZC_COMPOSITION,
        EDITOR_SELECTION,
        RAW_KEY_EVENT,
    }

    fun target(
        hasComposition: Boolean,
        rawKeyEvents: Boolean,
    ): Target = when {
        hasComposition -> Target.MOZC_COMPOSITION
        rawKeyEvents -> Target.RAW_KEY_EVENT
        else -> Target.EDITOR_SELECTION
    }
}
