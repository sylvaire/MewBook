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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.LedgerType
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.ui.components.MewCompactTopAppBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerManagementUiState(
    val ledgers: List<Ledger> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val pendingDelete: Ledger? = null
)

@HiltViewModel
class LedgerManagementViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerManagementUiState())
    val uiState: StateFlow<LedgerManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ledgerRepository.getAllLedgers().collect { ledgers ->
                _uiState.update {
                    it.copy(
                        ledgers = ledgers,
                        isLoading = false
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
        }
    }

    fun moveLedgerUp(ledgerId: Long) {
        viewModelScope.launch {
            val sortableLedgers = _uiState.value.ledgers.filter { !it.isDefault }
            val currentIndex = sortableLedgers.indexOfFirst { it.id == ledgerId }
            if (currentIndex <= 0) return@launch

            val currentLedger = sortableLedgers[currentIndex]
            val previousLedger = sortableLedgers[currentIndex - 1]
            swapLedgerOrder(currentLedger, previousLedger)
        }
    }

    fun moveLedgerDown(ledgerId: Long) {
        viewModelScope.launch {
            val sortableLedgers = _uiState.value.ledgers.filter { !it.isDefault }
            val currentIndex = sortableLedgers.indexOfFirst { it.id == ledgerId }
            if (currentIndex == -1 || currentIndex >= sortableLedgers.lastIndex) return@launch

            val currentLedger = sortableLedgers[currentIndex]
            val nextLedger = sortableLedgers[currentIndex + 1]
            swapLedgerOrder(currentLedger, nextLedger)
        }
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
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAddDialog) {
        AddLedgerDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, type -> viewModel.addLedger(name, type) }
        )
    }

    uiState.pendingDelete?.let { ledger ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("删除分支") },
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
                title = "分支管理",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            IconButton(onClick = { viewModel.showAddDialog() }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "添加分支",
                        modifier = Modifier.padding(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
                text = "点击设为默认分支，长按删除，使用右侧箭头自定义排序",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.ledgers, key = { _, ledger -> ledger.id }) { index, ledger ->
                    LedgerItem(
                        ledger = ledger,
                        canMoveUp = !ledger.isDefault && uiState.ledgers.take(index).any { !it.isDefault },
                        canMoveDown = !ledger.isDefault && uiState.ledgers.drop(index + 1).any { !it.isDefault },
                        onClick = { viewModel.setDefaultLedger(ledger) },
                        onLongPress = { viewModel.requestDelete(ledger) },
                        onMoveUp = { viewModel.moveLedgerUp(ledger.id) },
                        onMoveDown = { viewModel.moveLedgerDown(ledger.id) }
                    )
                }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerTypeBadge(type = ledger.type, accentColor = Color(ledger.color))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (ledger.isDefault) "默认分支" else ledger.type.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ledger.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ledger.isDefault) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "默认分支",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移")
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
                .padding(10.dp)
                .size(22.dp)
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
        title = { Text("添加分支") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分支名称") },
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
        LedgerType.PERSONAL -> "个人分支"
        LedgerType.FAMILY -> "家庭分支"
        LedgerType.AA -> "AA分支"
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
