package com.mewbook.app.domain.policy

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyAmountPolicy {
    fun normalizeCurrency(value: Double): Double {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
