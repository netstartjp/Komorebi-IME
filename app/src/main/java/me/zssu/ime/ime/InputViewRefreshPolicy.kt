package me.zssu.ime.ime

/**
 * Decides how settings are applied to an already running IME view.
 *
 * Background file/opacity changes do not alter keyboard geometry. Replacing the whole input view
 * for them briefly lets some Android versions measure the new view before navigation insets are
 * delivered, which can move the keyboard over the navigation area. Updating the existing panel
 * avoids that transient layout entirely.
 */
internal object InputViewRefreshPolicy {
    enum class Target {
        NONE,
        UPDATE_PANEL,
        REBUILD,
    }

    fun target(structureChanged: Boolean, revisionChanged: Boolean): Target = when {
        structureChanged -> Target.REBUILD
        revisionChanged -> Target.UPDATE_PANEL
        else -> Target.NONE
    }
}
