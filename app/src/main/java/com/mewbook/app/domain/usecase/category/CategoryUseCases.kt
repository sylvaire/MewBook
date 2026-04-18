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
        val defaultParentCategories = DefaultCategories.expenseCategories + DefaultCategories.incomeCategories
        val existingCategories = categoryRepository.getAllCategoriesOnce()

        val existingParentKeys = existingCategories
            .filter { it.parentId == null }
            .map { Triple(it.name, it.type, it.parentId) }
            .toSet()

        val missingParents = defaultParentCategories.filterNot { parent ->
            Triple(parent.name, parent.type, parent.parentId) in existingParentKeys
        }

        if (missingParents.isNotEmpty()) {
            categoryRepository.insertCategories(missingParents)
        }

        val categoriesAfterParentInsert = categoryRepository.getAllCategoriesOnce()
        syncDefaultCategories(
            existingCategories = categoriesAfterParentInsert,
            expectedCategories = defaultParentCategories
        )

        val refreshedCategories = categoryRepository.getAllCategoriesOnce()
        val parentMap = refreshedCategories
            .filter { it.parentId == null }
            .associateBy { "${it.type.name}:${it.name}" }

        val subToParentMap = mapOf(
            "早餐" to "餐饮", "午餐" to "餐饮", "晚餐" to "餐饮", "零食" to "餐饮",
            "下午茶" to "餐饮", "外卖" to "餐饮", "水果" to "餐饮", "奶茶" to "餐饮",
            "咖啡" to "餐饮", "茶饮" to "餐饮", "肉类" to "餐饮", "蔬菜" to "餐饮",
            "公交" to "交通", "地铁" to "交通", "打车" to "交通", "油费" to "交通",
            "停车" to "交通", "火车" to "交通", "飞机" to "交通",
            "服装" to "购物", "数码" to "购物", "日用品" to "购物", "化妆品" to "购物",
            "母婴" to "购物", "家电" to "购物",
            "房租" to "居住", "水电费" to "居住", "物业费" to "居住", "燃气" to "居住", "话费" to "居住",
            "电影" to "娱乐", "游戏" to "娱乐", "旅游" to "娱乐", "健身" to "娱乐",
            "演唱会" to "娱乐", "ktv" to "娱乐",
            "手机话费" to "通讯", "宽带费" to "通讯",
            "健身房" to "运动健身", "运动装备" to "运动健身", "游泳" to "运动健身",
            "宠物食品" to "宠物", "宠物医疗" to "宠物", "宠物用品" to "宠物",
            "红包" to "人情往来", "礼物" to "人情往来", "请客" to "人情往来",
            "书籍" to "书籍文具", "文具" to "书籍文具", "电子书" to "书籍文具"
        )

        val expectedSubCategories = DefaultCategories.subCategories.mapNotNull { sub ->
            val parentName = subToParentMap[sub.name] ?: return@mapNotNull null
            val parentId = parentMap["${sub.type.name}:$parentName"]?.id ?: return@mapNotNull null
            sub.copy(parentId = parentId)
        }

        val existingSubKeys = refreshedCategories
            .filter { it.parentId != null }
            .map { Triple(it.name, it.type, it.parentId) }
            .toSet()

        val missingSubCategories = expectedSubCategories.filterNot { sub ->
            Triple(sub.name, sub.type, sub.parentId) in existingSubKeys
        }

        if (missingSubCategories.isNotEmpty()) {
            categoryRepository.insertCategories(missingSubCategories)
        }

        val categoriesAfterSubInsert = categoryRepository.getAllCategoriesOnce()
        syncDefaultCategories(
            existingCategories = categoriesAfterSubInsert,
            expectedCategories = expectedSubCategories
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
                existing.color != expected.color ||
                existing.parentId != expected.parentId

            if (needsUpdate) {
                categoryRepository.updateCategory(
                    existing.copy(
                        icon = expected.icon,
                        color = expected.color,
                        parentId = expected.parentId
                    )
                )
            }
        }
    }

    private fun categoryKey(category: Category): String {
        return "${category.type.name}:${category.parentId ?: "root"}:${category.name}"
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
