package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mewbook.app.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {

    @Query("SELECT * FROM recurring_templates WHERE ledgerId = :ledgerId ORDER BY isEnabled DESC, nextDueDate ASC, updatedAt DESC")
    fun getTemplatesByLedger(ledgerId: Long): Flow<List<RecurringTemplateEntity>>

    @Query(
        "SELECT * FROM recurring_templates WHERE ledgerId = :ledgerId AND isEnabled = 1 AND nextDueDate <= :untilEpochDay " +
            "ORDER BY nextDueDate ASC, updatedAt DESC"
    )
    fun getDueTemplatesByLedger(ledgerId: Long, untilEpochDay: Long): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates ORDER BY ledgerId ASC, isEnabled DESC, nextDueDate ASC, updatedAt DESC")
    suspend fun getAllTemplatesOnce(): List<RecurringTemplateEntity>

    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): RecurringTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<RecurringTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: RecurringTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: RecurringTemplateEntity)

    @Query("DELETE FROM recurring_templates")
    suspend fun deleteAllTemplates()
}
