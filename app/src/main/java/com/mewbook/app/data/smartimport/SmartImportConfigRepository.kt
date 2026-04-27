package com.mewbook.app.data.smartimport

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmartImportConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val hasApiKey: Boolean = false,
    val secureStorageAvailable: Boolean = true
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}

data class SmartImportCredentials(
    val baseUrl: String,
    val model: String,
    val apiKey: String
)

@Singleton
class SmartImportConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun loadConfig(): SmartImportConfig = withContext(Dispatchers.IO) {
        val preferences = encryptedPreferencesOrNull()
        SmartImportConfig(
            baseUrl = preferences?.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }
                ?: SmartImportConfig.DEFAULT_BASE_URL,
            model = preferences?.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() }
                ?: SmartImportConfig.DEFAULT_MODEL,
            hasApiKey = !preferences?.getString(KEY_API_KEY, null).isNullOrBlank(),
            secureStorageAvailable = preferences != null
        )
    }

    suspend fun loadCredentials(): SmartImportCredentials = withContext(Dispatchers.IO) {
        val preferences = encryptedPreferencesOrNull()
            ?: error("安全存储不可用，无法读取 API Key")
        val apiKey = preferences.getString(KEY_API_KEY, null)?.trim().orEmpty()
        require(apiKey.isNotBlank()) { "请先配置 API Key" }
        SmartImportCredentials(
            baseUrl = preferences.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }
                ?: SmartImportConfig.DEFAULT_BASE_URL,
            model = preferences.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() }
                ?: SmartImportConfig.DEFAULT_MODEL,
            apiKey = apiKey
        )
    }

    suspend fun saveConfig(baseUrl: String, model: String, apiKey: String?) = withContext(Dispatchers.IO) {
        val preferences = encryptedPreferencesOrNull()
            ?: error("安全存储不可用，无法保存 API Key")
        preferences.edit()
            .putString(KEY_BASE_URL, baseUrl.trim().ifBlank { SmartImportConfig.DEFAULT_BASE_URL })
            .putString(KEY_MODEL, model.trim().ifBlank { SmartImportConfig.DEFAULT_MODEL })
            .apply {
                val cleanedApiKey = apiKey?.trim().orEmpty()
                if (cleanedApiKey.isNotBlank()) {
                    putString(KEY_API_KEY, cleanedApiKey)
                }
            }
            .apply()
    }

    private fun encryptedPreferencesOrNull(): SharedPreferences? {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private companion object {
        const val PREFERENCES_NAME = "smart_import_config"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key"
    }
}
