package com.unsupportedpastels.hermesandroid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.theme.HermesAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ManagedVideoBlockTest {
    @get:Rule val rule = createComposeRule()

    @Test fun unavailableTransportDoesNotOfferPlayback() {
        rule.setContent { MaterialTheme { ManagedVideoBlock("/a/clip.mp4") } }
        rule.onAllNodesWithText("Video playback is unavailable").assertCountEquals(1)
        rule.onAllNodesWithContentDescription("Play video").assertCountEquals(0)
    }

    @Test fun downloadIsExplicitAndErrorsAreSafe() {
        var requested: String? = null
        rule.setContent {
            MaterialTheme {
                MarkdownMessage("MEDIA:/a/clip.mp4", loadManagedVideo = { path ->
                    requested = path
                    Result.failure(IllegalStateException("private transport detail"))
                })
            }
        }
        rule.runOnIdle { assertEquals(null, requested) }
        rule.onNodeWithContentDescription("Play video").performClick()
        rule.runOnIdle { assertEquals("/a/clip.mp4", requested) }
        rule.onAllNodesWithText("Could not load video").assertCountEquals(1)
        rule.onAllNodesWithText("private transport detail").assertCountEquals(0)
    }

    @Test fun standardLoadingKeepsScrimAndSpinnerUntilDownloadFinishes() {
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        try {
            rule.setContent {
                MaterialTheme {
                    ManagedVideoBlock(
                        source = "/a/clip.mp4",
                        onLoadManagedVideo = {
                            release.await()
                            Result.failure(IllegalStateException("stop"))
                        },
                    )
                }
            }
            rule.onNodeWithContentDescription("Play video").performClick()
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onAllNodesWithText("Loading video…").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("video-loading-scrim", useUnmergedTree = true).assertExists()
            rule.onAllNodes(progressIndicator()).assertCountEquals(1)
            rule.onAllNodesWithContentDescription("Open fullscreen video").assertCountEquals(0)
        } finally {
            release.complete(Unit)
        }
    }

    @Test fun paperLoadingStaysOnPosterTextWithoutScrimOrSpinner() {
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        try {
            rule.setContent {
                HermesAndroidTheme(profile = ReadingProfile.Paper) {
                    ManagedVideoBlock(
                        source = "/a/clip.mp4",
                        onLoadManagedVideo = {
                            release.await()
                            Result.failure(IllegalStateException("stop"))
                        },
                    )
                }
            }
            rule.onNodeWithContentDescription("Play video").performClick()
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onAllNodesWithText("Loading video…").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Loading video…").assertExists()
            rule.onNodeWithTag("video-loading-scrim", useUnmergedTree = true).assertDoesNotExist()
            rule.onAllNodes(progressIndicator()).assertCountEquals(0)
            rule.onAllNodesWithContentDescription("Open fullscreen video").assertCountEquals(0)
        } finally {
            release.complete(Unit)
        }
    }

    private fun progressIndicator() = SemanticsMatcher("progress indicator") { node ->
        node.config.contains(SemanticsProperties.ProgressBarRangeInfo)
    }
}

class ManagedVideoPlaybackGateTest {
    @Test fun playWhenReadyWaitsForThePlayPressDownload() {
        assertFalse(managedVideoPlayWhenReady(playPressed = false, downloadCompleted = false))
        assertFalse(managedVideoPlayWhenReady(playPressed = true, downloadCompleted = false))
        assertFalse(managedVideoPlayWhenReady(playPressed = false, downloadCompleted = true))
        assertTrue(managedVideoPlayWhenReady(playPressed = true, downloadCompleted = true))
    }
}
