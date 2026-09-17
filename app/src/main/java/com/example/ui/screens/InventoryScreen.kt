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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.data.entity.ProductEntity
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.AccountingTeal
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.AccountingViewModel
import java.util.UUID

@Composable
fun InventoryScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val productsWithStock by viewModel.productsWithStock.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Products, 1: Movements, 2: Transfer, 3: Reconcile
    var searchQuery by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val filteredProducts = productsWithStock.filter {
        it.productName.contains(searchQuery, ignoreCase = true) || it.productCode.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "المخزون وإدارة المستودعات",
                subtitle = "عدد الأصناف: ${productsWithStock.size}",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = AccountingTeal,
                    contentColor = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة صنف", fontWeight = FontWeight.Bold)
                    }
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
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("الأصناف (${productsWithStock.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("حركات المخزون", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("تحويل مخزني", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTabIndex == 3, onClick = { selectedTabIndex = 3 }, text = { Text("جرد وتسوية", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            }

            when (selectedTabIndex) {
                0 -> {
                    // PRODUCTS TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            placeholder = { Text("ابحث باسم الصنف أو الكود...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProducts) { item ->
                                val isLowStock = item.totalStock <= item.minLimit
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    "كود: ${item.productCode} • التصنيف: ${item.category} • الوحدة: ${item.unit}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isLowStock) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "مخزون منخفض",
                                                            tint = AccountingRed,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        "${item.totalStock} ${item.unit}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 16.sp,
                                                        color = if (isLowStock) AccountingRed else EmeraldDark
                                                    )
                                                }
                                                Text(
                                                    if (isLowStock) "تحت حد الطلب (${item.minLimit})" else "مخزون كافٍ",
                                                    fontSize = 10.sp,
                                                    color = if (isLowStock) AccountingRed else EmeraldLight
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "سعر التكلفة: ${formatCurrency(item.costPrice)}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "سعر البيع: ${formatCurrency(item.salePrice)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // MOVEMENTS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stockMovements) { mov ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(mov.productName, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${mov.movementNumber} • ${formatDate(mov.date)} • ${mov.notes}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        "${when (mov.type) {
                                            "INBOUND" -> "+ "
                                            "OUTBOUND" -> "- "
                                            else -> "± "
                                        }}${mov.quantity} ${mov.unit}",
                                        fontWeight = FontWeight.Bold,
                                        color = when (mov.type) {
                                            "INBOUND" -> EmeraldDark
                                            "OUTBOUND" -> AccountingRed
                                            else -> AccountingOrange
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TRANSFER TAB
                    StockTransferView(
                        products = productsWithStock,
                        warehouses = warehouses,
                        onTransfer = { prodId, fromWh, toWh, qty, notes ->
                            viewModel.transferStock(prodId, fromWh, toWh, qty, notes)
                        }
                    )
                }
                3 -> {
                    // RECONCILIATION TAB
                    StockReconcileView(
                        products = productsWithStock,
                        warehouses = warehouses,
                        onReconcile = { prodId, whId, actual, reason ->
                            viewModel.reconcileStock(prodId, whId, actual, reason)
                        }
                    )
                }
            }
        }
    }

    // ADD PRODUCT DIALOG
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { product, initialStock ->
                viewModel.saveProduct(product, initialStock)
                showAddProductDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferView(
    products: List<com.example.data.dao.StockProductItem>,
    warehouses: List<com.example.data.entity.WarehouseEntity>,
    onTransfer: (prodId: String, fromWh: String, toWh: String, qty: Double, notes: String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var fromWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var toWarehouse by remember { mutableStateOf(warehouses.getOrNull(1) ?: warehouses.firstOrNull()) }
    var quantityText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("تحويل بضاعة بين الفروع") }
    var expandedProduct by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("تحويل كميات بين المستودعات", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingTeal)
        Spacer(modifier = Modifier.height(12.dp))

        // Product selection
        Text("الصنف المراد تحويله:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        text = { Text("${p.productName} (الرصيد: ${p.totalStock} ${p.unit})") },
                        onClick = {
                            selectedProduct = p
                            expandedProduct = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fromWarehouse?.name ?: "المستودع المصدر",
                onValueChange = {},
                readOnly = true,
                label = { Text("من مستودع") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = toWarehouse?.name ?: "المستودع الهدف",
                onValueChange = {},
                readOnly = true,
                label = { Text("إلى مستودع") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("الكمية المراد نقلها (${selectedProduct?.unit ?: ""})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("البيان والسبب") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val p = selectedProduct ?: return@Button
                val fw = fromWarehouse ?: return@Button
                val tw = toWarehouse ?: return@Button
                val q = quantityText.toDoubleOrNull() ?: 0.0
                if (q > 0) {
                    onTransfer(p.productId, fw.id, tw.id, q, notesText)
                    quantityText = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccountingTeal),
            enabled = (quantityText.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Icon(Icons.Default.CompareArrows, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تنفيذ التحويل المخزني فوراً")
        }
    }
}

@Composable
fun StockReconcileView(
    products: List<com.example.data.dao.StockProductItem>,
    warehouses: List<com.example.data.entity.WarehouseEntity>,
    onReconcile: (prodId: String, whId: String, actual: Double, reason: String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var actualCountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("جرد دوري شهري") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الجرد المخزني والتسوية الجردية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingOrange)
        Spacer(modifier = Modifier.height(12.dp))

        Text("اختر الصنف المراد جرده:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        products.take(5).forEach { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedProduct?.productId == p.productId) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(p.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("الدفتري: ${p.totalStock} ${p.unit}", fontSize = 12.sp, color = Color.Gray)
                    Button(
                        onClick = { selectedProduct = p },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProduct?.productId == p.productId) AccountingOrange else EmeraldDark
                        )
                    ) {
                        Text("اختيار", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        selectedProduct?.let { p ->
            Text("الصنف المحدد: ${p.productName}", fontWeight = FontWeight.Bold, color = EmeraldDark)
            Text("الكمية الدفترية في النظام: ${p.totalStock} ${p.unit}")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = actualCountText,
                onValueChange = { actualCountText = it },
                label = { Text("الكمية الفعلية الموجودة على أرض الواقع") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            val actual = actualCountText.toDoubleOrNull() ?: p.totalStock
            val diff = actual - p.totalStock
            Text(
                "الفارق الناتج: ${if (diff > 0) "+$diff (زيادة)" else if (diff < 0) "$diff (عجز)" else "مطابق"}",
                color = if (diff < 0) AccountingRed else EmeraldDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it },
                label = { Text("سبب الفارق وملاحظات الجرد") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val wh = warehouses.firstOrNull() ?: return@Button
                    val a = actualCountText.toDoubleOrNull() ?: return@Button
                    onReconcile(p.productId, wh.id, a, reasonText)
                    actualCountText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccountingOrange)
            ) {
                Text("اعتماد التسوية الجردية وتحديث الرصيد")
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("PRD-" + (System.currentTimeMillis() % 10000)) }
    var category by remember { mutableStateOf("عام") }
    var unit by remember { mutableStateOf("حبة") }
    var costPriceText by remember { mutableStateOf("") }
    var salePriceText by remember { mutableStateOf("") }
    var initialStockText by remember { mutableStateOf("0") }
    var minLimitText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صنف جديد", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الصنف * (مثال: بوالد 50 لتر)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("باركود / كود الصنف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("التصنيف") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("الوحدة (حبة، كيس..)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = costPriceText,
                            onValueChange = { costPriceText = it },
                            label = { Text("سعر التكلفة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salePriceText,
                            onValueChange = { salePriceText = it },
                            label = { Text("سعر البيع *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = initialStockText,
                            onValueChange = { initialStockText = it },
                            label = { Text("الرصيد الافتتاحي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minLimitText,
                            onValueChange = { minLimitText = it },
                            label = { Text("حد الطلب الأدنى") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val product = ProductEntity(
                            id = UUID.randomUUID().toString(),
                            code = code.trim(),
                            name = name.trim(),
                            category = category.trim(),
                            unit = unit.trim(),
                            costPrice = costPriceText.toDoubleOrNull() ?: 0.0,
                            salePrice = salePriceText.toDoubleOrNull() ?: 0.0,
                            minLimit = minLimitText.toDoubleOrNull() ?: 5.0
                        )
                        val initialStock = initialStockText.toDoubleOrNull() ?: 0.0
                        onSave(product, initialStock)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingTeal),
                enabled = name.isNotBlank()
            ) {
                Text("حفظ الصنف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
