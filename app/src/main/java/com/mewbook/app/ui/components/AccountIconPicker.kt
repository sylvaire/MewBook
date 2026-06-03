package com.mewbook.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mewbook.app.domain.model.AccountType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountIconPicker(
    selectedIconName: String,
    accountType: AccountType,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onIconSelected: (String) -> Unit
) {
    val normalizedIconName = normalizeAccountIconName(selectedIconName, accountType)
    val groupedOptions = remember {
        accountIconOptions().groupBy { it.group }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountIconBadge(
                type = accountType,
                iconName = normalizedIconName,
                accentColor = accentColor,
                containerSize = 42.dp,
                iconSize = 24.dp,
                emphasized = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "账户图标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = accountIconOptions().firstOrNull { it.name == normalizedIconName }?.label ?: "默认图标",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        groupedOptions.forEach { (group, options) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = group,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val selected = normalizedIconName == option.name
                        FilterChip(
                            selected = selected,
                            onClick = { onIconSelected(option.name) },
                            label = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                AccountIconBadge(
                                    type = accountType,
                                    iconName = option.name,
                                    accentColor = accentColor,
                                    containerSize = 22.dp,
                                    iconSize = 14.dp,
                                    emphasized = selected
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                selectedLeadingIconColor = accentColor
                            )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
