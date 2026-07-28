package me.zssu.ime.ime

/**
 * Keeps the editor-facing and Mozc-facing composition state in sync when Android restarts input.
 */
internal object InputRestartRouting {

    enum class Target {
        PRESERVE_COMPOSITION,
        RESET_SESSION,
    }

    /**
     * A restart may preserve a live composition in the same editor. If the service no longer
     * considers anything composing, however, retaining Mozc's state would leave an invisible old
     * conversion behind that can be committed by the next key.
     */
    fun target(restarting: Boolean, hasComposition: Boolean): Target =
        if (restarting && hasComposition) {
            Target.PRESERVE_COMPOSITION
        } else {
            Target.RESET_SESSION
        }
}
