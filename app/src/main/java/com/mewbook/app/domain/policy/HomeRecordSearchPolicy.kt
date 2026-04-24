package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object HomeRecordSearchPolicy {

    fun search(
        query: String,
        activeLedgerId: Long,
        records: List<Record>,
        categoriesById: Map<Long, Category>,
        accountsById: Map<Long, Account>
    ): List<Record> {
        val tokens = query
            .trim()
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)

        if (tokens.isEmpty()) {
            return emptyList()
        }

        return records
            .asSequence()
            .filter { it.ledgerId == activeLedgerId }
            .filter { record ->
                val categoryName = categoriesById[record.categoryId]?.name.orEmpty()
                val accountName = record.accountId?.let(accountsById::get)?.name.orEmpty()
                tokens.all { token ->
                    matchesText(record.note, token) ||
                        matchesText(categoryName, token) ||
                        matchesText(accountName, token) ||
                    matchesAmount(record.amount, token)
                }
            }
            .toList()
            .let(HomeRecordOrderingPolicy::newestFirst)
    }

    private fun matchesText(source: String?, token: String): Boolean {
        return source
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.contains(token) == true
    }

    private fun matchesAmount(amount: Double, token: String): Boolean {
        val normalizedToken = normalizeNumeric(token)
        if (normalizedToken.isBlank()) {
            return false
        }

        val amountTokens = buildSet {
            val normalizedAmount = BigDecimal.valueOf(amount)
            add(normalizedAmount.stripTrailingZeros().toPlainString())
            add(normalizedAmount.setScale(2, RoundingMode.HALF_UP).toPlainString())
            add(String.format(Locale.US, "%.2f", amount))
        }.map(::normalizeNumeric)

        return amountTokens.any { it.contains(normalizedToken) }
    }

    private fun normalizeNumeric(value: String): String {
        return value
            .replace(",", "")
            .replace("，", "")
            .replace("￥", "")
            .replace("¥", "")
            .trim()
    }
}
