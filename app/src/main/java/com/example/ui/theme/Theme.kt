package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val OverPadDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA0E9FF),
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFC2F1FF),
    secondary = Color(0xFFA3F5C5),
    onSecondary = Color(0xFF003920),
    secondaryContainer = Color(0xFF005231),
    onSecondaryContainer = Color(0xFFBEFFD7),
    tertiary = Color(0xFFE0BBFF),
    onTertiary = Color(0xFF421968),
    tertiaryContainer = Color(0xFF5A3180),
    onTertiaryContainer = Color(0xFFF3DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1418),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF0E1418),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFBFC8CD),
    surfaceContainerLowest = Color(0xFF090F12),
    surfaceContainerLow = Color(0xFF161C20),
    surfaceContainer = Color(0xFF1A2024),
    surfaceContainerHigh = Color(0xFF252B2F),
    surfaceContainerHighest = Color(0xFF2F363A),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C)
)

private val OverPadLightColorScheme = lightColorScheme(
    primary = Color(0xFF00677D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EBFF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF006D43),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8FF7BE),
    onSecondaryContainer = Color(0xFF002111),
    tertiary = Color(0xFF724899),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF2B004E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFD),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF5FAFD),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E9),
    onSurfaceVariant = Color(0xFF40484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4F7),
    surfaceContainer = Color(0xFFEAEFF2),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E7),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFBFC8CD)
)

val OverPadShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun OverPadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OverPadDarkColorScheme
        else -> OverPadLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OverPadTypography,
        shapes = OverPadShapes,
        content = content
    )
}
