package com.bignerdrancn.android.handheldbarcodescanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NightGreen,
    secondary = NightAmber,
    tertiary = Color(0xFF93C5FD),
    background = Color(0xFF0B1218),
    surface = NightSurface,
    onPrimary = Color(0xFF052E2B),
    onSecondary = Color(0xFF332200),
    onBackground = Color(0xFFE5EEF5),
    onSurface = Color(0xFFE5EEF5)
)

private val LightColorScheme = lightColorScheme(
    primary = WarehouseGreen,
    secondary = SignalAmber,
    tertiary = Color(0xFF2563EB),
    background = WorkSurface,
    surface = Color.White,
    surfaceVariant = WorkSurfaceVariant,
    outline = WorkOutline,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun HandheldBarcodeScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
