package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.repository.DavAutoBackupStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.davAutoBackupPreferencesDataStore by preferencesDataStore(
    name = "dav_auto_backup_preferences"
)

@Singleton
class DavAutoBackupPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : DavAutoBackupStatusRepository {

    private val lastAttemptDateKey: Preferences.Key<String> = stringPreferencesKey("last_attempt_date")
    private val lastAttemptTimeKey: Preferences.Key<String> = stringPreferencesKey("last_attempt_time")
    private val lastSuccessTimeKey: Preferences.Key<String> = stringPreferencesKey("last_success_time")
    private val lastMessageKey: Preferences.Key<String> = stringPreferencesKey("last_message")
    private val lastMessageIsErrorKey: Preferences.Key<Boolean> = booleanPreferencesKey("last_message_is_error")

    override val status: Flow<DavAutoBackupStatus> = context.davAutoBackupPreferencesDataStore.data.map { preferences ->
        DavAutoBackupStatus(
            lastAttemptDate = preferences[lastAttemptDateKey]?.let(::parseLocalDateOrNull),
            lastAttemptTime = preferences[lastAttemptTimeKey]?.let(::parseLocalDateTimeOrNull),
            lastSuccessTime = preferences[lastSuccessTimeKey]?.let(::parseLocalDateTimeOrNull),
            lastMessage = preferences[lastMessageKey],
            lastMessageIsError = preferences[lastMessageIsErrorKey] ?: false
        )
    }

    override suspend fun getLastAttemptDateOnce(): LocalDate? {
        return status.first().lastAttemptDate
    }

    override suspend fun recordAttempt(date: LocalDate, time: LocalDateTime) {
        context.davAutoBackupPreferencesDataStore.edit { preferences ->
            preferences[lastAttemptDateKey] = date.toString()
            preferences[lastAttemptTimeKey] = time.toString()
            preferences.remove(lastMessageKey)
            preferences[lastMessageIsErrorKey] = false
        }
    }

    override suspend fun recordSuccess(time: LocalDateTime, message: String?) {
        context.davAutoBackupPreferencesDataStore.edit { preferences ->
            preferences[lastSuccessTimeKey] = time.toString()
            if (message.isNullOrBlank()) {
                preferences.remove(lastMessageKey)
            } else {
                preferences[lastMessageKey] = message
            }
            preferences[lastMessageIsErrorKey] = false
        }
    }

    override suspend fun recordFailure(message: String) {
        context.davAutoBackupPreferencesDataStore.edit { preferences ->
            preferences[lastMessageKey] = message
            preferences[lastMessageIsErrorKey] = true
        }
    }

    private fun parseLocalDateOrNull(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    private fun parseLocalDateTimeOrNull(value: String): LocalDateTime? {
        return runCatching { LocalDateTime.parse(value) }.getOrNull()
    }
}
