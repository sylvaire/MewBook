package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringTemplateRepository {
    fun getTemplatesByLedger(ledgerId: Long): Flow<List<RecurringTemplate>>
    fun getDueTemplatesByLedger(ledgerId: Long, untilDate: LocalDate): Flow<List<RecurringTemplate>>
    suspend fun getTemplateById(id: Long): RecurringTemplate?
    suspend fun saveTemplate(template: RecurringTemplate): Long
    suspend fun deleteTemplate(template: RecurringTemplate)
    suspend fun generateOccurrence(templateId: Long): Result<Long>
    suspend fun skipOccurrence(templateId: Long): Result<Boolean>
    suspend fun autoCloseDueTemplates(referenceDate: LocalDate = LocalDate.now()): Result<Int>
    suspend fun getAllTemplatesOnce(): List<RecurringTemplate>
    suspend fun deleteAllTemplates()
    suspend fun saveTemplates(templates: List<RecurringTemplate>)
}
