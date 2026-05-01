package com.mewbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mewbook.app.R
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.ui.theme.clayCardShadow
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================================
// Claymorphism Record Item Card
// 温暖黏土风记账卡片
// ============================================

@Composable
fun RecordItem(
    record: Record,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Claymorphism 卡片 - 多层柔和阴影
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clayCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // 使用自定义阴影
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ClayDesign.CardPadding + 4.dp), // 20dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                category = com.mewbook.app.domain.model.Category(
                    name = categoryName,
                    icon = categoryIcon,
                    color = categoryColor,
                    type = record.type,
                    isDefault = true,
                    sortOrder = 0
                ),
                emphasized = true,
                containerSize = 52.dp,
                iconSize = 26.dp
            )

            Spacer(modifier = Modifier.width(ClayDesign.CardSpacing + 4.dp)) // 16dp

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.note?.ifBlank { record.date.format(DateTimeFormatter.ofPattern("MM月dd日")) }
                        ?: record.date.format(DateTimeFormatter.ofPattern("MM月dd日")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 金额 - 使用语义颜色
            Text(
                text = "${if (record.type == RecordType.INCOME) "+" else "-"}${formatCurrency(record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (record.type == RecordType.INCOME) IncomeGreen else ExpenseRed
            )
        }
    }
}

@Composable
fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        // 基础分类
        "apple" -> ImageVector.vectorResource(id = R.drawable.ic_apple)
        "restaurant" -> Icons.Filled.Restaurant
        "directions_car" -> Icons.Filled.DirectionsCar
        "directions_bus" -> Icons.Filled.DirectionsBus
        "shopping_bag" -> Icons.Filled.ShoppingBag
        "local_mall" -> Icons.Filled.LocalMall
        "movie" -> Icons.Filled.Movie
        "medical_services" -> Icons.Filled.LocalHospital
        "medication" -> Icons.Filled.Medication
        "health_and_safety" -> Icons.Filled.HealthAndSafety
        "school" -> Icons.Filled.School
        "more_horiz" -> Icons.Filled.MoreHoriz
        "payments" -> Icons.Filled.Payments
        "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
        "card_giftcard" -> Icons.Filled.CardGiftcard
        "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
        "attach_money" -> Icons.Filled.AttachMoney
        "work" -> Icons.Filled.Work
        "account_balance" -> Icons.Filled.AccountBalance
        "savings" -> Icons.Filled.Savings
        "home" -> Icons.Filled.Home
        "house" -> Icons.Filled.Home
        "weekend" -> Icons.Filled.Weekend

        // 餐饮相关
        "free_breakfast" -> Icons.Filled.FreeBreakfast
        "lunch_dining" -> Icons.Filled.LunchDining
        "dinner_dining" -> Icons.Filled.DinnerDining
        "local_cafe" -> Icons.Filled.LocalCafe
        "local_drink" -> Icons.Filled.LocalDrink
        "emoji_food_beverage" -> Icons.Filled.EmojiFoodBeverage
        "nutrition" -> Icons.Filled.EmojiNature
        "emoji_nature" -> Icons.Filled.EmojiNature
        "takeout_dining" -> Icons.Filled.TakeoutDining

        // 交通相关
        "train" -> Icons.Filled.Train
        "flight" -> Icons.Filled.Flight
        "local_taxi" -> Icons.Filled.LocalTaxi
        "local_gas_station" -> Icons.Filled.LocalGasStation
        "local_parking" -> Icons.Filled.LocalParking
        "local_shipping" -> Icons.Filled.LocalShipping

        // 购物相关
        "devices" -> Icons.Filled.Devices
        "face" -> Icons.Filled.Face
        "brush" -> Icons.Filled.Brush
        "baby_changing_station" -> Icons.Filled.BabyChangingStation
        "boy" -> Icons.Filled.Boy
        "checkroom" -> Icons.Filled.ShoppingBag
        "cleaning_services" -> Icons.Filled.CleaningServices
        "child_care" -> Icons.Filled.ChildCare
        "kitchen" -> Icons.Filled.Kitchen

        // 居住相关
        "water_drop" -> Icons.Filled.WaterDrop
        "manage_accounts" -> Icons.Filled.MeetingRoom
        "local_fire_department" -> Icons.Filled.LocalFireDepartment
        "phone_android" -> Icons.Filled.PhoneAndroid

        // 娱乐相关
        "sports_esports" -> Icons.Filled.SportsEsports
        "fitness_center" -> Icons.Filled.FitnessCenter
        "directions_run" -> Icons.AutoMirrored.Filled.DirectionsRun
        "music_note" -> Icons.Filled.MusicNote
        "mic" -> Icons.Filled.Mic
        "pool" -> Icons.Filled.Pool
        "forum" -> Icons.Filled.Forum

        // 宠物相关
        "pets" -> Icons.Filled.Pets
        "local_hospital" -> Icons.Filled.LocalHospital

        // 烟酒相关
        "local_bar" -> Icons.Filled.LocalBar

        // 人情往来
        "people" -> Icons.Filled.People
        "redeem" -> Icons.Filled.Redeem

        // 书籍文具
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "auto_stories" -> Icons.Filled.AutoStories
        "edit" -> Icons.Filled.Edit
        "tablet_android" -> Icons.Filled.TabletAndroid

        // 蔬菜
        "eco" -> Icons.Filled.Eco

        // 运动
        "sports_basketball" -> Icons.Filled.SportsBasketball

        // 网络
        "wifi" -> Icons.Filled.Wifi

        // 虚拟产品相关
        "inbox" -> Icons.Filled.Inbox
        "cloud" -> Icons.Filled.Cloud
        "star" -> Icons.Filled.Star
        "favorite" -> Icons.Filled.Favorite
        "monetization_on" -> Icons.Filled.MonetizationOn

        // 零食
        "cookie" -> Icons.Filled.Cookie

        else -> Icons.Filled.MoreHoriz
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(kotlin.math.abs(amount))
}
