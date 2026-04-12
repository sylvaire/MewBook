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
        val count = categoryRepository.getCategoryCount()
        if (count == 0) {
            // First insert parent categories
            val parentCategories = DefaultCategories.expenseCategories + DefaultCategories.incomeCategories
            categoryRepository.insertCategories(parentCategories)

            // Get all categories to find parent IDs - need to get fresh data after insert
            val allCategories = categoryRepository.getAllCategoriesOnce()
            val parentMap = allCategories.associateBy { it.name }

            // Build a mapping of subcategory name to parent name
            val subToParentMap = mapOf(
                // 餐饮子分类 -> 餐饮
                "早餐" to "餐饮", "午餐" to "餐饮", "晚餐" to "餐饮", "零食" to "餐饮",
                "下午茶" to "餐饮", "外卖" to "餐饮", "水果" to "餐饮", "奶茶" to "餐饮",
                "咖啡" to "餐饮", "茶饮" to "餐饮", "肉类" to "餐饮", "蔬菜" to "餐饮",
                // 交通子分类 -> 交通
                "公交" to "交通", "地铁" to "交通", "打车" to "交通", "油费" to "交通",
                "停车" to "交通", "火车" to "交通", "飞机" to "交通",
                // 购物子分类 -> 购物
                "服装" to "购物", "数码" to "购物", "日用品" to "购物", "化妆品" to "购物",
                "母婴" to "购物", "家电" to "购物",
                // 居住子分类 -> 居住
                "房租" to "居住", "水电费" to "居住", "物业费" to "居住", "燃气" to "居住", "话费" to "居住",
                // 娱乐子分类 -> 娱乐
                "电影" to "娱乐", "游戏" to "娱乐", "旅游" to "娱乐", "健身" to "娱乐",
                "演唱会" to "娱乐", "ktv" to "娱乐",
                // 通讯子分类 -> 通讯
                "手机话费" to "通讯", "宽带费" to "通讯",
                // 运动健身子分类 -> 运动健身
                "健身房" to "运动健身", "运动装备" to "运动健身", "游泳" to "运动健身",
                // 宠物子分类 -> 宠物
                "宠物食品" to "宠物", "宠物医疗" to "宠物", "宠物用品" to "宠物",
                // 人情往来子分类 -> 人情往来
                "红包" to "人情往来", "礼物" to "人情往来", "请客" to "人情往来",
                // 书籍文具子分类 -> 书籍文具
                "书籍" to "书籍文具", "文具" to "书籍文具", "电子书" to "书籍文具"
            )

            // Create subCategories with proper parentId
            val subCategories = DefaultCategories.subCategories.mapNotNull { sub ->
                val parentName = subToParentMap[sub.name]
                if (parentName != null) {
                    val parentId = parentMap[parentName]?.id
                    if (parentId != null) {
                        sub.copy(parentId = parentId)
                    } else {
                        null // Skip if parent not found
                    }
                } else {
                    null // Skip if not in mapping
                }
            }

            // Insert subcategories
            categoryRepository.insertCategories(subCategories)
        }
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
