package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account

object AccountNamingPolicy {

    fun hasDuplicateNameInLedger(
        accounts: List<Account>,
        ledgerId: Long,
        candidateName: String,
        excludeAccountId: Long? = null
    ): Boolean {
        val normalizedName = candidateName.trim()
        if (normalizedName.isBlank()) {
            return false
        }

        return accounts.any { account ->
            account.ledgerId == ledgerId &&
                account.id != excludeAccountId &&
                account.name.trim() == normalizedName
        }
    }
}
