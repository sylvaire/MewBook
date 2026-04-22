package com.mewbook.app.ui.screens.recurring

data class RecurringTemplateUsageStep(
    val title: String,
    val detail: String
)

object RecurringTemplateUsageGuide {
    const val title: String = "使用说明"
    const val summary: String =
        "周期模板适合工资、房租、订阅、水电、信用卡还款等固定收支，先把重复记账整理成可复用模板。"
    const val expandLabel: String = "点击展开完整说明"
    const val collapseLabel: String = "收起说明"
    const val defaultExpanded: Boolean = false

    val steps: List<RecurringTemplateUsageStep> = listOf(
        RecurringTemplateUsageStep(
            title = "1. 先建模板",
            detail = "填写模板名称、金额、分类、账户和周期，常见固定收支都可以先沉淀成一条模板。"
        ),
        RecurringTemplateUsageStep(
            title = "2. 看下次到期",
            detail = "开始日期决定模板起点，下次到期决定下一次待处理时间，结束日期可以按需要开启。"
        ),
        RecurringTemplateUsageStep(
            title = "3. 到期后处理",
            detail = "需要入账时点“生成本期”创建记录；本期不需要记账时点“跳过”推进到下一次。"
        ),
        RecurringTemplateUsageStep(
            title = "4. 当前是手动闭环",
            detail = "P0 阶段先以手动处理为主，提醒开关当前用于标记模板，后续再扩展系统通知。"
        )
    )

    fun visibleSteps(isExpanded: Boolean): List<RecurringTemplateUsageStep> {
        return if (isExpanded) steps else emptyList()
    }
}
