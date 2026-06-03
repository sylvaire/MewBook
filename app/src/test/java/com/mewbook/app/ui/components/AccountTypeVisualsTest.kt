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

    @Test
    fun accountIconOptionsIncludePaymentAndBankCardChoices() {
        val iconNames = accountIconOptions().map { it.name }

        assertEquals(iconNames.distinct(), iconNames)
        assertEquals(
            listOf("bank_card", "bank_card_chip", "bank_card_contactless", "bank_card_branch"),
            iconNames.filter { it.startsWith("bank_card") }
        )
        assert("alipay" in iconNames)
        assert("wechat" in iconNames)
        assert("payments" in iconNames)
        assert("qr_code_scanner" in iconNames)
    }

    @Test
    fun defaultAccountIconsAreSelectable() {
        val iconNames = accountIconOptions().map { it.name }.toSet()

        AccountType.entries.forEach { type ->
            assert(type.defaultIconName() in iconNames) {
                "${type.name} default icon ${type.defaultIconName()} should be selectable"
            }
        }
    }

    @Test
    fun normalizeAccountIconNameFallsBackToTypeDefault() {
        assertEquals("bank_card", normalizeAccountIconName("bank_card_contactless", AccountType.BANK))
        assertEquals("bank_card", normalizeAccountIconName("missing_icon", AccountType.BANK))
        assertEquals("wechat", normalizeAccountIconName("", AccountType.WECHAT))
    }

    @Test
    fun onlyOtherAccountsCanUseCustomIcons() {
        assertEquals("account_balance_wallet", normalizeAccountIconName("payments", AccountType.CASH))
        assertEquals("bank_card", normalizeAccountIconName("bank_card_contactless", AccountType.BANK))
        assertEquals("alipay", normalizeAccountIconName("wechat", AccountType.ALIPAY))
        assertEquals("credit_card", normalizeAccountIconName("bank_card_chip", AccountType.CREDIT_CARD))
        assertEquals("savings", normalizeAccountIconName("qr_code_scanner", AccountType.INVESTMENT))

        assertEquals("bank_card_contactless", normalizeAccountIconName("bank_card_contactless", AccountType.OTHER))
        assertEquals("wechat", normalizeAccountIconName("wechat", AccountType.OTHER))
        assertEquals("more_horiz", normalizeAccountIconName("missing_icon", AccountType.OTHER))
    }
}
