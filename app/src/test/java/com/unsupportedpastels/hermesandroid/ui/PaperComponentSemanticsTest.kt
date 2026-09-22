package com.unsupportedpastels.hermesandroid.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.app.ApprovalInteraction
import com.unsupportedpastels.hermesandroid.app.DurableSessionId
import com.unsupportedpastels.hermesandroid.app.RunEventState
import com.unsupportedpastels.hermesandroid.app.SessionSummary
import com.unsupportedpastels.hermesandroid.gateway.ChatMessage
import com.unsupportedpastels.hermesandroid.gateway.ChatMessageRole
import com.unsupportedpastels.hermesandroid.gateway.ChatSessionSnapshot
import com.unsupportedpastels.hermesandroid.gateway.RuntimeSessionId
import com.unsupportedpastels.hermesandroid.theme.HermesAndroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class PaperComponentSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paperCodeShowsTwelveLinesCopyTargetAndKeepsTheCopyDescription() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                MarkdownMessage(fencedCode(13))
            }
        }
        compose.onNodeWithContentDescription("Copy code")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        compose.onNodeWithText("kotlin").assertIsDisplayed()
        compose.onNodeWithText("line-12", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("line-13", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Show more").assertIsDisplayed().performClick()
        compose.onNodeWithText("line-13", substring = true).assertIsDisplayed()
    }

    @Test fun standardCodeKeepsTheFilledBlockAnd32DpCopyTarget() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Standard) {
                MarkdownMessage(fencedCode(13))
            }
        }
        compose.onNodeWithContentDescription("Copy code")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(32.dp)
            .assertHeightIsEqualTo(32.dp)
        compose.onNodeWithText("line-13", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("Show more").assertCountEquals(0)
    }

    @Test fun paperActivityRowIs48DpAndStaysCollapsed() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                TurnActivityGroup(sampleActivity(), expanded = false, onToggle = {}, sessionKey = "paper")
            }
        }
        compose.onNodeWithTag("Turn activity")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
        compose.onNodeWithContentDescription("Activity, 1 step, collapsed").assertIsDisplayed()
        compose.onNodeWithText("Activity · 1 step").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Show thinking").assertCountEquals(0)
    }

    @Test fun standardActivityRowStays32Dp() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Standard) {
                TurnActivityGroup(sampleActivity(), expanded = false, onToggle = {}, sessionKey = "standard")
            }
        }
        compose.onNodeWithTag("Turn activity").assertHeightIsEqualTo(32.dp)
    }

    @Test fun paperJumpControlIs48DpAndKeepsItsDescription() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                TranscriptJumpControl(paper = true, onClick = {})
            }
        }
        compose.onNodeWithContentDescription("Scroll to latest message")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test fun standardJumpControlStays44Dp() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Standard) {
                TranscriptJumpControl(paper = false, onClick = {})
            }
        }
        compose.onNodeWithContentDescription("Scroll to latest message")
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(44.dp)
    }

    @Test fun paperSessionKeepsBackSendStopAndApprovalWording() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                SessionDetailScreen(
                    session = SessionSummary(
                        id = DurableSessionId("paper-semantics"),
                        title = "Paper",
                        preview = "",
                    ),
                    chat = ChatSessionSnapshot(
                        messages = listOf(ChatMessage(ChatMessageRole.User, "Status check")),
                        runState = RunEventState(
                            approval = ApprovalInteraction(
                                runtimeSessionId = RuntimeSessionId("runtime"),
                                requestId = "approval-1",
                                commandPreview = "ls",
                                descriptionPreview = null,
                                choices = listOf("allow once", "deny"),
                            ),
                        ),
                    ),
                    voiceInputScopeKey = "paper",
                    draft = "",
                    onDraftChanged = {},
                    canSend = true,
                    attachments = emptyList(),
                    hostReferences = emptyList(),
                    onAddAttachments = { emptyList() },
                    onRemoveAttachment = {},
                    onRemoveHostReference = {},
                    onSend = {},
                    onSteer = {},
                    onReasoningSelected = {},
                    onFastSelected = {},
                    onOpenModelPicker = {},
                    onClarificationResponse = { _, _, _ -> },
                    onApprovalResponse = { _, _ -> },
                    onBlockingResponse = { _, _, _ -> },
                    showStop = false,
                    stopping = false,
                    onStop = {},
                    onLoadSessionInsights = {},
                    maintenanceAvailable = false,
                    maintenanceEnabled = false,
                    onCompressSession = {},
                    onUndoSession = {},
                    onBranchSession = { _, _ -> },
                    showBack = true,
                    onBack = {},
                    onLoadManagedImage = { Result.failure(IllegalStateException("unused")) },
                    onLoadHostFiles = { Result.failure(IllegalStateException("unused")) },
                    onLoadManagedFile = { Result.failure(IllegalStateException("unused")) },
                    onAttachHostReference = {},
                    readingProfile = ReadingProfile.Paper,
                )
            }
        }
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.onNodeWithContentDescription("Send message").assertIsDisplayed()
        compose.onNodeWithContentDescription("Approval pending").assertIsDisplayed()
        compose.onNodeWithText("allow once").assertIsDisplayed()
        compose.onNodeWithText("deny").assertIsDisplayed()
        compose.onAllNodesWithText("Approve").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Approve").assertCountEquals(0)
    }

    @Test fun paperStopKeepsItsDescription() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                SessionDetailScreen(
                    session = SessionSummary(
                        id = DurableSessionId("paper-stop"),
                        title = "Paper",
                        preview = "",
                    ),
                    chat = ChatSessionSnapshot(isSending = true),
                    voiceInputScopeKey = "paper-stop",
                    draft = "",
                    onDraftChanged = {},
                    canSend = true,
                    attachments = emptyList(),
                    hostReferences = emptyList(),
                    onAddAttachments = { emptyList() },
                    onRemoveAttachment = {},
                    onRemoveHostReference = {},
                    onSend = {},
                    onSteer = {},
                    onReasoningSelected = {},
                    onFastSelected = {},
                    onOpenModelPicker = {},
                    onClarificationResponse = { _, _, _ -> },
                    onApprovalResponse = { _, _ -> },
                    onBlockingResponse = { _, _, _ -> },
                    showStop = true,
                    stopping = false,
                    onStop = {},
                    onLoadSessionInsights = {},
                    maintenanceAvailable = false,
                    maintenanceEnabled = false,
                    onCompressSession = {},
                    onUndoSession = {},
                    onBranchSession = { _, _ -> },
                    showBack = true,
                    onBack = {},
                    onLoadManagedImage = { Result.failure(IllegalStateException("unused")) },
                    onLoadHostFiles = { Result.failure(IllegalStateException("unused")) },
                    onLoadManagedFile = { Result.failure(IllegalStateException("unused")) },
                    onAttachHostReference = {},
                    readingProfile = ReadingProfile.Paper,
                )
            }
        }
        compose.onNodeWithContentDescription("Stop Hermes response").assertIsDisplayed()
    }

    @Test fun paperStreamingTailIsNotALiveRegion() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                SessionDetailScreen(
                    session = SessionSummary(
                        id = DurableSessionId("paper-stream"),
                        title = "Paper",
                        preview = "",
                    ),
                    chat = ChatSessionSnapshot(
                        messages = listOf(
                            ChatMessage(
                                role = ChatMessageRole.Assistant,
                                text = "partial tail",
                                isStreaming = true,
                            ),
                        ),
                    ),
                    voiceInputScopeKey = "paper-stream",
                    draft = "",
                    onDraftChanged = {},
                    canSend = true,
                    attachments = emptyList(),
                    hostReferences = emptyList(),
                    onAddAttachments = { emptyList() },
                    onRemoveAttachment = {},
                    onRemoveHostReference = {},
                    onSend = {},
                    onSteer = {},
                    onReasoningSelected = {},
                    onFastSelected = {},
                    onOpenModelPicker = {},
                    onClarificationResponse = { _, _, _ -> },
                    onApprovalResponse = { _, _ -> },
                    onBlockingResponse = { _, _, _ -> },
                    showStop = false,
                    stopping = false,
                    onStop = {},
                    onLoadSessionInsights = {},
                    maintenanceAvailable = false,
                    maintenanceEnabled = false,
                    onCompressSession = {},
                    onUndoSession = {},
                    onBranchSession = { _, _ -> },
                    showBack = false,
                    onBack = {},
                    onLoadManagedImage = { Result.failure(IllegalStateException("unused")) },
                    onLoadHostFiles = { Result.failure(IllegalStateException("unused")) },
                    onLoadManagedFile = { Result.failure(IllegalStateException("unused")) },
                    onAttachHostReference = {},
                    readingProfile = ReadingProfile.Paper,
                )
            }
        }
        compose.onNodeWithTag("Streaming assistant text")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        compose.onNodeWithText("partial tail").assertIsDisplayed()
    }

    @Test fun paperStatusUsesWords() {
        compose.setContent {
            HermesAndroidTheme(profile = ReadingProfile.Paper) {
                WorkingIndicator()
            }
        }
        compose.onNodeWithText("Agent is working").assertIsDisplayed()
        compose.onNodeWithContentDescription("Agent is working").assertIsDisplayed()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
    }

    private fun fencedCode(lineCount: Int): String {
        val body = (1..lineCount).joinToString(separator = "\n") { "line-$it" }
        return "```kotlin\n$body\n```"
    }

    private fun sampleActivity(): FoldedEntry.TurnActivity = FoldedEntry.TurnActivity(
        steps = listOf(
            IndexedChatMessage(1, ChatMessage(ChatMessageRole.Tool, "read_file · notes")),
        ),
        answerReasoning = null,
        answerIndex = 0,
    )
}
