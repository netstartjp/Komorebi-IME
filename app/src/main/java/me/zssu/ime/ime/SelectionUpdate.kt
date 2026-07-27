package me.zssu.ime.ime

/**
 * Pure selection policy kept separate from [ZinnaImeService] so editor edge cases can be tested
 * without constructing an Android [android.inputmethodservice.InputMethodService].
 */
internal object SelectionUpdate {

    /**
     * A composing editor normally reports a collapsed selection at Mozc's cursor inside its
     * composing span. A different caret, a range selection, or a missing span means the editor
     * changed independently.
     */
    fun invalidatesComposition(
        isComposing: Boolean,
        newSelectionStart: Int,
        newSelectionEnd: Int,
        composingStart: Int,
        composingEnd: Int,
        preeditCursor: Int,
    ): Boolean =
        isComposing &&
            (
                composingStart < 0 ||
                    composingEnd < composingStart ||
                    newSelectionStart != newSelectionEnd ||
                    newSelectionStart != composingStart + preeditCursor
                )
}
