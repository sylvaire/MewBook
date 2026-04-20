package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Ledger

enum class LedgerMoveDirection {
    UP,
    DOWN
}

object LedgerOrderingPolicy {

    fun displayOrder(ledgers: List<Ledger>): List<Ledger> {
        return ledgers.sortedBy(Ledger::createdAt)
    }

    fun swapPairForMove(
        ledgers: List<Ledger>,
        ledgerId: Long,
        direction: LedgerMoveDirection
    ): Pair<Long, Long>? {
        val orderedLedgers = displayOrder(ledgers)
        val currentIndex = orderedLedgers.indexOfFirst { it.id == ledgerId }
        if (currentIndex == -1) {
            return null
        }

        val swapIndex = when (direction) {
            LedgerMoveDirection.UP -> currentIndex - 1
            LedgerMoveDirection.DOWN -> currentIndex + 1
        }

        if (swapIndex !in orderedLedgers.indices) {
            return null
        }

        return orderedLedgers[currentIndex].id to orderedLedgers[swapIndex].id
    }
}
