import Foundation

/// Redirect policy for Hermes-origin requests, matching Android's
/// `followRedirects = false` on the connection and media clients: a redirect
/// is never followed, so a bearer token, cookie, or ticket scoped to the
/// configured origin can never be forwarded to another host or replayed over
/// a plain-HTTP downgrade. The 3xx response surfaces to the caller unchanged
/// and is classified like any other non-2xx status.
final class RedirectRefusingSessionDelegate: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    static let shared = RedirectRefusingSessionDelegate()

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        completionHandler(nil)
    }
}

enum HermesURLSession {
    /// A session over `configuration` that refuses every redirect.
    static func make(_ configuration: URLSessionConfiguration) -> URLSession {
        URLSession(
            configuration: configuration,
            delegate: RedirectRefusingSessionDelegate.shared,
            delegateQueue: nil
        )
    }

    /// Process-wide default for Hermes-origin clients. Ephemeral, no shared
    /// cookie jar: basic-auth cookies live in `OriginCookieStore` (Keychain,
    /// origin-scoped) so they cannot leak across scheme or port.
    static let noRedirects: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.httpCookieStorage = nil
        config.httpShouldSetCookies = false
        config.httpCookieAcceptPolicy = .never
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        config.timeoutIntervalForRequest = 20
        return make(config)
    }()
}
