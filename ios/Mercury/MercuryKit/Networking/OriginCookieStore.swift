import Foundation
import Security

/// Origin-scoped basic-auth cookies, persisted in the Keychain as
/// `ThisDeviceOnly` items. Replaces process-wide `HTTPCookieStorage.shared`
/// so cookies cannot leak across scheme/port or survive as unencrypted
/// domain cookies.
final class OriginCookieStore: @unchecked Sendable {
    static let shared = OriginCookieStore()
    static let service = "com.unsupportedpastels.mercury.cookies"

    private let lock = NSLock()
    private var memory: [String: [CookieRecord]] = [:]
    private let normalize: (String) -> String?

    init(normalize: @escaping (String) -> String? = { ServerOrigin.normalize($0) }) {
        self.normalize = normalize
    }

    func cookies(forOrigin rawOrigin: String) -> [HTTPCookie] {
        guard let origin = normalize(rawOrigin),
              let parts = OriginCookiePolicy.OriginParts(origin: origin)
        else { return [] }
        return records(for: origin).compactMap { record in
            guard let properties = OriginCookiePolicy.pinnedProperties(
                name: record.name,
                value: record.value,
                origin: origin
            ) else { return nil }
            return HTTPCookie(properties: properties).flatMap { cookie in
                OriginCookiePolicy.cookieMatches(cookie, origin: parts) ? cookie : nil
            }
        }
    }

    func hasCookies(forOrigin rawOrigin: String) -> Bool {
        !cookies(forOrigin: rawOrigin).isEmpty
    }

    func store(_ cookies: [HTTPCookie], origin rawOrigin: String) {
        guard let origin = normalize(rawOrigin),
              let parts = OriginCookiePolicy.OriginParts(origin: origin)
        else { return }
        let admitted = cookies.filter { OriginCookiePolicy.cookieMatches($0, origin: parts) }
        guard !admitted.isEmpty else { return }
        lock.lock()
        defer { lock.unlock() }
        var bucket = loadLocked(origin)
        var keyed = Dictionary(uniqueKeysWithValues: bucket.map { ($0.name, $0) })
        for cookie in admitted {
            if cookie.value.isEmpty {
                keyed.removeValue(forKey: cookie.name)
            } else {
                keyed[cookie.name] = CookieRecord(name: cookie.name, value: cookie.value)
            }
        }
        persistLocked(origin: origin, records: Array(keyed.values))
    }

    func store(nameValues: [String: String], origin rawOrigin: String) {
        let cookies = nameValues.compactMap { name, value -> HTTPCookie? in
            guard let properties = OriginCookiePolicy.pinnedProperties(
                name: name, value: value, origin: rawOrigin
            ) else { return nil }
            return HTTPCookie(properties: properties)
        }
        store(cookies, origin: rawOrigin)
    }

    func merge(response: HTTPURLResponse, for url: URL, origin: String) {
        let headers = response.allHeaderFields.reduce(into: [String: String]()) { result, field in
            guard let name = field.key as? String, let value = field.value as? String else { return }
            result[name] = value
        }
        let cookies = HTTPCookie.cookies(withResponseHeaderFields: headers, for: url)
        store(cookies, origin: origin)
    }

    func purge(origin rawOrigin: String) {
        guard let origin = normalize(rawOrigin) else { return }
        lock.lock()
        defer { lock.unlock() }
        memory.removeValue(forKey: origin)
        var query = MercuryPrivateKeychainScope.query(service: Self.service, account: origin)
        SecItemDelete(query as CFDictionary)
        for legacy in ServerOrigin.legacyCredentialAccountCandidates(for: origin) {
            query = MercuryPrivateKeychainScope.query(service: Self.service, account: legacy)
            SecItemDelete(query as CFDictionary)
        }
    }

    // MARK: - persistence

    private struct CookieRecord: Codable {
        var name: String
        var value: String
    }

    private func records(for origin: String) -> [CookieRecord] {
        lock.lock()
        defer { lock.unlock() }
        return loadLocked(origin)
    }

    private func loadLocked(_ origin: String) -> [CookieRecord] {
        if let cached = memory[origin] { return cached }
        var query = MercuryPrivateKeychainScope.query(service: Self.service, account: origin)
        query[kSecReturnData as String] = kCFBooleanTrue
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess, let data = item as? Data,
              let decoded = try? JSONDecoder().decode([CookieRecord].self, from: data)
        else {
            memory[origin] = []
            return []
        }
        memory[origin] = decoded
        return decoded
    }

    private func persistLocked(origin: String, records: [CookieRecord]) {
        memory[origin] = records
        var query = MercuryPrivateKeychainScope.query(service: Self.service, account: origin)
        if records.isEmpty {
            SecItemDelete(query as CFDictionary)
            return
        }
        guard let payload = try? JSONEncoder().encode(records) else { return }
        let attributes: [String: Any] = [
            kSecValueData as String: payload,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            query[kSecValueData as String] = payload
            query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(query as CFDictionary, nil)
        }
    }
}
