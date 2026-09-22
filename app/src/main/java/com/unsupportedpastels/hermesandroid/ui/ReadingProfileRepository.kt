package com.unsupportedpastels.hermesandroid.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ReadingProfilePreferencesDataStoreName = "reading_profile_preference"
private val ReadingProfilePreferenceKey =
    stringPreferencesKey("reading_profile_preference_v1")
private val Context.readingProfilePreferencesDataStore by preferencesDataStore(
    name = ReadingProfilePreferencesDataStoreName,
)

interface ReadingProfileRepository {
    val preference: Flow<ReadingProfilePreference>

    suspend fun savePreference(preference: ReadingProfilePreference)
}

class DataStoreReadingProfileRepository(
    private val dataStore: DataStore<Preferences>,
) : ReadingProfileRepository {
    constructor(context: Context) : this(context.applicationContext.readingProfilePreferencesDataStore)

    override val preference: Flow<ReadingProfilePreference> = dataStore.data.map { preferences ->
        preferences[ReadingProfilePreferenceKey]
            ?.let { stored -> ReadingProfilePreference.entries.firstOrNull { it.name == stored } }
            ?: ReadingProfilePreference.Auto
    }

    override suspend fun savePreference(preference: ReadingProfilePreference) {
        dataStore.edit { preferences ->
            preferences[ReadingProfilePreferenceKey] = preference.name
        }
    }
}
