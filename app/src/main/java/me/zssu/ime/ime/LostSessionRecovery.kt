package me.zssu.ime.ime

/**
 * Recovery plan for a missing conversion-engine response.
 *
 * The editor's last composing text is preserved as ordinary text. Deleting it would lose user
 * input; leaving it marked composing would keep routing future keys into an unknown native state.
 */
internal object LostSessionRecovery {
    data class Plan(val finishEditorComposition: Boolean)

    fun plan(hadComposition: Boolean): Plan =
        Plan(finishEditorComposition = hadComposition)
}
