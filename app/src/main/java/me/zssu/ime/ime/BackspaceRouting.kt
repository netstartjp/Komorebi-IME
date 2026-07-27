package me.zssu.ime.ime

/**
 * Decides who owns one Backspace before either Mozc or the editor mutates its state.
 *
 * In particular, the last composing character disappearing must not make the same key press fall
 * through and delete a second character from the editor.
 */
internal object BackspaceRouting {

    enum class Target {
        MOZC_COMPOSITION,
        EDITOR_SELECTION,
        EDITOR_PREVIOUS_CODE_POINT,
    }

    fun target(hadComposition: Boolean, hasSelection: Boolean): Target = when {
        hadComposition -> Target.MOZC_COMPOSITION
        hasSelection -> Target.EDITOR_SELECTION
        else -> Target.EDITOR_PREVIOUS_CODE_POINT
    }

    /**
     * An empty Mozc preedit can mean either "the old composition was deleted" or "text was
     * committed". Calling finishComposingText for the former would preserve the old editor text as
     * a confirmed hiragana string, so only that transition must replace the composing span with
     * empty text.
     */
    fun shouldRemoveOldEditorComposition(
        hadComposition: Boolean,
        nextHasComposition: Boolean,
        committedText: String,
    ): Boolean =
        hadComposition && !nextHasComposition && committedText.isEmpty()
}
