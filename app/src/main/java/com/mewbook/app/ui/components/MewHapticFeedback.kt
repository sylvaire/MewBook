package com.mewbook.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mewbook.app.domain.policy.HapticFeedbackPolicy

class MewHapticFeedback internal constructor(
    private val feedback: HapticFeedback,
    private val preferenceEnabled: Boolean
) {
    fun perform(
        interaction: HapticFeedbackPolicy.Interaction,
        controlEnabled: Boolean = true
    ) {
        if (HapticFeedbackPolicy.shouldPerform(preferenceEnabled, interaction, controlEnabled)) {
            @Suppress("DEPRECATION")
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberMewHapticFeedback(preferenceEnabled: Boolean): MewHapticFeedback {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, preferenceEnabled) {
        MewHapticFeedback(feedback = feedback, preferenceEnabled = preferenceEnabled)
    }
}
