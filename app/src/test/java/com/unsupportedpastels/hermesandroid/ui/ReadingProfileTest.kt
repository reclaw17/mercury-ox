package com.unsupportedpastels.hermesandroid.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReadingProfileDetectorTest {
    @Test
    fun emptySignalsStayStandard() {
        assertEquals(
            ReadingProfile.Standard,
            detectReadingProfile(
                manufacturer = "",
                brand = "",
                model = "",
                fingerprint = "",
            ),
        )
    }

    @Test
    fun samsungLikeSignalsStayStandard() {
        assertEquals(
            ReadingProfile.Standard,
            detectReadingProfile(
                manufacturer = "samsung",
                brand = "samsung",
                model = "SM-S928B",
                fingerprint = "samsung/e3q/e3q:14/UP1A.231005.007/release-keys",
            ),
        )
    }

    @Test
    fun booxOnlyInModelSelectsPaper() {
        assertEquals(
            ReadingProfile.Paper,
            detectReadingProfile(
                manufacturer = "Generic",
                brand = "Generic",
                model = "Note-boox",
                fingerprint = "generic/note/note:13/release-keys",
            ),
        )
    }

    @Test
    fun onyxOnlyInFingerprintSelectsPaper() {
        assertEquals(
            ReadingProfile.Paper,
            detectReadingProfile(
                manufacturer = "Generic",
                brand = "Generic",
                model = "Note",
                fingerprint = "generic/note/note:13/onyx/release-keys",
            ),
        )
    }

    @Test
    fun mixedCaseMatchSelectsPaper() {
        assertEquals(
            ReadingProfile.Paper,
            detectReadingProfile(
                manufacturer = "BoOx",
                brand = "",
                model = "",
                fingerprint = "",
            ),
        )
        assertEquals(
            ReadingProfile.Paper,
            detectReadingProfile(
                manufacturer = "",
                brand = "OnYx",
                model = "",
                fingerprint = "",
            ),
        )
    }

    @Test
    fun storedOverrideIgnoresDeviceSignals() {
        assertEquals(
            ReadingProfile.Standard,
            resolveReadingProfile(
                preference = ReadingProfilePreference.Standard,
                manufacturer = "boox",
                brand = "onyx",
                model = "BOOX",
                fingerprint = "Onyx/Note/note:13/boox/release-keys",
            ),
        )
        assertEquals(
            ReadingProfile.Paper,
            resolveReadingProfile(
                preference = ReadingProfilePreference.Paper,
                manufacturer = "samsung",
                brand = "samsung",
                model = "SM-S928B",
                fingerprint = "samsung/e3q/e3q:14/UP1A.231005.007/release-keys",
            ),
        )
        assertEquals(
            ReadingProfile.Paper,
            resolveReadingProfile(
                preference = ReadingProfilePreference.Auto,
                manufacturer = "Generic",
                brand = "Generic",
                model = "Note-boox",
                fingerprint = "generic/note/note:13/release-keys",
            ),
        )
    }
}

class ReadingProfileRepositoryTest {
    @Test
    fun missingPreferenceDefaultsToAuto() = runTest {
        val repository = DataStoreReadingProfileRepository(
            InMemoryReadingProfileDataStore(emptyPreferences()),
        )

        assertEquals(ReadingProfilePreference.Auto, repository.preference.first())
    }

    @Test
    fun savedPreferenceSurvivesRepositoryRecreation() = runTest {
        val dataStore = InMemoryReadingProfileDataStore(emptyPreferences())
        val firstRepository = DataStoreReadingProfileRepository(dataStore)

        firstRepository.savePreference(ReadingProfilePreference.Paper)
        val restoredPaper = DataStoreReadingProfileRepository(dataStore).preference.first()
        assertEquals(ReadingProfilePreference.Paper, restoredPaper)

        DataStoreReadingProfileRepository(dataStore)
            .savePreference(ReadingProfilePreference.Standard)
        val restoredStandard = DataStoreReadingProfileRepository(dataStore).preference.first()
        assertEquals(ReadingProfilePreference.Standard, restoredStandard)
    }

    @Test
    fun storedOverrideWinsAfterRepositoryRecreation() = runTest {
        val dataStore = InMemoryReadingProfileDataStore(emptyPreferences())
        DataStoreReadingProfileRepository(dataStore)
            .savePreference(ReadingProfilePreference.Standard)

        val restored = DataStoreReadingProfileRepository(dataStore).preference.first()

        assertEquals(
            ReadingProfile.Standard,
            resolveReadingProfile(
                preference = restored,
                manufacturer = "Onyx",
                brand = "onyx",
                model = "Note-boox",
                fingerprint = "onyx/note/note:13/boox/release-keys",
            ),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileStateFlowUsesAutoThenHonorsStoredOverride() = runTest(dispatcher) {
        val repository = DataStoreReadingProfileRepository(
            InMemoryReadingProfileDataStore(emptyPreferences()),
        )
        val viewModel = ReadingProfileViewModel(
            repository = repository,
            manufacturer = "Generic",
            brand = "Generic",
            model = "Note-boox",
            fingerprint = "generic/note/note:13/release-keys",
        )
        val profiles = mutableListOf<ReadingProfile>()
        val preferences = mutableListOf<ReadingProfilePreference>()
        val profileCollection = launch { viewModel.profile.collect { profiles.add(it) } }
        val preferenceCollection = launch { viewModel.preference.collect { preferences.add(it) } }

        advanceUntilIdle()
        assertEquals(listOf(ReadingProfile.Paper), profiles.distinct())
        assertEquals(listOf(ReadingProfilePreference.Auto), preferences.distinct())

        viewModel.savePreference(ReadingProfilePreference.Standard)
        advanceUntilIdle()

        assertEquals(
            listOf(ReadingProfile.Paper, ReadingProfile.Standard),
            profiles.distinct(),
        )
        assertEquals(
            listOf(ReadingProfilePreference.Auto, ReadingProfilePreference.Standard),
            preferences.distinct(),
        )
        profileCollection.cancel()
        preferenceCollection.cancel()
    }
}

private class InMemoryReadingProfileDataStore(
    initial: Preferences,
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
