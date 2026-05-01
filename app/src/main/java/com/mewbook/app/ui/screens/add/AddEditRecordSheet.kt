package com.mewbook.app.ui.screens.add

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.CategorySelectionPolicy
import com.mewbook.app.ui.components.AccountTypeIconBadge
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.CategoryChip
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeaderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecordSheet(
    categories: List<Category>,
    accounts: List<Account>,
    recentNotesByCategory: Map<Long, List<String>>,
    defaultAccountId: Long?,
    defaultDate: LocalDate,
    editingRecord: Record?,
    initialType: RecordType = RecordType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (Double, RecordType, Long, String?, LocalDate, Long?) -> Unit,
    onDelete: (Long) -> Unit
) {
    val editingKey = editingRecord?.id ?: -1L
    val initialTypeName = initialType.name
    var selectedTypeName by rememberSaveable(editingKey, initialTypeName) {
        mutableStateOf((editingRecord?.type ?: initialType).name)
    }
    var selectedCategoryId by rememberSaveable(editingKey, initialTypeName) {
        mutableLongStateOf(editingRecord?.categoryId ?: 0L)
    }
    var selectedAccountId by rememberSaveable(editingKey, initialTypeName) {
        mutableLongStateOf(
            editingRecord?.accountId ?: if (editingRecord == null) {
                defaultAccountId ?: 0L
            } else {
                0L
            }
        )
    }
    var note by rememberSaveable(editingKey, initialTypeName) { mutableStateOf(editingRecord?.note.orEmpty()) }
    var noteDraft by rememberSaveable(editingKey, initialTypeName) { mutableStateOf(editingRecord?.note.orEmpty()) }
    var selectedDateEpochDay by rememberSaveable(editingKey, initialTypeName) {
        mutableLongStateOf((editingRecord?.date ?: defaultDate).toEpochDay())
    }
    var amountExpression by rememberSaveable(editingKey, initialTypeName) {
        mutableStateOf(editingRecord?.let { AmountExpressionHelper.formatInitialExpression(it.amount) }.orEmpty())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var isKeyboardVisible by rememberSaveable(editingKey, initialTypeName) {
        mutableStateOf((editingRecord?.categoryId ?: 0L) > 0L)
    }

    val selectedType = remember(selectedTypeName) { RecordType.valueOf(selectedTypeName) }
    val selectedDate = remember(selectedDateEpochDay) { LocalDate.ofEpochDay(selectedDateEpochDay) }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val displayCategories = remember(categories, selectedType, selectedCategoryId) {
        CategorySelectionPolicy.recordSelectionCandidates(
            categories = categories,
            type = selectedType,
            selectedCategoryId = selectedCategoryId
        ).sortedWith(
            compareBy<Category> { it.parentId != null }
                .thenBy { parentSortOrder(it, categoriesById) }
                .thenBy { it.sortOrder }
                .thenBy { it.id }
        )
    }
    val selectedCategory = remember(selectedCategoryId, categoriesById) { categoriesById[selectedCategoryId] }
    val recentNotes = remember(selectedCategoryId, recentNotesByCategory) {
        recentNotesByCategory[selectedCategoryId].orEmpty()
    }
    val canSave = selectedCategory != null && AmountExpressionHelper.canSave(amountExpression)
    val showKeyboard = isKeyboardVisible && selectedCategory != null
    val actionTint = if (selectedType == RecordType.EXPENSE) ExpenseRed else IncomeGreen
    val keyboardSurfaceColor = if (selectedType == RecordType.EXPENSE) Color(0xFF4B2B1F) else Color(0xFF1F4332)
    val density = LocalDensity.current
    val collapseKeyboardInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(selectedType) {
        if (selectedCategoryId != 0L && categoriesById[selectedCategoryId]?.type != selectedType) {
            selectedCategoryId = 0L
            isKeyboardVisible = false
        }
    }

    LaunchedEffect(editingRecord?.id, defaultAccountId) {
        if (editingRecord == null && selectedAccountId == 0L && defaultAccountId != null) {
            selectedAccountId = defaultAccountId
        }
    }

    LaunchedEffect(editingRecord?.id, defaultDate) {
        if (editingRecord == null) {
            selectedDateEpochDay = defaultDate.toEpochDay()
        }
    }

    BackHandler(onBack = onDismiss)
    AddRecordScreenSystemBarsEffect(
        showKeyboard = showKeyboard,
        keyboardSurfaceColor = keyboardSurfaceColor
    )
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                AddRecordHeader(editingRecord != null, onDismiss, editingRecord?.let { { onDelete(it.id) } })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .then(
                            if (showKeyboard) {
                                Modifier.clickable(
                                    interactionSource = collapseKeyboardInteraction,
                                    indication = null
                                ) { isKeyboardVisible = false }
                            } else {
                                Modifier
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selectedType == RecordType.EXPENSE, { selectedTypeName = RecordType.EXPENSE.name }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("支出") }
                        SegmentedButton(selectedType == RecordType.INCOME, { selectedTypeName = RecordType.INCOME.name }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("收入") }
                    }
                    CategoryPanel(
                        categories = displayCategories,
                        selectedCategory = selectedCategory,
                        selectedCategoryId = selectedCategoryId,
                        modifier = Modifier.weight(1f),
                        onCategorySelected = {
                            selectedCategoryId = it.id
                            isKeyboardVisible = true
                        }
                    )
                }
                selectedCategory?.let { category ->
                    AnimatedVisibility(showKeyboard, enter = slideInVertically { with(density) { 48.dp.roundToPx() } } + fadeIn(), exit = slideOutVertically { with(density) { 48.dp.roundToPx() } } + fadeOut()) {
                        KeyboardPanel(
                            selectedCategory = category,
                            selectedDate = selectedDate,
                            note = note,
                            recentNotes = recentNotes,
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            keyboardSurfaceColor = keyboardSurfaceColor.copy(alpha = 0.96f),
                            keyAccent = actionTint,
                            amountExpression = amountExpression,
                            canSave = canSave,
                            onDateClick = { showDatePicker = true },
                            onNoteClick = {
                                noteDraft = note
                                showNoteDialog = true
                            },
                            onRecentNoteSelected = {
                                note = it
                                noteDraft = it
                            },
                            onAccountSelected = { selectedAccountId = if (selectedAccountId == it.id) 0L else it.id },
                            onKeyPress = { amountExpression = AmountExpressionHelper.append(amountExpression, it) },
                            onDelete = { amountExpression = AmountExpressionHelper.backspace(amountExpression) },
                            onClear = { amountExpression = AmountExpressionHelper.clear() },
                            onDismissKeyboard = { isKeyboardVisible = false },
                            onSave = {
                                val amount = AmountExpressionHelper.evaluate(amountExpression)
                                if (amount != null && amount > 0) {
                                    onSave(amount, selectedType, category.id, note.ifBlank { null }, selectedDate, selectedAccountId.takeIf { it > 0L })
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDateEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showNoteDialog) {
        NoteEditorDialog(
            value = noteDraft,
            onValueChange = { noteDraft = it },
            onDismiss = { showNoteDialog = false },
            onConfirm = { note = noteDraft.trim(); showNoteDialog = false }
        )
    }
}

@Composable
private fun AddRecordScreenSystemBarsEffect(
    showKeyboard: Boolean,
    keyboardSurfaceColor: Color
) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return
    val window = activity.window
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navBarAppearanceColor = if (showKeyboard) keyboardSurfaceColor else MaterialTheme.colorScheme.surface
    val statusBarAppearanceColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    DisposableEffect(showKeyboard, keyboardSurfaceColor, window, view) {
        val previousNavigationBarColor = window.navigationBarColor
        val previousStatusBarColor = window.statusBarColor
        val previousLightNavigationBars =
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars
        val previousLightStatusBars =
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars
        val previousNavigationBarContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced
        } else {
            null
        }

        window.statusBarColor = statusBarAppearanceColor.toArgb()
        window.navigationBarColor = navBarAppearanceColor.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            statusBarAppearanceColor.luminance() > 0.5f
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
            navBarAppearanceColor.luminance() > 0.5f

        onDispose {
            window.statusBarColor = previousStatusBarColor
            window.navigationBarColor = previousNavigationBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && previousNavigationBarContrastEnforced != null) {
                window.isNavigationBarContrastEnforced = previousNavigationBarContrastEnforced
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                previousLightStatusBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                previousLightNavigationBars
        }
    }
}

@Composable
private fun AddRecordHeader(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "关闭")
        }
        Text(
            text = if (isEditing) "编辑记账" else "新增记账",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除记录", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun CompactInfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AmountConsoleCard(
    selectedCategory: Category?,
    amountExpression: String
) {
    val amountScrollState = rememberScrollState()

    LaunchedEffect(amountExpression, amountScrollState.maxValue) {
        amountScrollState.scrollTo(amountScrollState.maxValue)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SelectedCategoryBadge(selectedCategory)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(amountScrollState)
            ) {
                Text(
                    text = amountExpression.ifBlank { "0" },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        letterSpacing = (-1.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SelectedCategoryBadge(selectedCategory: Category?) {
    if (selectedCategory == null) {
        return
    }

    Surface(shape = RoundedCornerShape(18.dp), color = Color(selectedCategory.color).copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CategoryIconBadge(
                category = selectedCategory,
                emphasized = true,
                containerSize = 20.dp,
                iconSize = 12.dp
            )
            Text(
                text = selectedCategory.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryPanel(
    categories: List<Category>,
    selectedCategory: Category?,
    selectedCategoryId: Long,
    modifier: Modifier = Modifier,
    onCategorySelected: (Category) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                selectedCategory?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(it.color),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories, key = { it.id }, contentType = { "category" }) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardPanel(
    selectedCategory: Category,
    selectedDate: LocalDate,
    note: String,
    recentNotes: List<String>,
    accounts: List<Account>,
    selectedAccountId: Long,
    keyboardSurfaceColor: Color,
    keyAccent: Color,
    amountExpression: String,
    canSave: Boolean,
    onDateClick: () -> Unit,
    onNoteClick: () -> Unit,
    onRecentNoteSelected: (String) -> Unit,
    onAccountSelected: (Account) -> Unit,
    onKeyPress: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onDismissKeyboard: () -> Unit,
    onSave: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > 56f) onDismissKeyboard()
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragOffset = 0f
                    }
                )
            },
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        color = keyboardSurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.28f))
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactInfoPill(
                    icon = Icons.Filled.CalendarMonth,
                    text = selectedDate.format(HeaderDateFormatter),
                    onClick = onDateClick
                )
                CompactInfoPill(
                    icon = Icons.Filled.EditNote,
                    text = note.ifBlank { "添加备注" },
                    onClick = onNoteClick
                )
            }
            if (recentNotes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(recentNotes, key = { it }, contentType = { "recent-note" }) { recentNote ->
                        FilterChip(
                            selected = note.trim() == recentNote,
                            onClick = { onRecentNoteSelected(recentNote) },
                            label = {
                                Text(
                                    text = recentNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White.copy(alpha = 0.82f),
                                selectedContainerColor = keyAccent.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
            AmountConsoleCard(
                selectedCategory = selectedCategory,
                amountExpression = amountExpression
            )
            if (accounts.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(accounts, key = { it.id }, contentType = { "account" }) { account ->
                        FilterChip(
                            selected = selectedAccountId == account.id,
                            onClick = { onAccountSelected(account) },
                            label = {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                AccountTypeIconBadge(
                                    type = account.type,
                                    accentColor = Color(account.color),
                                    containerSize = 16.dp,
                                    iconSize = 10.dp,
                                    emphasized = selectedAccountId == account.id
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White.copy(alpha = 0.82f),
                                selectedContainerColor = keyAccent.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                                iconColor = Color.White.copy(alpha = 0.82f),
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }
            KeyboardRow {
                KeyboardKey("7", { onKeyPress('7') })
                KeyboardKey("8", { onKeyPress('8') })
                KeyboardKey("9", { onKeyPress('9') })
                KeyboardKey("删", onDelete, containerColor = Color.White.copy(alpha = 0.12f))
            }
            KeyboardRow {
                KeyboardKey("4", { onKeyPress('4') })
                KeyboardKey("5", { onKeyPress('5') })
                KeyboardKey("6", { onKeyPress('6') })
                KeyboardKey("+", { onKeyPress('+') }, containerColor = keyAccent.copy(alpha = 0.18f))
            }
            KeyboardRow {
                KeyboardKey("1", { onKeyPress('1') })
                KeyboardKey("2", { onKeyPress('2') })
                KeyboardKey("3", { onKeyPress('3') })
                KeyboardKey("-", { onKeyPress('-') }, containerColor = keyAccent.copy(alpha = 0.18f))
            }
            KeyboardRow {
                KeyboardKey(".", { onKeyPress('.') })
                KeyboardKey("0", { onKeyPress('0') })
                KeyboardKey("清空", onClear, containerColor = Color.White.copy(alpha = 0.12f))
                KeyboardKey(
                    label = if (amountExpression.isBlank()) "保存" else "完成",
                    onClick = onSave,
                    enabled = canSave,
                    containerColor = keyAccent
                )
            }
        }
    }
}

@Composable
private fun KeyboardRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun RowScope.KeyboardKey(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = Color.White.copy(alpha = 0.10f)
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.45f)
        )
    }
}

@Composable
private fun NoteEditorDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑备注") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("比如：工作日咖啡、周末和朋友吃饭") },
                colors = OutlinedTextFieldDefaults.colors()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun parentSortOrder(
    category: Category,
    categoriesById: Map<Long, Category>
): Int {
    return category.parentId?.let { categoriesById[it]?.sortOrder } ?: category.sortOrder
}
