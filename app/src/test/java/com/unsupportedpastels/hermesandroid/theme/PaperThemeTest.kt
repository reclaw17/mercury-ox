package com.unsupportedpastels.hermesandroid.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.ui.ReadingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

class StandardPaletteLockTest {
    @Test
    fun lightSchemeKeepsCanonicalHexes() {
        assertEquals(Color(0xFF1B6969), LightColors.primary)
        assertEquals(Color(0xFFE0FFFE), LightColors.onPrimary)
        assertEquals(Color(0xFFA8EFEE), LightColors.primaryContainer)
        assertEquals(Color(0xFF005C5C), LightColors.onPrimaryContainer)
        assertEquals(Color(0xFF4A6463), LightColors.secondary)
        assertEquals(Color.White, LightColors.onSecondary)
        assertEquals(Color(0xFFCCE8E7), LightColors.secondaryContainer)
        assertEquals(Color(0xFF3D5656), LightColors.onSecondaryContainer)
        assertEquals(Color(0xFF765A00), LightColors.tertiary)
        assertEquals(Color.White, LightColors.onTertiary)
        assertEquals(Color(0xFFFFDF92), LightColors.tertiaryContainer)
        assertEquals(Color(0xFF261A00), LightColors.onTertiaryContainer)
        assertEquals(Color(0xFFFAFCFB), LightColors.background)
        assertEquals(Color(0xFF191C1B), LightColors.onBackground)
        assertEquals(Color(0xFFFAFCFB), LightColors.surface)
        assertEquals(Color(0xFF191C1B), LightColors.onSurface)
        assertEquals(Color(0xFFDBE5E2), LightColors.surfaceVariant)
        assertEquals(Color(0xFF3F4947), LightColors.onSurfaceVariant)
        assertEquals(Color(0xFFDAE0DD), LightColors.surfaceDim)
        assertEquals(Color(0xFFFAFCFB), LightColors.surfaceBright)
        assertEquals(Color.White, LightColors.surfaceContainerLowest)
        assertEquals(Color(0xFFF4F7F5), LightColors.surfaceContainerLow)
        assertEquals(Color(0xFFECF2EF), LightColors.surfaceContainer)
        assertEquals(Color(0xFFE6ECE9), LightColors.surfaceContainerHigh)
        assertEquals(Color(0xFFE0E5E2), LightColors.surfaceContainerHighest)
        assertEquals(Color(0xFFBA1A1A), LightColors.error)
        assertEquals(Color.White, LightColors.onError)
        assertEquals(Color(0xFFFFDAD6), LightColors.errorContainer)
        assertEquals(Color(0xFF410002), LightColors.onErrorContainer)
        assertEquals(Color(0xFF6F7977), LightColors.outline)
        assertEquals(Color(0xFFBEC9C6), LightColors.outlineVariant)
        assertEquals(Color(0xFF2D3130), LightColors.inverseSurface)
        assertEquals(Color(0xFFEFF1EF), LightColors.inverseOnSurface)
        assertEquals(Color(0xFF9BD0CF), LightColors.inversePrimary)
        assertEquals(Color.Black, LightColors.scrim)
    }

    @Test
    fun darkSchemeKeepsCanonicalHexes() {
        assertEquals(Color(0xFF9BD0CF), DarkColors.primary)
        assertEquals(Color(0xFF0C4848), DarkColors.onPrimary)
        assertEquals(Color(0xFF255A5A), DarkColors.primaryContainer)
        assertEquals(Color(0xFFB7EDEC), DarkColors.onPrimaryContainer)
        assertEquals(Color(0xFFC5C5C5), DarkColors.secondary)
        assertEquals(Color(0xFF1A1A1A), DarkColors.onSecondary)
        assertEquals(Color(0xFF27403F), DarkColors.secondaryContainer)
        assertEquals(Color(0xFFA9C5C4), DarkColors.onSecondaryContainer)
        assertEquals(Color(0xFFF2C64D), DarkColors.tertiary)
        assertEquals(Color(0xFF3F2E00), DarkColors.onTertiary)
        assertEquals(Color(0xFF5B4400), DarkColors.tertiaryContainer)
        assertEquals(Color(0xFFFFDF92), DarkColors.onTertiaryContainer)
        assertEquals(Color(0xFF000000), DarkColors.background)
        assertEquals(Color(0xFFF4F4F4), DarkColors.onBackground)
        assertEquals(Color(0xFF000000), DarkColors.surface)
        assertEquals(Color(0xFFF4F4F4), DarkColors.onSurface)
        assertEquals(Color(0xFF2E2E2E), DarkColors.surfaceVariant)
        assertEquals(Color(0xFFC5C5C5), DarkColors.onSurfaceVariant)
        assertEquals(Color(0xFF000000), DarkColors.surfaceDim)
        assertEquals(Color(0xFF262626), DarkColors.surfaceBright)
        assertEquals(Color(0xFF0A0A0A), DarkColors.surfaceContainerLowest)
        assertEquals(Color(0xFF141414), DarkColors.surfaceContainerLow)
        assertEquals(Color(0xFF1C1C1C), DarkColors.surfaceContainer)
        assertEquals(Color(0xFF232323), DarkColors.surfaceContainerHigh)
        assertEquals(Color(0xFF2B2B2B), DarkColors.surfaceContainerHighest)
        assertEquals(Color(0xFFFFB4AB), DarkColors.error)
        assertEquals(Color(0xFF690005), DarkColors.onError)
        assertEquals(Color(0xFF93000A), DarkColors.errorContainer)
        assertEquals(Color(0xFFFFDAD6), DarkColors.onErrorContainer)
        assertEquals(Color(0xFF8A8A8A), DarkColors.outline)
        assertEquals(Color(0xFF3A3A3A), DarkColors.outlineVariant)
        assertEquals(Color(0xFFE8E8E8), DarkColors.inverseSurface)
        assertEquals(Color(0xFF1C1C1C), DarkColors.inverseOnSurface)
        assertEquals(Color(0xFF336767), DarkColors.inversePrimary)
        assertEquals(Color.Black, DarkColors.scrim)
    }

    @Test
    fun semanticColorsKeepCanonicalHexes() {
        assertEquals(Color(0xFFC68A16), LightSemanticColors.active)
        assertEquals(Color(0xFF241A00), LightSemanticColors.onActive)
        assertEquals(Color(0xFF2D6A43), LightSemanticColors.completed)
        assertEquals(Color.White, LightSemanticColors.onCompleted)
        assertEquals(Color(0xFFF2C64D), DarkSemanticColors.active)
        assertEquals(Color(0xFF241A00), DarkSemanticColors.onActive)
        assertEquals(Color(0xFF8ED6A5), DarkSemanticColors.completed)
        assertEquals(Color(0xFF0C3A1E), DarkSemanticColors.onCompleted)
    }

    @Test
    fun paperSchemeIsOnlyOpaqueBlackOrWhite() {
        val roles = paperSchemeRoles()
        assertEquals(48, roles.size)
        roles.forEach { color ->
            assertEquals(1f, color.alpha)
            assertTrue(color == PaperBlack || color == PaperWhite)
        }
    }

    @Test
    fun paperContainersMatchTheWhiteBackground() {
        paperContainerRoles().forEach { color ->
            assertEquals(PaperColors.background, color)
            assertEquals(PaperWhite, color)
        }
        assertEquals(PaperBlack, PaperColors.onSurface)
        assertEquals(PaperBlack, PaperColors.onBackground)
        assertEquals(PaperBlack, PaperColors.error)
        assertEquals(PaperBlack, PaperColors.outline)
        assertEquals(PaperBlack, PaperColors.outlineVariant)
        assertEquals(PaperWhite, PaperColors.surfaceTint)
    }

    @Test
    fun paperSemanticColorsAreOpaqueBlackOrWhite() {
        listOf(
            PaperSemanticColors.active,
            PaperSemanticColors.onActive,
            PaperSemanticColors.completed,
            PaperSemanticColors.onCompleted,
        ).forEach { color ->
            assertEquals(1f, color.alpha)
            assertTrue(color == PaperBlack || color == PaperWhite)
        }
    }

    @Test
    fun paperBodyIsRegularOrHeavierWithLineHeightAtLeastOnePointFour() {
        listOf(
            PaperTypography.bodyLarge,
            PaperTypography.bodyMedium,
            PaperTypography.bodySmall,
        ).forEach { style ->
            val weight = style.fontWeight ?: FontWeight.Normal
            assertTrue(weight >= FontWeight.Normal)
            assertTrue(style.lineHeight.value / style.fontSize.value >= 1.4f)
        }
    }

    @Test
    fun paperButtonChromeInvertsOnPressAndKeepsOutlineWhenDisabled() {
        val pressed = paperButtonChrome(enabled = true, pressed = true, focused = false)
        assertEquals(PaperBlack, pressed.container)
        assertEquals(PaperWhite, pressed.label)
        assertEquals(PaperButtonOutline, pressed.outlineWidth)
        assertEquals(1.5.dp, pressed.outlineWidth)

        val focused = paperButtonChrome(enabled = true, pressed = false, focused = true)
        assertEquals(PaperWhite, focused.container)
        assertEquals(PaperBlack, focused.label)
        assertEquals(PaperFocusRing, focused.outlineWidth)
        assertEquals(2.dp, focused.outlineWidth)

        val disabled = paperButtonChrome(enabled = false, pressed = true, focused = false)
        assertEquals(PaperWhite, disabled.container)
        assertEquals(PaperBlack, disabled.label)
        assertEquals(PaperBlack, disabled.outline)
        assertEquals(1f, disabled.container.alpha)
        assertEquals(1f, disabled.label.alpha)
        assertEquals(1f, disabled.outline.alpha)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HermesAndroidThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun standardThemeUsesTheLockedSchemesAndKeepsDefaultIndication() {
        composeRule.setContent {
            HermesAndroidTheme(darkTheme = false) {
                assertEquals(LightColors.primary, MaterialTheme.colorScheme.primary)
                assertEquals(LightColors.background, MaterialTheme.colorScheme.background)
                assertEquals(LightColors.surfaceContainer, MaterialTheme.colorScheme.surfaceContainer)
                assertEquals(LightSemanticColors, LocalHermesSemanticColors.current)
                assertEquals(ReadingProfile.Standard, LocalReadingProfile.current)
                assertNotEquals(PaperFocusIndication, LocalIndication.current)
                assertNotEquals(null, LocalRippleConfiguration.current)
                AssertStandardTypography()
            }
            HermesAndroidTheme(darkTheme = true, profile = ReadingProfile.Standard) {
                assertEquals(DarkColors.primary, MaterialTheme.colorScheme.primary)
                assertEquals(DarkColors.background, MaterialTheme.colorScheme.background)
                assertEquals(DarkSemanticColors, LocalHermesSemanticColors.current)
            }
        }
    }

    @Test
    fun paperThemeIgnoresDarkThemeAndDisablesRipple() {
        composeRule.setContent {
            HermesAndroidTheme(darkTheme = true, profile = ReadingProfile.Paper) {
                assertEquals(PaperColors.background, MaterialTheme.colorScheme.background)
                assertEquals(PaperWhite, MaterialTheme.colorScheme.background)
                assertEquals(PaperBlack, MaterialTheme.colorScheme.onSurface)
                assertEquals(PaperSemanticColors, LocalHermesSemanticColors.current)
                assertEquals(ReadingProfile.Paper, LocalReadingProfile.current)
                assertEquals(PaperFocusIndication, LocalIndication.current)
                assertEquals(null, LocalRippleConfiguration.current)
                val body = MaterialTheme.typography.bodySmall
                val weight = body.fontWeight ?: FontWeight.Normal
                assertTrue(weight >= FontWeight.Normal)
                assertTrue(body.lineHeight.value / body.fontSize.value >= 1.4f)
            }
        }
    }
}

@Composable
private fun AssertStandardTypography() {
    val defaults = Typography()
    assertEquals(defaults.bodyLarge, MaterialTheme.typography.bodyLarge)
    assertEquals(defaults.bodyMedium, MaterialTheme.typography.bodyMedium)
    assertEquals(defaults.bodySmall, MaterialTheme.typography.bodySmall)
}
