package com.unsupportedpastels.hermesandroid.share

import com.unsupportedpastels.hermesandroid.app.ComposerAttachment
import com.unsupportedpastels.mercury.core.attachment.AttachmentAddResult
import com.unsupportedpastels.hermesandroid.attachment.AttachmentPolicy
import com.unsupportedpastels.hermesandroid.attachment.checkAdd
import com.unsupportedpastels.mercury.core.attachment.ShareAdmissionPolicy

/** Pure admission policy for untrusted Android share metadata. */
object SharePayloadPolicy {
    const val MAX_TEXT_CHARS = ShareAdmissionPolicy.MAX_TEXT_CHARS

    fun build(
        text: String?,
        candidates: List<SharedAttachmentCandidate>,
        requestId: Long = 0,
    ): SharePayloadBuildResult {
        val accepted = mutableListOf<ComposerAttachment>()
        val rejections = mutableListOf<String>()
        candidates.forEach { candidate ->
            if (!ShareAdmissionPolicy.isAllowedContentUri(candidate.uri)) {
                rejections += ShareAdmissionPolicy.REJECT_NOT_DOCUMENT
                return@forEach
            }
            val displayName = AttachmentPolicy.sanitizeDisplayName(candidate.displayName)
            if (!ShareAdmissionPolicy.isAllowedMime(candidate.mimeType, displayName)) {
                rejections += ShareAdmissionPolicy.REJECT_UNSUPPORTED_TYPE
                return@forEach
            }
            val attachment = ComposerAttachment(
                id = candidate.uri,
                uri = candidate.uri,
                displayName = displayName,
                mimeType = candidate.mimeType?.take(ShareAdmissionPolicy.MAX_MIME_CHARS)?.takeIf(String::isNotBlank),
                sizeBytes = candidate.sizeBytes,
            )
            when (val result = AttachmentPolicy.checkAdd(accepted, attachment)) {
                AttachmentAddResult.Accepted -> accepted += attachment
                is AttachmentAddResult.Rejected -> rejections += result.reason
            }
        }
        val boundedText = text.orEmpty().take(MAX_TEXT_CHARS)
        if (boundedText.isBlank() && accepted.isEmpty()) {
            rejections += ShareAdmissionPolicy.REJECT_EMPTY
        }
        val payload = SharePayload(
            requestId = requestId,
            text = boundedText,
            attachments = accepted,
            rejections = rejections.toList(),
        )
        return SharePayloadBuildResult(payload, rejections.toList())
    }
}
