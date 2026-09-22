import XCTest
@testable import Mercury

final class OriginCookiePolicyTests: XCTestCase {
    func testFilterRequiresMatchingSchemeHostAndPort() {
        let origin = "https://hermes.test:8443"
        let request = URL(string: "https://hermes.test:8443/api/status")!
        let otherScheme = URL(string: "http://hermes.test:8443/api/status")!
        let otherPort = URL(string: "https://hermes.test/api/status")!
        guard let properties = OriginCookiePolicy.pinnedProperties(
            name: "hermes_session", value: "token", origin: origin
        ), let cookie = HTTPCookie(properties: properties) else {
            return XCTFail("cookie")
        }

        XCTAssertEqual(OriginCookiePolicy.filter([cookie], requestURL: request, origin: origin).count, 1)
        XCTAssertTrue(OriginCookiePolicy.filter([cookie], requestURL: otherScheme, origin: origin).isEmpty)
        XCTAssertTrue(OriginCookiePolicy.filter([cookie], requestURL: otherPort, origin: origin).isEmpty)
    }

    func testParentDomainCookiesAreNotReplayed() {
        let origin = "https://hermes.test"
        let request = URL(string: origin)!
        let parent = HTTPCookie(properties: [
            .domain: ".test", .path: "/", .name: "sid", .value: "x", .secure: "TRUE",
        ])!
        XCTAssertTrue(OriginCookiePolicy.filter([parent], requestURL: request, origin: origin).isEmpty)
    }
}
