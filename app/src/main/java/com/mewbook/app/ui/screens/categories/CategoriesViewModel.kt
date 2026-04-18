package com.mewbook.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.usecase.category.AddCategoryUseCase
import com.mewbook.app.domain.usecase.category.DeleteCategoryUseCase
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCategory: Category? = null,
    val selectedType: RecordType = RecordType.EXPENSE
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCategoriesUseCase.getAll().collect { categories ->
                _uiState.update {
                    it.copy(
                        categories = categories,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun showAddDialog(type: RecordType) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                selectedType = type
            )
        }
    }

    fun hideAddDialog() {
        _uiState.update {
            it.copy(showAddDialog = false)
        }
    }

    fun showEditDialog(category: Category) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editingCategory = category
            )
        }
    }

    fun hideEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                editingCategory = null
            )
        }
    }

    fun addCategory(name: String, icon: String, color: Long, type: RecordType, parentId: Long? = null) {
        viewModelScope.launch {
            val maxSortOrder = _uiState.value.categories
                .filter { it.type == type && it.parentId == parentId }
                .maxOfOrNull { it.sortOrder } ?: -1

            val category = Category(
                id = 0,
                name = name,
                icon = icon,
                color = color,
                type = type,
                isDefault = false,
                sortOrder = maxSortOrder + 1,
                parentId = parentId
            )
            addCategoryUseCase(category)
            hideAddDialog()
        }
    }

    fun updateCategory(category: Category, newName: String, newIcon: String, newColor: Long) {
        viewModelScope.launch {
            val updated = category.copy(
                name = newName,
                icon = newIcon,
                color = newColor
            )
            updateCategoryUseCase(updated)
            hideEditDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (!category.isDefault) {
                deleteCategoryUseCase(category)
            }
        }
    }

    fun moveCategoryUp(category: Category) {
        viewModelScope.launch {
            val siblings = _uiState.value.categories
                .filter { it.type == category.type && it.parentId == category.parentId }
                .sortedBy { it.sortOrder }
            val currentIndex = siblings.indexOfFirst { it.id == category.id }
            if (currentIndex <= 0) return@launch

            val current = siblings[currentIndex]
            val previous = siblings[currentIndex - 1]
            updateCategoryUseCase(current.copy(sortOrder = previous.sortOrder))
            updateCategoryUseCase(previous.copy(sortOrder = current.sortOrder))
        }
    }

    fun moveCategoryDown(category: Category) {
        viewModelScope.launch {
            val siblings = _uiState.value.categories
                .filter { it.type == category.type && it.parentId == category.parentId }
                .sortedBy { it.sortOrder }
            val currentIndex = siblings.indexOfFirst { it.id == category.id }
            if (currentIndex == -1 || currentIndex >= siblings.lastIndex) return@launch

            val current = siblings[currentIndex]
            val next = siblings[currentIndex + 1]
            updateCategoryUseCase(current.copy(sortOrder = next.sortOrder))
            updateCategoryUseCase(next.copy(sortOrder = current.sortOrder))
        }
    }
}
