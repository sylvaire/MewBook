package com.mewbook.app.ui.screens.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.usecase.account.InitializeDefaultAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetUiState(
    val totalAsset: Double = 0.0,
    val totalLiability: Double = 0.0,
    val netAsset: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val initializeDefaultAccountsUseCase: InitializeDefaultAccountsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetUiState())
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            initializeDefaultAccountsUseCase()
            loadAccounts()
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            accountRepository.getAllAccounts().collect { accounts ->
                val totalAsset = accounts
                    .filter { it.type != AccountType.CREDIT_CARD }
                    .sumOf { it.balance }
                val totalLiability = accounts
                    .filter { it.type == AccountType.CREDIT_CARD }
                    .sumOf { kotlin.math.abs(it.balance) }
                val netAsset = totalAsset - totalLiability

                _uiState.value = AssetUiState(
                    totalAsset = totalAsset,
                    totalLiability = totalLiability,
                    netAsset = netAsset,
                    accounts = accounts,
                    isLoading = false
                )
            }
        }
    }
}
