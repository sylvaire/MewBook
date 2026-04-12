package com.mewbook.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: RecordType,
    val isDefault: Boolean,
    val sortOrder: Int,
    val parentId: Long? = null  // null表示一级分类
)

object DefaultCategories {
    // 一级分类 - 支出（扩展更多常见分类）
    val expenseCategories = listOf(
        Category(name = "餐饮", icon = "restaurant", color = 0xFFFF6B6B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "交通", icon = "directions_car", color = 0xFF4ECDC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "购物", icon = "shopping_bag", color = 0xFFFFE66D, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "居住", icon = "home", color = 0xFF95E1D3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "娱乐", icon = "sports_esports", color = 0xFFAA96DA, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "医疗", icon = "medical_services", color = 0xFFF38181, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),
        Category(name = "教育", icon = "school", color = 0xFF7C83FD, type = RecordType.EXPENSE, isDefault = true, sortOrder = 6),
        Category(name = "通讯", icon = "phone_android", color = 0xFF45B7D1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 7),
        Category(name = "运动健身", icon = "fitness_center", color = 0xFF4CAF50, type = RecordType.EXPENSE, isDefault = true, sortOrder = 8),
        Category(name = "宠物", icon = "pets", color = 0xFFFF9F43, type = RecordType.EXPENSE, isDefault = true, sortOrder = 9),
        Category(name = "烟酒", icon = "local_bar", color = 0xFFB8860B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 10),
        Category(name = "人情往来", icon = "people", color = 0xFFFF69B4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 11),
        Category(name = "书籍文具", icon = "menu_book", color = 0xFF5D4037, type = RecordType.EXPENSE, isDefault = true, sortOrder = 12),
        Category(name = "虚拟产品", icon = "inbox", color = 0xFF9C27B0, type = RecordType.EXPENSE, isDefault = true, sortOrder = 13),
        Category(name = "其他", icon = "more_horiz", color = 0xFF9E9E9E, type = RecordType.EXPENSE, isDefault = true, sortOrder = 14)
    )

    val incomeCategories = listOf(
        Category(name = "工资", icon = "payments", color = 0xFF4CAF50, type = RecordType.INCOME, isDefault = true, sortOrder = 0),
        Category(name = "奖金", icon = "card_giftcard", color = 0xFFFFB6B9, type = RecordType.INCOME, isDefault = true, sortOrder = 1),
        Category(name = "投资收益", icon = "trending_up", color = 0xFFB5EAD7, type = RecordType.INCOME, isDefault = true, sortOrder = 2),
        Category(name = "兼职", icon = "work", color = 0xFFA8D8EA, type = RecordType.INCOME, isDefault = true, sortOrder = 3),
        Category(name = "理财收益", icon = "account_balance", color = 0xFF81C784, type = RecordType.INCOME, isDefault = true, sortOrder = 4),
        Category(name = "其他", icon = "attach_money", color = 0xFFC7CEEA, type = RecordType.INCOME, isDefault = true, sortOrder = 5)
    )

    // 二级分类（会关联到对应的一级分类ID）
    val subCategories = listOf(
        // 餐饮子分类
        Category(name = "早餐", icon = "free_breakfast", color = 0xFFFF9F43, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "午餐", icon = "lunch_dining", color = 0xFFFF6B6B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "晚餐", icon = "dinner_dining", color = 0xFFE74C3C, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "零食", icon = "cookie", color = 0xFFFFE66D, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "下午茶", icon = "local_cafe", color = 0xFFBCA7E5, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "外卖", icon = "takeout_dining", color = 0xFF4ECDC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),
        Category(name = "水果", icon = "nutrition", color = 0xFFFF6347, type = RecordType.EXPENSE, isDefault = true, sortOrder = 6),
        Category(name = "奶茶", icon = "local_drink", color = 0xFFFF69B4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 7),
        Category(name = "咖啡", icon = "local_cafe", color = 0xFF8B4513, type = RecordType.EXPENSE, isDefault = true, sortOrder = 8),
        Category(name = "茶饮", icon = "emoji_food_beverage", color = 0xFF8BC34A, type = RecordType.EXPENSE, isDefault = true, sortOrder = 9),
        Category(name = "肉类", icon = "restaurant", color = 0xFFE57373, type = RecordType.EXPENSE, isDefault = true, sortOrder = 10),
        Category(name = "蔬菜", icon = "eco", color = 0xFF81C784, type = RecordType.EXPENSE, isDefault = true, sortOrder = 11),

        // 交通子分类
        Category(name = "公交", icon = "directions_bus", color = 0xFF4ECDC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "地铁", icon = "train", color = 0xFF45B7D1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "打车", icon = "local_taxi", color = 0xFFF38181, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "油费", icon = "local_gas_station", color = 0xFFFFC107, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "停车", icon = "local_parking", color = 0xFF95A3A4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "火车", icon = "train", color = 0xFF795548, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),
        Category(name = "飞机", icon = "flight", color = 0xFF2196F3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 6),

        // 购物子分类
        Category(name = "服装", icon = "checkroom", color = 0xFFFFE66D, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "数码", icon = "devices", color = 0xFF7C83FD, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "日用品", icon = "cleaning_services", color = 0xFF95E1D3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "化妆品", icon = "face", color = 0xFFFCBAD3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "母婴", icon = "child_care", color = 0xFFFFB6C1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "家电", icon = "kitchen", color = 0xFF607D8B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),

        // 居住子分类
        Category(name = "房租", icon = "house", color = 0xFF95E1D3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "水电费", icon = "water_drop", color = 0xFF45B7D1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "物业费", icon = "manage_accounts", color = 0xFFAA96DA, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "燃气", icon = "local_fire_department", color = 0xFFFF5722, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "话费", icon = "phone_android", color = 0xFF4CAF50, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),

        // 娱乐子分类
        Category(name = "电影", icon = "movie", color = 0xFFAA96DA, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "游戏", icon = "sports_esports", color = 0xFF7C83FD, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "旅游", icon = "flight", color = 0xFF4ECDC4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "健身", icon = "fitness_center", color = 0xFFFF6B6B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "演唱会", icon = "music_note", color = 0xFFFF4081, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4),
        Category(name = "ktv", icon = "mic", color = 0xFF9C27B0, type = RecordType.EXPENSE, isDefault = true, sortOrder = 5),

        // 通讯子分类
        Category(name = "手机话费", icon = "phone_android", color = 0xFF45B7D1, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "宽带费", icon = "wifi", color = 0xFF2196F3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),

        // 运动健身子分类
        Category(name = "健身房", icon = "fitness_center", color = 0xFF4CAF50, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "运动装备", icon = "sports_basketball", color = 0xFFFF9800, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "游泳", icon = "pool", color = 0xFF00BCD4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),

        // 宠物子分类
        Category(name = "宠物食品", icon = "pets", color = 0xFFFF9F43, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "宠物医疗", icon = "local_hospital", color = 0xFFF38181, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "宠物用品", icon = "shopping_bag", color = 0xFFFFE66D, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),

        // 人情往来子分类
        Category(name = "红包", icon = "card_giftcard", color = 0xFFFF69B4, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "礼物", icon = "redeem", color = 0xFFE91E63, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "请客", icon = "restaurant", color = 0xFFFF6B6B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),

        // 书籍文具子分类
        Category(name = "书籍", icon = "menu_book", color = 0xFF5D4037, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "文具", icon = "edit", color = 0xFF9E9E9E, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "电子书", icon = "tablet_android", color = 0xFF607D8B, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),

        // 虚拟产品子分类
        Category(name = "游戏充值", icon = "sports_esports", color = 0xFF9C27B0, type = RecordType.EXPENSE, isDefault = true, sortOrder = 0),
        Category(name = "会员订阅", icon = "star", color = 0xFFFFD700, type = RecordType.EXPENSE, isDefault = true, sortOrder = 1),
        Category(name = "软件订阅", icon = "cloud", color = 0xFF2196F3, type = RecordType.EXPENSE, isDefault = true, sortOrder = 2),
        Category(name = "直播打赏", icon = "favorite", color = 0xFFE91E63, type = RecordType.EXPENSE, isDefault = true, sortOrder = 3),
        Category(name = "虚拟货币", icon = "monetization_on", color = 0xFFFF9800, type = RecordType.EXPENSE, isDefault = true, sortOrder = 4)
    )

    val all = expenseCategories + incomeCategories
}
