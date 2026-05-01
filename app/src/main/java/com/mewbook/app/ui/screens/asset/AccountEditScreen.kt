package com.mewbook.app.ui.screens.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.ui.components.AccountTypeIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.toDisplayName
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountEditUiState(
    val account: Account? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountEditUiState())
    val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val account = accountRepository.getAccountById(accountId)
            _uiState.update { it.copy(account = account, isLoading = false) }
        }
    }

    fun updateBalance(balance: Double) {
        viewModelScope.launch {
            val account = _uiState.value.account ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            accountRepository.updateBalance(account.id, balance)
            val updated = accountRepository.getAccountById(account.id)
            _uiState.update { it.copy(account = updated, isSaving = false) }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            val account = _uiState.value.account ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            val updated = account.copy(name = name)
            accountRepository.updateAccount(updated)
            _uiState.update { it.copy(account = updated, isSaving = false) }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val account = _uiState.value.account ?: return@launch
            accountRepository.deleteAccount(account)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AccountEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var balanceText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        viewModel.loadAccount(accountId)
    }

    LaunchedEffect(uiState.account) {
        uiState.account?.let {
            if (balanceText.isEmpty()) {
                balanceText = if (it.balance == 0.0) "" else it.balance.toString()
            }
            if (nameText.isEmpty()) {
                nameText = it.name
            }
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除账户") },
            text = { Text("确定要删除这个账户吗？删除后相关记账记录将无法关联到此账户。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "编辑账户",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                uiState.account?.let { account ->
                    // Account icon and type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountTypeIconBadge(
                            type = account.type,
                            accentColor = Color(account.color),
                            containerSize = 52.dp,
                            iconSize = 28.dp,
                            emphasized = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = account.type.toDisplayName(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account name
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("账户名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Balance
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                                balanceText = newValue
                            }
                        },
                        label = { Text("账户余额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("¥") },
                        supportingText = {
                            Text(
                                text = if (account.type.name == "CREDIT_CARD") "信用卡请输入负数，如 -1000.00" else "输入当前账户余额"
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Current balance display
                    val currentBalance = balanceText.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "当前余额: ${formatCurrency(currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (currentBalance >= 0) IncomeGreen else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (nameText.isNotBlank()) {
                                viewModel.updateName(nameText)
                            }
                            balanceText.toDoubleOrNull()?.let { balance ->
                                viewModel.updateBalance(balance)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && (nameText.isNotBlank() || balanceText.isNotBlank())
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
                }
            }
        }
    }
}
