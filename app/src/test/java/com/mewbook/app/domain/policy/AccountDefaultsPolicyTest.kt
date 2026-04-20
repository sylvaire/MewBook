package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDefaultsPolicyTest {

    @Test
    fun resolveDefaultAccountId_returnsFirstDefaultAccountInCurrentOrder() {
        val accounts = listOf(
            account(id = 1L, name = "现金", isDefault = true),
            account(id = 2L, name = "银行卡", isDefault = true),
            account(id = 3L, name = "支付宝", isDefault = false)
        )

        val defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(accounts)

        assertEquals(1L, defaultAccountId)
    }

    @Test
    fun applyDefaultSelection_marksOnlySelectedAccountAsDefault() {
        val accounts = listOf(
            account(id = 1L, name = "现金", isDefault = true),
            account(id = 2L, name = "银行卡", isDefault = false),
            account(id = 3L, name = "支付宝", isDefault = true)
        )

        val updatedAccounts = AccountDefaultsPolicy.applyDefaultSelection(accounts, selectedAccountId = 2L)

        assertFalse(updatedAccounts.first { it.id == 1L }.isDefault)
        assertTrue(updatedAccounts.first { it.id == 2L }.isDefault)
        assertFalse(updatedAccounts.first { it.id == 3L }.isDefault)
    }

    private fun account(
        id: Long,
        name: String,
        isDefault: Boolean
    ) = Account(
        id = id,
        name = name,
        type = AccountType.BANK,
        balance = 0.0,
        icon = "account_balance",
        color = 0xFF2196F3,
        isDefault = isDefault,
        sortOrder = id.toInt(),
        ledgerId = 1L
    )
}
