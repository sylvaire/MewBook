package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.dao.RecurringTemplateDao
import com.mewbook.app.data.local.entity.RecurringTemplateEntity
import com.mewbook.app.data.local.entity.RecordEntity
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.database.MewBookDatabase
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import com.mewbook.app.domain.policy.RecurringTemplateAutoClosePolicy
import com.mewbook.app.domain.policy.RecurringTemplateSchedulePolicy
import com.mewbook.app.domain.repository.RecurringTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTemplateRepositoryImpl @Inject constructor(
    private val database: MewBookDatabase,
    private val recordDao: RecordDao,
    private val accountDao: AccountDao,
    private val recurringTemplateDao: RecurringTemplateDao
) : RecurringTemplateRepository {

    override fun getTemplatesByLedger(ledgerId: Long): Flow<List<RecurringTemplate>> {
        return recurringTemplateDao.getTemplatesByLedger(ledgerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDueTemplatesByLedger(ledgerId: Long, untilDate: LocalDate): Flow<List<RecurringTemplate>> {
        return recurringTemplateDao.getDueTemplatesByLedger(ledgerId, untilDate.toEpochDay()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTemplateById(id: Long): RecurringTemplate? {
        return recurringTemplateDao.getTemplateById(id)?.toDomain()
    }

    override suspend fun saveTemplate(template: RecurringTemplate): Long {
        return recurringTemplateDao.insertTemplate(template.toEntity())
    }

    override suspend fun deleteTemplate(template: RecurringTemplate) {
        recurringTemplateDao.deleteTemplate(template.toEntity())
    }

    override suspend fun generateOccurrence(templateId: Long): Result<Long> {
        return runCatching {
            val template = recurringTemplateDao.getTemplateById(templateId)
                ?: throw IllegalArgumentException("模板不存在")
            val domainTemplate = template.toDomain()
            require(RecurringTemplateSchedulePolicy.canProcessCurrentOccurrence(domainTemplate)) {
                "模板已停用或超过结束日期"
            }
            val now = LocalDateTime.now()
            val recordDate = domainTemplate.nextDueDate
            val nextDueDate = RecurringTemplateSchedulePolicy.advanceNextDueDate(
                currentDueDate = domainTemplate.nextDueDate,
                scheduleType = domainTemplate.scheduleType,
                intervalCount = domainTemplate.intervalCount
            )
            val stillEnabled = domainTemplate.endDate?.let { nextDueDate <= it } ?: true

            database.withTransaction {
                val accountBalances = loadAccountBalances()
                val recordId = recordDao.insertRecord(
                    createRecordEntity(
                        template = domainTemplate,
                        recordDate = recordDate,
                        eventEpochSecond = now.toEpochSecond(ZoneOffset.UTC)
                    )
                )
                applyAccountBalanceChange(
                    accountBalances = accountBalances,
                    accountId = domainTemplate.accountId,
                    type = domainTemplate.type,
                    amount = domainTemplate.amount
                )
                recurringTemplateDao.updateTemplate(
                    template.copy(
                        nextDueDate = nextDueDate.toEpochDay(),
                        lastGeneratedDate = recordDate.toEpochDay(),
                        isEnabled = stillEnabled,
                        updatedAt = now.toEpochSecond(ZoneOffset.UTC)
                    )
                )
                recordId
            }
        }
    }

    override suspend fun skipOccurrence(templateId: Long): Result<Boolean> {
        return runCatching {
            val template = recurringTemplateDao.getTemplateById(templateId)
                ?: throw IllegalArgumentException("模板不存在")
            val domainTemplate = template.toDomain()
            require(RecurringTemplateSchedulePolicy.canProcessCurrentOccurrence(domainTemplate)) {
                "模板已停用或超过结束日期"
            }
            val now = LocalDateTime.now()
            val nextDueDate = RecurringTemplateSchedulePolicy.advanceNextDueDate(
                currentDueDate = domainTemplate.nextDueDate,
                scheduleType = domainTemplate.scheduleType,
                intervalCount = domainTemplate.intervalCount
            )
            val stillEnabled = domainTemplate.endDate?.let { nextDueDate <= it } ?: true

            recurringTemplateDao.updateTemplate(
                template.copy(
                    nextDueDate = nextDueDate.toEpochDay(),
                    isEnabled = stillEnabled,
                    updatedAt = now.toEpochSecond(ZoneOffset.UTC)
                )
            )
            true
        }
    }

    override suspend fun autoCloseDueTemplates(referenceDate: LocalDate): Result<Int> {
        return runCatching {
            val templates = recurringTemplateDao.getAllTemplatesOnce()
            if (templates.isEmpty()) {
                return@runCatching 0
            }

            val nowEpochSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            var createdCount = 0

            database.withTransaction {
                val accountBalances = loadAccountBalances()
                templates.forEach { entity ->
                    val template = entity.toDomain()
                    val plan = RecurringTemplateAutoClosePolicy.buildPlan(
                        template = template,
                        referenceDate = referenceDate
                    )
                    val shouldUpdateTemplate =
                        plan.occurrenceDates.isNotEmpty() || plan.shouldRemainEnabled != template.isEnabled
                    if (!shouldUpdateTemplate) {
                        return@forEach
                    }

                    plan.occurrenceDates.forEach { recordDate ->
                        recordDao.insertRecord(
                            createRecordEntity(
                                template = template,
                                recordDate = recordDate,
                                eventEpochSecond = nowEpochSecond + createdCount
                            )
                        )
                        applyAccountBalanceChange(
                            accountBalances = accountBalances,
                            accountId = template.accountId,
                            type = template.type,
                            amount = template.amount
                        )
                        createdCount += 1
                    }

                    recurringTemplateDao.updateTemplate(
                        entity.copy(
                            nextDueDate = plan.nextDueDate.toEpochDay(),
                            lastGeneratedDate = plan.occurrenceDates.lastOrNull()?.toEpochDay()
                                ?: entity.lastGeneratedDate,
                            isEnabled = plan.shouldRemainEnabled,
                            updatedAt = nowEpochSecond + createdCount
                        )
                    )
                }
            }

            createdCount
        }
    }

    override suspend fun getAllTemplatesOnce(): List<RecurringTemplate> {
        return recurringTemplateDao.getAllTemplatesOnce().map { it.toDomain() }
    }

    override suspend fun deleteAllTemplates() {
        recurringTemplateDao.deleteAllTemplates()
    }

    override suspend fun saveTemplates(templates: List<RecurringTemplate>) {
        recurringTemplateDao.insertTemplates(templates.map { it.toEntity() })
    }

    private suspend fun loadAccountBalances(): MutableMap<Long, Double> {
        return accountDao.getAllAccountsOnce()
            .associate { it.id to it.balance }
            .toMutableMap()
    }

    private suspend fun applyAccountBalanceChange(
        accountBalances: MutableMap<Long, Double>,
        accountId: Long?,
        type: RecordType,
        amount: Double
    ) {
        val resolvedAccountId = accountId ?: return
        val currentBalance = accountBalances[resolvedAccountId] ?: return
        val nextBalance = if (type == RecordType.INCOME) {
            currentBalance + amount
        } else {
            currentBalance - amount
        }
        accountBalances[resolvedAccountId] = nextBalance
        accountDao.updateBalance(resolvedAccountId, nextBalance)
    }

    private fun createRecordEntity(
        template: RecurringTemplate,
        recordDate: LocalDate,
        eventEpochSecond: Long
    ): RecordEntity {
        return RecordEntity(
            amount = template.amount,
            type = template.type.name,
            categoryId = template.categoryId,
            note = template.noteTemplate,
            date = recordDate.toEpochDay(),
            createdAt = eventEpochSecond,
            updatedAt = eventEpochSecond,
            syncId = UUID.randomUUID().toString(),
            ledgerId = template.ledgerId,
            accountId = template.accountId
        )
    }

    private fun RecurringTemplateEntity.toDomain(): RecurringTemplate {
        return RecurringTemplate(
            id = id,
            name = name,
            amount = amount,
            type = RecordType.valueOf(type),
            categoryId = categoryId,
            noteTemplate = noteTemplate,
            ledgerId = ledgerId,
            accountId = accountId,
            scheduleType = RecurringTemplateScheduleType.valueOf(scheduleType),
            intervalCount = intervalCount,
            startDate = LocalDate.ofEpochDay(startDate),
            nextDueDate = LocalDate.ofEpochDay(nextDueDate),
            endDate = endDate?.let(LocalDate::ofEpochDay),
            isEnabled = isEnabled,
            reminderEnabled = reminderEnabled,
            lastGeneratedDate = lastGeneratedDate?.let(LocalDate::ofEpochDay),
            createdAt = LocalDateTime.ofEpochSecond(createdAt, 0, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofEpochSecond(updatedAt, 0, ZoneOffset.UTC)
        )
    }

    private fun RecurringTemplate.toEntity(): RecurringTemplateEntity {
        val nowCreatedAt = createdAt.toEpochSecond(ZoneOffset.UTC)
        val nowUpdatedAt = updatedAt.toEpochSecond(ZoneOffset.UTC)
        return RecurringTemplateEntity(
            id = id,
            name = name,
            amount = amount,
            type = type.name,
            categoryId = categoryId,
            noteTemplate = noteTemplate,
            ledgerId = ledgerId,
            accountId = accountId,
            scheduleType = scheduleType.name,
            intervalCount = intervalCount.coerceAtLeast(1),
            startDate = startDate.toEpochDay(),
            nextDueDate = nextDueDate.toEpochDay(),
            endDate = endDate?.toEpochDay(),
            isEnabled = isEnabled,
            reminderEnabled = reminderEnabled,
            lastGeneratedDate = lastGeneratedDate?.toEpochDay(),
            createdAt = nowCreatedAt,
            updatedAt = nowUpdatedAt
        )
    }
}
