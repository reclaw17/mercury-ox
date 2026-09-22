package com.unsupportedpastels.hermesandroid.ui

/** Closed code fences on Paper show this many lines until the reader expands them. */
internal const val PaperCodePreviewLineLimit = 12

internal data class PaperCodePreview(
    val text: String,
    val canExpand: Boolean,
)

/**
 * Paper shows the first [lineLimit] lines of a fence and an expand affordance.
 * A trailing newline left by a fence closer is not an extra blank line.
 * Expanded text is the source fence body unchanged.
 */
internal fun paperCodePreview(
    code: String,
    expanded: Boolean,
    lineLimit: Int = PaperCodePreviewLineLimit,
): PaperCodePreview {
    val normalized = code.replace("\r\n", "\n").replace('\r', '\n')
    if (expanded) return PaperCodePreview(normalized, canExpand = false)
    val pieces = normalized.split('\n')
    val contentLines = if (pieces.size > 1 && pieces.last().isEmpty()) {
        pieces.dropLast(1)
    } else {
        pieces
    }
    if (contentLines.size <= lineLimit) {
        return PaperCodePreview(normalized, canExpand = false)
    }
    return PaperCodePreview(
        text = contentLines.take(lineLimit).joinToString(separator = "\n"),
        canExpand = true,
    )
}
