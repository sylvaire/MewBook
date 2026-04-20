package com.mewbook.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["ledgerId", "categoryId", "periodType", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?,     // null表示总预算，非null表示分类预算
    @ColumnInfo(name = "month")
    val periodKey: String,
    val periodType: String = "MONTH",
    val amount: Double,
    val ledgerId: Long         // 关联账本
)
