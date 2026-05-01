package com.mewbook.app.ui.screens.recurring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTemplateUsageGuideTest {

    @Test
    fun guide_coversCoreUsageExpectations() {
        assertEquals("使用说明", RecurringTemplateUsageGuide.title)
        assertTrue(RecurringTemplateUsageGuide.summary.contains("工资"))
        assertTrue(RecurringTemplateUsageGuide.summary.contains("订阅"))
        assertEquals(4, RecurringTemplateUsageGuide.steps.size)
        assertFalse(RecurringTemplateUsageGuide.defaultExpanded)
        assertTrue(RecurringTemplateUsageGuide.visibleSteps(false).isEmpty())
        assertEquals(4, RecurringTemplateUsageGuide.visibleSteps(true).size)
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.detail.contains("下次到期") })
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.detail.contains("生成本期") })
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.detail.contains("跳过") })
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.title.contains("闭环") })
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.detail.contains("推进到下一次") })
        assertTrue(RecurringTemplateUsageGuide.steps.any { it.detail.contains("结束日期") })
        assertFalse(RecurringTemplateUsageGuide.steps.any { it.detail.contains("P0") })
    }
}
