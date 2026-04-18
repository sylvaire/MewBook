package com.mewbook.app.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val icon: String,
    val color: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val ledgerId: Long
)

enum class AccountType {
    CASH,           // 现金
    BANK,           // 银行卡
    ALIPAY,         // 支付宝
    WECHAT,         // 微信
    CREDIT_CARD,    // 信用卡
    INVESTMENT,     // 投资账户
    OTHER           // 其他
}

object DefaultAccounts {
    val defaultAccounts = listOf(
        Account(name = "现金", type = AccountType.CASH, balance = 0.0, icon = "account_balance_wallet", color = 0xFF4CAF50, isDefault = true, sortOrder = 0, ledgerId = 1),
        Account(name = "银行卡", type = AccountType.BANK, balance = 0.0, icon = "account_balance", color = 0xFF2196F3, isDefault = true, sortOrder = 1, ledgerId = 1),
        Account(name = "支付宝", type = AccountType.ALIPAY, balance = 0.0, icon = "alipay", color = 0xFF1890FF, isDefault = true, sortOrder = 2, ledgerId = 1),
        Account(name = "微信", type = AccountType.WECHAT, balance = 0.0, icon = "wechat", color = 0xFF07C160, isDefault = true, sortOrder = 3, ledgerId = 1)
    )
}
