package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appUpdatePreferencesDataStore by preferencesDataStore(
    name = "app_update_preferences"
)

@Singleton
class AppUpdatePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val snoozedVersionNameKey: Preferences.Key<String> = stringPreferencesKey("snoozed_version_name")
    private val updateEnabledKey: Preferences.Key<Boolean> = booleanPreferencesKey("update_enabled")

    val updateEnabled: Flow<Boolean> = context.appUpdatePreferencesDataStore.data.map { preferences ->
        preferences[updateEnabledKey] ?: true
    }

    val snoozedVersionName: Flow<String?> = context.appUpdatePreferencesDataStore.data.map { preferences ->
        preferences[snoozedVersionNameKey]
    }

    suspend fun setSnoozedVersion(versionName: String) {
        context.appUpdatePreferencesDataStore.edit { preferences ->
            preferences[snoozedVersionNameKey] = versionName
        }
    }

    suspend fun clearSnoozedVersion() {
        context.appUpdatePreferencesDataStore.edit { preferences ->
            preferences.remove(snoozedVersionNameKey)
        }
    }

    suspend fun setUpdateEnabled(enabled: Boolean) {
        context.appUpdatePreferencesDataStore.edit { preferences ->
            preferences[updateEnabledKey] = enabled
        }
    }

    suspend fun isUpdateEnabledOnce(): Boolean {
        return updateEnabled.first()
    }

    suspend fun getSnoozedVersionOnce(): String? {
        return snoozedVersionName.first()
    }
}
