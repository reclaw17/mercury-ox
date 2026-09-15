package com.unsupportedpastels.mercury.core.origin

/** Outcome of canonicalizing user-entered server input. */
sealed interface OriginParseResult {
    data class Valid(val origin: String) : OriginParseResult
    data class Invalid(val reason: String) : OriginParseResult
}

/**
 * Server-origin canonicalization decided once for both clients.
 *
 * Unified rules (divergences resolved 2026-08-30, see plan doc):
 * - Bare hosts are accepted; [useTls] (the "Use HTTPS" checkbox, default on)
 *   picks https vs http. An explicit `http://`/`https://` in the input wins.
 * - Default ports are elided (`:443` on https, `:80` on http) so the same
 *   server always canonicalizes to the same string on both platforms.
 * - Credentials, paths (beyond one trailing slash), queries, and fragments
 *   are rejected; unbracketed IPv6 is rejected, bracketed accepted.
 * - Non-ASCII hosts use platform IDNA primitives after common compatibility
 *   mapping, then common STD3 validation. This preserves Android's prior
 *   transitional behavior while producing the same scoped key on iOS.
 *
 * Cleartext HTTP is allowed only for loopback and true private-network
 * addresses (RFC1918, IPv6 ULA, IPv6/IPv4 link-local). It is rejected for:
 * - public hosts
 * - mDNS `*.local` names (spoofable on LAN)
 * - Tailscale / CGNAT `100.64.0.0/10` (use HTTPS Tailscale Serve)
 *
 * Rejection reasons are user-visible contract (Android's dialog shows them).
 */
object ServerOriginPolicy {
    private const val INVALID_INPUT = "Enter a valid HTTP or HTTPS server origin"
    private const val INVALID_SCHEME = "Server origin must use HTTP or HTTPS"
    private const val MISSING_HOST = "Server origin must include a host"
    private const val INVALID_HOST = "Server origin must include a valid host"
    private const val HAS_CREDENTIALS = "Server origin must not include credentials"
    private const val HAS_QUERY_OR_FRAGMENT = "Server origin must not include a query or fragment"
    private const val HAS_PATH = "Server origin must not include a path"
    private const val INVALID_PORT = "Server origin contains an invalid port"
    private const val IPV6_NEEDS_BRACKETS = "IPv6 server origins must use brackets"
    const val PUBLIC_CLEARTEXT =
        "Plain HTTP is allowed only for local or private-network servers"
    const val MDNS_CLEARTEXT =
        "Plain HTTP is not allowed for .local names because mDNS can be spoofed. Use HTTPS or a numeric address."
    const val TAILSCALE_CLEARTEXT =
        "Tailscale addresses (100.64/10) must use HTTPS (Tailscale Serve). Plain HTTP is not allowed."

    fun canonicalize(input: String, useTls: Boolean = true): OriginParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return OriginParseResult.Invalid(INVALID_INPUT)

        var scheme = if (useTls) "https" else "http"
        var remainder = trimmed
        val schemeSeparator = trimmed.indexOf("://")
        if (schemeSeparator >= 0) {
            val candidate = trimmed.substring(0, schemeSeparator).lowercase()
            if (candidate != "http" && candidate != "https") {
                return OriginParseResult.Invalid(INVALID_SCHEME)
            }
            scheme = candidate
            remainder = trimmed.substring(schemeSeparator + 3)
        }

        if (remainder.isEmpty()) return OriginParseResult.Invalid(MISSING_HOST)
        if (remainder.endsWith("/")) {
            remainder = remainder.dropLast(1)
            if (remainder.isEmpty()) return OriginParseResult.Invalid(MISSING_HOST)
        }
        if (remainder.contains('?') || remainder.contains('#')) {
            return OriginParseResult.Invalid(HAS_QUERY_OR_FRAGMENT)
        }
        if (remainder.contains('/')) return OriginParseResult.Invalid(HAS_PATH)
        if (remainder.contains('@')) return OriginParseResult.Invalid(HAS_CREDENTIALS)
        if (remainder.contains('\\') || remainder.any { it.isWhitespace() || it.isISOControl() }) {
            return OriginParseResult.Invalid(INVALID_HOST)
        }

        val hostAndPort = parseAuthority(remainder)
        val host: String
        val port: Int
        when (hostAndPort) {
            is AuthorityResult.Invalid -> return OriginParseResult.Invalid(hostAndPort.reason)
            is AuthorityResult.Valid -> {
                host = hostAndPort.host
                port = hostAndPort.port
            }
        }

        val defaultPort = if (scheme == "https") 443 else 80
        val portSuffix = if (port == -1 || port == defaultPort) "" else ":$port"
        val origin = "$scheme://$host$portSuffix"
        if (scheme == "http") {
            val cleartextReason = cleartextRejectionReason(origin)
            if (cleartextReason != null) return OriginParseResult.Invalid(cleartextReason)
        }
        return OriginParseResult.Valid(origin)
    }

    /**
     * Converts an explicit HTTP(S) origin to the matching WebSocket scheme.
     * A scheme is required; bare-host input must be canonicalized first.
     * Returns null when the input is not a valid schemed origin.
     */
    fun webSocketValue(origin: String): String? {
        val trimmed = origin.trim()
        if (!trimmed.contains("://")) return null
        val result = canonicalize(trimmed)
        val canonical = (result as? OriginParseResult.Valid)?.origin ?: return null
        return when {
            canonical.startsWith("https://") -> "wss://" + canonical.removePrefix("https://")
            else -> "ws://" + canonical.removePrefix("http://")
        }
    }

    /**
     * True when the host of a normalized origin is loopback or a true
     * private-network address. mDNS `*.local` and Tailscale CGNAT are not.
     */
    fun isLoopbackOrPrivate(origin: String): Boolean {
        val host = hostOf(origin) ?: return false
        return hostIsLoopbackOrPrivate(host)
    }

    /**
     * The host to show for an origin or dashboard URL in lists: no scheme, no
     * port, no path, IPv6 without brackets. Null when the value has no host.
     * Both apps label server and cloud-agent rows with this so the two lists
     * read the same; a user-set label always wins over it.
     */
    fun displayHost(originOrUrl: String): String? = hostOf(originOrUrl.trim())

    /** Cleartext HTTP is acceptable only for loopback, ULA, or RFC1918 hosts. */
    fun allowsCleartextHttp(origin: String): Boolean =
        origin.startsWith("http://") && isLoopbackOrPrivate(origin) &&
            cleartextRejectionReason(origin) == null

    /**
     * Gate for every outbound request URL, not just the saved origin. Public
     * HTTP, mDNS HTTP, and Tailscale HTTP are denied even when they appear as
     * redirects, image URLs, or library fetches.
     */
    fun requestUrlAllowed(url: String): Boolean {
        val trimmed = url.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd < 0) return false
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        if (scheme == "https" || scheme == "wss") return true
        if (scheme != "http" && scheme != "ws") return false
        val rest = trimmed.substring(schemeEnd + 3)
        val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isEmpty() || '@' in authority) return false
        return when (val result = canonicalize("http://$authority", useTls = false)) {
            is OriginParseResult.Valid -> true
            is OriginParseResult.Invalid -> false
        }
    }

    /** User-visible reason when [origin] must not use plain HTTP, or null if allowed. */
    fun cleartextRejectionReason(origin: String): String? {
        val host = hostOf(origin) ?: return PUBLIC_CLEARTEXT
        val normalized = host.lowercase().trim('[', ']').removeSuffix(".")
        if (isMdnsLocalName(normalized)) return MDNS_CLEARTEXT
        if (isTailscaleCgnat(normalized)) return TAILSCALE_CLEARTEXT
        if (!hostIsLoopbackOrPrivate(normalized)) return PUBLIC_CLEARTEXT
        return null
    }

    // MARK: authority parsing

    private sealed interface AuthorityResult {
        data class Valid(val host: String, val port: Int) : AuthorityResult
        data class Invalid(val reason: String) : AuthorityResult
    }

    private fun parseAuthority(authority: String): AuthorityResult {
        if (authority.startsWith("[")) {
            val closingBracket = authority.indexOf(']')
            if (closingBracket <= 1) return AuthorityResult.Invalid(INVALID_HOST)
            val host = authority.substring(1, closingBracket).lowercase()
            val suffix = authority.substring(closingBracket + 1)
            val port = when {
                suffix.isEmpty() -> -1
                suffix.startsWith(":") -> parsePort(suffix.drop(1)) ?: return AuthorityResult.Invalid(INVALID_PORT)
                else -> return AuthorityResult.Invalid(INVALID_HOST)
            }
            return AuthorityResult.Valid("[$host]", port)
        }

        if (authority.count { it == ':' } > 1) {
            return AuthorityResult.Invalid(IPV6_NEEDS_BRACKETS)
        }
        val separator = authority.lastIndexOf(':')
        val hostInput = if (separator >= 0) authority.substring(0, separator) else authority
        val port = if (separator >= 0) {
            parsePort(authority.substring(separator + 1)) ?: return AuthorityResult.Invalid(INVALID_PORT)
        } else {
            -1
        }
        if (hostInput.isBlank()) return AuthorityResult.Invalid(MISSING_HOST)
        val host = idnToAscii(hostInput) ?: return AuthorityResult.Invalid(INVALID_HOST)
        return AuthorityResult.Valid(host, port)
    }

    private fun parsePort(value: String): Int? {
        if (value.isEmpty() || value.any { it !in '0'..'9' }) return null
        val port = value.toIntOrNull() ?: return null
        return port.takeIf { it in 1..65535 }
    }

    // MARK: IDN normalization

    private fun idnToAscii(host: String): String? {
        // Java's released IDNA2003 path rejects capital sharp-S while modern
        // Darwin URL handling maps it to a live ACE label. Reject it commonly
        // rather than letting the same spelling target different servers.
        if ('\u1E9E' in host) return null
        val mapped = host.idna2003CompatibilityMap()
        val asciiHost = platformIdnToAscii(mapped)?.lowercase() ?: return null
        val hasRootLabel = asciiHost.endsWith('.')
        val labels = (if (hasRootLabel) asciiHost.dropLast(1) else asciiHost).split('.')
        if (labels.any(String::isEmpty)) return null
        for (ascii in labels) {
            if (ascii.isEmpty() || ascii.length > 63) return null
            if (ascii.any { it !in 'a'..'z' && it !in '0'..'9' && it != '-' }) return null
            if (ascii.startsWith('-') || ascii.endsWith('-')) return null
        }
        return asciiHost.takeIf { it.length <= if (hasRootLabel) 254 else 253 }
    }

    /**
     * Compatibility mappings for the deviation characters that Java's
     * IDNA2003 implementation handled before this policy moved to commonMain.
     * Keeping these mappings prevents a Unicode spelling from silently
     * retargeting an existing Android server origin during migration.
     */
    private fun String.idna2003CompatibilityMap(): String = buildString(length) {
        for (character in this@idna2003CompatibilityMap) {
            when {
                character == '\u00DF' -> append("ss")
                character == '\u03C2' -> append('\u03C3')
                character == '\u200C' || character == '\u200D' -> Unit
                character in '\uFF01'..'\uFF5E' -> append((character.code - 0xFEE0).toChar())
                else -> append(character)
            }
        }
    }
    // MARK: host helpers

    private fun hostOf(origin: String): String? {
        val schemeEnd = origin.indexOf("://")
        if (schemeEnd < 0) return null
        var rest = origin.substring(schemeEnd + 3)
        rest = rest.substringBefore('/')
        rest = rest.substringBefore('?')
        rest = rest.substringBefore('#')
        if (rest.startsWith("[")) {
            val closing = rest.indexOf(']')
            if (closing <= 1) return null
            return rest.substring(1, closing).lowercase()
        }
        val host = rest.substringBefore(':')
        return host.ifEmpty { null }?.lowercase()
    }

    private fun hostIsLoopbackOrPrivate(rawHost: String): Boolean {
        val host = rawHost.lowercase().trim('[', ']').removeSuffix(".")
        if (host == "localhost" || host == "::1" || host.endsWith(".localhost")) {
            return true
        }
        // mDNS *.local is intentionally not private: any LAN host can answer.
        if (isMdnsLocalName(host)) return false
        if (isTailscaleCgnat(host)) return false

        if (host.startsWith("::ffff:")) {
            val mapped = host.removePrefix("::ffff:")
            if ('.' in mapped) return hostIsLoopbackOrPrivate(mapped)
        }
        if (':' in host) return isIpv6LoopbackOrPrivate(host)

        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return when (octets[0]) {
            127, 10 -> true
            169 -> octets[1] == 254
            172 -> octets[1] in 16..31
            192 -> octets[1] == 168
            else -> false
        }
    }

    private fun isMdnsLocalName(host: String): Boolean =
        host == "local" || host.endsWith(".local")

    /**
     * Tailscale userspace networking (and other CGNAT) lives in 100.64.0.0/10.
     * Those addresses traverse a virtual overlay, not a broadcast LAN, so
     * cleartext HTTP is not equivalent to RFC1918. Hermes on Tailscale must
     * use HTTPS Serve / MagicDNS HTTPS.
     */
    fun isTailscaleCgnat(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return octets[0] == 100 && octets[1] in 64..127
    }

    private fun isIpv6LoopbackOrPrivate(host: String): Boolean {
        if (host == "::1" || host == "0:0:0:0:0:0:0:1") return true
        val first = host.substringBefore(':')
        if (first.isEmpty()) return host == "::1"
        val hextet = first.toIntOrNull(16) ?: return false
        // Unique local fc00::/7 and link-local fe80::/10.
        return (hextet and 0xfe00) == 0xfc00 || (hextet and 0xffc0) == 0xfe80
    }
}

/** Platform IDNA primitive; common policy supplies mapping and final STD3 validation. */
internal expect fun platformIdnToAscii(host: String): String?
