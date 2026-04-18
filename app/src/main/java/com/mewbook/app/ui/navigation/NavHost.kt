package com.mewbook.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mewbook.app.ui.screens.asset.AccountEditScreen
import com.mewbook.app.ui.screens.asset.AddAccountScreen
import com.mewbook.app.ui.screens.asset.AssetScreen
import com.mewbook.app.ui.screens.budget.BudgetScreen
import com.mewbook.app.ui.screens.categories.CategoriesScreen
import com.mewbook.app.ui.screens.dav.DavSettingsScreen
import com.mewbook.app.ui.screens.export.ExportScreen
import com.mewbook.app.ui.screens.home.HomeScreen
import com.mewbook.app.ui.screens.ledger.LedgerManagementScreen
import com.mewbook.app.ui.screens.settings.SettingsScreen
import com.mewbook.app.ui.screens.statistics.StatisticsScreen

// ============================================
// Warm Claymorphism Navigation
// 温暖黏土风底部导航
// ============================================

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "记账", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Statistics, "报表", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Screen.Asset, "资产", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomNavItem(Screen.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MewBookNavHost() {
    val isDarkTheme = isSystemInDarkTheme()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var isHomeAddSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentDestination?.route) {
        if (currentDestination?.route != Screen.Home.route) {
            isHomeAddSheetVisible = false
        }
    }

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Statistics.route,
        Screen.Asset.route,
        Screen.Settings.route
    ) && !isHomeAddSheetVisible

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.08f else 0.15f)
                            )
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.04f else 0.10f)
                            )
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddSheetVisibilityChanged = { visible ->
                        isHomeAddSheetVisible = visible
                    }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Asset.route) {
                AssetScreen(
                    onNavigateToAccountEdit = { accountId ->
                        navController.navigate(Screen.AccountEdit.createRoute(accountId))
                    },
                    onNavigateToAddAccount = {
                        navController.navigate(Screen.AddAccount.route)
                    }
                )
            }
            composable(Screen.Budget.route) {
                BudgetScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route)
                    },
                    onNavigateToDavSettings = {
                        navController.navigate(Screen.DavSettings.route)
                    },
                    onNavigateToBudget = {
                        navController.navigate(Screen.Budget.route)
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    },
                    onNavigateToLedgerManagement = {
                        navController.navigate(Screen.LedgerManagement.route)
                    }
                )
            }
            composable(Screen.LedgerManagement.route) {
                LedgerManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.DavSettings.route) {
                DavSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Export.route) {
                ExportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.AccountEdit.route) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull() ?: 0L
                AccountEditScreen(
                    accountId = accountId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.AddAccount.route) {
                AddAccountScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
