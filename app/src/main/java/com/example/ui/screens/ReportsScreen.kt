package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccountingBlue
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun ReportsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val productsWithStock by viewModel.productsWithStock.collectAsState()
    val expensesRevenues by viewModel.expensesRevenues.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Income Statement, 1: Inventory, 2: Trial Balance

    // Income Statement Calculations
    val totalSales = invoices.filter { it.type == "SALE" && it.status == "ACTIVE" }.sumOf { it.total }
    val totalReturns = invoices.filter { it.type == "SALE_RETURN" }.sumOf { it.total }
    val netSales = (totalSales - totalReturns).coerceAtLeast(0.0)

    // COGS estimation (from products sold or accounts)
    val cogs = accounts.find { it.id == "acc_cogs" }?.balance ?: (netSales * 0.70)
    val grossProfit = netSales - cogs

    val totalExpenses = expensesRevenues.filter { it.type == "EXPENSE" }.sumOf { it.amount } +
            (accounts.find { it.id == "acc_expenses" }?.balance ?: 0.0)
    val otherRevenues = expensesRevenues.filter { it.type == "REVENUE" }.sumOf { it.amount }
    val netProfit = grossProfit - totalExpenses + otherRevenues

    // Inventory Valuation
    val totalStockQty = productsWithStock.sumOf { it.totalStock }
    val totalStockValuation = productsWithStock.sumOf { it.totalStock * it.costPrice }
    val totalSaleValuation = productsWithStock.sumOf { it.totalStock * it.salePrice }
    val expectedProfit = totalSaleValuation - totalStockValuation

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "التقارير المالية والمحاسبية",
                subtitle = "تحليلات الأداء والأرباح والمخزون",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("قائمة الدخل والأرباح", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("تقرير المخزون", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("ميزان المراجعة", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // INCOME STATEMENT
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("قائمة الأرباح والخسائر (Income Statement)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldDark)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    ReportRow(label = "إجمالي المبيعات", value = formatCurrency(totalSales), isPositive = true)
                                    ReportRow(label = "مردودات المبيعات (المرتجع)", value = "- " + formatCurrency(totalReturns), isPositive = false)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    ReportRow(label = "صافي المبيعات", value = formatCurrency(netSales), isBold = true)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ReportRow(label = "تكلفة البضاعة المباعة (COGS)", value = "- " + formatCurrency(cogs), isPositive = false)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    ReportRow(label = "مجمل الربح (Gross Profit)", value = formatCurrency(grossProfit), isBold = true, color = EmeraldLight)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ReportRow(label = "المصروفات التشغيلية والإدارية", value = "- " + formatCurrency(totalExpenses), isPositive = false)
                                    if (otherRevenues > 0) {
                                        ReportRow(label = "إيرادات أخرى متنوعة", value = "+ " + formatCurrency(otherRevenues), isPositive = true)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    // NET PROFIT
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (netProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("صافي الربح النهائي (Net Profit)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                            Text(if (netProfit >= 0) "المنشأة تحقق أرباحاً جيدة" else "عجز صافي", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Text(
                                            formatCurrency(netProfit),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = if (netProfit >= 0) EmeraldDark else AccountingRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // INVENTORY VALUATION
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("تقييم المخزون الحالي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldDark)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    ReportRow(label = "إجمالي عدد الوحدات في المستودع", value = "$totalStockQty قطعة / وحدة")
                                    ReportRow(label = "قيمة المخزون بسعر التكلفة (رأس المال المجمد)", value = formatCurrency(totalStockValuation))
                                    ReportRow(label = "قيمة المخزون المتوقعة بسعر البيع", value = formatCurrency(totalSaleValuation))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    ReportRow(
                                        label = "الربح المتوقع عند بيع كامل المخزون",
                                        value = formatCurrency(expectedProfit),
                                        isBold = true,
                                        color = EmeraldLight
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // TRIAL BALANCE
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("ميزان المراجعة بالأرصدة (Trial Balance)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldDark)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    accounts.forEach { acc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${acc.code} - ${acc.name}", fontSize = 13.sp)
                                            Text(
                                                formatCurrency(acc.balance),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (acc.type == "LIABILITY" || acc.type == "EQUITY") AccountingBlue else EmeraldDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    isPositive: Boolean? = null,
    isBold: Boolean = false,
    color: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color ?: when (isPositive) {
                true -> EmeraldDark
                false -> AccountingRed
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
