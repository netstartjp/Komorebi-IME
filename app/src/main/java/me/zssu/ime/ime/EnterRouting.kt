package me.zssu.ime.ime

/**
 * Chooses one Enter behaviour without letting a multiline flag hide an explicit editor action.
 */
internal object EnterRouting {

    enum class Target {
        SUBMIT_COMPOSITION,
        EDITOR_ACTION,
        NEWLINE,
        RAW_KEY_EVENT,
    }

    fun target(
        hadComposition: Boolean,
        rawKeyEvents: Boolean,
        hasEnabledEditorAction: Boolean,
        multiline: Boolean,
    ): Target = when {
        hadComposition -> Target.SUBMIT_COMPOSITION
        rawKeyEvents -> Target.RAW_KEY_EVENT
        hasEnabledEditorAction -> Target.EDITOR_ACTION
        multiline -> Target.NEWLINE
        else -> Target.RAW_KEY_EVENT
    }
}
