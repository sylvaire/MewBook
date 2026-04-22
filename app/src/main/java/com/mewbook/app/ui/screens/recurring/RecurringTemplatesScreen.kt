package com.mewbook.app.ui.screens.recurring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import com.mewbook.app.domain.policy.CategorySelectionPolicy
import com.mewbook.app.domain.policy.RecurringTemplateSchedulePolicy
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val RecurringDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年MM月dd日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTemplatesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringTemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = uiState.showEditor) {
        viewModel.dismissEditor()
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                MewCompactTopAppBar(
                    title = "周期模板",
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openCreateTemplate() }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新建模板")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TemplatesSummaryCard(templates = uiState.templates)
                }
                item {
                    RecurringTemplateGuideCard()
                }

                if (uiState.templates.isEmpty()) {
                    item {
                        EmptyTemplatesCard(onCreateClick = { viewModel.openCreateTemplate() })
                    }
                } else {
                    val categoryNames = uiState.categories.associateBy({ it.id }, { it.name })
                    val accountNames = uiState.accounts.associateBy({ it.id }, { it.name })
                    items(uiState.templates, key = { it.id }) { template ->
                        RecurringTemplateCard(
                            template = template,
                            categoryName = categoryNames[template.categoryId] ?: "未知分类",
                            accountName = template.accountId?.let(accountNames::get),
                            isBusy = uiState.busyTemplateId == template.id || uiState.isSaving,
                            onEdit = { viewModel.openEditTemplate(template) },
                            onGenerate = { viewModel.generateOccurrence(template) },
                            onSkip = { viewModel.skipOccurrence(template) },
                            onDelete = { viewModel.requestDelete(template) }
                        )
                    }
                }
            }
        }

        if (uiState.deleteCandidate != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("删除模板") },
                text = {
                    Text("删除后模板记录不会自动恢复，确定要删除这个周期模板吗？")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("取消")
                    }
                }
            )
        }

        if (uiState.showEditor) {
            RecurringTemplateEditorSheet(
                editorState = uiState.editor,
                categories = uiState.categories,
                accounts = uiState.accounts,
                isSaving = uiState.isSaving,
                onDismiss = { viewModel.dismissEditor() },
                onUpdate = viewModel::updateEditor,
                onSave = { viewModel.saveEditor() }
            )
        }
    }
}

@Composable
private fun RecurringTemplateGuideCard() {
    var isExpanded by rememberSaveable { mutableStateOf(RecurringTemplateUsageGuide.defaultExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = RecurringTemplateUsageGuide.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) {
                            RecurringTemplateUsageGuide.collapseLabel
                        } else {
                            RecurringTemplateUsageGuide.expandLabel
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Text(
                text = RecurringTemplateUsageGuide.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecurringTemplateUsageGuide.visibleSteps(isExpanded).forEach { step ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = step.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesSummaryCard(
    templates: List<RecurringTemplate>
) {
    val enabledCount = templates.count { it.isEnabled }
    val dueSoonCount = templates.count { template ->
        template.isEnabled && template.nextDueDate <= LocalDate.now().plusDays(30)
    }
    val nextDue = templates.filter { it.isEnabled }.minByOrNull { it.nextDueDate }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "即将到期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "启用中 ${enabledCount} 个，30 天内到期 ${dueSoonCount} 个",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = nextDue?.let {
                    "最近到期：${it.name} · ${it.nextDueDate.format(RecurringDateFormatter)}"
                } ?: "还没有可用的周期模板，先创建一个吧。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyTemplatesCard(
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "还没有周期模板",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "把工资、房租、订阅这些固定支出做成模板后，就能一键生成当期记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("创建模板")
            }
        }
    }
}

@Composable
private fun RecurringTemplateCard(
    template: RecurringTemplate,
    categoryName: String,
    accountName: String?,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onGenerate: () -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = if (template.type == RecordType.EXPENSE) ExpenseRed else IncomeGreen
    val statusText = remember(template) { templateStatusText(template) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${template.type.name} · ${RecurringTemplateSchedulePolicy.describeSchedule(template.scheduleType, template.intervalCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatCurrency(template.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Text(
                text = "下次到期 ${template.nextDueDate.format(RecurringDateFormatter)} · $statusText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "分类 $categoryName · 账户 ${accountName ?: "未绑定"}" +
                    if (template.reminderEnabled) " · 已开启提醒" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit, enabled = !isBusy) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("编辑")
                }
                TextButton(onClick = onGenerate, enabled = !isBusy) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    }
                    Text("生成本期")
                }
                TextButton(onClick = onSkip, enabled = !isBusy) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                    Text("跳过")
                }
                TextButton(onClick = onDelete, enabled = !isBusy) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTemplateEditorSheet(
    editorState: RecurringTemplateEditorState,
    categories: List<Category>,
    accounts: List<Account>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdate: ((RecurringTemplateEditorState) -> RecurringTemplateEditorState) -> Unit,
    onSave: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val availableCategories = remember(editorState.type, categories) {
        CategorySelectionPolicy.visibleTopLevelCategories(
            categories = categories,
            type = editorState.type
        )
    }
    val selectedCategory = remember(editorState.categoryId, availableCategories) {
        availableCategories.firstOrNull { it.id == editorState.categoryId }
    }
    val selectedAccount = remember(editorState.accountId, accounts) {
        accounts.firstOrNull { it.id == editorState.accountId }
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var startDatePickerOpen by remember { mutableStateOf(false) }
    var nextDatePickerOpen by remember { mutableStateOf(false) }
    var endDatePickerOpen by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editorState.startDate.toEpochMillis()
    )
    val nextDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editorState.nextDueDate.toEpochMillis()
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editorState.endDate.toEpochMillis()
    )

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = if (editorState.id == 0L) "新建周期模板" else "编辑周期模板",
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = editorState.name,
                    onValueChange = { value -> onUpdate { it.copy(name = value) } },
                    label = { Text("模板名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = editorState.amount,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            onUpdate { it.copy(amount = value) }
                        }
                    },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = editorState.type == RecordType.EXPENSE,
                        onClick = {
                            val defaultCategoryId = CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
                                categories = categories,
                                type = RecordType.EXPENSE
                            )
                            onUpdate {
                                it.copy(
                                    type = RecordType.EXPENSE,
                                    categoryId = if (defaultCategoryId != 0L) defaultCategoryId else it.categoryId
                                )
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("支出")
                    }
                    SegmentedButton(
                        selected = editorState.type == RecordType.INCOME,
                        onClick = {
                            val defaultCategoryId = CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
                                categories = categories,
                                type = RecordType.INCOME
                            )
                            onUpdate {
                                it.copy(
                                    type = RecordType.INCOME,
                                    categoryId = if (defaultCategoryId != 0L) defaultCategoryId else it.categoryId
                                )
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("收入")
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = availableCategories.isNotEmpty() && it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        placeholder = { Text(if (availableCategories.isEmpty()) "当前类型没有分类" else "请选择分类") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onUpdate { it.copy(categoryId = category.id) }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "不绑定账户",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("账户") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("不绑定账户") },
                            onClick = {
                                onUpdate { it.copy(accountId = null) }
                                accountExpanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    onUpdate { it.copy(accountId = account.id) }
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "周期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = editorState.scheduleType == RecurringTemplateScheduleType.WEEKLY,
                        onClick = { onUpdate { it.copy(scheduleType = RecurringTemplateScheduleType.WEEKLY) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("按周")
                    }
                    SegmentedButton(
                        selected = editorState.scheduleType == RecurringTemplateScheduleType.MONTHLY,
                        onClick = { onUpdate { it.copy(scheduleType = RecurringTemplateScheduleType.MONTHLY) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("按月")
                    }
                    SegmentedButton(
                        selected = editorState.scheduleType == RecurringTemplateScheduleType.CUSTOM_DAYS,
                        onClick = { onUpdate { it.copy(scheduleType = RecurringTemplateScheduleType.CUSTOM_DAYS) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("自定义天数")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = editorState.intervalCount,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onUpdate { it.copy(intervalCount = value) }
                        }
                    },
                    label = { Text("重复间隔") },
                    supportingText = {
                        Text(RecurringTemplateSchedulePolicy.describeSchedule(editorState.scheduleType, editorState.intervalCount.toIntOrNull() ?: 1))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(
                        label = "开始日期",
                        date = editorState.startDate,
                        onClick = { startDatePickerOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = "下次到期",
                        date = editorState.nextDueDate,
                        onClick = { nextDatePickerOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用到期提醒",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "后续可以扩展为通知提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editorState.reminderEnabled,
                        onCheckedChange = { checked -> onUpdate { it.copy(reminderEnabled = checked) } }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用模板",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "停用后不会再参与手动生成或到期提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editorState.isEnabled,
                        onCheckedChange = { checked -> onUpdate { it.copy(isEnabled = checked) } }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = editorState.noteTemplate,
                    onValueChange = { value -> onUpdate { it.copy(noteTemplate = value) } },
                    label = { Text("备注模板") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                DateField(
                    label = "结束日期（可选）",
                    date = editorState.endDate,
                    onClick = { endDatePickerOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editorState.endDateEnabled
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用结束日期",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "到期后可自动停用模板",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editorState.endDateEnabled,
                        onCheckedChange = { checked ->
                            onUpdate { it.copy(endDateEnabled = checked) }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    }
                    Text("保存模板")
                }
            }
        }

        if (startDatePickerOpen) {
            DatePickerDialog(
                onDismissRequest = { startDatePickerOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            startDatePickerState.selectedDateMillis?.let { millis ->
                                onUpdate { it.copy(startDate = millis.toLocalDate()) }
                            }
                            startDatePickerOpen = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { startDatePickerOpen = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = startDatePickerState)
            }
        }

        if (nextDatePickerOpen) {
            DatePickerDialog(
                onDismissRequest = { nextDatePickerOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            nextDatePickerState.selectedDateMillis?.let { millis ->
                                onUpdate { it.copy(nextDueDate = millis.toLocalDate()) }
                            }
                            nextDatePickerOpen = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { nextDatePickerOpen = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = nextDatePickerState)
            }
        }

        if (endDatePickerOpen) {
            DatePickerDialog(
                onDismissRequest = { endDatePickerOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            endDatePickerState.selectedDateMillis?.let { millis ->
                                onUpdate {
                                    it.copy(
                                        endDateEnabled = true,
                                        endDate = millis.toLocalDate()
                                    )
                                }
                            }
                            endDatePickerOpen = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { endDatePickerOpen = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = endDatePickerState)
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = if (enabled) {
                "$label：${date.format(RecurringDateFormatter)}"
            } else {
                "$label：未设置"
            }
        )
    }
}

private fun templateStatusText(template: RecurringTemplate): String {
    if (!template.isEnabled) {
        return "已停用"
    }
    val diffDays = template.nextDueDate.toEpochDay() - LocalDate.now().toEpochDay()
    return when {
        diffDays < 0 -> "已逾期 ${abs(diffDays)} 天"
        diffDays == 0L -> "今天到期"
        diffDays == 1L -> "明天到期"
        else -> "${diffDays} 天后到期"
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
