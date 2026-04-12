package com.mewbook.app.domain.usecase.account

import com.mewbook.app.domain.model.DefaultAccounts
import com.mewbook.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InitializeDefaultAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke() {
        val existingAccounts = accountRepository.getAllAccounts().first()
        if (existingAccounts.isEmpty()) {
            DefaultAccounts.defaultAccounts.forEach { account ->
                accountRepository.insertAccount(account)
            }
        }
    }
}
