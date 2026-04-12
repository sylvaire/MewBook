package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesByType(type: RecordType): Flow<List<Category>>
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getAllCategoriesOnce(): List<Category>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun getDefaultCategories(): List<Category>
    suspend fun insertCategory(category: Category): Long
    suspend fun insertCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryCount(): Int
}
