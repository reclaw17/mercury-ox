package com.unsupportedpastels.hermesandroid.ui

enum class ReadingProfile {
    Standard,
    Paper,
}

enum class ReadingProfilePreference {
    Auto,
    Standard,
    Paper,
}

/**
 * A stored [ReadingProfilePreference.Standard] or [ReadingProfilePreference.Paper] wins.
 * [ReadingProfilePreference.Auto] is the only path that reads device signals.
 */
fun resolveReadingProfile(
    preference: ReadingProfilePreference,
    manufacturer: String,
    brand: String,
    model: String,
    fingerprint: String,
): ReadingProfile = when (preference) {
    ReadingProfilePreference.Standard -> ReadingProfile.Standard
    ReadingProfilePreference.Paper -> ReadingProfile.Paper
    ReadingProfilePreference.Auto -> detectReadingProfile(
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        fingerprint = fingerprint,
    )
}

/**
 * Paper when any signal contains "boox" or "onyx", ignoring case.
 * Blank and unrelated devices stay [ReadingProfile.Standard].
 */
fun detectReadingProfile(
    manufacturer: String,
    brand: String,
    model: String,
    fingerprint: String,
): ReadingProfile {
    val matchesPaperDevice = listOf(manufacturer, brand, model, fingerprint).any { signal ->
        val normalized = signal.lowercase()
        "boox" in normalized || "onyx" in normalized
    }
    return if (matchesPaperDevice) ReadingProfile.Paper else ReadingProfile.Standard
}
