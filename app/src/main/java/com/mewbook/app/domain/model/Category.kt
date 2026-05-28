package com.mewbook.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: RecordType,
    val isDefault: Boolean,
    val sortOrder: Int
)

object DefaultCategories {
    private val baseExpenseCategories = listOf(
        Category(name = "餐饮", icon = "restaurant", color = 0xFFFF6B6B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "交通", icon = "directions_bus", color = 0xFF4ECDC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "购物", icon = "local_mall", color = 0xFFFFE66D, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "居住", icon = "home", color = 0xFF95E1D3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "娱乐", icon = "sports_esports", color = 0xFFAA96DA, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "旅行", icon = "flight", color = 0xFF4DB6AC, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),
        Category(name = "医疗", icon = "medical_services", color = 0xFFF38181, type = RecordType.EXPENSE, isDefault = true, sortOrder = 6),
        Category(name = "教育", icon = "school", color = 0xFF7C83FD, type = RecordType.EXPENSE, isDefault = true, sortOrder = 7),
        Category(name = "通讯", icon = "phone_android", color = 0xFF45B7D1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 8),
        Category(name = "运动健身", icon = "directions_run", color = 0xFF4CAF50, type = RecordType.EXPENSE, isDefault = true, sortOrder = 9),
        Category(name = "宠物", icon = "pets", color = 0xFFFF9F43, type = RecordType.EXPENSE, isDefault = true, sortOrder = 10),
        Category(name = "美妆", icon = "brush", color = 0xFFFF8FB1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 11),
        Category(name = "母婴", icon = "baby_changing_station", color = 0xFFFFB6C1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 12),
        Category(name = "办公", icon = "work", color = 0xFF90A4AE, type = RecordType.EXPENSE, isDefault = true, sortOrder = 13),
        Category(name = "烟酒", icon = "local_bar", color = 0xFFB8860B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 14),
        Category(name = "人情往来", icon = "people", color = 0xFFFF69B4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 15),
        Category(name = "书籍文具", icon = "menu_book", color = 0xFF5D4037, type = RecordType.EXPENSE, isDefault = true, sortOrder = 16),
        Category(name = "虚拟产品", icon = "cloud", color = 0xFF9C27B0, type = RecordType.EXPENSE, isDefault = true, sortOrder = 17),
        Category(name = "日用", icon = "cleaning_services", color = 0xFFA1887F, type = RecordType.EXPENSE, isDefault = true, sortOrder = 18),
        Category(name = "饮料", icon = "local_drink", color = 0xFF64B5F6, type = RecordType.EXPENSE, isDefault = true, sortOrder = 19),
        Category(name = "水果", icon = "apple", color = 0xFF81C784, type = RecordType.EXPENSE, isDefault = true, sortOrder = 20),
        Category(name = "药品", icon = "medication", color = 0xFFE57373, type = RecordType.EXPENSE, isDefault = true, sortOrder = 21),
        Category(name = "保险", icon = "health_and_safety", color = 0xFF90CAF9, type = RecordType.EXPENSE, isDefault = true, sortOrder = 22),
        Category(name = "零食", icon = "cookie", color = 0xFFFFCC80, type = RecordType.EXPENSE, isDefault = true, sortOrder = 23),
        Category(name = "还款", icon = "payments", color = 0xFF80CBC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 24),
        Category(name = "理财", icon = "savings", color = 0xFFA5D6A7, type = RecordType.EXPENSE, isDefault = true, sortOrder = 25),
        Category(name = "运动", icon = "directions_run", color = 0xFF66BB6A, type = RecordType.EXPENSE, isDefault = true, sortOrder = 26),
        Category(name = "服饰", icon = "checkroom", color = 0xFFB39DDB, type = RecordType.EXPENSE, isDefault = true, sortOrder = 27),
        Category(name = "居家", icon = "weekend", color = 0xFFBCAAA4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 28),
        Category(name = "快递", icon = "local_shipping", color = 0xFFB0BEC5, type = RecordType.EXPENSE, isDefault = true, sortOrder = 29),
        Category(name = "孩子", icon = "boy", color = 0xFFFFAB91, type = RecordType.EXPENSE, isDefault = true, sortOrder = 30),
        Category(name = "社交", icon = "forum", color = 0xFFF48FB1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 31),
        Category(name = "学习", icon = "auto_stories", color = 0xFF9FA8DA, type = RecordType.EXPENSE, isDefault = true, sortOrder = 32),
        Category(name = "礼金", icon = "attach_money", color = 0xFFFFD180, type = RecordType.EXPENSE, isDefault = true, sortOrder = 33),
        Category(name = "礼物", icon = "card_giftcard", color = 0xFFE1BEE7, type = RecordType.EXPENSE, isDefault = true, sortOrder = 34),
        Category(name = "其他", icon = "more_horiz", color = 0xFF9E9E9E, type = RecordType.EXPENSE, isDefault = true, sortOrder = 35)
    )

    val expenseCategories = normalizeSortOrder(baseExpenseCategories)

    val incomeCategories = listOf(
        Category(name = "工资", icon = "payments", color = 0xFF4CAF50, type = RecordType.INCOME, isDefault = true, sortOrder = 0),
        Category(name = "奖金", icon = "card_giftcard", color = 0xFFFFB6B9, type = RecordType.INCOME, isDefault = true, sortOrder = 1),
        Category(name = "投资收益", icon = "trending_up", color = 0xFFB5EAD7, type = RecordType.INCOME, isDefault = true, sortOrder = 2),
        Category(name = "兼职", icon = "work", color = 0xFFA8D8EA, type = RecordType.INCOME, isDefault = true, sortOrder = 3),
        Category(name = "理财收益", icon = "savings", color = 0xFF81C784, type = RecordType.INCOME, isDefault = true, sortOrder = 4),
        Category(name = "报销", icon = "receipt_long", color = 0xFFFFD180, type = RecordType.INCOME, isDefault = true, sortOrder = 5),
        Category(name = "退款", icon = "payments", color = 0xFF80CBC4, type = RecordType.INCOME, isDefault = true, sortOrder = 6),
        Category(name = "其他", icon = "attach_money", color = 0xFFC7CEEA, type = RecordType.INCOME, isDefault = true, sortOrder = 7)
    )

    val all = expenseCategories + incomeCategories

    private fun normalizeSortOrder(categories: List<Category>): List<Category> {
        return categories
            .distinctBy { it.type to it.name }
            .mapIndexed { index, category -> category.copy(sortOrder = index) }
    }
}
