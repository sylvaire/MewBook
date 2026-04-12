package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,          // CASH, BANK, ALIPAY, WECHAT, CREDIT_CARD, INVESTMENT, OTHER
    val balance: Double,
    val icon: String,
    val color: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val ledgerId: Long          // 关联账本
)
