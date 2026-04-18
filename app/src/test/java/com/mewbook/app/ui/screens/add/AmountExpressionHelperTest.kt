package com.mewbook.app.ui.screens.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountExpressionHelperTest {

    @Test
    fun `evaluates addition and subtraction expressions`() {
        assertEquals(15.0, evaluated("12+3"), 0.0001)
        assertEquals(13.0, evaluated("12.5+0.5"), 0.0001)
        assertEquals(15.0, evaluated("20-3-2"), 0.0001)
    }

    @Test
    fun `blocks consecutive operators and repeated decimals`() {
        assertEquals("12+", AmountExpressionHelper.append("12", '+'))
        assertEquals("12+", AmountExpressionHelper.append("12+", '-'))
        assertEquals("12.3", AmountExpressionHelper.append("12.3", '.'))
        assertEquals("12.34", AmountExpressionHelper.append("12.34", '5'))
    }

    @Test
    fun `supports delete and clear style workflows`() {
        assertEquals("12+", AmountExpressionHelper.backspace("12+3"))
        assertEquals("", AmountExpressionHelper.backspace("7"))
        assertEquals("", AmountExpressionHelper.clear())
    }

    @Test
    fun `rejects incomplete or non positive expressions`() {
        assertNull(AmountExpressionHelper.evaluate(""))
        assertNull(AmountExpressionHelper.evaluate("12+"))
        assertEquals(0.0, evaluated("3-3"), 0.0001)
        assertEquals(-5.0, evaluated("3-8"), 0.0001)
        assertFalse(AmountExpressionHelper.canSave(""))
        assertFalse(AmountExpressionHelper.canSave("3-3"))
        assertFalse(AmountExpressionHelper.canSave("3-8"))
        assertTrue(AmountExpressionHelper.canSave("8.5"))
    }

    @Test
    fun `formats edit amount into reusable expression`() {
        assertEquals("12", AmountExpressionHelper.formatInitialExpression(12.0))
        assertEquals("12.5", AmountExpressionHelper.formatInitialExpression(12.5))
    }

    private fun evaluated(expression: String): Double {
        return requireNotNull(AmountExpressionHelper.evaluate(expression))
    }
}
