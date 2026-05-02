package com.mewbook.app.domain.usecase.account

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EnsureDefaultAccountForLedgerUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(ledgerId: Long) {
        val ledgerAccounts = accountRepository.getAccountsByLedger(ledgerId).first()
        if (ledgerAccounts.isEmpty()) {
            accountRepository.insertAccount(
                Account(
                    name = "现金",
                    type = AccountType.CASH,
                    balance = 0.0,
                    icon = "account_balance_wallet",
                    color = 0xFF4CAF50,
                    isDefault = true,
                    sortOrder = 0,
                    ledgerId = ledgerId
                )
            )
        }
    }
}
