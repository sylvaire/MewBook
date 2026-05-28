package com.mewbook.app.domain.policy

object HapticFeedbackPolicy {
    enum class Interaction {
        AmountKey,
        DialogAction,
        ExternalLink,
        LongPressAction,
        RowClick,
        Selection,
        Toggle
    }

    fun shouldPerform(
        preferenceEnabled: Boolean,
        interaction: Interaction,
        controlEnabled: Boolean = true
    ): Boolean {
        val isSuitableInteraction = when (interaction) {
            Interaction.AmountKey,
            Interaction.DialogAction,
            Interaction.ExternalLink,
            Interaction.LongPressAction,
            Interaction.RowClick,
            Interaction.Selection,
            Interaction.Toggle -> true
        }
        return preferenceEnabled && controlEnabled && isSuitableInteraction
    }
}
