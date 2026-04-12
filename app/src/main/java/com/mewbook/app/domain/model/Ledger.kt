package com.mewbook.app.domain.model

data class Ledger(
    val id: Long = 0,
    val name: String,
    val type: LedgerType,
    val icon: String,
    val color: Long,
    val createdAt: Long,
    val isDefault: Boolean = false
)

enum class LedgerType {
    PERSONAL,  // 个人账本
    FAMILY,   // 家庭账本
    AA        // AA账本
}

object DefaultLedgers {
    val personalLedger = Ledger(
        name = "我的账本",
        type = LedgerType.PERSONAL,
        icon = "person",
        color = 0xFF4CAF50,
        createdAt = System.currentTimeMillis(),
        isDefault = true
    )
}
