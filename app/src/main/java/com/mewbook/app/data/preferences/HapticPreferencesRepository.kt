package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hapticPreferencesDataStore by preferencesDataStore(
    name = "haptic_preferences"
)

@Singleton
class HapticPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyPressHapticEnabledKey: Preferences.Key<Boolean> =
        booleanPreferencesKey("key_press_haptic_enabled")

    val keyPressHapticEnabled: Flow<Boolean> = context.hapticPreferencesDataStore.data.map { preferences ->
        preferences[keyPressHapticEnabledKey] ?: true
    }

    suspend fun setKeyPressHapticEnabled(enabled: Boolean) {
        context.hapticPreferencesDataStore.edit { preferences ->
            preferences[keyPressHapticEnabledKey] = enabled
        }
    }

    suspend fun isKeyPressHapticEnabledOnce(): Boolean {
        return keyPressHapticEnabled.first()
    }
}
