package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.QuickEntryMemory
import com.mewbook.app.domain.policy.QuickEntryTimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quickEntryPreferencesDataStore by preferencesDataStore(name = "quick_entry_preferences")

@Singleton
class QuickEntryPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val memoriesKey: Preferences.Key<String> = stringPreferencesKey("quick_entry_memories")

    val memories: Flow<List<QuickEntryMemory>> = context.quickEntryPreferencesDataStore.data.map { preferences ->
        QuickEntryMemorySerializer.decode(preferences[memoriesKey])
    }

    suspend fun rememberQuickEntry(memory: QuickEntryMemory) {
        context.quickEntryPreferencesDataStore.edit { preferences ->
            val current = QuickEntryMemorySerializer.decode(preferences[memoriesKey])
            val updated = (listOf(memory) + current)
                .distinctBy { remembered ->
                    listOf(
                        remembered.ledgerId,
                        remembered.type,
                        remembered.timeSlot,
                        remembered.categoryId,
                        remembered.accountId,
                        remembered.amount,
                        remembered.note
                    )
                }
                .take(MAX_MEMORY_COUNT)
            preferences[memoriesKey] = QuickEntryMemorySerializer.encode(updated)
        }
    }

    suspend fun clearQuickEntryMemory() {
        context.quickEntryPreferencesDataStore.edit { preferences ->
            preferences.remove(memoriesKey)
        }
    }

    suspend fun getMemoriesOnce(): List<QuickEntryMemory> = memories.first()

    companion object {
        private const val MAX_MEMORY_COUNT = 24
    }
}

object QuickEntryMemorySerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val listSerializer = ListSerializer(QuickEntryMemoryDto.serializer())

    fun encode(memories: List<QuickEntryMemory>): String {
        return json.encodeToString(listSerializer, memories.map(QuickEntryMemoryDto::fromDomain))
    }

    fun decode(encoded: String?): List<QuickEntryMemory> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(listSerializer, encoded).mapNotNull(QuickEntryMemoryDto::toDomain)
        }.getOrElse { emptyList() }
    }
}

@Serializable
private data class QuickEntryMemoryDto(
    val ledgerId: Long,
    val type: String,
    val timeSlot: String,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val amount: Double? = null,
    val note: String? = null,
    val savedAtEpochMillis: Long
) {
    fun toDomain(): QuickEntryMemory? {
        val recordType = runCatching { RecordType.valueOf(type) }.getOrNull() ?: return null
        val resolvedTimeSlot = runCatching { QuickEntryTimeSlot.valueOf(timeSlot) }.getOrNull() ?: return null
        return QuickEntryMemory(
            ledgerId = ledgerId,
            type = recordType,
            timeSlot = resolvedTimeSlot,
            categoryId = categoryId,
            accountId = accountId,
            amount = amount,
            note = note,
            savedAtEpochMillis = savedAtEpochMillis
        )
    }

    companion object {
        fun fromDomain(memory: QuickEntryMemory): QuickEntryMemoryDto {
            return QuickEntryMemoryDto(
                ledgerId = memory.ledgerId,
                type = memory.type.name,
                timeSlot = memory.timeSlot.name,
                categoryId = memory.categoryId,
                accountId = memory.accountId,
                amount = memory.amount,
                note = memory.note,
                savedAtEpochMillis = memory.savedAtEpochMillis
            )
        }
    }
}
