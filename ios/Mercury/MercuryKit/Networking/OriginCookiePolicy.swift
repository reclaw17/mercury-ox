import Foundation

/// Filters HTTP cookies so they cannot leak across scheme, host, or port.
///
/// `HTTPCookieStorage` matches by cookie-domain, which collapses
/// `http://host:8080` and `https://host` into one jar. Hermes scopes every
/// other credential by normalized origin; cookies must do the same.
enum OriginCookiePolicy {
    struct OriginParts: Equatable {
        let scheme: String
        let host: String
        let port: Int

        init?(url: URL) {
            guard let scheme = url.scheme?.lowercased(),
                  let host = url.host?.lowercased(),
                  !host.isEmpty
            else { return nil }
            let port = url.port ?? (scheme == "https" || scheme == "wss" ? 443 : 80)
            self.scheme = scheme
            self.host = host
            self.port = port
        }

        init?(origin: String) {
            guard let url = URL(string: origin) else { return nil }
            self.init(url: url)
        }
    }

    static func filter(_ cookies: [HTTPCookie], requestURL: URL, origin: String) -> [HTTPCookie] {
        guard let request = OriginParts(url: requestURL),
              let bound = OriginParts(origin: origin),
              request == bound
        else { return [] }
        return cookies.filter { cookie in
            cookieMatches(cookie, origin: bound)
        }
    }

    static func cookieMatches(_ cookie: HTTPCookie, origin: OriginParts) -> Bool {
        var domain = cookie.domain.lowercased()
        if domain.hasPrefix(".") { domain.removeFirst() }
        // Exact host only — no parent-domain sharing across profiles or ports.
        guard domain == origin.host else { return false }
        if cookie.isSecure {
            guard origin.scheme == "https" || origin.scheme == "wss" else { return false }
        }
        if !cookie.portList.isEmpty {
            let ports = cookie.portList.compactMap { $0.intValue }
            if !ports.contains(origin.port) { return false }
        }
        return true
    }

    /// Properties used when we mint a session cookie from a name/value pair so
    /// it cannot be replayed on a different scheme or port of the same host.
    static func pinnedProperties(name: String, value: String, origin: String) -> [HTTPCookiePropertyKey: Any]? {
        guard let parts = OriginParts(origin: origin) else { return nil }
        var properties: [HTTPCookiePropertyKey: Any] = [
            .domain: parts.host,
            .path: "/",
            .name: name,
            .value: value,
            .port: "\(parts.port)",
        ]
        if parts.scheme == "https" || parts.scheme == "wss" {
            properties[.secure] = "TRUE"
        }
        return properties
    }
}
