package com.unsupportedpastels.hermesandroid.ui

import com.unsupportedpastels.hermesandroid.gateway.ChatMessage
import com.unsupportedpastels.hermesandroid.gateway.ChatMessageRole
import com.unsupportedpastels.hermesandroid.gateway.ChatSessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptUiCadenceTest {
    @Test
    fun burstOfOneHundredUpdatesPublishesAtMostEightAndKeepsTheFullString() {
        val full = "a".repeat(100)
        val published = mutableListOf<TranscriptUiCadenceState>()
        var state = freshTranscriptUiCadenceState()
        for (index in 0 until 100) {
            val text = full.substring(0, index + 1)
            val streaming = index < 99
            val next = advanceTranscriptUiCadence(
                state = state,
                messages = listOf(assistant(text, streaming)),
                nowMillis = index.toLong(),
                limited = true,
            )
            if (next.publishedMessages !== state.publishedMessages) published += next
            state = next
        }

        assertTrue(
            "100 updates in 100 ms published ${published.size} snapshots",
            published.size <= TranscriptUiMaxPublicationsPerSecond,
        )
        assertEquals(full, published.last().publishedMessages.single().text)
        assertEquals(full, state.rows.single().message.text)
        assertEquals(false, state.rows.single().message.isStreaming)
        assertEquals(listOf(transcriptUiRowKey(0)), published.map { it.rows.single().key }.distinct())
        assertEquals(
            foldedKeys(published.first().publishedMessages),
            foldedKeys(published.last().publishedMessages),
        )
    }

    @Test
    fun standardCadencePublishesEveryUpdate() {
        var state = freshTranscriptUiCadenceState()
        var publications = 0
        for (index in 0 until 100) {
            val next = advanceTranscriptUiCadence(
                state = state,
                messages = listOf(assistant("a".repeat(index + 1), streaming = true)),
                nowMillis = index.toLong(),
                limited = false,
            )
            if (next.publishedMessages !== state.publishedMessages) publications += 1
            state = next
        }
        assertEquals(100, publications)
        assertEquals("a".repeat(100), state.publishedMessages.single().text)
    }

    @Test
    fun paragraphBoundaryPublishesImmediatelyAndReusesClosedSegments() {
        var state = freshTranscriptUiCadenceState()
        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("First paragraph.", streaming = true)),
            nowMillis = 0L,
            limited = true,
        )
        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("First paragraph.\n\n", streaming = true)),
            nowMillis = 10L,
            limited = true,
        )
        val closed = state.rows.single()
        assertEquals("First paragraph.\n\n", closed.message.text)
        assertEquals(listOf("First paragraph.\n\n"), closed.stablePrefixSegments)
        assertEquals("", closed.streamingTail)

        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("First paragraph.\n\nSecond paragraph.\n\n", streaming = true)),
            nowMillis = 20L,
            limited = true,
        )
        val extended = state.rows.single()
        assertSame(closed.stablePrefixSegments.single(), extended.stablePrefixSegments.first())
        assertEquals(
            listOf("First paragraph.\n\n", "Second paragraph.\n\n"),
            extended.stablePrefixSegments,
        )
        assertEquals("First paragraph.\n\nSecond paragraph.\n\n", extended.message.text)

        val beforeTail = extended
        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("First paragraph.\n\nSecond paragraph.\n\nTail", streaming = true)),
            nowMillis = 30L,
            limited = true,
        )
        assertSame(beforeTail, state.rows.single())
    }

    @Test
    fun blankLineInsideAnOpenCodeFenceStaysOnThePreviousSnapshot() {
        var state = freshTranscriptUiCadenceState()
        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("```python\nprint('partial')", streaming = true)),
            nowMillis = 0L,
            limited = true,
        )
        val published = state.publishedMessages
        state = advanceTranscriptUiCadence(
            state,
            listOf(assistant("```python\nprint('partial')\n\n**unclosed", streaming = true)),
            nowMillis = 15L,
            limited = true,
        )
        assertSame(published, state.publishedMessages)
        assertEquals(0, stableMarkdownPrefixLength(state.pendingMessages!!.single().text))
    }

    @Test
    fun heldStreamingTailPublishesTheExactSourceTextWhenTheIntervalElapses() {
        var state = freshTranscriptUiCadenceState()
        val first = assistant("Hello", streaming = true)
        state = advanceTranscriptUiCadence(state, listOf(first), nowMillis = 0L, limited = true)
        val grown = assistant("Hello, world", streaming = true)
        val held = advanceTranscriptUiCadence(state, listOf(grown), nowMillis = 40L, limited = true)
        assertSame(state.publishedMessages, held.publishedMessages)
        assertEquals(listOf(grown), held.pendingMessages)

        val flushed = advanceTranscriptUiCadence(
            held,
            listOf(grown),
            nowMillis = TranscriptUiMinIntervalMillis,
            limited = true,
        )
        assertSame(grown, flushed.publishedMessages.single())
        assertEquals("Hello, world", flushed.rows.single().message.text)
        assertEquals("Hello, world", flushed.rows.single().streamingTail)
        assertSame(state.rows.single().stablePrefix, flushed.rows.single().stablePrefix)
    }

    @Test
    fun foldedEntryKeysStayStableAcrossPublishes() {
        val user = ChatMessage(ChatMessageRole.User, "Question")
        var state = freshTranscriptUiCadenceState()
        state = advanceTranscriptUiCadence(
            state,
            listOf(user, assistant("Partial", streaming = true)),
            nowMillis = 0L,
            limited = true,
        )
        val firstKeys = foldedKeys(state.publishedMessages)
        state = advanceTranscriptUiCadence(
            state,
            listOf(user, assistant("Partial answer", streaming = true)),
            nowMillis = 50L,
            limited = true,
        )
        val completed = advanceTranscriptUiCadence(
            state,
            listOf(user, assistant("Partial answer done", streaming = false)),
            nowMillis = 60L,
            limited = true,
        )
        assertEquals(firstKeys, foldedKeys(state.publishedMessages))
        assertEquals(firstKeys, foldedKeys(completed.publishedMessages))
        assertEquals("Partial answer done", completed.publishedMessages.last().text)
        assertEquals(transcriptUiRowKey(0), completed.rows[0].key)
        assertEquals(transcriptUiRowKey(1), completed.rows[1].key)
    }

    private fun assistant(text: String, streaming: Boolean) =
        ChatMessage(ChatMessageRole.Assistant, text, isStreaming = streaming)

    private fun foldedKeys(messages: List<ChatMessage>): List<String> {
        val chat = ChatSessionSnapshot(messages = messages)
        return foldTranscriptTurns(messages, turnActive = true).map { foldedEntryKey(it, chat) }
    }
}
