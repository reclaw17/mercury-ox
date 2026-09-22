package com.unsupportedpastels.hermesandroid.connection

import com.unsupportedpastels.mercury.core.origin.ServerOriginPolicy
import io.ktor.client.plugins.api.createClientPlugin

/**
 * Enforces [ServerOriginPolicy.requestUrlAllowed] on every Ktor request so a
 * redirect, managed-image URL, or library fetch cannot bypass the origin
 * parser and leak a password over public HTTP, spoofable mDNS, or Tailscale
 * CGNAT cleartext.
 */
class HermesCleartextBlockedException(
    reason: String = ServerOriginPolicy.PUBLIC_CLEARTEXT,
) : IllegalStateException(reason)

val HermesCleartextGate = createClientPlugin("HermesCleartextGate") {
    onRequest { request, _ ->
        val url = request.url.toString()
        if (!ServerOriginPolicy.requestUrlAllowed(url)) {
            throw HermesCleartextBlockedException(
                ServerOriginPolicy.cleartextRejectionReason(url)
                    ?: ServerOriginPolicy.PUBLIC_CLEARTEXT,
            )
        }
    }
}
