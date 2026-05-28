package com.mewbook.app.domain.policy

import com.mewbook.app.domain.policy.HapticFeedbackPolicy.Interaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticFeedbackPolicyTest {
    @Test
    fun suppressesFeedbackWhenPreferenceIsDisabled() {
        Interaction.entries.forEach { interaction ->
            assertFalse(HapticFeedbackPolicy.shouldPerform(preferenceEnabled = false, interaction = interaction))
        }
    }

    @Test
    fun suppressesFeedbackForDisabledControls() {
        assertFalse(
            HapticFeedbackPolicy.shouldPerform(
                preferenceEnabled = true,
                interaction = Interaction.RowClick,
                controlEnabled = false
            )
        )
    }

    @Test
    fun allowsFeedbackForSuitableTouchInteractionsWhenEnabled() {
        Interaction.entries.forEach { interaction ->
            assertTrue(HapticFeedbackPolicy.shouldPerform(preferenceEnabled = true, interaction = interaction))
        }
    }
}
