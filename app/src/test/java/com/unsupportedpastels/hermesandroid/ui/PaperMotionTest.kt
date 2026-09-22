package com.unsupportedpastels.hermesandroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.theme.HermesAndroidTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp")
class PaperMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paperDockWidthSnapsWhenCollapsed() {
        compose.mainClock.autoAdvance = false
        var state by mutableStateOf(ProjectDockState.Expanded)
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                dock(state)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { state = ProjectDockState.Collapsed }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Project dock, collapsed").assertWidthIsEqualTo(76.dp)
    }

    @Test fun standardDockWidthDoesNotSnapInOneFrame() {
        compose.mainClock.autoAdvance = false
        var state by mutableStateOf(ProjectDockState.Expanded)
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Standard) {
                dock(state)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val expanded = compose.onNodeWithContentDescription("Project dock, expanded")
            .fetchSemanticsNode().boundsInRoot.width
        compose.runOnIdle { state = ProjectDockState.Collapsed }
        compose.mainClock.advanceTimeByFrame()
        val collapsed = compose.onNodeWithContentDescription("Project dock, collapsed")
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            "Standard dock must keep animating its width",
            collapsed > expanded * 0.5f,
        )
    }

    @Test fun paperWorkingIndicatorIsStaticTextMark() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                WorkingIndicator()
            }
        }
        compose.onNodeWithContentDescription("Agent is working").assertIsDisplayed()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
    }

    @Test fun standardWorkingIndicatorKeepsTheSpinner() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Standard) {
                WorkingIndicator()
            }
        }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Agent is working").assertIsDisplayed()
    }

    @Composable
    private fun dock(state: ProjectDockState) {
        ProjectDock(
            state = state,
            projects = emptyList(),
            selectedProjectId = null,
            projectIcons = emptyMap(),
            canStartNewTask = true,
            settingsSelected = false,
            onProjectSelected = {},
            onChooseProjectIcon = {},
            onCreateProject = {},
            onNewTask = {},
            onSettings = {},
            onExpand = {},
            onCollapse = {},
            onHide = {},
        )
    }
}
