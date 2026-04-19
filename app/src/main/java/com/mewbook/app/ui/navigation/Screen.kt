package com.mewbook.app.ui.navigation

import java.time.LocalDate

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Statistics : Screen("statistics")
    data object Asset : Screen("asset")
    data object Budget : Screen("budget")
    data object Settings : Screen("settings")
    data object LedgerManagement : Screen("ledger_management")
    data object Categories : Screen("categories")
    data object DavSettings : Screen("dav_settings")
    data object Export : Screen("export")
    data object AccountEdit : Screen("account_edit/{accountId}") {
        fun createRoute(accountId: Long) = "account_edit/$accountId"
    }
    data object AddAccount : Screen("add_account")

    /** 统计页支出构成分类明细：参数为 epoch day */
    data object CategoryExpenseDetail : Screen("statistics_category_expense/{categoryId}/{startEpoch}/{endEpoch}") {
        fun createRoute(categoryId: Long, periodStart: LocalDate, periodEnd: LocalDate): String {
            return "statistics_category_expense/$categoryId/${periodStart.toEpochDay()}/${periodEnd.toEpochDay()}"
        }
    }
}
