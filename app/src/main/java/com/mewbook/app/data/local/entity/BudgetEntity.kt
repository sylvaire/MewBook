package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?,     // null表示总预算，非null表示分类预算
    val month: String,         // 格式: "yyyy-MM"
    val amount: Double,
    val ledgerId: Long         // 关联账本
)
