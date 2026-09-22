package com.unsupportedpastels.hermesandroid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.gateway.ConnectionState
import com.unsupportedpastels.hermesandroid.gateway.HermesGatewaySnapshot
import com.unsupportedpastels.hermesandroid.theme.HermesAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReadingSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingPaperReportsThePreference() {
        var selected = ReadingProfilePreference.Auto
        composeRule.setContent {
            HermesAndroidTheme {
                ReadingSettingsScreen(
                    preference = selected,
                    resolvedProfile = ReadingProfile.Standard,
                    showBack = true,
                    onBack = {},
                    onPreferenceChange = { selected = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reading Paper").performClick()
        composeRule.runOnIdle {
            assertEquals(ReadingProfilePreference.Paper, selected)
        }
    }

    @Test
    fun paperChoiceShowsTheSelectedProfileInverted() {
        composeRule.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                ReadingSettingsScreen(
                    preference = ReadingProfilePreference.Paper,
                    resolvedProfile = ReadingProfile.Paper,
                    showBack = false,
                    onBack = {},
                    onPreferenceChange = {},
                )
            }
        }

        composeRule.onNodeWithText("Using Paper").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reading Paper").assertIsSelected()
        composeRule.onNodeWithContentDescription("Reading Standard").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Reading Auto").assertIsNotSelected()
    }

    @Test
    fun unauthenticatedHubOpensReadingWithoutServerSections() {
        composeRule.setContent {
            HermesAndroidTheme {
                HermesApp(
                    snapshot = HermesGatewaySnapshot(connectionState = ConnectionState.Connected),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithContentDescription("Open Reading settings").performClick()
        composeRule.onNodeWithText("Using Standard").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reading Auto").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Default model settings").assertDoesNotExist()
    }
}
