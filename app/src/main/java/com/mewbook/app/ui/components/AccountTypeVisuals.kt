package com.mewbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyYuan
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mewbook.app.R
import com.mewbook.app.domain.model.AccountType

data class AccountIconOption(
    val name: String,
    val label: String,
    val group: String
)

fun accountIconOptions(): List<AccountIconOption> {
    return listOf(
        AccountIconOption("bank_card", "磁条卡", "银行卡"),
        AccountIconOption("bank_card_chip", "芯片卡", "银行卡"),
        AccountIconOption("bank_card_contactless", "闪付卡", "银行卡"),
        AccountIconOption("bank_card_branch", "银行账户", "银行卡"),
        AccountIconOption("alipay", "支付宝", "支付方式"),
        AccountIconOption("wechat", "微信", "支付方式"),
        AccountIconOption("payments", "收付款", "支付方式"),
        AccountIconOption("qr_code_scanner", "扫码支付", "支付方式"),
        AccountIconOption("wallet", "钱包", "支付方式"),
        AccountIconOption("account_balance_wallet", "现金", "支付方式"),
        AccountIconOption("credit_card", "信用卡", "账户"),
        AccountIconOption("account_balance", "银行", "账户"),
        AccountIconOption("savings", "储蓄", "账户"),
        AccountIconOption("currency_yuan", "人民币", "账户"),
        AccountIconOption("attach_money", "资金", "账户"),
        AccountIconOption("more_horiz", "其他", "账户")
    )
}

fun AccountType.supportsCustomIcon(): Boolean {
    return this == AccountType.OTHER
}

fun normalizeAccountIconName(iconName: String, type: AccountType): String {
    if (!type.supportsCustomIcon()) {
        return type.defaultIconName()
    }

    val supportedIconNames = accountIconOptions().map { it.name }.toSet()
    return iconName.takeIf { it in supportedIconNames } ?: type.defaultIconName()
}

@Composable
fun AccountType.toIcon(): ImageVector {
    return when (this) {
        AccountType.CASH -> Icons.Filled.AccountBalanceWallet
        AccountType.BANK -> ImageVector.vectorResource(id = R.drawable.ic_bank_card)
        AccountType.ALIPAY -> ImageVector.vectorResource(id = R.drawable.ic_alipay)
        AccountType.WECHAT -> ImageVector.vectorResource(id = R.drawable.ic_wechat)
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.INVESTMENT -> Icons.Filled.Savings
        AccountType.OTHER -> Icons.Filled.MoreHoriz
    }
}

@Composable
fun accountIconByName(iconName: String, fallbackType: AccountType): ImageVector {
    return when (normalizeAccountIconName(iconName, fallbackType)) {
        "account_balance_wallet" -> Icons.Filled.AccountBalanceWallet
        "bank_card" -> ImageVector.vectorResource(id = R.drawable.ic_bank_card)
        "bank_card_chip" -> ImageVector.vectorResource(id = R.drawable.ic_bank_card_chip)
        "bank_card_contactless" -> ImageVector.vectorResource(id = R.drawable.ic_bank_card_contactless)
        "bank_card_branch" -> ImageVector.vectorResource(id = R.drawable.ic_bank_card_branch)
        "alipay" -> ImageVector.vectorResource(id = R.drawable.ic_alipay)
        "wechat" -> ImageVector.vectorResource(id = R.drawable.ic_wechat)
        "credit_card" -> Icons.Filled.CreditCard
        "payments" -> Icons.Filled.Payments
        "qr_code_scanner" -> Icons.Filled.QrCodeScanner
        "wallet" -> Icons.Filled.Wallet
        "account_balance" -> Icons.Filled.AccountBalance
        "savings" -> Icons.Filled.Savings
        "currency_yuan" -> Icons.Filled.CurrencyYuan
        "attach_money" -> Icons.Filled.AttachMoney
        else -> Icons.Filled.MoreHoriz
    }
}

fun AccountType.usesBrandIconTint(): Boolean {
    return this == AccountType.ALIPAY || this == AccountType.WECHAT
}

fun usesBrandAccountIconTint(iconName: String): Boolean {
    return iconName == "alipay" || iconName == "wechat"
}

fun AccountType.defaultColorValue(): Long {
    return when (this) {
        AccountType.CASH -> 0xFF4CAF50
        AccountType.BANK -> 0xFF2196F3
        AccountType.ALIPAY -> 0xFF1890FF
        AccountType.WECHAT -> 0xFF07C160
        AccountType.CREDIT_CARD -> 0xFFFF9800
        AccountType.INVESTMENT -> 0xFF7E57C2
        AccountType.OTHER -> 0xFF78909C
    }
}

fun AccountType.defaultIconName(): String {
    return when (this) {
        AccountType.CASH -> "account_balance_wallet"
        AccountType.BANK -> "bank_card"
        AccountType.ALIPAY -> "alipay"
        AccountType.WECHAT -> "wechat"
        AccountType.CREDIT_CARD -> "credit_card"
        AccountType.INVESTMENT -> "savings"
        AccountType.OTHER -> "more_horiz"
    }
}

@Composable
fun AccountTypeIconBadge(
    type: AccountType,
    accentColor: Color = Color(type.defaultColorValue()),
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    emphasized: Boolean = false
) {
    AccountIconBadge(
        type = type,
        iconName = type.defaultIconName(),
        accentColor = accentColor,
        modifier = modifier,
        containerSize = containerSize,
        iconSize = iconSize,
        emphasized = emphasized
    )
}

@Composable
fun AccountIconBadge(
    type: AccountType,
    iconName: String,
    accentColor: Color = Color(type.defaultColorValue()),
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    emphasized: Boolean = false
) {
    val normalizedIconName = normalizeAccountIconName(iconName, type)
    val backgroundColor = if (usesBrandAccountIconTint(normalizedIconName)) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (emphasized) 0.72f else 0.48f)
    } else {
        accentColor.copy(alpha = if (emphasized) 0.20f else 0.14f)
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = accountIconByName(normalizedIconName, type),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (usesBrandAccountIconTint(normalizedIconName)) Color.Unspecified else accentColor
        )
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
