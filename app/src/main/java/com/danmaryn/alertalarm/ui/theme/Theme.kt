package com.danmaryn.alertalarm.ui.theme

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
import com.danmaryn.alertalarm.data.model.ThemeMode

// Apple iOS / Samsung One UI inspired dark palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),      // iOS System Blue (Dark)
    secondary = Color(0xFF5E5CE6),
    tertiary = Color(0xFFFF453A),     // iOS System Red
    background = Color(0xFF000000),   // True OLED Black (Samsung/Apple)
    surface = Color(0xFF1C1C1E),      // Elevated Card Background
    surfaceVariant = Color(0xFF2C2C2E)
)

// Samsung One UI / iOS inspired light palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),      // iOS System Blue (Light)
    secondary = Color(0xFF5856D6),
    tertiary = Color(0xFFFF3B30),
    background = Color(0xFFF2F2F7),   // Grouped background
    surface = Color(0xFFFFFFFF),      // Clean white card
    surfaceVariant = Color(0xFFE5E5EA)
)

@Composable
fun AlertAlarmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
