package com.unsupportedpastels.hermesandroid.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.unsupportedpastels.hermesandroid.gateway.ChatMessage
import com.unsupportedpastels.hermesandroid.gateway.ChatMessageRole
import kotlinx.coroutines.delay

/** Paper transcript frames. Eight publishes per second, paragraph boundaries included. */
internal const val TranscriptUiMaxPublicationsPerSecond = 8

/** Minimum gap between ordinary Paper publishes. Paragraph and completion flushes skip this. */
internal const val TranscriptUiMinIntervalMillis: Long = 1_000L / TranscriptUiMaxPublicationsPerSecond

/**
 * One transcript row as the session list should render it.
 *
 * [stablePrefixSegments] are closed markdown regions. A later paragraph appends a
 * segment and reuses the earlier strings, so those blocks are not rebuilt.
 * [message] is the source [ChatMessage] for this publish — its [ChatMessage.text]
 * is never rewritten.
 */
internal data class TranscriptUiRow(
    val key: String,
    val message: ChatMessage,
    val stablePrefixSegments: List<String>,
    val stablePrefix: String,
    val streamingTail: String,
) {
    init {
        if (message.role == ChatMessageRole.Assistant && message.isStreaming) {
            check(stablePrefixSegments.joinToString(separator = "") == stablePrefix) {
                "Stable prefix segments must concatenate to the stable prefix"
            }
            check(stablePrefix + streamingTail == message.text) {
                "Streaming projection must cover the source message text"
            }
        }
    }
}

/**
 * Last published transcript projection.
 *
 * [publishedMessages] keeps the source list reference from the publish that won.
 * [pendingMessages] is the newer source list held back by the rate limit.
 */
internal data class TranscriptUiCadenceState(
    val rows: List<TranscriptUiRow>,
    val publishedMessages: List<ChatMessage>,
    val lastPublishAtMillis: Long,
    val pendingMessages: List<ChatMessage>?,
    val nextPublishAtMillis: Long?,
)

internal fun freshTranscriptUiCadenceState(): TranscriptUiCadenceState =
    TranscriptUiCadenceState(
        rows = emptyList(),
        publishedMessages = emptyList(),
        lastPublishAtMillis = Long.MIN_VALUE / 2,
        pendingMessages = null,
        nextPublishAtMillis = null,
    )

/** Index key for a projected row. LazyColumn identity stays [foldedEntryKey]. */
internal fun transcriptUiRowKey(index: Int): String = "index:$index"

/**
 * Projects [messages] into the next transcript UI snapshot.
 *
 * [limited] is the Paper gate. When it is false every distinct source list is
 * published immediately, which is the Standard timing. When it is true, ordinary
 * streaming tail growth waits [TranscriptUiMinIntervalMillis]. A longer stable
 * markdown prefix (blank line outside a fence) and `isStreaming` becoming false
 * publish on that call. The published [ChatMessage.text] is always a source
 * message, and the final non-streaming message is that source unchanged.
 */
internal fun advanceTranscriptUiCadence(
    state: TranscriptUiCadenceState,
    messages: List<ChatMessage>,
    nowMillis: Long,
    limited: Boolean,
    stablePrefixLength: (String) -> Int = ::stableMarkdownPrefixLength,
): TranscriptUiCadenceState {
    if (!limited) {
        if (sameMessages(state.publishedMessages, messages) && state.pendingMessages == null) {
            return state
        }
        return publishTranscriptUi(state, messages, nowMillis, stablePrefixLength)
    }
    if (sameMessages(state.publishedMessages, messages)) {
        return if (state.pendingMessages == null) {
            state
        } else {
            state.copy(pendingMessages = null, nextPublishAtMillis = null)
        }
    }
    val force = shouldForceTranscriptPublish(state.rows, messages, stablePrefixLength)
    val intervalElapsed = nowMillis - state.lastPublishAtMillis >= TranscriptUiMinIntervalMillis
    if (!force && !intervalElapsed) {
        val due = state.lastPublishAtMillis + TranscriptUiMinIntervalMillis
        if (state.pendingMessages != null &&
            sameMessages(state.pendingMessages, messages) &&
            state.nextPublishAtMillis == due
        ) {
            return state
        }
        return state.copy(pendingMessages = messages, nextPublishAtMillis = due)
    }
    return publishTranscriptUi(state, messages, nowMillis, stablePrefixLength)
}

private fun shouldForceTranscriptPublish(
    rows: List<TranscriptUiRow>,
    messages: List<ChatMessage>,
    stablePrefixLength: (String) -> Int,
): Boolean {
    if (messages.size != rows.size) return true
    for (index in messages.indices) {
        val previous = rows[index].message
        val message = messages[index]
        if (previous.role != message.role) return true
        if (previous.isStreaming != message.isStreaming) return true
        if (!message.isStreaming && previous != message) return true
        if (message.role == ChatMessageRole.Assistant && message.isStreaming) {
            if (previous.text.isEmpty() && message.text.isNotEmpty()) return true
            val stableLen = stablePrefixLength(message.text)
            if (stableLen != rows[index].stablePrefix.length) return true
        }
    }
    return false
}

private fun publishTranscriptUi(
    state: TranscriptUiCadenceState,
    messages: List<ChatMessage>,
    nowMillis: Long,
    stablePrefixLength: (String) -> Int,
): TranscriptUiCadenceState {
    val rows = messages.mapIndexed { index, message ->
        val key = transcriptUiRowKey(index)
        val previous = state.rows.getOrNull(index)?.takeIf { it.key == key }
        if (previous != null && previous.message == message) {
            previous
        } else {
            projectTranscriptRow(key, message, previous, stablePrefixLength)
        }
    }
    return TranscriptUiCadenceState(
        rows = rows,
        publishedMessages = messages,
        lastPublishAtMillis = nowMillis,
        pendingMessages = null,
        nextPublishAtMillis = null,
    )
}

private fun projectTranscriptRow(
    key: String,
    message: ChatMessage,
    previous: TranscriptUiRow?,
    stablePrefixLength: (String) -> Int,
): TranscriptUiRow {
    val streaming = message.role == ChatMessageRole.Assistant && message.isStreaming
    if (!streaming) {
        return TranscriptUiRow(
            key = key,
            message = message,
            stablePrefixSegments = emptyList(),
            stablePrefix = "",
            streamingTail = "",
        )
    }
    val stableLen = stablePrefixLength(message.text).coerceIn(0, message.text.length)
    val previousSegments = previous?.stablePrefixSegments.orEmpty()
    val previousPrefix = previous?.stablePrefix.orEmpty()
    val canExtend = previous != null && message.text.startsWith(previousPrefix)
    val segments: List<String>
    val prefix: String
    when {
        canExtend && stableLen == previousPrefix.length -> {
            segments = previousSegments
            prefix = previousPrefix
        }
        canExtend && stableLen > previousPrefix.length -> {
            val added = message.text.substring(previousPrefix.length, stableLen)
            segments = previousSegments + added
            prefix = previousPrefix + added
        }
        stableLen == 0 -> {
            segments = emptyList()
            prefix = ""
        }
        else -> {
            val rebuilt = message.text.substring(0, stableLen)
            segments = listOf(rebuilt)
            prefix = rebuilt
        }
    }
    return TranscriptUiRow(
        key = key,
        message = message,
        stablePrefixSegments = segments,
        stablePrefix = prefix,
        streamingTail = message.text.substring(stableLen),
    )
}

private fun sameMessages(left: List<ChatMessage>, right: List<ChatMessage>): Boolean {
    if (left.size != right.size) return false
    for (index in left.indices) {
        if (left[index] != right[index]) return false
    }
    return true
}

private class TranscriptUiCadenceMemory {
    var state: TranscriptUiCadenceState = freshTranscriptUiCadenceState()
}

/**
 * Paper-only transcript projector for the session screen.
 *
 * Standard ([limited] false) returns [messages] on this frame and does not
 * schedule a flush. Paper publishes synchronously when the cadence allows it,
 * and wakes once when a held tail is due so the latest source text still lands.
 */
@Composable
internal fun rememberTranscriptUiCadence(
    sessionKey: String,
    messages: List<ChatMessage>,
    limited: Boolean,
): TranscriptUiCadenceState {
    val memory = remember(sessionKey, limited) { TranscriptUiCadenceMemory() }
    var flushTick by remember(sessionKey, limited) { mutableIntStateOf(0) }
    // Reading the tick subscribes this composition to the delayed Paper flush.
    val publishGeneration = flushTick
    val projected = if (!limited) {
        TranscriptUiCadenceState(
            rows = emptyList(),
            publishedMessages = messages,
            lastPublishAtMillis = publishGeneration.toLong(),
            pendingMessages = null,
            nextPublishAtMillis = null,
        )
    } else {
        advanceTranscriptUiCadence(
            state = memory.state,
            messages = messages,
            nowMillis = SystemClock.uptimeMillis(),
            limited = true,
        )
    }
    SideEffect {
        if (limited && memory.state != projected) memory.state = projected
    }
    val dueAt = if (limited) projected.nextPublishAtMillis else null
    LaunchedEffect(sessionKey, limited, messages, dueAt) {
        if (dueAt == null) return@LaunchedEffect
        val wait = dueAt - SystemClock.uptimeMillis()
        if (wait > 0) delay(wait)
        flushTick += 1
    }
    return projected
}
