package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccountsByLedger(ledgerId: Long): Flow<List<Account>>
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun updateBalance(id: Long, balance: Double)
}
