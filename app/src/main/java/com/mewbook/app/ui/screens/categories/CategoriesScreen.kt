package com.mewbook.app.ui.screens.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.CategorySelectionPolicy
import com.mewbook.app.domain.policy.HapticFeedbackPolicy
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.getIconForCategory
import com.mewbook.app.ui.components.rememberMewHapticFeedback
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.LocalMewBookSemanticColors
import com.mewbook.app.ui.theme.clayCardShadow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// 可选的图标列表
val availableIcons = listOf(
    "restaurant", "free_breakfast", "lunch_dining", "dinner_dining", "local_cafe",
    "coffee", "local_drink", "takeout_dining", "bakery_dining", "ramen_dining",
    "restaurant_menu", "emoji_food_beverage", "emoji_nature", "apple",
    "cookie", "local_bar",
    "directions_car", "directions_bus", "commute", "train", "directions_subway",
    "tram", "flight", "airport_shuttle", "directions_boat", "local_taxi",
    "local_gas_station", "local_parking", "local_shipping", "car_repair",
    "car_rental", "local_car_wash", "electric_car", "ev_station", "two_wheeler",
    "moped", "electric_bike", "electric_scooter", "pedal_bike",
    "shopping_bag", "local_mall", "shopping_basket", "local_grocery_store",
    "local_convenience_store", "store", "storefront", "sell", "inventory_2",
    "devices", "laptop", "computer", "print", "face", "brush", "spa",
    "diamond", "watch", "toys", "baby_changing_station", "boy", "checkroom",
    "cleaning_services", "child_care", "kitchen",
    "home", "apartment", "weekend", "bed", "chair", "yard", "water_drop",
    "meeting_room", "local_fire_department", "construction", "plumbing",
    "electrical_services", "home_repair_service", "phone_android", "wifi",
    "medical_services", "local_hospital", "medication", "local_pharmacy", "vaccines",
    "medical_information", "health_and_safety",
    "school", "menu_book", "auto_stories", "edit", "tablet_android", "science",
    "calculate", "work",
    "sports_esports", "fitness_center", "directions_run", "sports_basketball",
    "music_note", "mic", "pool", "forum", "theater_comedy", "photo_camera",
    "palette",
    "pets", "people", "redeem", "volunteer_activism", "card_giftcard",
    "travel_explore", "luggage", "hotel", "beach_access",
    "payments", "receipt_long", "receipt", "trending_up", "attach_money", "paid",
    "wallet", "account_balance_wallet", "credit_card", "currency_yuan",
    "account_balance", "savings", "monetization_on",
    "inbox", "cloud", "star", "favorite", "more_horiz", "eco"
)

// 可选的颜色列表
val availableColors = listOf(
    0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFE66D, 0xFF95E1D3, 0xFFAA96DA,
    0xFFF38181, 0xFF7C83FD, 0xFF45B7D1, 0xFF4CAF50, 0xFFFF9F43,
    0xFF9C27B0, 0xFFFF69B4, 0xFF795548, 0xFF607D8B, 0xFF9E9E9E,
    0xFF8B4513, 0xFFB8860B, 0xFF5D4037, 0xFF00BCD4, 0xFFE91E63
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = rememberMewHapticFeedback(uiState.keyPressHapticEnabled)
    val semanticColors = LocalMewBookSemanticColors.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentType = if (selectedTabIndex == 0) RecordType.EXPENSE else RecordType.INCOME

    fun performWithHaptic(
        interaction: HapticFeedbackPolicy.Interaction,
        action: () -> Unit
    ) {
        hapticFeedback.perform(interaction)
        action()
    }

    val expenseCategories = CategorySelectionPolicy.visibleCategories(
        categories = uiState.categories,
        type = RecordType.EXPENSE
    )
    val incomeCategories = CategorySelectionPolicy.visibleCategories(
        categories = uiState.categories,
        type = RecordType.INCOME
    )

    // Add Dialog
    if (uiState.showAddDialog) {
        AddCategoryDialog(
            type = uiState.selectedType,
            hapticFeedbackEnabled = uiState.keyPressHapticEnabled,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, icon, color ->
                viewModel.addCategory(name, icon, color, uiState.selectedType)
            }
        )
    }

    // Edit Dialog
    if (uiState.showEditDialog && uiState.editingCategory != null) {
        EditCategoryDialog(
            category = uiState.editingCategory!!,
            hapticFeedbackEnabled = uiState.keyPressHapticEnabled,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, icon, color ->
                viewModel.updateCategory(uiState.editingCategory!!, name, icon, color)
            },
            onDelete = {
                viewModel.deleteCategory(uiState.editingCategory!!)
                viewModel.hideEditDialog()
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MewCompactTopAppBar(
                title = "分类管理",
                navigationIcon = {
                    IconButton(
                        onClick = {
                            performWithHaptic(HapticFeedbackPolicy.Interaction.RowClick, onNavigateBack)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    performWithHaptic(HapticFeedbackPolicy.Interaction.RowClick) {
                        viewModel.showAddDialog(if (selectedTabIndex == 0) RecordType.EXPENSE else RecordType.INCOME)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类")
            }
        }
    ) { paddingValues ->
        val displayedCategories = if (selectedTabIndex == 0) expenseCategories else incomeCategories
        val typeLabel = if (selectedTabIndex == 0) "支出" else "收入"
        val accentColor = if (selectedTabIndex == 0) semanticColors.expense else semanticColors.income
        val lazyListState = rememberLazyListState()

        val reorderableState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            onMove = { from, to ->
                val fromCategoryId = from.key as? Long
                val toCategoryId = to.key as? Long
                if (fromCategoryId != null && toCategoryId != null) {
                    viewModel.moveCategory(
                        fromCategoryId = fromCategoryId,
                        toCategoryId = toCategoryId,
                        type = currentType
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "overview", contentType = "overview") {
                CategoryOverviewCard(
                    typeLabel = typeLabel,
                    totalCount = displayedCategories.size,
                    defaultCount = displayedCategories.count { it.isDefault },
                    customCount = displayedCategories.count { !it.isDefault },
                    accentColor = accentColor,
                    icon = if (selectedTabIndex == 0) Icons.Filled.Restaurant else Icons.Filled.Payments
                )
            }

            item(key = "type-switcher", contentType = "type-switcher") {
                CategoryTypeSwitcher(
                    selectedTabIndex = selectedTabIndex,
                    expenseCount = expenseCategories.size,
                    incomeCount = incomeCategories.size,
                    onSelect = {
                        performWithHaptic(HapticFeedbackPolicy.Interaction.Selection) {
                            selectedTabIndex = it
                        }
                    }
                )
            }

            item(key = "section-header", contentType = "section-header") {
                SettingsSectionHeader(
                    title = "$typeLabel 分类列表",
                    description = "默认与自定义分类统一显示，当前顺序会同步到记账入口"
                )
            }

            if (!uiState.isLoading && displayedCategories.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    EmptyCategoriesState(typeLabel = typeLabel)
                }
            }

            items(
                items = displayedCategories,
                key = { it.id },
                contentType = { "category" }
            ) { category ->
                ReorderableItem(reorderableState, key = category.id) { isDragging ->
                    CategoryItemCard(
                        category = category,
                        onEditClick = {
                            performWithHaptic(HapticFeedbackPolicy.Interaction.RowClick) {
                                viewModel.showEditDialog(category)
                            }
                        },
                        isDragging = isDragging,
                        dragHandleModifier = Modifier
                            .size(48.dp)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.LongPressAction)
                                },
                                onDragStopped = {
                                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryOverviewCard(
    typeLabel: String,
    totalCount: Int,
    defaultCount: Int,
    customCount: Int,
    accentColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$typeLabel 分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "单层分类会同步用于记账、预算和统计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryMetricPill(
                    label = "全部",
                    value = totalCount,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                CategoryMetricPill(
                    label = "默认",
                    value = defaultCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                CategoryMetricPill(
                    label = "自定义",
                    value = customCount,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryMetricPill(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
        color = color.copy(alpha = 0.09f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryTypeSwitcher(
    selectedTabIndex: Int,
    expenseCount: Int,
    incomeCount: Int,
    onSelect: (Int) -> Unit
) {
    val semanticColors = LocalMewBookSemanticColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CategoryTypeSegment(
                selected = selectedTabIndex == 0,
                label = "支出",
                count = expenseCount,
                icon = Icons.Filled.Restaurant,
                accentColor = semanticColors.expense,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f)
            )
            CategoryTypeSegment(
                selected = selectedTabIndex == 1,
                label = "收入",
                count = incomeCount,
                icon = Icons.Filled.Payments,
                accentColor = semanticColors.income,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryTypeSegment(
    selected: Boolean,
    label: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        accentColor.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val borderColor = if (selected) {
        accentColor.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(ClayDesign.ButtonRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryItemCard(
    category: Category,
    onEditClick: () -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    val accentColor = Color(category.color)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isDragging) {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(ClayDesign.ButtonRadius))
                    .clickable(onClick = onEditClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    category = category,
                    emphasized = true,
                    containerSize = 44.dp,
                    iconSize = 23.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CategoryKindBadge(isDefault = category.isDefault)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = dragHandleModifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDragging) {
                            accentColor.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDragging) {
                            accentColor.copy(alpha = 0.34f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "长按拖拽排序",
                    tint = if (isDragging) {
                        accentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryKindBadge(isDefault: Boolean) {
    val backgroundColor = if (isDefault) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isDefault) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Text(
        text = if (isDefault) "默认" else "自定义",
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun EmptyCategoriesState(typeLabel: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "暂无$typeLabel 分类",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "可从右下角添加新的$typeLabel 分类",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    type: RecordType,
    hapticFeedbackEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(availableIcons.first()) }
    var selectedColor by remember { mutableStateOf(availableColors.first()) }
    val hapticFeedback = rememberMewHapticFeedback(hapticFeedbackEnabled)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建${if (type == RecordType.EXPENSE) "支出" else "收入"}分类") },
        text = {
            CategoryEditorFields(
                type = type,
                name = name,
                selectedIcon = selectedIcon,
                selectedColor = selectedColor,
                onNameChange = { name = it },
                onIconSelect = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                    selectedIcon = it
                },
                onColorSelect = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                    selectedColor = it
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                    onConfirm(name, selectedIcon, selectedColor)
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                    onDismiss()
                }
            ) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCategoryDialog(
    category: Category,
    hapticFeedbackEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.icon) }
    var selectedColor by remember { mutableStateOf(category.color) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val hapticFeedback = rememberMewHapticFeedback(hapticFeedbackEnabled)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${category.name}」吗？删除后相关记账记录将无法关联到此分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            CategoryEditorFields(
                type = category.type,
                name = name,
                selectedIcon = selectedIcon,
                selectedColor = selectedColor,
                onNameChange = { name = it },
                onIconSelect = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                    selectedIcon = it
                },
                onColorSelect = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                    selectedColor = it
                }
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        hapticFeedback.perform(HapticFeedbackPolicy.Interaction.LongPressAction)
                        showDeleteConfirm = true
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = {
                        hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                        onConfirm(name, selectedIcon, selectedColor)
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                    onDismiss()
                }
            ) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorFields(
    type: RecordType,
    name: String,
    selectedIcon: String,
    selectedColor: Long,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onColorSelect: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CategoryPreviewCard(
            name = name.ifBlank { "新分类" },
            icon = selectedIcon,
            color = selectedColor,
            type = type
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("分类名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(ClayDesign.InputRadius)
        )

        CategoryEditorSectionLabel(text = "图标")
        CategoryIconPickerGrid(
            selectedIcon = selectedIcon,
            selectedColor = selectedColor,
            onSelect = onIconSelect
        )

        CategoryEditorSectionLabel(text = "颜色")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableColors.forEach { color ->
                CategoryColorOption(
                    color = color,
                    selected = selectedColor == color,
                    onSelect = { onColorSelect(color) }
                )
            }
        }
    }
}

@Composable
private fun CategoryPreviewCard(
    name: String,
    icon: String,
    color: Long,
    type: RecordType
) {
    val category = Category(
        name = name,
        icon = icon,
        color = color,
        type = type,
        isDefault = false,
        sortOrder = 0
    )

    Surface(
        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
        color = Color(color).copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(color).copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                category = category,
                emphasized = true,
                containerSize = 44.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (type == RecordType.EXPENSE) "支出分类" else "收入分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryEditorSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CategoryColorOption(
    color: Long,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val swatchColor = Color(color)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "选中颜色",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CategoryIconPickerGrid(
    selectedIcon: String,
    selectedColor: Long,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 236.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableIcons, key = { it }) { iconName ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selectedIcon == iconName) {
                            Color(selectedColor).copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        }
                    )
                    .border(
                        width = if (selectedIcon == iconName) 2.dp else 1.dp,
                        color = if (selectedIcon == iconName) {
                            Color(selectedColor)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(iconName) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(iconName),
                    contentDescription = "选择图标 $iconName",
                    tint = if (selectedIcon == iconName) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
