package com.mewbook.app.ui.screens.add

import java.math.BigDecimal
import java.math.RoundingMode

internal object AmountExpressionHelper {

    fun append(expression: String, input: Char): String {
        return when {
            input.isDigit() -> appendDigit(expression, input)
            input == '.' -> appendDecimal(expression)
            input == '+' || input == '-' -> appendOperator(expression, input)
            else -> expression
        }
    }

    fun backspace(expression: String): String {
        return expression.dropLast(1)
    }

    fun clear(): String = ""

    fun evaluate(expression: String): Double? {
        if (expression.isBlank()) return null
        val trimmed = expression.trim()
        if (trimmed.last() in OPERATORS || trimmed.last() == '.') return null

        var total = BigDecimal.ZERO
        var currentOperator = '+'
        val currentNumber = StringBuilder()

        for (character in trimmed) {
            if (character.isDigit() || character == '.') {
                currentNumber.append(character)
                continue
            }

            if (character !in OPERATORS || currentNumber.isEmpty()) return null

            val number = currentNumber.toString().toBigDecimalOrNull() ?: return null
            total = applyOperation(total, number, currentOperator)
            currentOperator = character
            currentNumber.clear()
        }

        val lastNumber = currentNumber.toString().toBigDecimalOrNull() ?: return null
        return applyOperation(total, lastNumber, currentOperator)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun canSave(expression: String): Boolean {
        val result = evaluate(expression) ?: return false
        return result > 0.0
    }

    fun formatInitialExpression(amount: Double): String {
        return BigDecimal.valueOf(amount)
            .stripTrailingZeros()
            .toPlainString()
            .removeSuffix(".0")
            .ifBlank { "0" }
    }

    fun formatDisplayResult(expression: String): String {
        val result = evaluate(expression) ?: 0.0
        return BigDecimal.valueOf(result)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    private fun appendDigit(expression: String, digit: Char): String {
        if (expression.isEmpty()) return digit.toString()

        val term = currentTerm(expression)
        if (term == "0" && !term.contains('.')) {
            return if (digit == '0') expression else expression.dropLast(1) + digit
        }

        if (term.contains('.')) {
            val decimalDigits = term.substringAfter('.', missingDelimiterValue = "")
            if (decimalDigits.length >= 2) return expression
        }

        return expression + digit
    }

    private fun appendDecimal(expression: String): String {
        if (expression.isEmpty()) return "0."
        if (expression.last() in OPERATORS) return "${expression}0."

        val term = currentTerm(expression)
        return if (term.contains('.')) expression else "$expression."
    }

    private fun appendOperator(expression: String, operator: Char): String {
        if (expression.isEmpty()) return expression
        val last = expression.last()
        if (last in OPERATORS || last == '.') return expression
        return expression + operator
    }

    private fun currentTerm(expression: String): String {
        val lastOperatorIndex = expression.indexOfLast { it in OPERATORS }
        return expression.substring(lastOperatorIndex + 1)
    }

    private fun applyOperation(
        total: BigDecimal,
        amount: BigDecimal,
        operator: Char
    ): BigDecimal {
        return if (operator == '-') total.subtract(amount) else total.add(amount)
    }

    private val OPERATORS = setOf('+', '-')
}
