package com.mewbook.app.ui.components

import com.mewbook.app.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountTypeVisualsTest {
    @Test
    fun bankAccountUsesDistinctBankCardIconName() {
        assertEquals("bank_card", AccountType.BANK.defaultIconName())
        assertEquals("credit_card", AccountType.CREDIT_CARD.defaultIconName())
        assertNotEquals(AccountType.BANK.defaultIconName(), AccountType.CREDIT_CARD.defaultIconName())
    }
}
