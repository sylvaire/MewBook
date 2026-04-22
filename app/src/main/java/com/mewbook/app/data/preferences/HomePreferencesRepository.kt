package com.mewbook.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val showHomeOverviewCardsKey: Preferences.Key<Boolean> =
        booleanPreferencesKey("show_home_overview_cards")

    val selectedHomePeriod: Flow<BudgetPeriodType> = context.homePreferencesDataStore.data.map { preferences ->
        HomePeriodSelectionPolicy.fromStorageValue(preferences[selectedHomePeriodKey])
    }

    val showHomeOverviewCards: Flow<Boolean> = context.homePreferencesDataStore.data.map { preferences ->
        preferences[showHomeOverviewCardsKey] ?: true
    }

    suspend fun setSelectedHomePeriod(periodType: BudgetPeriodType) {
        context.homePreferencesDataStore.edit { preferences ->
            preferences[selectedHomePeriodKey] = HomePeriodSelectionPolicy.toStorageValue(periodType)
        }
    }

    suspend fun setShowHomeOverviewCards(show: Boolean) {
        context.homePreferencesDataStore.edit { preferences ->
            preferences[showHomeOverviewCardsKey] = show
        }
    }

    suspend fun getSelectedHomePeriodOnce(): BudgetPeriodType {
        return selectedHomePeriod.first()
    }

    suspend fun getShowHomeOverviewCardsOnce(): Boolean {
        return showHomeOverviewCards.first()
    }
}
