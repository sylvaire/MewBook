package com.mewbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
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

@Composable
fun AccountType.toIcon(): ImageVector {
    return when (this) {
        AccountType.CASH -> Icons.Filled.AccountBalanceWallet
        AccountType.BANK -> Icons.Filled.AccountBalance
        AccountType.ALIPAY -> ImageVector.vectorResource(id = R.drawable.ic_alipay)
        AccountType.WECHAT -> ImageVector.vectorResource(id = R.drawable.ic_wechat)
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.INVESTMENT -> Icons.Filled.Savings
        AccountType.OTHER -> Icons.Filled.MoreHoriz
    }
}

fun AccountType.usesBrandIconTint(): Boolean {
    return this == AccountType.ALIPAY || this == AccountType.WECHAT
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

@Composable
fun AccountTypeIconBadge(
    type: AccountType,
    accentColor: Color = Color(type.defaultColorValue()),
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    emphasized: Boolean = false
) {
    val backgroundColor = if (type.usesBrandIconTint()) {
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
            imageVector = type.toIcon(),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (type.usesBrandIconTint()) Color.Unspecified else accentColor
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
