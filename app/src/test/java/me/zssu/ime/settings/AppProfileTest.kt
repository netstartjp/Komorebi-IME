package me.zssu.ime.settings

import me.zssu.ime.keyboard.KeyboardStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppProfileTest {
    @Test
    fun storedNamesResolveSafely() {
        val profile = AppProfileStore.Profile(
            packageName = "com.example.editor",
            keyboardStyle = "MIXED",
            oneHandMode = "RIGHT",
        )
        assertEquals(KeyboardStyle.MIXED, profile.resolvedKeyboardStyle)
        assertEquals(ImeSettings.OneHandMode.RIGHT, profile.resolvedOneHandMode)
    }

    @Test
    fun unknownStoredNamesFallBack() {
        val profile = AppProfileStore.Profile(
            packageName = "com.example.editor",
            keyboardStyle = "FUTURE_STYLE",
            oneHandMode = "FUTURE_MODE",
        )
        assertEquals(KeyboardStyle.FLICK, profile.resolvedKeyboardStyle)
        assertEquals(ImeSettings.OneHandMode.OFF, profile.resolvedOneHandMode)
    }

    @Test
    fun packageNameValidationRejectsPaths() {
        assertEquals(true, AppProfileStore.validPackageName("com.example.editor"))
        assertEquals(false, AppProfileStore.validPackageName("../files/profile"))
    }
}
