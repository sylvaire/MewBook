package com.mewbook.app.domain.usecase.category

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: RecordType): Flow<List<Category>> {
        return categoryRepository.getCategoriesByType(type)
    }

    fun getAll(): Flow<List<Category>> {
        return categoryRepository.getAllCategories()
    }
}

class InitializeDefaultCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke() {
        val defaultCategories = DefaultCategories.all
        val existingCategories = categoryRepository.getAllCategoriesOnce()

        val existingKeys = existingCategories
            .map { it.name to it.type }
            .toSet()

        val missingCategories = defaultCategories.filterNot { category ->
            category.name to category.type in existingKeys
        }

        if (missingCategories.isNotEmpty()) {
            categoryRepository.insertCategories(missingCategories)
        }

        val categoriesAfterInsert = categoryRepository.getAllCategoriesOnce()
        syncDefaultCategories(
            existingCategories = categoriesAfterInsert,
            expectedCategories = defaultCategories
        )
    }

    private suspend fun syncDefaultCategories(
        existingCategories: List<Category>,
        expectedCategories: List<Category>
    ) {
        val existingByKey = existingCategories.associateBy { categoryKey(it) }
        expectedCategories.forEach { expected ->
            val existing = existingByKey[categoryKey(expected)] ?: return@forEach
            if (!existing.isDefault) return@forEach

            val needsUpdate = existing.icon != expected.icon ||
                existing.color != expected.color

            if (needsUpdate) {
                categoryRepository.updateCategory(
                    existing.copy(
                        icon = expected.icon,
                        color = expected.color
                    )
                )
            }
        }
    }

    private fun categoryKey(category: Category): String {
        return "${category.type.name}:${category.name}"
    }
}

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Long {
        return categoryRepository.insertCategory(category)
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        categoryRepository.updateCategory(category)
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        categoryRepository.deleteCategory(category)
    }
}

class ReorderCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(updates: List<Pair<Long, Int>>) {
        val allCategories = categoryRepository.getAllCategoriesOnce()
        val sortOrderMap = updates.toMap()
        val updatedCategories = allCategories
            .filter { it.id in sortOrderMap }
            .map { it.copy(sortOrder = sortOrderMap[it.id]!!) }
        if (updatedCategories.isNotEmpty()) {
            categoryRepository.updateCategories(updatedCategories)
        }
    }
}
