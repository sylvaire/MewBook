package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.CategoryDao
import com.mewbook.app.data.local.entity.CategoryEntity
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategoriesByType(type: RecordType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllCategoriesOnce(): List<Category> {
        return categoryDao.getAllCategoriesOnce().map { it.toDomain() }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun getDefaultCategories(): List<Category> {
        return categoryDao.getDefaultCategories().map { it.toDomain() }
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun getCategoryCount(): Int {
        return categoryDao.getCategoryCount()
    }

    private fun CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            type = RecordType.valueOf(type),
            isDefault = isDefault,
            sortOrder = sortOrder,
            parentId = parentId
        )
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            icon = icon,
            color = color,
            type = type.name,
            isDefault = isDefault,
            sortOrder = sortOrder,
            parentId = parentId
        )
    }
}
