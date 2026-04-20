package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account

object AccountDefaultsPolicy {

    fun resolveDefaultAccountId(accounts: List<Account>): Long? {
        return accounts.firstOrNull { it.isDefault }?.id
    }

    fun applyDefaultSelection(
        accounts: List<Account>,
        selectedAccountId: Long?
    ): List<Account> {
        return accounts.map { account ->
            account.copy(isDefault = selectedAccountId != null && account.id == selectedAccountId)
        }
    }
}
