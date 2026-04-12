package com.mewbook.app.domain.usecase.ledger

import com.mewbook.app.domain.model.DefaultLedgers
import com.mewbook.app.domain.repository.LedgerRepository
import javax.inject.Inject

class InitializeDefaultLedgerUseCase @Inject constructor(
    private val ledgerRepository: LedgerRepository
) {
    suspend operator fun invoke() {
        val existingLedger = ledgerRepository.getDefaultLedger()
        if (existingLedger == null) {
            ledgerRepository.insertLedger(DefaultLedgers.personalLedger)
        }
    }
}
