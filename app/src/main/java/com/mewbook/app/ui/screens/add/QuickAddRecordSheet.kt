package com.mewbook.app.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddRecordSheet(
    type: RecordType,
    categories: List<Category>,
    accounts: List<Account>,
    defaultAccountId: Long?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onOpenFullEditor: () -> Unit,
    onSave: (Double, RecordType, Long, String?, LocalDate, Long?) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val defaultCategoryId = categories.firstOrNull()?.id ?: 0L
    val resolvedDefaultAccountId = defaultAccountId ?: accounts.firstOrNull()?.id

    var amountExpression by rememberSaveable(type.name) { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable(type.name, defaultCategoryId) {
        mutableLongStateOf(defaultCategoryId)
    }
    var selectedAccountId by rememberSaveable(type.name, resolvedDefaultAccountId) {
        mutableStateOf(resolvedDefaultAccountId)
    }
    var note by rememberSaveable(type.name) { mutableStateOf("") }

    val amount = AmountExpressionHelper.evaluate(amountExpression)
    val canSave = amount != null && amount > 0.0 && selectedCategoryId > 0L
    val title = if (type == RecordType.EXPENSE) "快速记支出" else "快速记收入"

    LaunchedEffect(type, defaultCategoryId, resolvedDefaultAccountId, categories, accounts) {
        if ((selectedCategoryId == 0L || categories.none { it.id == selectedCategoryId }) && defaultCategoryId > 0L) {
            selectedCategoryId = defaultCategoryId
        }
        if ((selectedAccountId == null || accounts.none { it.id == selectedAccountId }) && resolvedDefaultAccountId != null) {
            selectedAccountId = resolvedDefaultAccountId
        }
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = amountExpression,
                    onValueChange = { amountExpression = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("金额（支持 20+3 ）") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "常用分类",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (categories.isEmpty()) {
                        Text(
                            text = "还没有可用分类，先去完整记账里选择一次分类。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(6).forEach { category ->
                                FilterChip(
                                    selected = selectedCategoryId == category.id,
                                    onClick = { selectedCategoryId = category.id },
                                    label = { Text(category.name) }
                                )
                            }
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "账户",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.take(4).forEach { account ->
                                FilterChip(
                                    selected = selectedAccountId == account.id,
                                    onClick = { selectedAccountId = account.id },
                                    label = { Text(account.name) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        QuickAddActionText("取消")
                    }
                    OutlinedButton(
                        onClick = onOpenFullEditor,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        QuickAddActionText("完整记账")
                    }
                    Button(
                        onClick = {
                            val resolvedAmount = amount ?: return@Button
                            onSave(
                                resolvedAmount,
                                type,
                                selectedCategoryId,
                                note.trim().ifBlank { null },
                                defaultDate,
                                selectedAccountId
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        QuickAddActionText("快速保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddActionText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
        textAlign = TextAlign.Center
    )
}
