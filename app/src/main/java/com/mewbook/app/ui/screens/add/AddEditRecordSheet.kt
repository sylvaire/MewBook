package com.mewbook.app.ui.screens.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.components.CategoryChip
import com.mewbook.app.ui.theme.ClayDesign
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 检查两个分类是否相关（用于辅助匹配）
private fun isRelatedCategory(subName: String, parentName: String): Boolean {
    val relations = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "零食", "下午茶", "外卖", "水果", "奶茶", "咖啡", "茶饮", "肉类", "蔬菜"),
        "交通" to listOf("公交", "地铁", "打车", "油费", "停车", "火车", "飞机"),
        "购物" to listOf("服装", "数码", "日用品", "化妆品", "母婴", "家电"),
        "居住" to listOf("房租", "水电费", "物业费", "燃气", "话费"),
        "娱乐" to listOf("电影", "游戏", "旅游", "健身", "演唱会", "ktv"),
        "通讯" to listOf("手机话费", "宽带费"),
        "运动健身" to listOf("健身房", "运动装备", "游泳"),
        "宠物" to listOf("宠物食品", "宠物医疗", "宠物用品"),
        "人情往来" to listOf("红包", "礼物", "请客"),
        "书籍文具" to listOf("书籍", "文具", "电子书"),
        "虚拟产品" to listOf("游戏充值", "会员订阅", "软件订阅", "直播打赏", "虚拟货币")
    )
    return relations[parentName]?.contains(subName) ?: false
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecordSheet(
    categories: List<Category>,
    accounts: List<Account>,
    editingRecord: Record?,
    onDismiss: () -> Unit,
    onSave: (Double, RecordType, Long, String?, LocalDate, Long?) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember(editingRecord) {
        mutableStateOf(if (editingRecord != null) editingRecord.amount.toString() else "")
    }
    var selectedType by remember(editingRecord) {
        mutableStateOf(editingRecord?.type ?: RecordType.EXPENSE)
    }
    var selectedCategoryId by remember(editingRecord) {
        mutableLongStateOf(editingRecord?.categoryId ?: 0)
    }
    var selectedParentCategoryId by remember(editingRecord) {
        mutableLongStateOf(0)
    }
    var selectedAccountId by remember(editingRecord) {
        mutableLongStateOf(editingRecord?.accountId ?: 0)
    }
    var note by remember(editingRecord) {
        mutableStateOf(editingRecord?.note ?: "")
    }
    var selectedDate by remember(editingRecord) {
        mutableStateOf(editingRecord?.date ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // 分离一级分类和二级分类
    val parentCategories = categories.filter { it.parentId == null && it.type == selectedType }
    val subCategories = categories.filter { it.parentId != null && it.type == selectedType }

    // 根据选中的一级分类筛选二级分类
    val availableSubCategories = if (selectedParentCategoryId > 0) {
        val subs = subCategories.filter { it.parentId == selectedParentCategoryId }
        // 如果筛选结果为空，尝试通过分类名称匹配
        if (subs.isEmpty()) {
            val selectedParent = parentCategories.find { it.id == selectedParentCategoryId }
            if (selectedParent != null) {
                subCategories.filter { sub ->
                    // 尝试通过名称模式匹配
                    sub.name.contains(selectedParent.name) ||
                    isRelatedCategory(sub.name, selectedParent.name)
                }
            } else emptyList()
        } else subs
    } else {
        emptyList()
    }

    // 判断选中的是否是二级分类
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val isSelectedSubCategory = selectedCategory?.parentId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        shape = RoundedCornerShape(topStart = ClayDesign.CardRadius, topEnd = ClayDesign.CardRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRecord != null) "编辑记录" else "添加记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (editingRecord != null) {
                    IconButton(
                        onClick = { onDelete(editingRecord.id) }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = newValue
                    }
                },
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == RecordType.EXPENSE,
                    onClick = {
                        selectedType = RecordType.EXPENSE
                        selectedCategoryId = 0
                        selectedParentCategoryId = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("支出")
                }
                SegmentedButton(
                    selected = selectedType == RecordType.INCOME,
                    onClick = {
                        selectedType = RecordType.INCOME
                        selectedCategoryId = 0
                        selectedParentCategoryId = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("收入")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "选择分类",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 一级分类选择
            Text(
                text = "一级分类",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(((parentCategories.size / 4 + 1) * 80).coerceAtLeast(240).coerceAtMost(480).dp)
            ) {
                items(parentCategories, key = { it.id }) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedParentCategoryId == category.id && !isSelectedSubCategory,
                        onClick = {
                            selectedParentCategoryId = category.id
                            selectedCategoryId = category.id
                        }
                    )
                }
            }

            // 二级分类选择（如果有）
            AnimatedVisibility(
                visible = availableSubCategories.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "二级分类",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 固定高度确保至少显示3行，最多6行
                    val gridHeight = ((availableSubCategories.size / 4 + 1) * 80).coerceAtLeast(240).coerceAtMost(480)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(gridHeight.dp)
                    ) {
                        items(availableSubCategories, key = { it.id }) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = {
                                    selectedCategoryId = category.id
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account selector
            Text(
                text = "选择账户",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = {
                            selectedAccountId = if (selectedAccountId == account.id) 0 else account.id
                        },
                        label = { Text(account.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "日期: ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedCategoryId > 0) {
                        onSave(amount, selectedType, selectedCategoryId, note.ifBlank { null }, selectedDate, if (selectedAccountId > 0) selectedAccountId else null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = ClayDesign.ButtonShadowElevation,
                        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(ClayDesign.ButtonRadius),
                enabled = amountText.toDoubleOrNull() != null &&
                        (amountText.toDoubleOrNull() ?: 0.0) > 0 &&
                        selectedCategoryId > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (editingRecord != null) "保存" else "添加",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
