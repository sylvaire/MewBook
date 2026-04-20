package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.policy.HomePeriodSelectionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homePreferencesDataStore by preferencesDataStore(name = "home_preferences")

@Singleton
class HomePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val selectedHomePeriodKey: Preferences.Key<String> =
        stringPreferencesKey("selected_home_period")

    val selectedHomePeriod: Flow<BudgetPeriodType> = context.homePreferencesDataStore.data.map { preferences ->
        HomePeriodSelectionPolicy.fromStorageValue(preferences[selectedHomePeriodKey])
    }

    suspend fun setSelectedHomePeriod(periodType: BudgetPeriodType) {
        context.homePreferencesDataStore.edit { preferences ->
            preferences[selectedHomePeriodKey] = HomePeriodSelectionPolicy.toStorageValue(periodType)
        }
    }

    suspend fun getSelectedHomePeriodOnce(): BudgetPeriodType {
        return selectedHomePeriod.first()
    }
}
