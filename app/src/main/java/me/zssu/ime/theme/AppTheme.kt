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
 * Material 3 theme for the settings screen.
 *
 * Follows the same rules as the keyboard itself (see [MaterialYouTheme]): the wallpaper palette on
 * Android 12 and later, a static scheme below that, and light or dark from the system rather than
 * from a setting of our own. A keyboard that recolours with the wallpaper next to a settings screen
 * frozen in Compose's default purple looks like two different apps.
 *
 * @param dark defaults to the system setting; passed explicitly only by previews.
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
            primary = KomorebiJade,
            secondary = KomorebiGold,
            tertiary = KomorebiCoral,
        )

        else -> lightColorScheme(
            primary = KomorebiForest,
            secondary = KomorebiGoldDark,
            tertiary = KomorebiCoral,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

// Static fallback palette sampled from the generated Komorebi launcher artwork.
private val KomorebiForest = Color(0xFF075149)
private val KomorebiJade = Color(0xFF8FCB78)
private val KomorebiGold = Color(0xFFFFE47A)
private val KomorebiGoldDark = Color(0xFF8A6A13)
private val KomorebiCoral = Color(0xFFFF7258)
