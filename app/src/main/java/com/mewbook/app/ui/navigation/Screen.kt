package com.mewbook.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Statistics : Screen("statistics")
    data object Asset : Screen("asset")
    data object Budget : Screen("budget")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object DavSettings : Screen("dav_settings")
    data object Export : Screen("export")
    data object AccountEdit : Screen("account_edit/{accountId}") {
        fun createRoute(accountId: Long) = "account_edit/$accountId"
    }
    data object AddAccount : Screen("add_account")
}
