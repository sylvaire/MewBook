package com.mewbook.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.data.preferences.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferencesRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppThemeMode.SYSTEM
    )

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeMode(themeMode)
        }
    }
}
