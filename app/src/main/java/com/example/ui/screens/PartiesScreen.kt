package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PartyEntity
import com.example.ui.theme.AccountingBlue
import com.example.ui.theme.AccountingPurple
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.AccountingViewModel
import java.util.UUID

@Composable
fun PartiesScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val settings by viewModel.companySettings.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Customers, 1: Suppliers
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var selectedPartyForStatement by remember { mutableStateOf<PartyEntity?>(null) }

    val currentList = if (selectedTabIndex == 0) customers else suppliers
    val filtered = currentList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = if (selectedTabIndex == 0) "إدارة العملاء والمدينون" else "إدارة الموردين والدائنون",
                subtitle = "إجمالي الحسابات: ${currentList.size}",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = if (selectedTabIndex == 0) EmeraldDark else AccountingBlue,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedTabIndex == 0) "إضافة عميل" else "إضافة مورد", fontWeight = FontWeight.Bold)
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("العملاء (${customers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("الموردون (${suppliers.size})", fontWeight = FontWeight.Bold) }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(if (selectedTabIndex == 0) "ابحث عن اسم العميل أو الهاتف..." else "ابحث عن المورد...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { party ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (party.type == "CUSTOMER") EmeraldLight.copy(alpha = 0.15f)
                                                else AccountingBlue.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (party.type == "CUSTOMER") EmeraldLight else AccountingBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(party.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            if (party.phone.isNotBlank()) party.phone else "بدون رقم هاتف",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatCurrency(party.balance),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (party.balance > 0) {
                                            if (party.type == "CUSTOMER") AccountingRed else AccountingBlue
                                        } else EmeraldDark
                                    )
                                    Text(
                                        if (party.type == "CUSTOMER") "رصيد مدين (عليه)" else "رصيد دائن (له)",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: WhatsApp, Call, Account Statement
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val companyName = settings?.companyName ?: "المحاسب الذكي"
                                        val msg = viewModel.repository.buildDebtReminderWhatsAppMessage(
                                            companyName = companyName,
                                            customerName = party.name,
                                            amount = party.balance,
                                            dueDate = System.currentTimeMillis()
                                        )
                                        openWhatsApp(context, party.phone, msg)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("واتساب", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { dialPhoneNumber(context, party.phone) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = AccountingBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("اتصال", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { selectedPartyForStatement = party },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كشف حساب", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD PARTY DIALOG
    if (showAddPartyDialog) {
        AddPartyDialog(
            isCustomer = selectedTabIndex == 0,
            onDismiss = { showAddPartyDialog = false },
            onSave = { party ->
                viewModel.saveParty(party)
                showAddPartyDialog = false
            }
        )
    }

    // STATEMENT DIALOG
    selectedPartyForStatement?.let { party ->
        val partyInvoices = invoices.filter { it.partyId == party.id }
        val partyVouchers = vouchers.filter { it.partyId == party.id }

        PartyStatementDialog(
            party = party,
            invoices = partyInvoices,
            vouchers = partyVouchers,
            onDismiss = { selectedPartyForStatement = null }
        )
    }
}

@Composable
fun AddPartyDialog(
    isCustomer: Boolean,
    onDismiss: () -> Unit,
    onSave: (PartyEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCustomer) "إضافة عميل جديد" else "إضافة مورد جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (للواتساب والاتصال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المدينة") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = { Text("سقف الائتمان (الحد الأقصى للديون)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val party = PartyEntity(
                            id = UUID.randomUUID().toString(),
                            accountId = if (isCustomer) "acc_ar" else "acc_ap",
                            name = name.trim(),
                            type = if (isCustomer) "CUSTOMER" else "SUPPLIER",
                            phone = phone.trim(),
                            address = address.trim(),
                            creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                            balance = 0.0,
                            notes = notes.trim()
                        )
                        onSave(party)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                enabled = name.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun PartyStatementDialog(
    party: PartyEntity,
    invoices: List<com.example.data.entity.InvoiceEntity>,
    vouchers: List<com.example.data.entity.PaymentVoucherEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("كشف حساب تفصيلي", fontWeight = FontWeight.Bold, color = EmeraldDark)
                Text(party.name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7F6))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الرصيد الصافي الحالي:", fontWeight = FontWeight.Bold)
                        Text(
                            formatCurrency(party.balance),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (party.balance > 0) AccountingRed else EmeraldDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("الحركات المسجلة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(invoices) { inv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${if (inv.type == "SALE") "فاتورة مبيعات" else "فاتورة مشتريات"} ${inv.invoiceNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(formatDate(inv.createdAt), fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(formatCurrency(inv.total), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(vouchers) { v ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${if (v.type == "RECEIPT") "سند قبض" else "سند صرف"} ${v.voucherNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(formatDate(v.date), fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(formatCurrency(v.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
