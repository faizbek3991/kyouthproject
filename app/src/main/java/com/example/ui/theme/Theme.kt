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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = SambalPrimaryDark,
  onPrimary = OnSambalDark,
  primaryContainer = SambalContainerDark,
  onPrimaryContainer = OnSambalContainerDark,
  secondary = PandanSecondaryDark,
  onSecondary = OnPandanDark,
  secondaryContainer = PandanContainerDark,
  onSecondaryContainer = OnPandanContainerDark,
  tertiary = SaffronTertiaryDark,
  onTertiary = OnSaffronDark,
  tertiaryContainer = SaffronContainerDark,
  onTertiaryContainer = OnSaffronContainerDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  surfaceContainerLow = SurfaceContainerLowDark,
  surfaceContainer = SurfaceContainerDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  error = ErrorDark,
  errorContainer = ErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
  primary = SambalPrimaryLight,
  onPrimary = OnSambalLight,
  primaryContainer = SambalContainerLight,
  onPrimaryContainer = OnSambalContainerLight,
  secondary = PandanSecondaryLight,
  onSecondary = OnPandanLight,
  secondaryContainer = PandanContainerLight,
  onSecondaryContainer = OnPandanContainerLight,
  tertiary = SaffronTertiaryLight,
  onTertiary = OnSaffronLight,
  tertiaryContainer = SaffronContainerLight,
  onTertiaryContainer = OnSaffronContainerLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  surfaceContainerLow = SurfaceContainerLowLight,
  surfaceContainer = SurfaceContainerLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  error = ErrorLight,
  errorContainer = ErrorContainerLight
)

val DapurShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DapurPassTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = DapurShapes,
    content = content
  )
}
