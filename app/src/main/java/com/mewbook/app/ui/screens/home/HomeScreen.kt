package com.mewbook.app.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.mewbook.app.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.policy.HomeScreenLayoutPolicy
import com.mewbook.app.ui.components.BudgetPeriodNavigator
import com.mewbook.app.ui.components.RecordItem
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.screens.add.AddEditRecordSheet
import com.mewbook.app.ui.screens.add.QuickAddRecordSheet
import com.mewbook.app.ui.theme.BudgetWarning
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ============================================
// Warm Claymorphism Home Screen
// 温暖黏土风首页
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    searchResetToken: Int = 0,
    onAddSheetVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isDarkTheme = LocalIsDarkTheme.current
    var pendingDeleteRecordId by remember { mutableLongStateOf(0L) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showQuickFabMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.showAddEditSheet) {
        onAddSheetVisibilityChanged(uiState.showAddEditSheet)
    }

    LaunchedEffect(searchResetToken) {
        if (searchResetToken > 0) {
            viewModel.exitSearchMode()
        }
    }

    BackHandler(
        enabled = HomeScreenLayoutPolicy.consumeBackPress(
            isSearchMode = uiState.isSearchMode,
            isAddEditSheetVisible = uiState.showAddEditSheet
        )
    ) {
        viewModel.exitSearchMode()
    }

    BackHandler(enabled = showQuickFabMenu) {
        showQuickFabMenu = false
    }

    val showScrollableHomeHeader = HomeScreenLayoutPolicy.showHomeHeaderAsScrollableContent(
        isSearchMode = uiState.isSearchMode,
        hasRecords = uiState.records.isNotEmpty()
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // 温暖渐变顶部导航栏
                MewCompactTopAppBar(
                    title = if (uiState.isSearchMode) "搜索记录" else "喵喵记账",
                    titleContent = if (uiState.isSearchMode) {
                        null
                    } else {
                        {
                            Text(
                                text = "🐱",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "喵喵记账",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (uiState.isSearchMode) {
                                    viewModel.exitSearchMode()
                                } else {
                                    viewModel.enterSearchMode()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isSearchMode) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = if (uiState.isSearchMode) "关闭搜索" else "搜索记录"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                HomeFloatingAddButton(
                    isMenuExpanded = showQuickFabMenu,
                    isDarkTheme = isDarkTheme,
                    onAddClick = {
                        showQuickFabMenu = false
                        viewModel.showAddSheet()
                    },
                    onLongPress = { showQuickFabMenu = true },
                    onQuickExpenseClick = {
                        showQuickFabMenu = false
                        viewModel.showQuickAddSheet(RecordType.EXPENSE)
                    },
                    onQuickIncomeClick = {
                        showQuickFabMenu = false
                        viewModel.showQuickAddSheet(RecordType.INCOME)
                    }
                )
            }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (uiState.isSearchMode) {
                        HomeSearchField(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) }
                        )
                    } else {
                        BudgetPeriodNavigator(
                            periodLabel = uiState.periodLabel,
                            canGoNext = uiState.canGoNext,
                            onPrevious = { viewModel.previousPeriod() },
                            onNext = { viewModel.nextPeriod() },
                            onPeriodLabelClick = { showDatePickerDialog = true },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    if (!uiState.isSearchMode && !uiState.isLoading && !showScrollableHomeHeader) {
                        if (uiState.showHomeOverviewCards) {
                            SummaryCard(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                totalIncome = uiState.totalIncome,
                                totalExpense = uiState.totalExpense,
                                totalBudget = uiState.totalBudget,
                                budgetRemaining = uiState.budgetRemaining
                            )
                        }
                    }

                    when {
                        !uiState.isSearchMode && uiState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        uiState.isSearchMode && uiState.searchQuery.isBlank() -> {
                            SearchStateMessage(
                                title = "搜索当前账本记录",
                                subtitle = "支持搜索备注、分类、金额和账户名",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }

                        uiState.isSearchMode && uiState.records.isEmpty() -> {
                            SearchStateMessage(
                                title = "没有找到结果",
                                subtitle = "换个关键词试试，比如分类、金额或账户名",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }

                        !uiState.isSearchMode && uiState.records.isEmpty() -> {
                            HomeEmptyState(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }

                        else -> {
                            if (uiState.isSearchMode) {
                                Text(
                                    text = "找到 ${uiState.searchResultCount} 条记录",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                                )
                            }
                            HomeRecordList(
                                modifier = Modifier.weight(1f),
                                records = uiState.records,
                                categories = uiState.categories,
                                headerContent = if (showScrollableHomeHeader && uiState.showHomeOverviewCards) {
                                    {
                                        SummaryCard(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            totalIncome = uiState.totalIncome,
                                            totalExpense = uiState.totalExpense,
                                            totalBudget = uiState.totalBudget,
                                            budgetRemaining = uiState.budgetRemaining
                                        )
                                    }
                                } else {
                                    null
                                },
                                onRecordClick = { record -> viewModel.showRecordDetail(record) }
                            )
                        }
                    }
                }
            }

        uiState.browsingRecord?.let { browsingRecord ->
            RecordDetailDialog(
                record = browsingRecord,
                category = uiState.categories[browsingRecord.categoryId],
                account = uiState.accounts.firstOrNull { account -> account.id == browsingRecord.accountId },
                onDismiss = { viewModel.hideRecordDetail() },
                onEdit = { record -> viewModel.editRecordFromDetail(record) }
            )
        }

        if (uiState.showAddEditSheet && uiState.addEntryMode == HomeAddEntryMode.QUICK && uiState.editingRecord == null) {
            QuickAddRecordSheet(
                type = uiState.newRecordType ?: RecordType.EXPENSE,
                categories = uiState.quickCategories,
                accounts = uiState.accounts,
                defaultAccountId = uiState.defaultAccountId,
                defaultDate = uiState.anchorDate,
                onDismiss = { viewModel.hideAddEditSheet() },
                onOpenFullEditor = { viewModel.expandQuickAddSheet() },
                onSave = { amount, type, categoryId, note, date, accountId ->
                    viewModel.saveRecord(amount, type, categoryId, note, date, accountId)
                }
            )
        } else if (uiState.showAddEditSheet) {
            AddEditRecordSheet(
                categories = uiState.categories.values.toList(),
                accounts = uiState.accounts,
                recentNotesByCategory = uiState.recentNotesByCategory,
                defaultAccountId = uiState.defaultAccountId,
                defaultDate = uiState.anchorDate,
                editingRecord = uiState.editingRecord,
                initialType = uiState.newRecordType ?: RecordType.EXPENSE,
                onDismiss = { viewModel.hideAddEditSheet() },
                onSave = { amount, type, categoryId, note, date, accountId ->
                    viewModel.saveRecord(amount, type, categoryId, note, date, accountId)
                },
                onDelete = { id ->
                    pendingDeleteRecordId = id
                    showDeleteConfirmDialog = true
                }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    pendingDeleteRecordId = 0L
                },
                title = { Text("确认删除") },
                text = { Text("删除后无法恢复，确定要删除这条记录吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteRecord(pendingDeleteRecordId)
                            viewModel.hideAddEditSheet()
                            showDeleteConfirmDialog = false
                            pendingDeleteRecordId = 0L
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            pendingDeleteRecordId = 0L
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }

        if (showDatePickerDialog) {
            HomeDatePickerDialog(
                initialDate = uiState.anchorDate,
                onDismiss = { showDatePickerDialog = false },
                onDateSelected = { selectedDate ->
                    viewModel.selectAnchorDate(selectedDate)
                    showDatePickerDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val initialDateMillis = initialDate.toDatePickerMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis,
        yearRange = 1900..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis.toDatePickerLocalDate() <= today
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= today.year
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.toDatePickerLocalDate()
                        ?.let(onDateSelected)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}

private fun LocalDate.toDatePickerMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toDatePickerLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFloatingAddButton(
    isMenuExpanded: Boolean,
    isDarkTheme: Boolean,
    onAddClick: () -> Unit,
    onLongPress: () -> Unit,
    onQuickExpenseClick: () -> Unit,
    onQuickIncomeClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val iconRotation by animateFloatAsState(
        targetValue = if (isMenuExpanded) 45f else 0f,
        label = "homeFabIconRotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            Column(
                modifier = Modifier.padding(end = 2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickFabAction(
                    label = "快速收入",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    tint = IncomeGreen,
                    onClick = onQuickIncomeClick
                )
                QuickFabAction(
                    label = "快速支出",
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    tint = ExpenseRed,
                    onClick = onQuickExpenseClick
                )
            }
        }

        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.18f else 0.4f)
                )
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.10f else 0.25f)
                )
                .size(56.dp)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = "添加记录",
                    onLongClickLabel = "显示快速记账",
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                    onClick = onAddClick
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加记录，长按显示快速记账",
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                        }
                )
            }
        }
    }
}

@Composable
private fun QuickFabAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = tint.copy(alpha = 0.18f)
            )
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HomeSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (query.isBlank()) {
                            Text(
                                text = "搜索备注、分类、金额、账户",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotBlank()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "清空搜索"
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchStateMessage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(34.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeRecordList(
    records: List<Record>,
    categories: Map<Long, Category>,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    onRecordClick: (com.mewbook.app.domain.model.Record) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(ClayDesign.CardSpacing)
    ) {
        if (headerContent != null) {
            item(
                key = "home-scrollable-header",
                contentType = "header"
            ) {
                headerContent()
            }
        }
        items(
            items = records,
            key = { it.id },
            contentType = { "record" }
        ) { record ->
            val category = categories[record.categoryId]
            RecordItem(
                record = record,
                categoryName = category?.name ?: "未知",
                categoryIcon = category?.icon ?: "more_horiz",
                categoryColor = category?.color ?: 0xFF808080,
                onClick = { onRecordClick(record) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    totalIncome: Double,
    totalExpense: Double,
    totalBudget: Double = 0.0,
    budgetRemaining: Double = 0.0
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val cardShadowPrimary = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.08f else 0.15f)
    val cardShadowSecondary = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.04f else 0.10f)
    val progressShadow = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.18f else 0.10f)

    // Claymorphism 卡片 - 多层阴影
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = ClayDesign.CardShadowElevation1,
                shape = RoundedCornerShape(ClayDesign.CardRadius),
                spotColor = cardShadowPrimary
            )
            .shadow(
                elevation = ClayDesign.CardShadowElevation2,
                shape = RoundedCornerShape(ClayDesign.CardRadius),
                spotColor = cardShadowSecondary
            ),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ClayDesign.CardPadding + 4.dp)
        ) {
            // 预算进度（如果有设置）
            if (totalBudget > 0) {
                // 预算剩余
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🍊 ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "预算剩余",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCurrency(budgetRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            budgetRemaining < 0 -> ExpenseRed
                            budgetRemaining < totalBudget * 0.2 -> BudgetWarning
                            else -> IncomeGreen
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 进度条 - Claymorphism 风格
                val progress = (totalExpense / totalBudget).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .shadow(2.dp, RoundedCornerShape(6.dp), spotColor = progressShadow)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(12.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = when {
                                        progress >= 0.9f -> listOf(ExpenseRed, ExpenseRed)
                                        progress >= 0.7f -> listOf(BudgetWarning, BudgetWarning)
                                        else -> listOf(IncomeGreen, IncomeGreen)
                                    }
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "已花费 ${formatCurrency(totalExpense)} / ${formatCurrency(totalBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 收入、支出、结余 - 三列布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            IncomeGreen.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "📈",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatCurrency(totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 支出
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            ExpenseRed.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "📉",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${formatCurrency(totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 结余
                val balance = totalIncome - totalExpense
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (balance >= 0) IncomeGreen.copy(alpha = 0.08f)
                            else ExpenseRed.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = if (balance >= 0) "💰" else "😿",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🍊",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击 + 添加第一笔记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
