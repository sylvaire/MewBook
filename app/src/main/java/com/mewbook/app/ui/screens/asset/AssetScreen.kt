package com.mewbook.app.ui.screens.asset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.ui.res.vectorResource
import com.mewbook.app.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetScreen(
    onNavigateToAccountEdit: (Long) -> Unit = {},
    onNavigateToAddAccount: () -> Unit = {},
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "资产"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddAccount,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加账户")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 净资产卡片
                item {
                    NetAssetCard(
                        netAsset = uiState.netAsset,
                        totalAsset = uiState.totalAsset,
                        totalLiability = uiState.totalLiability
                    )
                }

                // 账户列表
                item {
                    Text(
                        text = "账户",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.accounts) { account ->
                    AccountItem(
                        account = account,
                        onClick = { onNavigateToAccountEdit(account.id) }
                    )
                }

                if (uiState.accounts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "还没有账户",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "在设置中添加您的账户",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetAssetCard(
    netAsset: Double,
    totalAsset: Double,
    totalLiability: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "净资产",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(netAsset),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (netAsset >= 0) IncomeGreen else ExpenseRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "资产",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatCurrency(totalAsset),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "负债",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatCurrency(totalLiability),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    onClick: (Account) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(account) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WeChat和支付宝图标使用自定义颜色，不应用tint
            val useCustomIcon = account.type == AccountType.ALIPAY || account.type == AccountType.WECHAT
            Icon(
                imageVector = account.type.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (useCustomIcon) Color.Unspecified else Color(account.color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = account.type.toDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCurrency(account.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
            )
        }
    }
}

@Composable
fun AccountType.toIcon(): ImageVector {
    return when (this) {
        AccountType.CASH -> Icons.Default.AccountBalanceWallet
        AccountType.BANK -> ImageVector.vectorResource(id = R.drawable.ic_bank_card)
        AccountType.ALIPAY -> ImageVector.vectorResource(id = R.drawable.ic_alipay)
        AccountType.WECHAT -> ImageVector.vectorResource(id = R.drawable.ic_wechat)
        AccountType.CREDIT_CARD -> ImageVector.vectorResource(id = R.drawable.ic_credit_card)
        AccountType.INVESTMENT -> Icons.Default.AccountBalanceWallet
        AccountType.OTHER -> Icons.Default.AccountBalanceWallet
    }
}

fun AccountType.toDisplayName(): String {
    return when (this) {
        AccountType.CASH -> "现金"
        AccountType.BANK -> "银行卡"
        AccountType.ALIPAY -> "支付宝"
        AccountType.WECHAT -> "微信"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.INVESTMENT -> "投资"
        AccountType.OTHER -> "其他"
    }
}
