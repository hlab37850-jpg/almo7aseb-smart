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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseRevenueEntity
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.AccountingViewModel
import java.util.UUID

@Composable
fun CashBoxScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val expensesRevenues by viewModel.expensesRevenues.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Receipts, 1: Payments, 2: Expenses/Revenues
    var showNewVoucherDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    val receipts = vouchers.filter { it.type == "RECEIPT" }
    val payments = vouchers.filter { it.type == "PAYMENT" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "حركة الصندوق والمدفوعات",
                subtitle = "الرصيد: ${formatCurrency(stats.cashBalance)}",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 2) showExpenseDialog = true
                    else showNewVoucherDialog = true
                },
                containerColor = when (selectedTabIndex) {
                    0 -> EmeraldDark
                    1 -> AccountingRed
                    else -> AccountingOrange
                },
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (selectedTabIndex) {
                            0 -> "سند قبض جديد"
                            1 -> "سند صرف جديد"
                            else -> "تسجيل مصروف / إيراد"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // BOX & BANK BALANCES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "الصندوق الرئيسي (نقدية)",
                    amount = stats.cashBalance,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = AccountingOrange,
                    badgeColor = Color(0xFFFFF3E0),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "الحسابات البنكية",
                    amount = stats.bankBalance,
                    icon = Icons.Default.AccountBalance,
                    iconTint = EmeraldLight,
                    badgeColor = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f)
                )
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("المقبوضات (${receipts.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("المدفوعات (${payments.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("المصروفات (${expensesRevenues.size})", fontWeight = FontWeight.Bold) }
                )
            }

            // LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTabIndex == 0) {
                    items(receipts) { v ->
                        VoucherCardItem(v = v, isReceipt = true)
                    }
                } else if (selectedTabIndex == 1) {
                    items(payments) { v ->
                        VoucherCardItem(v = v, isReceipt = false)
                    }
                } else {
                    items(expensesRevenues) { er ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(er.category, fontWeight = FontWeight.Bold)
                                    Text(formatDate(er.date), fontSize = 11.sp, color = Color.Gray)
                                    if (er.notes.isNotBlank()) Text(er.notes, fontSize = 11.sp, color = Color.DarkGray)
                                }
                                Text(
                                    formatCurrency(er.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (er.type == "EXPENSE") AccountingRed else EmeraldDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // NEW VOUCHER DIALOG
    if (showNewVoucherDialog) {
        val isReceipt = selectedTabIndex == 0
        NewVoucherDialog(
            isReceipt = isReceipt,
            parties = if (isReceipt) customers else suppliers,
            onDismiss = { showNewVoucherDialog = false },
            onSave = { partyId, partyName, amount, notes ->
                if (isReceipt) {
                    viewModel.createReceipt(partyId, partyName, amount, null, notes)
                } else {
                    viewModel.createPayment(partyId, partyName, amount, notes)
                }
                showNewVoucherDialog = false
            }
        )
    }

    // NEW EXPENSE DIALOG
    if (showExpenseDialog) {
        NewExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { item ->
                viewModel.recordExpenseRevenue(item)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
fun VoucherCardItem(
    v: com.example.data.entity.PaymentVoucherEntity,
    isReceipt: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
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
                        .background(if (isReceipt) EmeraldLight.copy(alpha = 0.15f) else AccountingRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReceipt) Icons.Default.CallReceived else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (isReceipt) EmeraldLight else AccountingRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(v.partyName, fontWeight = FontWeight.Bold)
                    Text("${v.voucherNumber} • ${formatDate(v.date)}", fontSize = 11.sp, color = Color.Gray)
                    if (v.notes.isNotBlank()) Text(v.notes, fontSize = 11.sp, color = Color.DarkGray)
                }
            }
            Text(
                formatCurrency(v.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isReceipt) EmeraldDark else AccountingRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewVoucherDialog(
    isReceipt: Boolean,
    parties: List<com.example.data.entity.PartyEntity>,
    onDismiss: () -> Unit,
    onSave: (partyId: String?, partyName: String, amount: Double, notes: String) -> Unit
) {
    var selectedParty by remember { mutableStateOf(parties.firstOrNull()) }
    var manualPartyName by remember { mutableStateOf("") }
    var isManualParty by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isReceipt) "إصدار سند قبض نقدية" else "إصدار سند صرف نقدية",
                fontWeight = FontWeight.Bold,
                color = if (isReceipt) EmeraldDark else AccountingRed
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isManualParty) {
                    Text(if (isReceipt) "المستلم منه (العميل):" else "المصروف له (المورد):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedParty?.name ?: "اختر...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            parties.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (رصيد: ${formatCurrency(p.balance)})") },
                                    onClick = {
                                        selectedParty = p
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualPartyName,
                        onValueChange = { manualPartyName = it },
                        label = { Text("اسم الجهة / الحساب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !isManualParty,
                        onClick = { isManualParty = false },
                        label = { Text("من القائمة") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = isManualParty,
                        onClick = { isManualParty = true },
                        label = { Text("جهة أخرى") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ بالريال *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("البيان / ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val name = if (isManualParty) manualPartyName.trim() else (selectedParty?.name ?: "")
                    val partyId = if (isManualParty) null else selectedParty?.id
                    if (amount > 0 && name.isNotBlank()) {
                        onSave(partyId, name, amount, notesText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isReceipt) EmeraldDark else AccountingRed),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("تأكيد وحفظ السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun NewExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (ExpenseRevenueEntity) -> Unit
) {
    var isExpense by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("إيجار") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val categories = if (isExpense) listOf("إيجار", "رواتب وأجور", "كهرباء ومياه", "نقل وشحن", "صيانة", "ضيافة ونثريات", "أخرى")
    else listOf("أرباح استثنائية", "إيراد خدمات", "خصم مكتسب", "أخرى")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isExpense) "تسجيل مصروف جديد" else "تسجيل إيراد إضافي", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row {
                    FilterChip(selected = isExpense, onClick = { isExpense = true; category = "إيجار" }, label = { Text("مصروف") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = !isExpense, onClick = { isExpense = false; category = "إيراد خدمات" }, label = { Text("إيراد") })
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("البند / التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ بالريال *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تفاصيل وبيان المصروف") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val a = amountText.toDoubleOrNull() ?: 0.0
                    if (a > 0) {
                        onSave(
                            ExpenseRevenueEntity(
                                id = UUID.randomUUID().toString(),
                                type = if (isExpense) "EXPENSE" else "REVENUE",
                                category = category,
                                amount = a,
                                accountId = if (isExpense) "acc_expenses" else "acc_sales",
                                notes = notes.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
