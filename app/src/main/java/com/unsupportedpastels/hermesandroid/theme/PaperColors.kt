package com.unsupportedpastels.hermesandroid.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Opaque white. Paper never uses a partial alpha channel. */
internal val PaperWhite = Color(0xFFFFFFFF)

/** Opaque black. Paper never uses a gray tone. */
internal val PaperBlack = Color(0xFF000000)

internal val PaperDividerThickness: Dp = 1.dp
internal val PaperButtonOutline: Dp = 1.5.dp
internal val PaperFocusRing: Dp = 2.dp

private const val PaperBodyLineHeightRatio = 1.45f

/**
 * Every Material role is explicit so a default token cannot leak gray.
 * Containers and the elevation tint match [PaperWhite]. Text, outlines, and
 * error copy are [PaperBlack]. There is no alpha, brush, or gradient.
 */
internal val PaperColors = lightColorScheme(
    primary = PaperBlack,
    onPrimary = PaperWhite,
    primaryContainer = PaperWhite,
    onPrimaryContainer = PaperBlack,
    inversePrimary = PaperWhite,
    secondary = PaperBlack,
    onSecondary = PaperWhite,
    secondaryContainer = PaperWhite,
    onSecondaryContainer = PaperBlack,
    tertiary = PaperBlack,
    onTertiary = PaperWhite,
    tertiaryContainer = PaperWhite,
    onTertiaryContainer = PaperBlack,
    background = PaperWhite,
    onBackground = PaperBlack,
    surface = PaperWhite,
    onSurface = PaperBlack,
    surfaceVariant = PaperWhite,
    onSurfaceVariant = PaperBlack,
    surfaceTint = PaperWhite,
    inverseSurface = PaperBlack,
    inverseOnSurface = PaperWhite,
    error = PaperBlack,
    onError = PaperWhite,
    errorContainer = PaperWhite,
    onErrorContainer = PaperBlack,
    outline = PaperBlack,
    outlineVariant = PaperBlack,
    scrim = PaperBlack,
    surfaceBright = PaperWhite,
    surfaceDim = PaperWhite,
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = PaperWhite,
    surfaceContainer = PaperWhite,
    surfaceContainerHigh = PaperWhite,
    surfaceContainerHighest = PaperWhite,
    primaryFixed = PaperWhite,
    primaryFixedDim = PaperWhite,
    onPrimaryFixed = PaperBlack,
    onPrimaryFixedVariant = PaperBlack,
    secondaryFixed = PaperWhite,
    secondaryFixedDim = PaperWhite,
    onSecondaryFixed = PaperBlack,
    onSecondaryFixedVariant = PaperBlack,
    tertiaryFixed = PaperWhite,
    tertiaryFixedDim = PaperWhite,
    onTertiaryFixed = PaperBlack,
    onTertiaryFixedVariant = PaperBlack,
)

internal val PaperSemanticColors = HermesSemanticColors(
    active = PaperBlack,
    onActive = PaperWhite,
    completed = PaperBlack,
    onCompleted = PaperWhite,
)

internal val PaperTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = atLeastRegular(base.displayLarge),
        displayMedium = atLeastRegular(base.displayMedium),
        displaySmall = atLeastRegular(base.displaySmall),
        headlineLarge = atLeastRegular(base.headlineLarge),
        headlineMedium = atLeastRegular(base.headlineMedium),
        headlineSmall = atLeastRegular(base.headlineSmall),
        titleLarge = atLeastRegular(base.titleLarge),
        titleMedium = atLeastRegular(base.titleMedium),
        titleSmall = atLeastRegular(base.titleSmall),
        bodyLarge = paperBody(base.bodyLarge),
        bodyMedium = paperBody(base.bodyMedium),
        bodySmall = paperBody(base.bodySmall),
        labelLarge = atLeastRegular(base.labelLarge),
        labelMedium = atLeastRegular(base.labelMedium),
        labelSmall = atLeastRegular(base.labelSmall),
    )
}

/** Roles that must stay white so Material containers cannot paint gray. */
internal fun paperContainerRoles(): List<Color> = listOf(
    PaperColors.background,
    PaperColors.surface,
    PaperColors.surfaceVariant,
    PaperColors.surfaceTint,
    PaperColors.surfaceBright,
    PaperColors.surfaceDim,
    PaperColors.surfaceContainer,
    PaperColors.surfaceContainerHigh,
    PaperColors.surfaceContainerHighest,
    PaperColors.surfaceContainerLow,
    PaperColors.surfaceContainerLowest,
    PaperColors.primaryContainer,
    PaperColors.secondaryContainer,
    PaperColors.tertiaryContainer,
    PaperColors.errorContainer,
    PaperColors.primaryFixed,
    PaperColors.primaryFixedDim,
    PaperColors.secondaryFixed,
    PaperColors.secondaryFixedDim,
    PaperColors.tertiaryFixed,
    PaperColors.tertiaryFixedDim,
)

/** Every scheme color, so a new default role fails the opaque black/white check. */
internal fun paperSchemeRoles(): List<Color> = listOf(
    PaperColors.primary,
    PaperColors.onPrimary,
    PaperColors.primaryContainer,
    PaperColors.onPrimaryContainer,
    PaperColors.inversePrimary,
    PaperColors.secondary,
    PaperColors.onSecondary,
    PaperColors.secondaryContainer,
    PaperColors.onSecondaryContainer,
    PaperColors.tertiary,
    PaperColors.onTertiary,
    PaperColors.tertiaryContainer,
    PaperColors.onTertiaryContainer,
    PaperColors.background,
    PaperColors.onBackground,
    PaperColors.surface,
    PaperColors.onSurface,
    PaperColors.surfaceVariant,
    PaperColors.onSurfaceVariant,
    PaperColors.surfaceTint,
    PaperColors.inverseSurface,
    PaperColors.inverseOnSurface,
    PaperColors.error,
    PaperColors.onError,
    PaperColors.errorContainer,
    PaperColors.onErrorContainer,
    PaperColors.outline,
    PaperColors.outlineVariant,
    PaperColors.scrim,
    PaperColors.surfaceBright,
    PaperColors.surfaceDim,
    PaperColors.surfaceContainer,
    PaperColors.surfaceContainerHigh,
    PaperColors.surfaceContainerHighest,
    PaperColors.surfaceContainerLow,
    PaperColors.surfaceContainerLowest,
    PaperColors.primaryFixed,
    PaperColors.primaryFixedDim,
    PaperColors.onPrimaryFixed,
    PaperColors.onPrimaryFixedVariant,
    PaperColors.secondaryFixed,
    PaperColors.secondaryFixedDim,
    PaperColors.onSecondaryFixed,
    PaperColors.onSecondaryFixedVariant,
    PaperColors.tertiaryFixed,
    PaperColors.tertiaryFixedDim,
    PaperColors.onTertiaryFixed,
    PaperColors.onTertiaryFixedVariant,
)

internal data class PaperButtonChrome(
    val container: Color,
    val label: Color,
    val outline: Color,
    val outlineWidth: Dp,
)

/**
 * Outlined 1.5 dp at rest, 2 dp while focused. Pressed or selected swaps to
 * black fill and white label. Disabled keeps the black outline and black label
 * on white — no gray fill and no alpha.
 */
internal fun paperButtonChrome(
    enabled: Boolean,
    pressed: Boolean,
    focused: Boolean,
    selected: Boolean = false,
): PaperButtonChrome {
    val inverted = selected || (enabled && pressed)
    return PaperButtonChrome(
        container = if (inverted) PaperBlack else PaperWhite,
        label = if (inverted) PaperWhite else PaperBlack,
        outline = if (inverted && focused) PaperWhite else PaperBlack,
        outlineWidth = if (focused) PaperFocusRing else PaperButtonOutline,
    )
}

private fun atLeastRegular(style: TextStyle): TextStyle {
    val weight = style.fontWeight ?: FontWeight.Normal
    return if (weight < FontWeight.Normal) style.copy(fontWeight = FontWeight.Normal) else style
}

private fun paperBody(style: TextStyle): TextStyle {
    val regular = atLeastRegular(style)
    return regular.copy(lineHeight = regular.fontSize * PaperBodyLineHeightRatio)
}
