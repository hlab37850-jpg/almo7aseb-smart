package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccountingBlue
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingPurple
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.AccountingTeal
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.ScreenDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val destination: ScreenDestination
)

@Composable
fun HomeScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val settings by viewModel.companySettings.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val recentInvoices by viewModel.allInvoices.collectAsState()

    val pendingDebtsCount = appointments.count { it.status == "PENDING" && it.dueDate <= System.currentTimeMillis() + (2L * 86400000) }
    val currentDateStr = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())

    val quickActions = listOf(
        QuickActionItem("المبيعات", "فواتير ومرتجعات", Icons.Default.ShoppingCart, EmeraldLight, ScreenDestination.Sales),
        QuickActionItem("المشتريات", "توريد وموردين", Icons.Default.ShoppingBag, AccountingBlue, ScreenDestination.Purchases),
        QuickActionItem("الصندوق والبنك", "قبض وصرف نقدية", Icons.Default.AccountBalanceWallet, AccountingOrange, ScreenDestination.CashBox),
        QuickActionItem("العملاء والموردين", "أرصدة وكشوفات", Icons.Default.People, AccountingPurple, ScreenDestination.Customers),
        QuickActionItem("المخزون والمستودعات", "أرصدة وحركات", Icons.Default.Inventory, AccountingTeal, ScreenDestination.Inventory),
        QuickActionItem("القيود المحاسبية", "دليل الحسابات وJV", Icons.Default.Receipt, Color(0xFF34495E), ScreenDestination.AccountsJournal),
        QuickActionItem("المواعيد والتحصيل", "وعود وتذكير واتساب", Icons.Default.DateRange, AccountingRed, ScreenDestination.Appointments),
        QuickActionItem("المصروفات", "تشغيلية وإيرادات", Icons.Default.AttachMoney, Color(0xFF16A085), ScreenDestination.Expenses),
        QuickActionItem("التقارير المالية", "أرباح وميزان مراجعة", Icons.Default.Assessment, GoldAccent, ScreenDestination.Reports)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // TOP BRAND HEADER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(EmeraldDark)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المحاسب الذكي",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = settings?.companyName ?: "محلات العالمية للتجارة",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFC5E5DC)
                                )
                            )
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentDateStr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE2F3EC),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // METRIC STAT CARDS
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "مبيعات اليوم",
                        amount = stats.todaySales,
                        icon = Icons.Default.PointOfSale,
                        iconTint = EmeraldLight,
                        badgeColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenDestination.Sales) }
                    )
                    MetricStatCard(
                        title = "رصيد الصندوق",
                        amount = stats.cashBalance,
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = AccountingOrange,
                        badgeColor = Color(0xFFFFF3E0),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenDestination.CashBox) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "ديون العملاء (لنا)",
                        amount = stats.totalReceivables,
                        icon = Icons.Default.People,
                        iconTint = AccountingBlue,
                        badgeColor = Color(0xFFE3F2FD),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenDestination.Customers) }
                    )
                    MetricStatCard(
                        title = "مستحقات الموردين (علينا)",
                        amount = stats.totalPayables,
                        icon = Icons.Default.ShoppingBag,
                        iconTint = AccountingRed,
                        badgeColor = Color(0xFFFFEBEE),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenDestination.Suppliers) }
                    )
                }
            }
        }

        // OVERDUE DEBT ALERT BANNER
        if (pendingDebtsCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { viewModel.navigateTo(ScreenDestination.Appointments) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0B2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه استحقاق مديونيات!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            )
                            Text(
                                text = "يوجد $pendingDebtsCount مواعيد سداد مستحقة للتحصيل اليوم أو قريباً. اضغط لإرسال تذكير واتساب",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF795548),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // OPERATIONS SECTION TITLE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "العمليات السريعة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "نظام متكامل",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // ACTIONS GRID
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val chunked = quickActions.chunked(3)
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { action ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(105.dp)
                                    .clickable { viewModel.navigateTo(action.destination) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(action.color.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = action.title,
                                            tint = action.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = action.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // RECENT TRANSACTIONS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الحركات والفواتير",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "عرض الكل",
                    modifier = Modifier.clickable { viewModel.navigateTo(ScreenDestination.Sales) },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (recentInvoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد فواتير مسجلة حتى الآن",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }
            }
        } else {
            items(recentInvoices.take(4).size) { index ->
                val inv = recentInvoices[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (inv.type) {
                                            "SALE" -> EmeraldLight.copy(alpha = 0.15f)
                                            "PURCHASE" -> AccountingBlue.copy(alpha = 0.15f)
                                            else -> AccountingRed.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (inv.type) {
                                        "SALE" -> Icons.Default.ShoppingCart
                                        "PURCHASE" -> Icons.Default.ShoppingBag
                                        else -> Icons.Default.Receipt
                                    },
                                    contentDescription = null,
                                    tint = when (inv.type) {
                                        "SALE" -> EmeraldLight
                                        "PURCHASE" -> AccountingBlue
                                        else -> AccountingRed
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = inv.partyName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${inv.invoiceNumber} • ${formatDate(inv.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(inv.total),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            )
                            Text(
                                text = when (inv.paymentType) {
                                    "CASH" -> "نقداً"
                                    "CREDIT" -> "آجل"
                                    else -> "جزئي (متبقي ${formatCurrency(inv.remainingAmount)})"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = if (inv.remainingAmount > 0) AccountingRed else EmeraldLight
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
