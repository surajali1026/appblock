package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FocusGuardColorScheme = darkColorScheme(
    primary = LuminousEmerald,
    onPrimary = ObsidianVoid,
    primaryContainer = DeepEmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = ElectricCyan,
    onSecondary = ObsidianVoid,
    secondaryContainer = SlateElevated,
    onSecondaryContainer = PureSnow,
    tertiary = AmberPulse,
    onTertiary = ObsidianVoid,
    tertiaryContainer = DeepAmberContainer,
    onTertiaryContainer = AmberOnContainer,
    error = CrimsonGuard,
    onError = PureSnow,
    errorContainer = DeepCrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = ObsidianVoid,
    onBackground = PureSnow,
    surface = MidnightNavy,
    onSurface = PureSnow,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = MidnightNavy,
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = SlateElevated,
    surfaceContainerHighest = SlateOutline,
    outline = SlateOutline,
    outlineVariant = SlateElevated
)

private val FocusGuardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FocusGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocusGuardColorScheme,
        typography = FocusGuardTypography,
        shapes = FocusGuardShapes,
        content = content
    )
}
