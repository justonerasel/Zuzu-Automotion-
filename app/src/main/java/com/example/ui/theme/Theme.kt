package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ZuzuLightColorScheme = lightColorScheme(
  primary = ZuzuPurple,
  onPrimary = ZuzuWhite,
  primaryContainer = ZuzuPurpleSoft,
  onPrimaryContainer = ZuzuDeepPurple,
  secondary = ZuzuElectricBlue,
  onSecondary = ZuzuWhite,
  secondaryContainer = ZuzuTurquoiseSoft,
  onSecondaryContainer = ZuzuElectricBlue,
  tertiary = ZuzuPink,
  onTertiary = ZuzuWhite,
  tertiaryContainer = ZuzuMagentaSoft,
  onTertiaryContainer = ZuzuDeepPurple,
  background = ZuzuCanvasTop,
  onBackground = ZuzuInkPrimary,
  surface = ZuzuSurfaceWhite,
  onSurface = ZuzuInkPrimary,
  surfaceVariant = ZuzuSoftLavender,
  onSurfaceVariant = ZuzuInkSecondary,
  surfaceContainerLowest = ZuzuSurfaceWhite,
  surfaceContainerLow = ZuzuSoftGradientWhite,
  surfaceContainer = ZuzuSurfaceWhite,
  surfaceContainerHigh = ZuzuSoftLavender,
  surfaceContainerHighest = ZuzuSoftSky,
  outline = ZuzuBorderLight,
  outlineVariant = ZuzuPurpleSoft,
  error = ZuzuError,
  onError = ZuzuWhite,
)

val ZuzuShapes = Shapes(
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(22.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun ZuzuSocialAiTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ZuzuLightColorScheme,
    typography = ZuzuTypography,
    shapes = ZuzuShapes,
    content = content,
  )
}
