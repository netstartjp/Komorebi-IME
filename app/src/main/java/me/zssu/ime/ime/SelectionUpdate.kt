package me.zssu.ime.ime

/**
 * Pure selection policy kept separate from [ZinnaImeService] so editor edge cases can be tested
 * without constructing an Android [android.inputmethodservice.InputMethodService].
 */
internal object SelectionUpdate {

    /**
     * A composing editor normally reports a collapsed selection at the end of its composing span.
     * A moved caret, a range selection, or a missing span means the editor changed independently.
     */
    fun invalidatesComposition(
        isComposing: Boolean,
        newSelectionStart: Int,
        newSelectionEnd: Int,
        composingEnd: Int,
    ): Boolean =
        isComposing &&
            (newSelectionStart != composingEnd || newSelectionEnd != composingEnd)
}
