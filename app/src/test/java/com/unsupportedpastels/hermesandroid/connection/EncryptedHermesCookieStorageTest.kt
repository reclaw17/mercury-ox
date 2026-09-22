package com.unsupportedpastels.hermesandroid.connection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.CookieEncoding
import io.ktor.http.headersOf
import io.ktor.http.renderCookieHeader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncryptedHermesCookieStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferencesName = "hermes_cookie_store_tests"
    private val url = Url("https://hermes.example/")

    // A real Hermes dashboard session token is base64 (RFC 4648) and therefore
    // contains '=', '/', and '+'. The server sets it verbatim; the client must
    // replay it verbatim. URL-encoding these bytes corrupts the token and the
    // server rejects the session (observed as HTTP 503 on /api/auth/me).
    private val base64Token =
        "eyJzdWIiOiJhZG1pbiIsImtpbmQiOiJhY2Nlc3MifQ==.sig+with/slash+plus=="

    @Before
    fun clearCookiePreferences() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("$preferencesName.keyset", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun base64SessionTokenRoundTripsVerbatim() = runTest {
        val storage = EncryptedHermesCookieStorage(context, preferencesName)
        storage.addCookie(
            url,
            io.ktor.http.Cookie(
                name = "hermes_session_at",
                value = base64Token,
                encoding = CookieEncoding.RAW,
            ),
        )

        val restored = storage.get(url).single { it.name == "hermes_session_at" }

        // The stored value must come back byte-for-byte identical.
        assertEquals(base64Token, restored.value)
        // And it must be tagged RAW so the request pipeline sends it verbatim
        // instead of percent-encoding '=' '/' '+'. This is the actual on-wire
        // guarantee: the rendered Cookie header contains the untouched token.
        assertEquals(CookieEncoding.RAW, restored.encoding)
        assertEquals(
            "hermes_session_at=$base64Token",
            renderCookieHeader(restored),
        )
    }

    @Test
    fun persistedTokenSurvivesNewStorageInstanceVerbatim() = runTest {
        EncryptedHermesCookieStorage(context, preferencesName).addCookie(
            url,
            io.ktor.http.Cookie(
                name = "hermes_session_at",
                value = base64Token,
                encoding = CookieEncoding.RAW,
            ),
        )

        // A fresh instance (process restart) must decrypt and replay verbatim.
        val restored = EncryptedHermesCookieStorage(context, preferencesName)
            .get(url)
            .single { it.name == "hermes_session_at" }

        assertEquals(base64Token, restored.value)
        assertEquals(CookieEncoding.RAW, restored.encoding)
        assertEquals(
            "hermes_session_at=$base64Token",
            renderCookieHeader(restored),
        )
    }

    @Test
    fun jarOriginKeyMatchesServerOriginCanonicalForm() {
        val origin = ServerOrigin.parse("https://hermes.example:443/")
        assertEquals("https://hermes.example", origin.value)
        assertEquals(
            origin.value,
            cookieJarOriginKey(Url("${origin.value}/auth/password-login")),
        )
        assertEquals(
            origin.value,
            cookieJarOriginKey(Url("https://hermes.example:443/api/auth/me")),
        )
        assertEquals(
            origin.value,
            cookieJarOriginKey(Url("https://HERMES.EXAMPLE/api/auth/ws-ticket")),
        )
        assertEquals(
            listOf("https://hermes.example:443"),
            legacyCookieJarOriginKeys(origin.value),
        )
    }

    @Test
    fun passwordLoginAndAuthMeShareTheSameJarOriginKey() = runTest {
        val origin = ServerOrigin.parse("https://note-air.ts.net")
        val loginUrl = Url("${origin.value}/auth/password-login")
        val meUrl = Url("${origin.value}/api/auth/me")
        val ticketUrl = Url("${origin.value}/api/auth/ws-ticket")

        assertEquals(origin.value, cookieJarOriginKey(loginUrl))
        assertEquals(cookieJarOriginKey(loginUrl), cookieJarOriginKey(meUrl))
        assertEquals(cookieJarOriginKey(loginUrl), cookieJarOriginKey(ticketUrl))

        val storage = EncryptedHermesCookieStorage(context, preferencesName)
        storage.addCookie(
            loginUrl,
            io.ktor.http.Cookie(name = "__Host-hermes_session_at", value = base64Token),
        )

        val namesOnMe = storage.get(meUrl).map { it.name }
        assertEquals(listOf("__Host-hermes_session_at"), namesOnMe)

        val diagnostics = storage.diagnostics(meUrl)
        assertEquals(origin.value, diagnostics.originKey)
        assertTrue(diagnostics.hasSessionAccessCookie)
        assertEquals(listOf("__Host-hermes_session_at"), diagnostics.cookieNames)
        // Diagnostics must never echo secret material.
        assertFalse(diagnostics.toString().contains(base64Token))
    }

    @Test
    fun legacyPortSuffixedJarKeyMigratesOntoCanonicalOrigin() = runTest {
        val origin = ServerOrigin.parse("https://hermes.example")
        val storage = EncryptedHermesCookieStorage(context, preferencesName)
        storage.seedRawForTest(
            originKey = "https://hermes.example:443",
            values = mapOf("hermes_session_at" to base64Token),
        )

        val restored = storage.get(Url("${origin.value}/api/auth/me"))
            .single { it.name == "hermes_session_at" }
        assertEquals(base64Token, restored.value)

        // Second instance proves the migrated canonical key is what persists.
        val again = EncryptedHermesCookieStorage(context, preferencesName)
            .diagnostics(Url("${origin.value}/"))
        assertEquals(origin.value, again.originKey)
        assertEquals(listOf("hermes_session_at"), again.cookieNames)
        assertTrue(again.hasSessionAccessCookie)
    }

    @Test
    fun sharedHttpClientReplaysPasswordLoginCookieOnAuthMe() = runTest {
        // Hypothesis (2): password login and REST must share one HttpClient /
        // HttpCookies jar; blank Bearer is fine only when the jar sends the
        // session cookie.
        val origin = ServerOrigin.parse("https://hermes.example")
        val cookieStorage = EncryptedHermesCookieStorage(context, preferencesName)
        var authMeCookieHeader: String? = null
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/password-login" -> respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.SetCookie,
                        listOf("hermes_session_at=$base64Token; Path=/; HttpOnly; Secure"),
                    ),
                )
                "/api/auth/me" -> {
                    authMeCookieHeader = request.headers[HttpHeaders.Cookie]
                    respond(
                        content = """{"user_id":"admin"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> error("unexpected ${request.url}")
            }
        }
        val client = HttpClient(engine) {
            configureHermesHttpClient()
            install(HttpCookies) { storage = cookieStorage }
        }

        HttpHermesPasswordAuthClient(client).signIn(
            serverOrigin = origin,
            provider = "local",
            username = "admin",
            password = "fixture-password",
        )
        val beforeMe = cookieStorage.diagnostics(Url("${origin.value}/api/auth/me"))
        assertEquals(origin.value, beforeMe.originKey)
        assertTrue(beforeMe.hasSessionAccessCookie)

        client.get("${origin.value}/api/auth/me")

        assertTrue(
            "Cookie jar must attach hermes_session_at after password-login",
            authMeCookieHeader?.contains("hermes_session_at=") == true,
        )
        assertFalse(authMeCookieHeader!!.contains("Bearer"))
        // Values may appear on the wire; diagnostics still omit them.
        assertFalse(cookieStorage.diagnostics(Url("${origin.value}/")).toString().contains(base64Token))
        client.close()
    }
}
