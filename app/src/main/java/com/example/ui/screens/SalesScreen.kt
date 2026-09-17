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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.AccountingViewModel
import java.util.UUID

@Composable
fun SalesScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val productsWithStock by viewModel.productsWithStock.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CASH, CREDIT, PARTIAL, RETURN
    var showNewInvoiceDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }

    val salesOnly = invoices.filter { it.type == "SALE" || it.type == "SALE_RETURN" }
    val filteredInvoices = salesOnly.filter { inv ->
        val matchesQuery = inv.partyName.contains(searchQuery, ignoreCase = true) ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "CASH" -> inv.paymentType == "CASH" && inv.type == "SALE"
            "CREDIT" -> inv.paymentType == "CREDIT" && inv.type == "SALE"
            "PARTIAL" -> inv.paymentType == "PARTIAL" && inv.type == "SALE"
            "RETURN" -> inv.type == "SALE_RETURN"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "فواتير المبيعات والمرتجعات",
                subtitle = "إجمالي الفواتير: ${salesOnly.size}",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) },
                actions = {
                    IconButton(onClick = { showReturnDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AssignmentReturn,
                            contentDescription = "مرتجع مبيعات",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewInvoiceDialog = true },
                containerColor = EmeraldDark,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فاتورة جديدة", fontWeight = FontWeight.Bold)
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
            // SEARCH & FILTERS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث برقم الفاتورة أو اسم العميل...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("الكل (${salesOnly.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "CASH",
                            onClick = { selectedFilter = "CASH" },
                            label = { Text("نقداً") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "CREDIT",
                            onClick = { selectedFilter = "CREDIT" },
                            label = { Text("آجل") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "PARTIAL",
                            onClick = { selectedFilter = "PARTIAL" },
                            label = { Text("جزئي") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RETURN",
                            onClick = { selectedFilter = "RETURN" },
                            label = { Text("مرتجعات") }
                        )
                    }
                }
            }

            // INVOICES LIST
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد فواتير مطابقة للبحث",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices) { inv ->
                        InvoiceCardItem(
                            invoice = inv,
                            onClick = { selectedInvoiceForDetails = inv }
                        )
                    }
                }
            }
        }
    }

    // NEW INVOICE DIALOG
    if (showNewInvoiceDialog) {
        NewSalesInvoiceDialog(
            customers = customers,
            products = productsWithStock,
            warehouses = warehouses,
            onDismiss = { showNewInvoiceDialog = false },
            onSave = { partyId, partyName, warehouseId, paymentType, items, discount, paidAmount, dueDate, notes ->
                viewModel.createSaleInvoice(
                    partyId, partyName, warehouseId, paymentType, items, discount, paidAmount, dueDate, notes
                )
                showNewInvoiceDialog = false
            }
        )
    }

    // SALES RETURN DIALOG
    if (showReturnDialog) {
        SalesReturnDialog(
            customers = customers,
            products = productsWithStock,
            warehouses = warehouses,
            onDismiss = { showReturnDialog = false },
            onSave = { partyId, partyName, whId, items, refundCash, notes ->
                viewModel.createSaleReturn(null, partyId, partyName, whId, items, refundCash, notes)
                showReturnDialog = false
            }
        )
    }

    // INVOICE DETAILS DIALOG
    selectedInvoiceForDetails?.let { inv ->
        InvoiceDetailsDialog(
            invoice = inv,
            onDismiss = { selectedInvoiceForDetails = null }
        )
    }
}

@Composable
fun InvoiceCardItem(
    invoice: InvoiceEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                            .background(
                                if (invoice.type == "SALE_RETURN") AccountingRed.copy(alpha = 0.15f)
                                else EmeraldLight.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (invoice.type == "SALE_RETURN") Icons.Default.AssignmentReturn else Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = if (invoice.type == "SALE_RETURN") AccountingRed else EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = invoice.partyName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${invoice.invoiceNumber} • ${formatDate(invoice.createdAt)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                        )
                    }
                }

                Text(
                    text = formatCurrency(invoice.total),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (invoice.type == "SALE_RETURN") AccountingRed else EmeraldDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (invoice.paymentType) {
                        "CASH" -> "طريقة الدفع: نقداً"
                        "CREDIT" -> "طريقة الدفع: آجل بالكامل"
                        else -> "دفع جزئي (مسدد: ${formatCurrency(invoice.paidAmount)})"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (invoice.remainingAmount > 0) {
                    Text(
                        text = "المتبقي: ${formatCurrency(invoice.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AccountingRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSalesInvoiceDialog(
    customers: List<com.example.data.entity.PartyEntity>,
    products: List<com.example.data.dao.StockProductItem>,
    warehouses: List<com.example.data.entity.WarehouseEntity>,
    onDismiss: () -> Unit,
    onSave: (partyId: String, partyName: String, whId: String, paymentType: String, items: List<InvoiceItemEntity>, discount: Double, paid: Double, dueDate: Long?, notes: String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var paymentType by remember { mutableStateOf("CASH") } // CASH, CREDIT, PARTIAL
    val invoiceItems = remember { mutableStateListOf<InvoiceItemEntity>() }
    var discountText by remember { mutableStateOf("0") }
    var paidText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    // Line adder state
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var itemQtyText by remember { mutableStateOf("1") }
    var itemPriceText by remember { mutableStateOf(products.firstOrNull()?.salePrice?.toString() ?: "0") }

    var expandedCustomer by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf(false) }

    val subtotal = invoiceItems.sumOf { it.totalPrice }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val total = (subtotal - discount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("فاتورة مبيعات جديدة", fontWeight = FontWeight.Bold, color = EmeraldDark)
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Customer Selector
                item {
                    Text("العميل:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = expandedCustomer,
                        onExpandedChange = { expandedCustomer = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "اختر العميل...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomer) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCustomer,
                            onDismissRequest = { expandedCustomer = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (رصيد: ${formatCurrency(c.balance)})") },
                                    onClick = {
                                        selectedCustomer = c
                                        expandedCustomer = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Add Item Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("إضافة صنف للفاتورة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            // Product Dropdown
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
                                            text = { Text("${p.productName} (متوفر: ${p.totalStock} ${p.unit})") },
                                            onClick = {
                                                selectedProduct = p
                                                itemPriceText = p.salePrice.toString()
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
                                // CRITICAL: Quantity is NOT extracted from product name (e.g. "50 لتر"), it's user input
                                OutlinedTextField(
                                    value = itemQtyText,
                                    onValueChange = { itemQtyText = it },
                                    label = { Text("الكمية (${selectedProduct?.unit ?: ""})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = itemPriceText,
                                    onValueChange = { itemPriceText = it },
                                    label = { Text("سعر الحبة") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val prod = selectedProduct
                                    val qty = itemQtyText.toDoubleOrNull() ?: 0.0
                                    val price = itemPriceText.toDoubleOrNull() ?: 0.0
                                    if (prod != null && qty > 0 && price > 0) {
                                        invoiceItems.add(
                                            InvoiceItemEntity(
                                                id = UUID.randomUUID().toString(),
                                                invoiceId = "",
                                                productId = prod.productId,
                                                productName = prod.productName,
                                                unit = prod.unit,
                                                quantity = qty,
                                                unitPrice = price,
                                                totalPrice = qty * price
                                            )
                                        )
                                        itemQtyText = "1"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إدراج الصنف في الفاتورة")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Table of added items
                if (invoiceItems.isNotEmpty()) {
                    item {
                        Text("الأصناف المدرجة (${invoiceItems.size}):", fontWeight = FontWeight.Bold)
                        invoiceItems.forEachIndexed { index, itm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itm.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "${itm.quantity} ${itm.unit} × ${formatCurrency(itm.unitPrice)} = ${formatCurrency(itm.totalPrice)}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { invoiceItems.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AccountingRed)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Financials & Payment Method
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8F5))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المجموع الفرعي:", fontWeight = FontWeight.Medium)
                                Text(formatCurrency(subtotal), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it },
                                label = { Text("الخصم الممنوح") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الصافي الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatCurrency(total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldDark)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("طريقة السداد:", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = paymentType == "CASH",
                                    onClick = { paymentType = "CASH"; paidText = total.toString() },
                                    label = { Text("نقداً") }
                                )
                                FilterChip(
                                    selected = paymentType == "CREDIT",
                                    onClick = { paymentType = "CREDIT"; paidText = "0" },
                                    label = { Text("آجل بالكامل") }
                                )
                                FilterChip(
                                    selected = paymentType == "PARTIAL",
                                    onClick = { paymentType = "PARTIAL" },
                                    label = { Text("سداد جزئي") }
                                )
                            }

                            if (paymentType == "PARTIAL") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = paidText,
                                    onValueChange = { paidText = it },
                                    label = { Text("المبلغ المدفوع نقداً") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val p = paidText.toDoubleOrNull() ?: 0.0
                                Text(
                                    "المتبقي بالآجل: ${formatCurrency(total - p)}",
                                    color = AccountingRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = { Text("ملاحظات الفاتورة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val wh = selectedWarehouse ?: warehouses.firstOrNull() ?: return@Button
                    if (invoiceItems.isEmpty()) return@Button
                    val paid = when (paymentType) {
                        "CASH" -> total
                        "CREDIT" -> 0.0
                        else -> (paidText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, total)
                    }
                    val due = if (paymentType != "CASH") System.currentTimeMillis() + (14L * 86400000) else null
                    onSave(
                        cust.id, cust.name, wh.id, paymentType, invoiceItems.toList(),
                        discount, paid, due, notesText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                enabled = invoiceItems.isNotEmpty() && selectedCustomer != null
            ) {
                Text("حفظ وإصدار الفاتورة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun SalesReturnDialog(
    customers: List<com.example.data.entity.PartyEntity>,
    products: List<com.example.data.dao.StockProductItem>,
    warehouses: List<com.example.data.entity.WarehouseEntity>,
    onDismiss: () -> Unit,
    onSave: (partyId: String, partyName: String, whId: String, items: List<InvoiceItemEntity>, refundCash: Boolean, notes: String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf(products.firstOrNull()?.salePrice?.toString() ?: "0") }
    var refundCash by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("بضاعة مرتجعة بسبب عدم مطابقة المواصفات") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل مرتجع مبيعات", fontWeight = FontWeight.Bold, color = AccountingRed) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("العميل: ${selectedCustomer?.name}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("الصنف المرتجع: ${selectedProduct?.productName}")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("الكمية المرتجعة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر البيع المرتجع") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = refundCash,
                        onClick = { refundCash = true },
                        label = { Text("رد نقدية من الصندوق") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = !refundCash,
                        onClick = { refundCash = false },
                        label = { Text("خصم من دين العميل") }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("سبب الإرجاع") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = selectedCustomer ?: return@Button
                    val wh = selectedWarehouse ?: warehouses.firstOrNull() ?: return@Button
                    val p = selectedProduct ?: return@Button
                    val q = qtyText.toDoubleOrNull() ?: 1.0
                    val pr = priceText.toDoubleOrNull() ?: p.salePrice
                    val item = InvoiceItemEntity(
                        id = UUID.randomUUID().toString(),
                        invoiceId = "",
                        productId = p.productId,
                        productName = p.productName,
                        unit = p.unit,
                        quantity = q,
                        unitPrice = pr,
                        totalPrice = q * pr
                    )
                    onSave(c.id, c.name, wh.id, listOf(item), refundCash, notesText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingRed)
            ) {
                Text("تأكيد المرتجع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun InvoiceDetailsDialog(
    invoice: InvoiceEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفاصيل الفاتورة: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("الطرف: ${invoice.partyName}", fontWeight = FontWeight.Bold)
                Text("النوع: ${if (invoice.type == "SALE") "فاتورة مبيعات" else "مرتجع مبيعات"}")
                Text("التاريخ: ${formatDateTime(invoice.createdAt)}")
                Text("المجموع الإجمالي: ${formatCurrency(invoice.total)}", fontWeight = FontWeight.Bold, color = EmeraldDark)
                Text("المبلغ المسدد: ${formatCurrency(invoice.paidAmount)}")
                Text("المبلغ المتبقي: ${formatCurrency(invoice.remainingAmount)}", color = AccountingRed)
                if (invoice.notes.isNotBlank()) {
                    Text("الملاحظات: ${invoice.notes}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
