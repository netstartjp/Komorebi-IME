package me.zssu.ime.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme for the settings screen — vibrant & joyful Komorebi palette.
 */
@Composable
fun ZinnaTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> darkColorScheme(
            primary = KomorebiMint,
            onPrimary = Color(0xFF003730),
            primaryContainer = Color(0xFF005046),
            onPrimaryContainer = KomorebiMintLight,
            secondary = KomorebiPeach,
            onSecondary = Color(0xFF3E1100),
            secondaryContainer = Color(0xFF5C2200),
            onSecondaryContainer = KomorebiPeachLight,
            tertiary = KomorebiLavender,
            onTertiary = Color(0xFF1C005C),
            tertiaryContainer = Color(0xFF2D0090),
            onTertiaryContainer = KomorebiLavenderLight,
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE1E2E9),
            surfaceVariant = Color(0xFF1E2128),
            onSurfaceVariant = Color(0xFFC4C6D0),
            background = Color(0xFF111318),
            onBackground = Color(0xFFE1E2E9),
            error = KomorebiRose,
        )

        else -> lightColorScheme(
            primary = KomorebiForest,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFA7F5D0),
            onPrimaryContainer = Color(0xFF002117),
            secondary = KomorebiSunset,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFDBC8),
            onSecondaryContainer = Color(0xFF3A0B00),
            tertiary = KomorebiPlum,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF0DBFF),
            onTertiaryContainer = Color(0xFF2C0052),
            surface = Color(0xFFFEFBFF),
            onSurface = Color(0xFF1B1B21),
            surfaceVariant = Color(0xFFF0F0F8),
            onSurfaceVariant = Color(0xFF45464F),
            background = Color(0xFFFEFBFF),
            onBackground = Color(0xFF1B1B21),
            error = KomorebiRose,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

// Komorebi vibrant palette — energetic, warm, playful
private val KomorebiForest = Color(0xFF0B8460)
private val KomorebiMint = Color(0xFF4DDBA8)
private val KomorebiMintLight = Color(0xFFB8F5DD)
private val KomorebiSunset = Color(0xFFF06B3C)
private val KomorebiPeach = Color(0xFFFFB593)
private val KomorebiPeachLight = Color(0xFFFFDBC8)
private val KomorebiPlum = Color(0xFF7B3FCF)
private val KomorebiLavender = Color(0xFFC8ABFF)
private val KomorebiLavenderLight = Color(0xFFE5D6FF)
private val KomorebiRose = Color(0xFFFF4D6A)
