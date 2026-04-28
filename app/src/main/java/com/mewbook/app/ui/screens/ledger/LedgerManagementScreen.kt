package com.mewbook.app.ui.screens.ledger

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.LedgerType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.LedgerMoveDirection
import com.mewbook.app.domain.policy.LedgerOrderingPolicy
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.ui.components.AccountTypeIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.toDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerManagementUiState(
    val ledgers: List<Ledger> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val pendingDelete: Ledger? = null,
    val settingsLedger: Ledger? = null,
    val settingsAccounts: List<Account> = emptyList(),
    val isSettingsLoading: Boolean = false,
    val isSavingSettings: Boolean = false
)

@HiltViewModel
class LedgerManagementViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerManagementUiState())
    val uiState: StateFlow<LedgerManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ledgerRepository.getAllLedgers().collect { ledgers ->
                _uiState.update { state ->
                    state.copy(
                        ledgers = ledgers,
                        isLoading = false,
                        settingsLedger = state.settingsLedger?.let { current ->
                            ledgers.firstOrNull { it.id == current.id }
                        }
                    )
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun openLedgerSettings(ledger: Ledger) {
        _uiState.update {
            it.copy(
                settingsLedger = ledger,
                settingsAccounts = emptyList(),
                isSettingsLoading = true,
                isSavingSettings = false
            )
        }

        viewModelScope.launch {
            val accounts = accountRepository.getAccountsByLedger(ledger.id).first()
                .sortedBy(Account::sortOrder)
            _uiState.update { state ->
                if (state.settingsLedger?.id != ledger.id) {
                    state
                } else {
                    state.copy(
                        settingsAccounts = accounts,
                        isSettingsLoading = false
                    )
                }
            }
        }
    }

    fun dismissLedgerSettings() {
        _uiState.update {
            it.copy(
                settingsLedger = null,
                settingsAccounts = emptyList(),
                isSettingsLoading = false,
                isSavingSettings = false
            )
        }
    }

    fun requestDelete(ledger: Ledger) {
        if (!ledger.isDefault) {
            _uiState.update { it.copy(pendingDelete = ledger) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun addLedger(name: String, type: LedgerType) {
        viewModelScope.launch {
            val existingLedgers = _uiState.value.ledgers
            val nextOrderTime = (existingLedgers.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis()) + 1
            ledgerRepository.insertLedger(
                Ledger(
                    name = name,
                    type = type,
                    icon = type.iconName(),
                    color = type.defaultColor(),
                    createdAt = nextOrderTime,
                    isDefault = false
                )
            )
            hideAddDialog()
        }
    }

    fun deleteLedger() {
        val ledger = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            ledgerRepository.deleteLedger(ledger)
            dismissDeleteDialog()
        }
    }

    fun setDefaultLedger(ledger: Ledger) {
        viewModelScope.launch {
            ledgerRepository.setDefaultLedger(ledger.id)
            _uiState.update { state ->
                state.copy(
                    settingsLedger = state.settingsLedger?.let { current ->
                        if (current.id == ledger.id) current.copy(isDefault = true) else current
                    }
                )
            }
        }
    }

    fun setDefaultAccount(accountId: Long) {
        val selectedLedger = _uiState.value.settingsLedger ?: return
        val existingAccounts = _uiState.value.settingsAccounts
        val updatedAccounts = AccountDefaultsPolicy.applyDefaultSelection(existingAccounts, accountId)

        if (updatedAccounts == existingAccounts) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSettings = true) }
            updatedAccounts.zip(existingAccounts).forEach { (updated, original) ->
                if (updated.isDefault != original.isDefault) {
                    accountRepository.updateAccount(updated)
                }
            }
            _uiState.update { state ->
                if (state.settingsLedger?.id != selectedLedger.id) {
                    state
                } else {
                    state.copy(
                        settingsAccounts = updatedAccounts,
                        isSavingSettings = false
                    )
                }
            }
        }
    }

    fun moveLedgerUp(ledgerId: Long) {
        viewModelScope.launch {
            swapLedgersForMove(ledgerId, LedgerMoveDirection.UP)
        }
    }

    fun moveLedgerDown(ledgerId: Long) {
        viewModelScope.launch {
            swapLedgersForMove(ledgerId, LedgerMoveDirection.DOWN)
        }
    }

    private suspend fun swapLedgersForMove(ledgerId: Long, direction: LedgerMoveDirection) {
        val orderedLedgers = LedgerOrderingPolicy.displayOrder(_uiState.value.ledgers)
        val (firstId, secondId) = LedgerOrderingPolicy.swapPairForMove(
            ledgers = orderedLedgers,
            ledgerId = ledgerId,
            direction = direction
        ) ?: return

        val firstLedger = orderedLedgers.firstOrNull { it.id == firstId } ?: return
        val secondLedger = orderedLedgers.firstOrNull { it.id == secondId } ?: return
        swapLedgerOrder(firstLedger, secondLedger)
    }

    private suspend fun swapLedgerOrder(first: Ledger, second: Ledger) {
        ledgerRepository.updateLedger(first.copy(createdAt = second.createdAt))
        ledgerRepository.updateLedger(second.copy(createdAt = first.createdAt))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: LedgerManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayLedgers = remember(uiState.ledgers) {
        LedgerOrderingPolicy.displayOrder(uiState.ledgers)
    }

    if (uiState.showAddDialog) {
        AddLedgerDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, type -> viewModel.addLedger(name, type) }
        )
    }

    uiState.settingsLedger?.let { ledger ->
        LedgerSettingsDialog(
            ledger = ledger,
            accounts = uiState.settingsAccounts,
            isLoading = uiState.isSettingsLoading,
            isSaving = uiState.isSavingSettings,
            onDismiss = { viewModel.dismissLedgerSettings() },
            onSetDefaultLedger = { viewModel.setDefaultLedger(ledger) },
            onSelectDefaultAccount = { accountId -> viewModel.setDefaultAccount(accountId) }
        )
    }

    uiState.pendingDelete?.let { ledger ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("删除账本") },
            text = { Text("确定删除「${ledger.name}」吗？长按删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteLedger() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "账本管理",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加账本")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "点击账本进入编辑，长按删除，使用右侧箭头自定义排序",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(displayLedgers, key = { _, ledger -> ledger.id }) { index, ledger ->
                    LedgerItem(
                        ledger = ledger,
                        canMoveUp = index > 0,
                        canMoveDown = index < displayLedgers.lastIndex,
                        onClick = { viewModel.openLedgerSettings(ledger) },
                        onLongPress = { viewModel.requestDelete(ledger) },
                        onMoveUp = { viewModel.moveLedgerUp(ledger.id) },
                        onMoveDown = { viewModel.moveLedgerDown(ledger.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerSettingsDialog(
    ledger: Ledger,
    accounts: List<Account>,
    isLoading: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSetDefaultLedger: () -> Unit,
    onSelectDefaultAccount: (Long) -> Unit
) {
    val defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(accounts)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账本") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (ledger.isDefault) "当前默认账本" else ledger.type.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ledger.isDefault) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSetDefaultLedger,
                    enabled = !ledger.isDefault && !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (ledger.isDefault) "当前默认账本" else "设为默认账本")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "默认账户",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "仅在当前账本内生效",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Text(
                                text = "加载账户中…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    accounts.isEmpty() -> {
                        Text(
                            text = "这个账本还没有账户，添加账户后可在这里设置默认账户。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { account ->
                                DefaultAccountItem(
                                    account = account,
                                    isSelected = account.id == defaultAccountId,
                                    enabled = !isSaving,
                                    onClick = { onSelectDefaultAccount(account.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun DefaultAccountItem(
    account: Account,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountTypeIconBadge(
                type = account.type,
                accentColor = Color(account.color),
                containerSize = 32.dp,
                iconSize = 18.dp,
                emphasized = isSelected
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isSelected) "当前默认账户" else account.type.toDisplayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "默认账户",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "设为默认",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerItem(
    ledger: Ledger,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerTypeBadge(type = ledger.type, accentColor = Color(ledger.color))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (ledger.isDefault) "默认账本" else ledger.type.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ledger.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (ledger.isDefault) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "默认账本",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = "上移",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "下移",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTypeBadge(
    type: LedgerType,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.14f)
        )
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .padding(8.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun AddLedgerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LedgerType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LedgerType.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加账本") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账本名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LedgerType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { selectedType.defaultName() }, selectedType) },
                enabled = true
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun LedgerType.icon(): ImageVector {
    return when (this) {
        LedgerType.PERSONAL -> Icons.Filled.Person
        LedgerType.FAMILY -> Icons.Filled.Groups
        LedgerType.AA -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}

private fun LedgerType.displayName(): String {
    return when (this) {
        LedgerType.PERSONAL -> "个人"
        LedgerType.FAMILY -> "家庭"
        LedgerType.AA -> "AA"
    }
}

private fun LedgerType.defaultName(): String {
    return when (this) {
        LedgerType.PERSONAL -> "个人账本"
        LedgerType.FAMILY -> "家庭账本"
        LedgerType.AA -> "AA账本"
    }
}

private fun LedgerType.iconName(): String {
    return when (this) {
        LedgerType.PERSONAL -> "person"
        LedgerType.FAMILY -> "groups"
        LedgerType.AA -> "receipt_long"
    }
}

private fun LedgerType.defaultColor(): Long {
    return when (this) {
        LedgerType.PERSONAL -> 0xFF4CAF50
        LedgerType.FAMILY -> 0xFF42A5F5
        LedgerType.AA -> 0xFFFFA726
    }
}
