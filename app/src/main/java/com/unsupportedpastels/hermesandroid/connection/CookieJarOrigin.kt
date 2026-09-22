package com.unsupportedpastels.hermesandroid.connection

import com.unsupportedpastels.mercury.core.origin.OriginParseResult
import com.unsupportedpastels.mercury.core.origin.ServerOriginPolicy
import io.ktor.http.URLProtocol
import io.ktor.http.Url

/**
 * Cookie-jar origin keys must match [ServerOriginPolicy.canonicalize] /
 * [ServerOrigin.value]: default ports are elided (`:443` / `:80`), hosts are
 * lowercased, and IPv6 stays bracketed.
 *
 * A previous jar keyed every request as `scheme://host:${Url.port}`, so
 * `https://host` requests landed under `https://host:443` while the rest of
 * Mercury (token store, settings, diagnostics) used the elided origin. That
 * miss left HttpOnly session cookies stranded after `login_success`, so the
 * blank cookie-backed access token could not authorize `/api/auth/me` or
 * `/api/auth/ws-ticket`.
 */
internal fun cookieJarOriginKey(url: Url): String {
    val scheme = url.protocol.name.lowercase()
    val host = url.host.trim().lowercase()
    require(host.isNotEmpty()) { "Cookie jar URL is missing a host" }
    val authorityHost = if (':' in host && !host.startsWith("[")) "[$host]" else host
    val defaultPort = when (url.protocol) {
        URLProtocol.HTTPS, URLProtocol.WSS -> 443
        else -> 80
    }
    val port = url.port
    val portSuffix = when {
        port <= 0 || port == defaultPort -> ""
        else -> ":$port"
    }
    val candidate = "$scheme://$authorityHost$portSuffix"
    return when (val result = ServerOriginPolicy.canonicalize(candidate)) {
        is OriginParseResult.Valid -> result.origin
        is OriginParseResult.Invalid -> candidate
    }
}

/**
 * Legacy jar keys that always appended [Url.port], including defaults.
 * Used only to migrate encrypted prefs onto the canonical key.
 */
internal fun legacyCookieJarOriginKeys(canonicalOrigin: String): List<String> {
    val schemeEnd = canonicalOrigin.indexOf("://")
    if (schemeEnd < 0) return emptyList()
    val scheme = canonicalOrigin.substring(0, schemeEnd)
    val authority = canonicalOrigin.substring(schemeEnd + 3)
    val hasExplicitPort = when {
        authority.startsWith("[") -> authority.contains("]:")
        else -> authority.lastIndexOf(':') > authority.lastIndexOf(']')
    }
    if (hasExplicitPort) return emptyList()
    return when (scheme) {
        "https", "wss" -> listOf("$canonicalOrigin:443")
        "http", "ws" -> listOf("$canonicalOrigin:80")
        else -> emptyList()
    }
}

/** Safe diagnostics: origin key + cookie names only (never values). */
data class HermesCookieJarDiagnostics(
    val originKey: String,
    val cookieNames: List<String>,
) {
    val hasSessionAccessCookie: Boolean
        get() = cookieNames.any { name ->
            name == "hermes_session_at" ||
                name == "__Host-hermes_session_at" ||
                name == "__Secure-hermes_session_at"
        }
}
