package com.mewbook.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.preferences.HomePreferencesRepository
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.data.preferences.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val homePreferencesRepository: HomePreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferencesRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppThemeMode.SYSTEM
    )

    val showHomeOverviewCards: StateFlow<Boolean> = homePreferencesRepository.showHomeOverviewCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setShowHomeOverviewCards(show: Boolean) {
        viewModelScope.launch {
            homePreferencesRepository.setShowHomeOverviewCards(show)
        }
    }
}
