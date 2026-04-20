package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountNamingPolicyTest {

    @Test
    fun hasDuplicateNameInLedger_returnsTrueOnlyForSameLedger() {
        val accounts = listOf(
            account(id = 1L, name = "招商银行", ledgerId = 1L),
            account(id = 2L, name = "招商银行", ledgerId = 2L),
            account(id = 3L, name = "支付宝", ledgerId = 1L)
        )

        assertTrue(
            AccountNamingPolicy.hasDuplicateNameInLedger(
                accounts = accounts,
                ledgerId = 1L,
                candidateName = " 招商银行 "
            )
        )
        assertFalse(
            AccountNamingPolicy.hasDuplicateNameInLedger(
                accounts = accounts,
                ledgerId = 3L,
                candidateName = "招商银行"
            )
        )
    }

    private fun account(
        id: Long,
        name: String,
        ledgerId: Long
    ) = Account(
        id = id,
        name = name,
        type = AccountType.BANK,
        balance = 0.0,
        icon = "account_balance",
        color = 0xFF2196F3,
        isDefault = false,
        sortOrder = id.toInt(),
        ledgerId = ledgerId
    )
}
