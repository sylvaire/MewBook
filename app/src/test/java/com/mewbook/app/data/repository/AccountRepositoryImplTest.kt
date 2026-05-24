package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRepositoryImplTest {

    @Test
    fun updateBalance_roundsBinaryFloatingPointNoiseToCurrencyScale() = runBlocking {
        val dao = FakeAccountDao()
        val repository = AccountRepositoryImpl(dao)

        repository.updateBalance(id = 1L, balance = 898.19 - 0.01)

        assertEquals(898.18, requireNotNull(dao.lastUpdatedBalance), 0.0)
    }
}

private class FakeAccountDao : AccountDao {
    var lastUpdatedBalance: Double? = null
    private val accounts = MutableStateFlow<List<AccountEntity>>(emptyList())

    override fun getAccountsByLedger(ledgerId: Long): Flow<List<AccountEntity>> = accounts

    override fun getAllAccounts(): Flow<List<AccountEntity>> = accounts

    override suspend fun getAllAccountsOnce(): List<AccountEntity> = accounts.value

    override suspend fun getAccountById(id: Long): AccountEntity? {
        return accounts.value.firstOrNull { it.id == id }
    }

    override suspend fun insertAccount(account: AccountEntity): Long {
        accounts.value = accounts.value + account
        return account.id
    }

    override suspend fun insertAccounts(accounts: List<AccountEntity>) {
        this.accounts.value = this.accounts.value + accounts
    }

    override suspend fun updateAccount(account: AccountEntity) {
        accounts.value = accounts.value.map { if (it.id == account.id) account else it }
    }

    override suspend fun deleteAccount(account: AccountEntity) {
        accounts.value = accounts.value.filterNot { it.id == account.id }
    }

    override suspend fun deleteAllAccounts() {
        accounts.value = emptyList()
    }

    override suspend fun updateBalance(id: Long, balance: Double) {
        lastUpdatedBalance = balance
    }
}
