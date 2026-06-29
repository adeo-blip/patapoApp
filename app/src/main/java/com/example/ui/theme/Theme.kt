package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = ScadaDarkPrimary,
    secondary = ScadaBlueAccent,
    tertiary = ScadaBlueSecondary,
    background = ScadaDarkBg,
    surface = ScadaDarkSurface,
    onPrimary = ScadaBluePrimary,
    onSecondary = ScadaLightSurface,
    onBackground = ScadaLightBg,
    onSurface = ScadaLightBg,
    error = ScadaRedAlarm
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ScadaBluePrimary,
    secondary = ScadaBlueAccent,
    tertiary = ScadaBlueSecondary,
    background = ScadaLightBg,
    surface = ScadaLightSurface,
    onPrimary = ScadaLightSurface,
    onSecondary = ScadaLightSurface,
    onBackground = Color(0xFF0F172A), // Slate 900
    onSurface = Color(0xFF0F172A),     // Slate 900
    error = ScadaRedAlarm
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force custom theme consistency for industrial look
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
