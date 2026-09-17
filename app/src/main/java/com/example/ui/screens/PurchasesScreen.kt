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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItemEntity
import com.example.ui.theme.AccountingBlue
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.viewmodel.AccountingViewModel
import java.util.UUID

@Composable
fun PurchasesScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val productsWithStock by viewModel.productsWithStock.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showNewPurchaseDialog by remember { mutableStateOf(false) }

    val purchaseList = invoices.filter { it.type == "PURCHASE" || it.type == "PURCHASE_RETURN" }
    val filtered = purchaseList.filter {
        it.partyName.contains(searchQuery, ignoreCase = true) || it.invoiceNumber.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "فواتير المشتريات والتوريد",
                subtitle = "سجل التوريدات من الموردين",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPurchaseDialog = true },
                containerColor = AccountingBlue,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فاتورة مشتريات", fontWeight = FontWeight.Bold)
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("ابحث برقم الفاتورة أو المورد...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فواتير مشتريات مسجلة", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { inv ->
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
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(AccountingBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingBag,
                                                contentDescription = null,
                                                tint = AccountingBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(inv.partyName, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${inv.invoiceNumber} • ${formatDate(inv.createdAt)}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        formatCurrency(inv.total),
                                        fontWeight = FontWeight.Bold,
                                        color = AccountingBlue,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (inv.paymentType == "CASH") "دفع نقدي من الصندوق" else "شراء آجل للمورد",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    if (inv.remainingAmount > 0) {
                                        Text(
                                            text = "مستحق للمورد: ${formatCurrency(inv.remainingAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccountingRed
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

    // NEW PURCHASE DIALOG
    if (showNewPurchaseDialog) {
        NewPurchaseDialog(
            suppliers = suppliers,
            products = productsWithStock,
            warehouses = warehouses,
            onDismiss = { showNewPurchaseDialog = false },
            onSave = { suppId, suppName, whId, paymentType, items, discount, paid, due, notes ->
                viewModel.createPurchaseInvoice(
                    suppId, suppName, whId, paymentType, items, discount, paid, due, notes
                )
                showNewPurchaseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseDialog(
    suppliers: List<com.example.data.entity.PartyEntity>,
    products: List<com.example.data.dao.StockProductItem>,
    warehouses: List<com.example.data.entity.WarehouseEntity>,
    onDismiss: () -> Unit,
    onSave: (suppId: String, suppName: String, whId: String, paymentType: String, items: List<InvoiceItemEntity>, discount: Double, paid: Double, due: Long?, notes: String) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var paymentType by remember { mutableStateOf("CASH") }
    val purchaseItems = remember { mutableStateListOf<InvoiceItemEntity>() }
    var discountText by remember { mutableStateOf("0") }
    var notesText by remember { mutableStateOf("") }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var itemQtyText by remember { mutableStateOf("1") }
    var itemCostText by remember { mutableStateOf(products.firstOrNull()?.costPrice?.toString() ?: "0") }

    var expandedSupplier by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf(false) }

    val subtotal = purchaseItems.sumOf { it.totalPrice }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val total = (subtotal - discount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فاتورة مشتريات جديدة", fontWeight = FontWeight.Bold, color = AccountingBlue) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("المورد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    ExposedDropdownMenuBox(
                        expanded = expandedSupplier,
                        onExpandedChange = { expandedSupplier = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "اختر المورد...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplier) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSupplier,
                            onDismissRequest = { expandedSupplier = false }
                        ) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (رصيد مستحق: ${formatCurrency(s.balance)})") },
                                    onClick = {
                                        selectedSupplier = s
                                        expandedSupplier = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إضافة صنف مشتريات:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            ExposedDropdownMenuBox(
                                expanded = expandedProduct,
                                onExpandedChange = { expandedProduct = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedProduct?.productName ?: "اختر الصنف...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProduct) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedProduct,
                                    onDismissRequest = { expandedProduct = false }
                                ) {
                                    products.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.productName) },
                                            onClick = {
                                                selectedProduct = p
                                                itemCostText = p.costPrice.toString()
                                                expandedProduct = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = itemQtyText,
                                    onValueChange = { itemQtyText = it },
                                    label = { Text("الكمية") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = itemCostText,
                                    onValueChange = { itemCostText = it },
                                    label = { Text("سعر الشراء / التكلفة") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val p = selectedProduct
                                    val q = itemQtyText.toDoubleOrNull() ?: 0.0
                                    val c = itemCostText.toDoubleOrNull() ?: 0.0
                                    if (p != null && q > 0 && c > 0) {
                                        purchaseItems.add(
                                            InvoiceItemEntity(
                                                id = UUID.randomUUID().toString(),
                                                invoiceId = "",
                                                productId = p.productId,
                                                productName = p.productName,
                                                unit = p.unit,
                                                quantity = q,
                                                unitPrice = c,
                                                totalPrice = q * c
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccountingBlue)
                            ) {
                                Text("إدراج صنف المشتريات")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (purchaseItems.isNotEmpty()) {
                    item {
                        purchaseItems.forEachIndexed { index, itm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${itm.productName} (${itm.quantity} × ${formatCurrency(itm.unitPrice)})", fontSize = 12.sp)
                                IconButton(onClick = { purchaseItems.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AccountingRed)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                item {
                    Text("الإجمالي: ${formatCurrency(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentType == "CASH",
                            onClick = { paymentType = "CASH" },
                            label = { Text("سداد نقداً") }
                        )
                        FilterChip(
                            selected = paymentType == "CREDIT",
                            onClick = { paymentType = "CREDIT" },
                            label = { Text("شراء آجل (دين للمورد)") }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedSupplier ?: return@Button
                    val wh = selectedWarehouse ?: warehouses.firstOrNull() ?: return@Button
                    if (purchaseItems.isEmpty()) return@Button
                    val paid = if (paymentType == "CASH") total else 0.0
                    val due = if (paymentType != "CASH") System.currentTimeMillis() + (30L * 86400000) else null
                    onSave(s.id, s.name, wh.id, paymentType, purchaseItems.toList(), discount, paid, due, notesText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingBlue),
                enabled = purchaseItems.isNotEmpty() && selectedSupplier != null
            ) {
                Text("تأكيد حفظ المشتريات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
