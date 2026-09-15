package com.unsupportedpastels.mercury.core.attachment

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareAdmissionPolicyTest {

    @Test
    fun admitsCommonDocumentsAndImages() {
        assertTrue(ShareAdmissionPolicy.isAllowedMime("image/png", "photo.png"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime("application/pdf", "report.pdf"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime("text/plain", "notes.txt"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime("application/json", "data.json"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime("video/mp4", "clip.mp4"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime(null, "photo.webp"))
        assertTrue(ShareAdmissionPolicy.isAllowedMime("application/octet-stream", "photo.png"))
    }

    @Test
    fun rejectsExecutablesMarkupAndScripts() {
        assertFalse(ShareAdmissionPolicy.isAllowedMime("application/vnd.android.package-archive", "evil.apk"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime("text/html", "page.html"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime("image/svg+xml", "icon.svg"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime("application/javascript", "run.js"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime("application/x-sh", "setup.sh"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime("image/png", "payload.apk"))
        assertFalse(ShareAdmissionPolicy.isAllowedMime(null, "run.exe"))
    }

    @Test
    fun contentUrisMustBeWellFormed() {
        assertTrue(ShareAdmissionPolicy.isAllowedContentUri("content://com.android.providers.media.documents/document/1"))
        assertFalse(ShareAdmissionPolicy.isAllowedContentUri("file:///data/data/secret"))
        assertFalse(ShareAdmissionPolicy.isAllowedContentUri("http://evil.example/file"))
        assertFalse(ShareAdmissionPolicy.isAllowedContentUri("content://"))
        assertFalse(ShareAdmissionPolicy.isAllowedContentUri("content://null/foo"))
        assertFalse(ShareAdmissionPolicy.isAllowedContentUri("https://example.com"))
    }
}
