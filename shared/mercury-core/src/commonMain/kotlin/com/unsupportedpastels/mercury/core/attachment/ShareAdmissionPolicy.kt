package com.unsupportedpastels.mercury.core.attachment

/**
 * Admission for untrusted share-sheet payloads. The in-app picker is unchanged:
 * users can still attach any file they choose. Share ingress is narrower so a
 * random app cannot dump an APK, HTML document, or executable into the
 * composer, which then rides to an agent with shell access.
 *
 * Rejection strings are user-visible contract on both platforms.
 */
object ShareAdmissionPolicy {
    const val MAX_TEXT_CHARS = 32_768
    const val MAX_FORWARDED_URIS = 20
    const val MAX_MIME_CHARS = 256

    const val REJECT_NOT_DOCUMENT = "One shared item was not a readable document"
    const val REJECT_UNSUPPORTED_TYPE = "One shared item is not an allowed file type"
    const val REJECT_EMPTY = "Nothing readable was shared"

    private val ALLOWED_EXACT = setOf(
        "text/plain",
        "text/markdown",
        "text/csv",
        "text/xml",
        "text/calendar",
        "application/pdf",
        "application/json",
        "application/zip",
        "application/gzip",
        "application/x-gzip",
        "application/msword",
        "application/vnd.ms-excel",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet",
        "application/rtf",
        "application/epub+zip",
    )

    private val ALLOWED_PREFIXES = listOf(
        "image/",
        "audio/",
        "video/",
        "text/",
    )

    private val REJECTED_EXACT = setOf(
        "image/svg+xml",
        "text/html",
        "application/xhtml+xml",
        "application/javascript",
        "text/javascript",
        "application/x-javascript",
        "application/vnd.android.package-archive",
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-executable",
        "application/x-elf",
        "application/x-mach-binary",
        "application/x-sh",
        "application/x-shellscript",
        "application/x-csh",
        "application/x-bat",
        "application/vnd.microsoft.portable-executable",
    )

    private val REJECTED_EXTENSIONS = setOf(
        "apk", "apks", "aab", "dex", "exe", "msi", "dll", "scr", "com", "bat",
        "cmd", "ps1", "sh", "bash", "zsh", "html", "htm", "svg", "xhtml", "js",
        "mjs", "cjs", "wasm",
    )

    fun isAllowedMime(mimeType: String?, displayName: String): Boolean {
        val mime = mimeType?.lowercase()?.substringBefore(';')?.trim().orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension in REJECTED_EXTENSIONS) return false
        if (mime in REJECTED_EXACT) return false
        if (mime.isEmpty() || mime == "application/octet-stream" || mime == "binary/octet-stream") {
            // Unknown type: admit only when the extension is a known safe document.
            return extension in setOf(
                "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "avif",
                "pdf", "txt", "md", "csv", "json", "xml", "zip", "gz", "mp3", "mp4",
                "wav", "m4a", "webm", "mov", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "odt", "ods", "rtf", "epub",
            )
        }
        if (mime in ALLOWED_EXACT) return true
        return ALLOWED_PREFIXES.any { mime.startsWith(it) } && mime !in REJECTED_EXACT
    }

    /**
     * Android share URIs must be `content://` with a non-empty authority and
     * no `file:` / `http:` smuggling. Path traversal in the last segment is
     * stripped later by [AttachmentPolicy.sanitizeDisplayName].
     */
    fun isAllowedContentUri(uri: String): Boolean {
        if (!uri.startsWith("content://", ignoreCase = true)) return false
        val withoutScheme = uri.substring("content://".length)
        if (withoutScheme.isEmpty()) return false
        val authority = withoutScheme.substringBefore('/')
        if (authority.isBlank()) return false
        if (authority.equals("null", ignoreCase = true)) return false
        if ('\\' in uri || '\u0000' in uri) return false
        return true
    }
}
