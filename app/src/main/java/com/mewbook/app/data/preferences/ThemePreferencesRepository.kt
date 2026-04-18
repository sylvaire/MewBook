package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode(val storageValue: String, val displayName: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色模式"),
    DARK("dark", "深色模式");

    companion object {
        fun fromStorageValue(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

private val Context.themePreferencesDataStore by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")

    val themeMode: Flow<AppThemeMode> = context.themePreferencesDataStore.data.map { preferences ->
        AppThemeMode.fromStorageValue(preferences[themeModeKey])
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.themePreferencesDataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.storageValue
        }
    }

    suspend fun getThemeModeOnce(): AppThemeMode {
        return themeMode.first()
    }
}
