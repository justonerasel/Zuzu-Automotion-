package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Design 2 Typography System:
 * - Outfit for expressive creative headings & KPI numerals
 * - Plus Jakarta Sans for clean, modern UI copy, labels, and analytics tables
 */
val ZuzuDisplayFontFamily: FontFamily = FontFamily(
  Font(R.font.outfit, FontWeight.SemiBold),
  Font(R.font.outfit, FontWeight.Bold),
  Font(R.font.outfit, FontWeight.ExtraBold),
)

val ZuzuFontFamily: FontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val ZuzuTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = ZuzuDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.6).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = ZuzuDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.3).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = ZuzuDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.15).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = ZuzuDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.05.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.1.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.15.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.2.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.15.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.3.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = ZuzuFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp,
  ),
)
