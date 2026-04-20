package com.mewbook.app.ui.screens.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.AccountNamingPolicy
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.ui.components.AccountTypeIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.defaultColorValue
import com.mewbook.app.ui.components.toDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddAccountUiState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isDuplicateName: Boolean = false
)

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    fun saveAccount(
        name: String,
        type: AccountType,
        balance: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isDuplicateName = false, error = null) }

            // 如果名称为空且该类型没有默认名称，则提示用户输入
            if (name.isBlank() && getDefaultNameForType(type).isEmpty()) {
                _uiState.update { it.copy(isSaving = false, error = "请输入账户名称") }
                return@launch
            }

            // 检查是否使用默认名称
            val finalName = name.ifBlank { getDefaultNameForType(type) }.trim()
            val ledgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L

            // 检查是否有重名账户
            val existingAccounts = accountRepository.getAllAccounts().first()
            val isDuplicate = AccountNamingPolicy.hasDuplicateNameInLedger(
                accounts = existingAccounts,
                ledgerId = ledgerId,
                candidateName = finalName
            )

            if (isDuplicate) {
                _uiState.update { it.copy(isSaving = false, isDuplicateName = true, error = "账户名称已存在，请修改") }
                return@launch
            }

            try {
                val ledgerAccounts = existingAccounts.filter { it.ledgerId == ledgerId }
                val shouldSetAsDefault = AccountDefaultsPolicy.resolveDefaultAccountId(ledgerAccounts) == null
                val nextSortOrder = (ledgerAccounts.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val account = Account(
                    id = 0,
                    name = finalName,
                    type = type,
                    balance = balance,
                    icon = getDefaultIconNameForType(type),
                    color = getDefaultColorForType(type),
                    isDefault = shouldSetAsDefault,
                    sortOrder = nextSortOrder,
                    ledgerId = ledgerId
                )
                accountRepository.insertAccount(account)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, isDuplicateName = false) }
    }

    private fun getDefaultNameForType(type: AccountType): String {
        return when (type) {
            AccountType.CASH -> "现金"
            AccountType.BANK -> ""
            AccountType.ALIPAY -> "支付宝"
            AccountType.WECHAT -> "微信"
            AccountType.CREDIT_CARD -> "信用卡"
            AccountType.INVESTMENT -> "投资账户"
            AccountType.OTHER -> ""
        }
    }

    private fun getDefaultColorForType(type: AccountType): Long {
        return type.defaultColorValue()
    }

    private fun getDefaultIconNameForType(type: AccountType): String {
        return when (type) {
            AccountType.CASH -> "account_balance_wallet"
            AccountType.BANK -> "account_balance"
            AccountType.ALIPAY -> "alipay"
            AccountType.WECHAT -> "wechat"
            AccountType.CREDIT_CARD -> "credit_card"
            AccountType.INVESTMENT -> "savings"
            AccountType.OTHER -> "more_horiz"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    preselectedType: AccountType? = null,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(preselectedType ?: AccountType.BANK) }
    var accountName by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }

    // 根据类型获取默认名称
    val defaultName = when (selectedType) {
        AccountType.ALIPAY -> "支付宝"
        AccountType.WECHAT -> "微信"
        AccountType.CASH -> "现金"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.INVESTMENT -> "投资账户"
        AccountType.OTHER -> ""
        AccountType.BANK -> ""
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "添加账户",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "账户类型",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountTypeChip(
                    type = AccountType.BANK,
                    isSelected = selectedType == AccountType.BANK,
                    onClick = { selectedType = AccountType.BANK }
                )
                AccountTypeChip(
                    type = AccountType.ALIPAY,
                    isSelected = selectedType == AccountType.ALIPAY,
                    onClick = { selectedType = AccountType.ALIPAY }
                )
                AccountTypeChip(
                    type = AccountType.WECHAT,
                    isSelected = selectedType == AccountType.WECHAT,
                    onClick = { selectedType = AccountType.WECHAT }
                )
                AccountTypeChip(
                    type = AccountType.CASH,
                    isSelected = selectedType == AccountType.CASH,
                    onClick = { selectedType = AccountType.CASH }
                )
                AccountTypeChip(
                    type = AccountType.CREDIT_CARD,
                    isSelected = selectedType == AccountType.CREDIT_CARD,
                    onClick = { selectedType = AccountType.CREDIT_CARD }
                )
                AccountTypeChip(
                    type = AccountType.INVESTMENT,
                    isSelected = selectedType == AccountType.INVESTMENT,
                    onClick = { selectedType = AccountType.INVESTMENT }
                )
                AccountTypeChip(
                    type = AccountType.OTHER,
                    isSelected = selectedType == AccountType.OTHER,
                    onClick = { selectedType = AccountType.OTHER }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 账户名称 - 可编辑
            OutlinedTextField(
                value = if (accountName.isEmpty()) defaultName else accountName,
                onValueChange = { accountName = it },
                label = { Text("账户名称") },
                placeholder = { Text("输入账户名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                        balance = newValue
                    }
                },
                label = { Text("账户余额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("¥") },
                supportingText = {
                    Text(
                        text = if (selectedType == AccountType.CREDIT_CARD) "信用卡请输入负数，如 -1000.00" else "输入当前账户余额"
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val balanceValue = balance.toDoubleOrNull() ?: 0.0
                    viewModel.clearError()
                    viewModel.saveAccount(accountName, selectedType, balanceValue)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存")
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AccountTypeChip(
    type: AccountType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = type.toDisplayName(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            AccountTypeIconBadge(
                type = type,
                accentColor = Color(type.defaultColorValue()),
                containerSize = 22.dp,
                iconSize = 14.dp,
                emphasized = isSelected
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
