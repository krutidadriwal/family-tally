package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches the Gateway of Tally home screen: blue header, light-blue page,
// white cards, dark-blue text and gold accents.
private val TallyColorScheme = lightColorScheme(
  primary = TallyBlueHeader,
  onPrimary = Color.White,
  primaryContainer = TallyBlueContainer,
  onPrimaryContainer = TallyTextBlue,
  secondary = TallyGold,
  onSecondary = Color.Black,
  secondaryContainer = TallyGoldContainer,
  onSecondaryContainer = TallyOnGoldContainer,
  tertiary = TallyTextBlue,
  onTertiary = Color.White,
  background = TallyBackground,
  onBackground = TallyTextBlue,
  surface = Color.White,
  onSurface = TallyTextDark,
  surfaceVariant = TallySurfaceVariant,
  onSurfaceVariant = TallySubHeader,
  surfaceContainer = Color.White,
  surfaceContainerLow = Color.White,
  surfaceContainerHigh = Color.White,
  surfaceContainerHighest = TallySurfaceVariant,
  outline = TallyBorder,
  outlineVariant = Color(0xFFE2E8F0)
)

/**
 * The home screen is drawn in fixed light colours, so the app always uses the
 * same light blue scheme (also in system dark mode) to keep every screen consistent.
 */
@Composable
fun FamilyTallyTheme(
  @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TallyColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  FamilyTallyTheme(darkTheme = darkTheme, content = content)
}
