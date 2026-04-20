package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.LedgerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedgerOrderingPolicyTest {

    @Test
    fun displayOrder_ignoresDefaultFlagAndKeepsManualOrder() {
        val orderedIds = LedgerOrderingPolicy.displayOrder(
            listOf(
                ledger(id = 1L, createdAt = 10L, isDefault = false),
                ledger(id = 2L, createdAt = 20L, isDefault = true),
                ledger(id = 3L, createdAt = 30L, isDefault = false)
            )
        ).map(Ledger::id)

        assertEquals(listOf(1L, 2L, 3L), orderedIds)
    }

    @Test
    fun swapPairForMove_allowsDefaultLedgerToMoveUsingVisibleNeighbors() {
        val swapPair = LedgerOrderingPolicy.swapPairForMove(
            ledgers = listOf(
                ledger(id = 1L, createdAt = 10L, isDefault = false),
                ledger(id = 2L, createdAt = 20L, isDefault = true),
                ledger(id = 3L, createdAt = 30L, isDefault = false)
            ),
            ledgerId = 2L,
            direction = LedgerMoveDirection.DOWN
        )

        assertEquals(2L to 3L, swapPair)
    }

    @Test
    fun swapPairForMove_returnsNullWhenLedgerIsAlreadyAtBoundary() {
        val swapPair = LedgerOrderingPolicy.swapPairForMove(
            ledgers = listOf(
                ledger(id = 1L, createdAt = 10L),
                ledger(id = 2L, createdAt = 20L)
            ),
            ledgerId = 1L,
            direction = LedgerMoveDirection.UP
        )

        assertNull(swapPair)
    }

    private fun ledger(
        id: Long,
        createdAt: Long,
        isDefault: Boolean = false
    ) = Ledger(
        id = id,
        name = "Ledger-$id",
        type = LedgerType.PERSONAL,
        icon = "person",
        color = 0xFF4CAF50,
        createdAt = createdAt,
        isDefault = isDefault
    )
}
