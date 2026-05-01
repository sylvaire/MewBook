package com.mewbook.app.domain.policy

import java.util.Locale

data class CategorySemanticCandidate(
    val id: Long,
    val name: String,
    val type: String,
    val parentId: Long?,
    val semanticLabel: String?
)

data class CategorySemanticMatch(
    val categoryId: Long,
    val reason: String
)

object CategorySemanticPolicy {

    fun resolveExistingCategory(
        candidates: List<CategorySemanticCandidate>,
        incomingName: String,
        incomingType: String,
        incomingSemanticLabel: String?,
        targetParentId: Long?
    ): CategorySemanticMatch? {
        val sameType = candidates.filter { it.type == incomingType }
        val normalizedIncomingName = normalize(incomingName)

        sameType.uniqueMatch {
            it.parentId == targetParentId && normalize(it.name) == normalizedIncomingName
        }?.let { return CategorySemanticMatch(it.id, "同名分类") }

        if (targetParentId == null) {
            sameType.uniqueMatch { normalize(it.name) == normalizedIncomingName }
                ?.let { return CategorySemanticMatch(it.id, "同名分类") }
        }

        val incomingSemanticKey = semanticLabelFor(incomingName, incomingSemanticLabel) ?: return null

        sameType.uniqueMatch {
            it.parentId == targetParentId && semanticLabelFor(it.name, it.semanticLabel) == incomingSemanticKey
        }?.let { return CategorySemanticMatch(it.id, "同义分类") }

        if (targetParentId == null) {
            sameType.uniqueMatch { semanticLabelFor(it.name, it.semanticLabel) == incomingSemanticKey }
                ?.let { return CategorySemanticMatch(it.id, "同义分类") }
        }

        return null
    }

    fun chooseIcon(
        type: String,
        categoryName: String,
        semanticLabel: String?,
        isChild: Boolean,
        proposedIcon: String?
    ): String {
        val cleanedProposedIcon = proposedIcon?.trim().orEmpty()
        if (cleanedProposedIcon in supportedIcons && cleanedProposedIcon !in genericFallbackIcons) {
            return cleanedProposedIcon
        }

        val semanticKey = semanticLabelFor(categoryName, semanticLabel)
        semanticIconByKey[semanticKey]?.let { return it }

        return when {
            type == "INCOME" -> "payments"
            isChild -> "sell"
            else -> "category"
        }
    }

    fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("\uFEFF", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("（", "")
            .replace("）", "")
            .replace("【", "")
            .replace("】", "")
            .replace("/", "")
            .replace("\\", "")
            .replace(":", "")
            .replace("：", "")
            .replace(".", "")
    }

    private fun List<CategorySemanticCandidate>.uniqueMatch(
        predicate: (CategorySemanticCandidate) -> Boolean
    ): CategorySemanticCandidate? {
        val matches = filter(predicate)
        return matches.singleOrNull()
    }

    fun semanticLabelFor(name: String, semanticLabel: String?): String? {
        val normalizedName = normalize(name)
        aliasGroups.forEach { (key, aliases) ->
            if (aliases.any { normalizedName == normalize(it) || normalizedName.contains(normalize(it)) }) {
                return key
            }
        }

        val normalizedLabel = normalize(semanticLabel.orEmpty())
        return semanticLabelAliases[normalizedLabel]
    }

    private val semanticLabelAliases = mapOf(
        "food" to "food",
        "meal" to "food",
        "dining" to "food",
        "breakfast" to "breakfast",
        "transport" to "transport",
        "transportation" to "transport",
        "taxi" to "taxi",
        "housing" to "housing",
        "home" to "housing",
        "rent" to "housing",
        "salary" to "salary",
        "wage" to "salary",
        "income" to "income",
        "refund" to "refund",
        "investment" to "investment",
        "shopping" to "shopping",
        "medical" to "medical",
        "education" to "education",
        "entertainment" to "entertainment",
        "daily" to "da3+ily"
    )

    private val aliasGroups = linkedMapOf(
        "breakfast" to setOf("早餐", "早饭", "早点", "早膳", "breakfast"),
        "salary" to setOf("工资", "薪资", "薪水", "工资收入", "工资薪金", "salary", "wage"),
        "taxi" to setOf("打车", "出租车", "网约车", "滴滴", "的士", "taxi"),
        "housing" to setOf("房租", "租房", "租金", "房贷", "住房", "居住", "housing", "rent"),
        "refund" to setOf("退款", "退费", "报销款", "报销", "refund"),
        "investment" to setOf("投资", "投资收益", "基金收益", "基金", "理财", "理财收益", "分红", "利息", "利息收入", "investment"),
        "daily" to setOf("日用", "日用品", "日常用品", "生活用品", "清洁用品", "daily"),
        "transport" to setOf("交通", "公交", "地铁", "火车", "飞机", "油费", "停车", "transport"),
        "food" to setOf("餐饮", "吃饭", "饮食", "美食", "外卖", "午餐", "晚餐", "food", "meal"),
        "shopping" to setOf("购物", "网购", "买东西", "商超", "shopping"),
        "medical" to setOf("医疗", "药品", "看病", "医院", "medical"),
        "education" to setOf("教育", "学习", "培训", "课程", "education"),
        "entertainment" to setOf("娱乐", "电影", "游戏", "会员", "entertainment"),
        "income" to setOf("收入", "奖金", "兼职", "income")
    )

    private val semanticIconByKey = mapOf(
        "breakfast" to "free_breakfast",
        "salary" to "payments",
        "taxi" to "local_taxi",
        "housing" to "home",
        "refund" to "receipt_long",
        "investment" to "trending_up",
        "daily" to "cleaning_services",
        "transport" to "directions_bus",
        "food" to "restaurant",
        "shopping" to "local_mall",
        "medical" to "medical_services",
        "education" to "school",
        "entertainment" to "sports_esports",
        "income" to "payments"
    )

    private val supportedIcons = setOf(
        "restaurant",
        "directions_car",
        "directions_bus",
        "shopping_bag",
        "local_mall",
        "movie",
        "medical_services",
        "medication",
        "health_and_safety",
        "school",
        "more_horiz",
        "payments",
        "receipt_long",
        "trending_up",
        "card_giftcard",
        "attach_money",
        "work",
        "account_balance",
        "savings",
        "home",
        "house",
        "weekend",
        "free_breakfast",
        "lunch_dining",
        "dinner_dining",
        "local_cafe",
        "local_drink",
        "emoji_food_beverage",
        "nutrition",
        "emoji_nature",
        "takeout_dining",
        "train",
        "flight",
        "local_taxi",
        "local_gas_station",
        "local_parking",
        "local_shipping",
        "devices",
        "face",
        "brush",
        "baby_changing_station",
        "boy",
        "checkroom",
        "cleaning_services",
        "child_care",
        "kitchen",
        "water_drop",
        "manage_accounts",
        "local_fire_department",
        "phone_android",
        "sports_esports",
        "fitness_center",
        "music_note",
        "mic",
        "pool",
        "forum",
        "pets",
        "local_hospital",
        "local_bar",
        "people",
        "redeem",
        "auto_stories",
        "edit",
        "tablet_android",
        "eco",
        "sports_basketball",
        "wifi",
        "inbox",
        "cloud",
        "star",
        "favorite",
        "monetization_on",
        "cookie",
        "category",
        "sell"
    )

    private val genericFallbackIcons = setOf(
        "category",
        "sell",
        "payments",
        "more_horiz"
    )
}
