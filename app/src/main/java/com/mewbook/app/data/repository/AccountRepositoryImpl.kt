package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.entity.AccountEntity
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAccountsByLedger(ledgerId: Long): Flow<List<Account>> {
        return accountDao.getAccountsByLedger(ledgerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun updateBalance(id: Long, balance: Double) {
        accountDao.updateBalance(id, balance)
    }

    private fun AccountEntity.toDomain(): Account {
        return Account(
            id = id,
            name = name,
            type = AccountType.valueOf(type),
            balance = balance,
            icon = icon,
            color = color,
            isDefault = isDefault,
            sortOrder = sortOrder,
            ledgerId = ledgerId
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            type = type.name,
            balance = balance,
            icon = icon,
            color = color,
            isDefault = isDefault,
            sortOrder = sortOrder,
            ledgerId = ledgerId
        )
    }
}
