package com.unsupportedpastels.hermesandroid.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadingProfileViewModel(
    private val repository: ReadingProfileRepository,
    private val manufacturer: String,
    private val brand: String,
    private val model: String,
    private val fingerprint: String,
) : ViewModel() {
    val preference: StateFlow<ReadingProfilePreference> = repository.preference.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ReadingProfilePreference.Auto,
    )
    val profile: StateFlow<ReadingProfile> = repository.preference
        .map(::resolve)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = resolve(ReadingProfilePreference.Auto),
        )

    fun savePreference(preference: ReadingProfilePreference) {
        viewModelScope.launch {
            repository.savePreference(preference)
        }
    }

    private fun resolve(preference: ReadingProfilePreference): ReadingProfile = resolveReadingProfile(
        preference = preference,
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        fingerprint = fingerprint,
    )

    /**
     * Same shape as [PaneLayoutPreferencesViewModel.Factory].
     * A later slice can construct this beside pane preferences and collect [profile].
     * Device signals are [Build.MANUFACTURER], [Build.BRAND], [Build.MODEL], and [Build.FINGERPRINT].
     */
    class Factory(context: Context) : ViewModelProvider.Factory {
        private val applicationContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReadingProfileViewModel::class.java))
            return ReadingProfileViewModel(
                repository = DataStoreReadingProfileRepository(applicationContext),
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                model = Build.MODEL.orEmpty(),
                fingerprint = Build.FINGERPRINT.orEmpty(),
            ) as T
        }
    }
}
