package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["parentId"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val parentId: Long? = null  // null表示一级分类，非null表示二级分类
)
