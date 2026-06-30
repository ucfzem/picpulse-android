package com.picpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBrown = Color(0xFF1A0F0A)
private val CardBrown = Color(0xFF2A1A12)
private val Gold = Color(0xFFC9950E)
private val GoldLight = Color(0xFFE8B830)
private val WarmWhite = Color(0xFFF5F0E8)
private val Beige = Color(0xFFF5EED6)
private val BrownText = Color(0xFF3A2A1A)
private val BrownAccent = Color(0xFFB8860B)
private val BrownSecondary = Color(0xFF8B4513)
private val LightBg = Color(0xFFF5EED6)

private val LightColors = lightColorScheme(
    primary = BrownAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E0C0),
    secondary = BrownSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0D8B8),
    background = LightBg,
    surface = Color.White,
    surfaceVariant = Color(0xFFF8F0E0),
    onBackground = BrownText,
    onSurface = BrownText,
    onSurfaceVariant = Color(0xFF7A6A5A),
    outline = Color(0xFFD4C4A8),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = DarkBrown,
    primaryContainer = Color(0xFF3A2A1A),
    secondary = GoldLight,
    onSecondary = DarkBrown,
    secondaryContainer = Color(0xFF2A1A12),
    background = DarkBrown,
    surface = CardBrown,
    surfaceVariant = Color(0xFF241510),
    onBackground = WarmWhite,
    onSurface = WarmWhite,
    onSurfaceVariant = Color(0xFFB8A898),
    outline = Color(0xFF3A2A1A),
    error = Color(0xFFF87171),
)

@Composable
fun PicPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
