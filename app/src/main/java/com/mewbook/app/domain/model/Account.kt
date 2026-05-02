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
