package com.unsupportedpastels.hermesandroid.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private fun inetAddress(value: String): InetAddress = InetAddress.getByName(value)

class RemoteMediaImageTest {
    @Test
    fun acceptsOnlyCredentialFreeHttpsMediaUrls() {
        val credentialedUrl = "https://user" + ":secret@cdn.example/image.png"
        assertTrue(validateRemoteMediaUrl("https://cdn.example/image.png"))
        assertFalse(validateRemoteMediaUrl("http://cdn.example/image.png"))
        assertFalse(validateRemoteMediaUrl(credentialedUrl))
        assertFalse(validateRemoteMediaUrl("https://cdn.example:8443/image.png"))
        assertFalse(validateRemoteMediaUrl("https://127.0.0.1/image.png"))
        assertFalse(validateRemoteMediaUrl("https://localhost/image.png"))
    }

    @Test
    fun rejectsHostsThatResolveToNonPublicAddresses() {
        assertFalse(hostResolvesToPublicNetwork("127.0.0.1"))
        assertFalse(hostResolvesToPublicNetwork("169.254.169.254"))
        assertFalse(hostResolvesToPublicNetwork("10.0.0.5"))
        assertFalse(hostResolvesToPublicNetwork("192.168.1.10"))
        assertFalse(hostResolvesToPublicNetwork("172.16.0.1"))
        assertFalse(hostResolvesToPublicNetwork("0.0.0.0"))
        assertFalse(hostResolvesToPublicNetwork("100.64.0.1"))
        assertTrue(hostResolvesToPublicNetwork("93.184.216.34"))
        assertTrue(hostResolvesToPublicNetwork("8.8.8.8"))
    }

    @Test
    fun rejectsHostResolvingToPrivateWhenAnyResolvedAddressIsPrivate() {
        // A hostname (DNS rebinding target) that also resolves to a private address is rejected.
        assertFalse(
            hostResolvesToPublicNetwork(
                "public.example.com",
                resolve = { listOf(inetAddress("93.184.216.34"), inetAddress("10.0.0.5")) },
            ),
        )
        assertTrue(
            hostResolvesToPublicNetwork(
                "public.example.com",
                resolve = { listOf(inetAddress("93.184.216.34"), inetAddress("8.8.8.8")) },
            ),
        )
    }

    @Test
    fun downloaderRefusesPrivateResolvingHostBeforeIssuingRequest() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests += 1
            respond(
                content = ByteArray(16),
                headers = headersOf(HttpHeaders.ContentType, "image/png"),
            )
        }) { configureRemoteImageHttpClient() }

        val downloader = RemoteImageDownloader(
            client = client,
            resolveHost = { host ->
                when (host) {
                    "internal.example.com" -> listOf(inetAddress("127.0.0.1"))
                    "cdn.example" -> listOf(inetAddress("93.184.216.34"))
                    else -> emptyList()
                }
            },
        )

        val rejected = downloader.download("https://internal.example.com/image.png")
        assertTrue(rejected is RemoteImageDownloadResult.InvalidUrl)
        assertEquals(0, requests)

        val accepted = downloader.download("https://cdn.example/image.png")
        assertTrue(accepted is RemoteImageDownloadResult.Success)
        assertEquals(1, requests)
        client.close()
    }

    @Test
    fun acceptsImageHostPathsButRejectsNonImageAndMalformedPaths() {
        assertTrue(validateGatewayMediaPath("/workspace/project/generated.jpg"))
        assertTrue(validateGatewayMediaPath("/workspace/project/generated.PNG"))
        assertFalse(validateGatewayMediaPath("relative/generated.jpg"))
        assertFalse(validateGatewayMediaPath("/workspace/project/notes.txt"))
        assertFalse(validateGatewayMediaPath("/workspace/project/no-extension"))
    }

    @Test
    fun acceptsVideoHostPathsButRejectsNonVideoAndMalformedPaths() {
        assertTrue(validateGatewayVideoPath("/workspace/scene-00/preview.mp4"))
        assertTrue(validateGatewayVideoPath("/workspace/scene-00/preview.WEBM"))
        assertFalse(validateGatewayVideoPath("relative/preview.mp4"))
        assertFalse(validateGatewayVideoPath("/workspace/scene-00/generated.jpg"))
        assertFalse(validateGatewayVideoPath("/workspace/scene-00/no-extension"))
    }

    @Test
    fun oversizedImageBodyIsRejectedBeforeDecode() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = ByteArray(2_049),
                headers = headersOf(HttpHeaders.ContentType, "image/png"),
            )
        }) { configureRemoteImageHttpClient() }

        val result = RemoteImageDownloader(
            client,
            maxBytes = 2_048,
            resolveHost = { listOf(inetAddress("93.184.216.34")) },
        ).download("https://cdn.example/image.png")

        assertTrue(result is RemoteImageDownloadResult.TooLarge)
        client.close()
    }

    @Test
    fun redirectIsNotFollowed() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests += 1
            respond(
                content = ByteArray(0),
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "https://other.example/image.png"),
            )
        }) { configureRemoteImageHttpClient() }

        val result = RemoteImageDownloader(
            client,
            resolveHost = { listOf(inetAddress("93.184.216.34")) },
        ).download("https://cdn.example/image.png")

        assertTrue(result is RemoteImageDownloadResult.HttpFailure)
        assertEquals(302, (result as RemoteImageDownloadResult.HttpFailure).statusCode)
        assertEquals(1, requests)
        client.close()
    }

    @Test
    fun paperSamples3000By1000Below1280AndStandardStaysAt2048() {
        assertEquals(2_048, MAX_REMOTE_IMAGE_RENDER_DIMENSION)
        assertEquals(1_280, PAPER_REMOTE_IMAGE_RENDER_DIMENSION)
        assertEquals(4, MAX_REMOTE_IMAGE_CACHE_ENTRIES)
        val paperSample = remoteImageSampleSize(
            width = 3_000,
            height = 1_000,
            maxRenderDimension = remoteImageRenderDimension(ReadingProfile.Paper),
        )
        val standardSample = remoteImageSampleSize(
            width = 3_000,
            height = 1_000,
            maxRenderDimension = remoteImageRenderDimension(ReadingProfile.Standard),
        )
        assertEquals(4, paperSample)
        assertEquals(2, standardSample)
        assertTrue(3_000 / paperSample <= PAPER_REMOTE_IMAGE_RENDER_DIMENSION)
        assertTrue(1_000 / paperSample <= PAPER_REMOTE_IMAGE_RENDER_DIMENSION)
        assertTrue(3_000 / standardSample <= MAX_REMOTE_IMAGE_RENDER_DIMENSION)
        assertTrue(3_000 / standardSample > PAPER_REMOTE_IMAGE_RENDER_DIMENSION)
    }

    @Test
    fun cacheEvictionStillCapsAtFour() {
        val cache = AccessOrderLruCache<String>(MAX_REMOTE_IMAGE_CACHE_ENTRIES)
        repeat(5) { index -> cache.put("image-$index", "bitmap-$index") }
        assertEquals(4, cache.size())
        assertFalse(cache.contains("image-0"))
        assertTrue(cache.contains("image-1"))
        assertTrue(cache.contains("image-4"))
    }

    @Test
    fun paperLeaveDropsPaperKeysAndKeepsStandardKeys() {
        val cache = AccessOrderLruCache<String>(MAX_REMOTE_IMAGE_CACHE_ENTRIES)
        val standard = profileScopedCacheKey("https://cdn.example/a.png", ReadingProfile.Standard)
        val paper = profileScopedCacheKey("https://cdn.example/b.png", ReadingProfile.Paper)
        cache.put(standard, "standard")
        cache.put(paper, "paper")
        cache.removeWhere(::isPaperBitmapCacheKey)
        assertEquals("https://cdn.example/a.png", standard)
        assertTrue(cache.contains(standard))
        assertFalse(cache.contains(paper))
        assertEquals(1, cache.size())
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteMediaImageDecodeTest {
    @Test
    fun source3000By1000SamplesToPaper1280AndStandard2048() {
        val bytes = pngBytes(width = 3_000, height = 1_000)
        val paper = decodeRemoteImage(bytes, ReadingProfile.Paper)
        val standard = decodeRemoteImage(bytes, ReadingProfile.Standard)
        assertNotNull(paper)
        assertNotNull(standard)
        val paperLongEdge = maxOf(paper!!.width, paper.height)
        val standardLongEdge = maxOf(standard!!.width, standard.height)
        assertTrue("paper long edge $paperLongEdge", paperLongEdge <= 1_280)
        assertTrue("standard long edge $standardLongEdge", standardLongEdge <= 2_048)
        assertTrue("standard long edge $standardLongEdge", standardLongEdge > 1_280)
    }

    @Test
    fun runtimeCacheEvictsTheFifthBitmap() {
        RemoteImageRuntime.clearForTest()
        try {
            repeat(5) { index ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
                RemoteImageRuntime.cache("https://cdn.example/cache-$index.png", bitmap)
            }
            assertEquals(4, RemoteImageRuntime.sizeForTest())
            assertFalse(RemoteImageRuntime.containsForTest("https://cdn.example/cache-0.png"))
            assertTrue(RemoteImageRuntime.containsForTest("https://cdn.example/cache-4.png"))
        } finally {
            RemoteImageRuntime.clearForTest()
        }
    }

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        bitmap.recycle()
        return stream.toByteArray()
    }
}
