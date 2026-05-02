package com.mewbook.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.CategorySelectionPolicy
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.getIconForCategory
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.clayCardShadow

// 可选的图标列表
val availableIcons = listOf(
    "restaurant", "directions_car", "shopping_bag", "movie", "school",
    "more_horiz", "payments", "card_giftcard", "trending_up", "attach_money",
    "work", "account_balance", "home", "free_breakfast", "lunch_dining",
    "dinner_dining", "local_cafe", "directions_bus", "train", "flight",
    "local_taxi", "local_gas_station", "local_parking", "devices", "face",
    "cleaning_services", "child_care", "kitchen", "water_drop", "phone_android",
    "sports_esports", "fitness_center", "music_note", "pets", "local_hospital",
    "local_bar", "people", "redeem", "menu_book", "edit", "sports_basketball",
    "wifi", "local_fire_department", "local_drink", "emoji_food_beverage", "nutrition",
    "tablet_android", "pool", "mic", "meeting_room", "inbox", "cloud", "star",
    "favorite", "monetization_on", "cookie", "eco", "local_mall", "medication",
    "health_and_safety", "directions_run", "weekend", "local_shipping", "forum",
    "auto_stories", "brush", "emoji_nature", "savings", "receipt_long", "apple",
    "baby_changing_station", "boy"
)

// 可选的颜色列表
val availableColors = listOf(
    0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFE66D, 0xFF95E1D3, 0xFFAA96DA,
    0xFFF38181, 0xFF7C83FD, 0xFF45B7D1, 0xFF4CAF50, 0xFFFF9F43,
    0xFF9C27B0, 0xFFFF69B4, 0xFF795548, 0xFF607D8B, 0xFF9E9E9E,
    0xFF8B4513, 0xFFB8860B, 0xFF5D4037, 0xFF00BCD4, 0xFFE91E63
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val expenseCategories = CategorySelectionPolicy.visibleTopLevelCategories(
        categories = uiState.categories,
        type = RecordType.EXPENSE
    )
    val incomeCategories = CategorySelectionPolicy.visibleTopLevelCategories(
        categories = uiState.categories,
        type = RecordType.INCOME
    )

    // Add Dialog
    if (uiState.showAddDialog) {
        AddCategoryDialog(
            type = uiState.selectedType,
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
        topBar = {
            MewCompactTopAppBar(
                title = "分类管理",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showAddDialog(if (selectedTabIndex == 0) RecordType.EXPENSE else RecordType.INCOME)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("支出") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("收入") }
                )
            }

            val displayedCategories = if (selectedTabIndex == 0) expenseCategories else incomeCategories

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingsSummaryCard(
                        icon = Icons.Filled.Edit,
                        title = if (selectedTabIndex == 0) "支出分类" else "收入分类",
                        subtitle = "当前显示 ${displayedCategories.size} 个分类。点击分类编辑，右侧箭头调整排序。"
                    )
                }

                item {
                    SettingsSectionHeader(title = "分类列表")
                }

                items(displayedCategories, key = { it.id }) { category ->
                    CategoryRowItem(
                        category = category,
                        canMoveUp = displayedCategories.firstOrNull()?.id != category.id,
                        canMoveDown = displayedCategories.lastOrNull()?.id != category.id,
                        onMoveUpClick = { viewModel.moveCategoryUp(category) },
                        onMoveDownClick = { viewModel.moveCategoryDown(category) },
                        onEditClick = { viewModel.showEditDialog(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(
    category: Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    onEditClick: () -> Unit
) {
    CategoryItemCard(
        category = category,
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        onMoveUpClick = onMoveUpClick,
        onMoveDownClick = onMoveDownClick,
        onClick = onEditClick
    )
}

@Composable
private fun CategoryItemCard(
    category: Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                category = category,
                emphasized = true,
                containerSize = 28.dp,
                iconSize = 15.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                if (category.isDefault) {
                    Text(
                        text = "默认分类",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "自定义",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onMoveUpClick,
                    enabled = canMoveUp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDownClick,
                    enabled = canMoveDown,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    type: RecordType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(availableIcons.first()) }
    var selectedColor by remember { mutableStateOf(availableColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加${if (type == RecordType.EXPENSE) "支出" else "收入"}分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategoryIconPickerGrid(
                    selectedIcon = selectedIcon,
                    selectedColor = selectedColor,
                    onSelect = { selectedIcon = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "选中",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.icon) }
    var selectedColor by remember { mutableStateOf(category.color) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${category.name}」吗？删除后相关记账记录将无法关联到此分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategoryIconPickerGrid(
                    selectedIcon = selectedIcon,
                    selectedColor = selectedColor,
                    onSelect = { selectedIcon = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "选中",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { showDeleteConfirm = true }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = { onConfirm(name, selectedIcon, selectedColor) },
                    enabled = name.isNotBlank()
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
            .heightIn(max = 220.dp),
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
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = if (selectedIcon == iconName) 2.dp else 1.dp,
                        color = if (selectedIcon == iconName) Color(selectedColor) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(iconName) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(iconName),
                    contentDescription = iconName,
                    tint = if (selectedIcon == iconName) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
