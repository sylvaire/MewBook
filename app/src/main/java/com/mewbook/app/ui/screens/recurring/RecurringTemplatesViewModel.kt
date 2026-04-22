package com.mewbook.app.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.model.RecurringTemplate
import com.mewbook.app.domain.model.RecurringTemplateScheduleType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.CategorySelectionPolicy
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.repository.RecurringTemplateRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class RecurringTemplateEditorState(
    val id: Long = 0,
    val name: String = "",
    val amount: String = "",
    val type: RecordType = RecordType.EXPENSE,
    val categoryId: Long = 0,
    val accountId: Long? = null,
    val noteTemplate: String = "",
    val scheduleType: RecurringTemplateScheduleType = RecurringTemplateScheduleType.MONTHLY,
    val intervalCount: String = "1",
    val startDate: LocalDate = LocalDate.now(),
    val nextDueDate: LocalDate = LocalDate.now(),
    val endDateEnabled: Boolean = false,
    val endDate: LocalDate = LocalDate.now().plusMonths(6),
    val reminderEnabled: Boolean = false,
    val ledgerId: Long = 1,
    val isEnabled: Boolean = true
)

data class RecurringTemplatesUiState(
    val templates: List<RecurringTemplate> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val activeLedger: Ledger? = null,
    val defaultAccountId: Long? = null,
    val showEditor: Boolean = false,
    val editor: RecurringTemplateEditorState = RecurringTemplateEditorState(),
    val deleteCandidate: RecurringTemplate? = null,
    val busyTemplateId: Long? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class RecurringTemplatesContextState(
    val templates: List<RecurringTemplate>,
    val categories: List<Category>,
    val accounts: List<Account>,
    val activeLedger: Ledger?
)

private data class RecurringTemplatesOverlayState(
    val showEditor: Boolean = false,
    val editor: RecurringTemplateEditorState = RecurringTemplateEditorState(),
    val deleteCandidate: RecurringTemplate? = null,
    val busyTemplateId: Long? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurringTemplatesViewModel @Inject constructor(
    private val recurringTemplateRepository: RecurringTemplateRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val activeLedgerFlow = ledgerRepository.getDefaultLedgerFlow()
    private val templatesFlow = activeLedgerFlow.flatMapLatest { ledger ->
        recurringTemplateRepository.getTemplatesByLedger(ledger?.id ?: 1L)
    }
    private val contextState = combine(
        templatesFlow,
        getCategoriesUseCase.getAll(),
        accountRepository.getAllAccounts(),
        activeLedgerFlow
    ) { templates, categories, accounts, activeLedger ->
        RecurringTemplatesContextState(
            templates = templates,
            categories = categories,
            accounts = accounts,
            activeLedger = activeLedger
        )
    }

    private val _overlayState = MutableStateFlow(RecurringTemplatesOverlayState())

    val uiState: StateFlow<RecurringTemplatesUiState> = combine(contextState, _overlayState) { context, overlay ->
        val ledgerAccounts = context.accounts.filter { it.ledgerId == (context.activeLedger?.id ?: 1L) }
        RecurringTemplatesUiState(
            templates = context.templates,
            categories = context.categories,
            accounts = ledgerAccounts,
            activeLedger = context.activeLedger,
            defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(ledgerAccounts),
            showEditor = overlay.showEditor,
            editor = overlay.editor,
            deleteCandidate = overlay.deleteCandidate,
            busyTemplateId = overlay.busyTemplateId,
            isSaving = overlay.isSaving,
            message = overlay.message,
            error = overlay.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecurringTemplatesUiState()
    )

    fun openCreateTemplate() {
        val state = uiState.value
        val ledgerId = state.activeLedger?.id ?: 1L
        val defaultCategoryId = CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
            categories = state.categories,
            type = RecordType.EXPENSE
        )
        _overlayState.value = _overlayState.value.copy(
            showEditor = true,
            editor = RecurringTemplateEditorState(
                type = RecordType.EXPENSE,
                categoryId = defaultCategoryId,
                accountId = state.defaultAccountId,
                startDate = LocalDate.now(),
                nextDueDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(6),
                ledgerId = ledgerId
            ),
            deleteCandidate = null,
            error = null,
            message = null
        )
    }

    fun openEditTemplate(template: RecurringTemplate) {
        val availableCategoryId = CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
            categories = uiState.value.categories,
            type = template.type,
            preferredCategoryId = template.categoryId
        )
        _overlayState.value = _overlayState.value.copy(
            showEditor = true,
            editor = RecurringTemplateEditorState(
                id = template.id,
                name = template.name,
                amount = template.amount.toString(),
                type = template.type,
                categoryId = availableCategoryId,
                accountId = template.accountId,
                noteTemplate = template.noteTemplate.orEmpty(),
                scheduleType = template.scheduleType,
                intervalCount = template.intervalCount.toString(),
                startDate = template.startDate,
                nextDueDate = template.nextDueDate,
                endDateEnabled = template.endDate != null,
                endDate = template.endDate ?: template.nextDueDate,
                reminderEnabled = template.reminderEnabled,
                ledgerId = template.ledgerId,
                isEnabled = template.isEnabled
            ),
            deleteCandidate = null,
            error = null,
            message = null
        )
    }

    fun updateEditor(transform: (RecurringTemplateEditorState) -> RecurringTemplateEditorState) {
        _overlayState.update { it.copy(editor = transform(it.editor)) }
    }

    fun dismissEditor() {
        _overlayState.update { it.copy(showEditor = false, error = null) }
    }

    fun saveEditor() {
        val editor = _overlayState.value.editor
        val amount = editor.amount.toDoubleOrNull()
        val interval = editor.intervalCount.toIntOrNull()?.coerceAtLeast(1)
        val availableCategoryIds = CategorySelectionPolicy
            .visibleTopLevelCategories(
                categories = uiState.value.categories,
                type = editor.type
            )
            .map(Category::id)
            .toSet()
        if (editor.name.isBlank()) {
            _overlayState.update { it.copy(error = "请输入模板名称") }
            return
        }
        if (amount == null || amount <= 0) {
            _overlayState.update { it.copy(error = "请输入有效的金额") }
            return
        }
        if (editor.categoryId == 0L || editor.categoryId !in availableCategoryIds) {
            _overlayState.update { it.copy(error = "请选择记账页可用的分类") }
            return
        }
        if (interval == null) {
            _overlayState.update { it.copy(error = "请输入有效的周期") }
            return
        }

        viewModelScope.launch {
            _overlayState.update { it.copy(isSaving = true, error = null, message = null) }

            val existing = if (editor.id != 0L) {
                recurringTemplateRepository.getTemplateById(editor.id)
            } else {
                null
            }
            val now = LocalDateTime.now()
            val result = runCatching {
                recurringTemplateRepository.saveTemplate(
                    RecurringTemplate(
                        id = editor.id,
                        name = editor.name.trim(),
                        amount = amount,
                        type = editor.type,
                        categoryId = editor.categoryId,
                        noteTemplate = editor.noteTemplate.trim().ifBlank { null },
                        ledgerId = editor.ledgerId,
                        accountId = editor.accountId,
                        scheduleType = editor.scheduleType,
                        intervalCount = interval,
                        startDate = editor.startDate,
                        nextDueDate = editor.nextDueDate,
                        endDate = if (editor.endDateEnabled) editor.endDate else null,
                        isEnabled = editor.isEnabled,
                        reminderEnabled = editor.reminderEnabled,
                        lastGeneratedDate = existing?.lastGeneratedDate,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            }

            _overlayState.update { it.copy(isSaving = false) }
            result.fold(
                onSuccess = {
                    val autoClosedCount = recurringTemplateRepository
                        .autoCloseDueTemplates()
                        .getOrDefault(0)
                    _overlayState.update {
                        it.copy(
                            showEditor = false,
                            message = buildString {
                                append(if (editor.id == 0L) "模板已创建" else "模板已更新")
                                if (autoClosedCount > 0) {
                                    append("，并自动生成 $autoClosedCount 条到期记录")
                                }
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _overlayState.update { it.copy(error = e.message ?: "保存模板失败") }
                }
            )
        }
    }

    fun requestDelete(template: RecurringTemplate) {
        _overlayState.update { it.copy(deleteCandidate = template) }
    }

    fun cancelDelete() {
        _overlayState.update { it.copy(deleteCandidate = null) }
    }

    fun confirmDelete() {
        val template = _overlayState.value.deleteCandidate ?: return
        viewModelScope.launch {
            _overlayState.update { it.copy(busyTemplateId = template.id, error = null, message = null) }
            runCatching { recurringTemplateRepository.deleteTemplate(template) }
                .onSuccess {
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            deleteCandidate = null,
                            message = "模板已删除"
                        )
                    }
                }
                .onFailure { e ->
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            error = e.message ?: "删除模板失败"
                        )
                    }
                }
        }
    }

    fun generateOccurrence(template: RecurringTemplate) {
        if (_overlayState.value.busyTemplateId != null) return
        viewModelScope.launch {
            _overlayState.update { it.copy(busyTemplateId = template.id, error = null, message = null) }
            recurringTemplateRepository.generateOccurrence(template.id).fold(
                onSuccess = {
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            message = "已生成本期记录"
                        )
                    }
                },
                onFailure = { e ->
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            error = e.message ?: "生成记录失败"
                        )
                    }
                }
            )
        }
    }

    fun skipOccurrence(template: RecurringTemplate) {
        if (_overlayState.value.busyTemplateId != null) return
        viewModelScope.launch {
            _overlayState.update { it.copy(busyTemplateId = template.id, error = null, message = null) }
            recurringTemplateRepository.skipOccurrence(template.id).fold(
                onSuccess = {
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            message = "已跳过本期并推进下一次到期"
                        )
                    }
                },
                onFailure = { e ->
                    _overlayState.update {
                        it.copy(
                            busyTemplateId = null,
                            error = e.message ?: "跳过失败"
                        )
                    }
                }
            )
        }
    }

    fun clearMessage() {
        _overlayState.update { it.copy(message = null) }
    }
}
