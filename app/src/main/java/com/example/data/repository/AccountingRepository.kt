package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.database.AppDatabase
import com.example.data.dao.StockProductItem
import com.example.data.entity.AccountEntity
import com.example.data.entity.AppointmentEntity
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseRevenueEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItemEntity
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.JournalLineEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.PaymentVoucherEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StockBalanceEntity
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.WarehouseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AccountingRepository(private val dao: AppDao) {

    // Flows for UI observing
    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val customers: Flow<List<PartyEntity>> = dao.getPartiesByType("CUSTOMER")
    val suppliers: Flow<List<PartyEntity>> = dao.getPartiesByType("SUPPLIER")
    val allParties: Flow<List<PartyEntity>> = dao.getAllParties()
    val warehouses: Flow<List<WarehouseEntity>> = dao.getAllWarehouses()
    val products: Flow<List<ProductEntity>> = dao.getAllProducts()
    val productsWithStock: Flow<List<StockProductItem>> = dao.getProductsWithStock()
    val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()
    val salesInvoices: Flow<List<InvoiceEntity>> = dao.getInvoicesByType("SALE")
    val purchaseInvoices: Flow<List<InvoiceEntity>> = dao.getInvoicesByType("PURCHASE")
    val vouchers: Flow<List<PaymentVoucherEntity>> = dao.getAllVouchers()
    val journalEntries: Flow<List<JournalEntryEntity>> = dao.getAllJournalEntries()
    val stockMovements: Flow<List<StockMovementEntity>> = dao.getAllStockMovements()
    val appointments: Flow<List<AppointmentEntity>> = dao.getAllAppointments()
    val pendingAppointments: Flow<List<AppointmentEntity>> = dao.getPendingAppointments()
    val expensesRevenues: Flow<List<ExpenseRevenueEntity>> = dao.getAllExpensesRevenues()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getRecentAuditLogs()
    val companySettings: Flow<CompanySettingsEntity?> = dao.getCompanySettings()

    // 1. SALES INVOICE (ATOMIC)
    suspend fun createSalesInvoice(
        partyId: String,
        partyName: String,
        warehouseId: String,
        paymentType: String, // CASH, CREDIT, PARTIAL
        items: List<InvoiceItemEntity>,
        discount: Double,
        paidAmount: Double,
        dueDate: Long?,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val subtotal = items.sumOf { it.totalPrice }
            val total = (subtotal - discount).coerceAtLeast(0.0)
            val actualPaid = when (paymentType) {
                "CASH" -> total
                "CREDIT" -> 0.0
                else -> paidAmount.coerceIn(0.0, total)
            }
            val remaining = total - actualPaid
            val invoiceId = UUID.randomUUID().toString()
            val invNumber = "INV-" + (System.currentTimeMillis() % 100000)

            val invoice = InvoiceEntity(
                id = invoiceId,
                invoiceNumber = invNumber,
                partyId = partyId,
                partyName = partyName,
                warehouseId = warehouseId,
                type = "SALE",
                paymentType = paymentType,
                subtotal = subtotal,
                discount = discount,
                tax = 0.0,
                total = total,
                paidAmount = actualPaid,
                remainingAmount = remaining,
                dueDate = dueDate,
                notes = notes
            )
            dao.insertInvoice(invoice)

            // Insert Items & Deduct Stock
            var totalCostOfGoods = 0.0
            val preparedItems = items.map { item ->
                val currentStock = dao.getStock(item.productId, warehouseId)?.quantity ?: 0.0
                val newStock = (currentStock - item.quantity).coerceAtLeast(0.0)
                dao.insertStockBalance(
                    StockBalanceEntity(
                        id = "${item.productId}_$warehouseId",
                        productId = item.productId,
                        warehouseId = warehouseId,
                        quantity = newStock
                    )
                )

                // Stock movement
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        movementNumber = "MOV-${System.currentTimeMillis() % 10000}",
                        type = "OUTBOUND",
                        productId = item.productId,
                        productName = item.productName,
                        fromWarehouseId = warehouseId,
                        quantity = item.quantity,
                        unit = item.unit,
                        notes = "صرف مبيعات فاتورة $invNumber"
                    )
                )

                val product = dao.getProductById(item.productId)
                val cost = (product?.costPrice ?: 0.0) * item.quantity
                totalCostOfGoods += cost

                item.copy(id = UUID.randomUUID().toString(), invoiceId = invoiceId)
            }
            dao.insertInvoiceItems(preparedItems)

            // Update Balances & Accounts
            if (actualPaid > 0) {
                dao.adjustAccountBalance("acc_cash", actualPaid)
                dao.insertVoucher(
                    PaymentVoucherEntity(
                        id = UUID.randomUUID().toString(),
                        voucherNumber = "RCV-${System.currentTimeMillis() % 10000}",
                        type = "RECEIPT",
                        partyId = partyId,
                        partyName = partyName,
                        accountId = "acc_cash",
                        amount = actualPaid,
                        invoiceId = invoiceId,
                        notes = "دفعة نقدية فاتورة مبيعات $invNumber"
                    )
                )
            }

            if (remaining > 0) {
                dao.adjustPartyBalance(partyId, remaining)
                dao.adjustAccountBalance("acc_ar", remaining)

                // Create due date appointment
                if (dueDate != null) {
                    val party = dao.getPartyById(partyId)
                    dao.insertAppointment(
                        AppointmentEntity(
                            id = UUID.randomUUID().toString(),
                            partyId = partyId,
                            partyName = partyName,
                            phone = party?.phone ?: "",
                            amount = remaining,
                            dueDate = dueDate,
                            notes = "استحقاق متبقي فاتورة $invNumber"
                        )
                    )
                }
            }

            dao.adjustAccountBalance("acc_sales", total)

            // Double Entry Journal Entry
            val entryId = UUID.randomUUID().toString()
            val entryNumber = "JV-${System.currentTimeMillis() % 100000}"
            dao.insertJournalEntry(
                JournalEntryEntity(
                    id = entryId,
                    entryNumber = entryNumber,
                    description = "قيد فاتورة مبيعات $invNumber للعميل $partyName",
                    referenceId = invoiceId,
                    referenceType = "SALE_INVOICE"
                )
            )

            val lines = mutableListOf<JournalLineEntity>()
            if (actualPaid > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cash", "الصندوق الرئيسي", debit = actualPaid, credit = 0.0))
            }
            if (remaining > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_ar", "العملاء (المدينون) - $partyName", debit = remaining, credit = 0.0))
            }
            lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_sales", "إيراد المبيعات", debit = 0.0, credit = total))

            // Cost of Goods Sold Journal Line
            if (totalCostOfGoods > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cogs", "تكلفة البضاعة المباعة", debit = totalCostOfGoods, credit = 0.0))
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_inv", "مخزون البضاعة", debit = 0.0, credit = totalCostOfGoods))
                dao.adjustAccountBalance("acc_cogs", totalCostOfGoods)
                dao.adjustAccountBalance("acc_inv", -totalCostOfGoods)
            }
            dao.insertJournalLines(lines)

            // Audit Log
            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "CREATE_SALE",
                    entityType = "INVOICE",
                    entityId = invoiceId,
                    details = "إصدار فاتورة مبيعات $invNumber بمبلغ $total ريال للعميل $partyName"
                )
            )

            Result.success(invNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. SALES RETURN (مرتجع مبيعات)
    suspend fun createSalesReturn(
        originalInvoiceId: String?,
        partyId: String,
        partyName: String,
        warehouseId: String,
        items: List<InvoiceItemEntity>,
        refundFromCash: Boolean,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val total = items.sumOf { it.totalPrice }
            val returnInvId = UUID.randomUUID().toString()
            val retNumber = "SRN-" + (System.currentTimeMillis() % 100000)

            val invoice = InvoiceEntity(
                id = returnInvId,
                invoiceNumber = retNumber,
                partyId = partyId,
                partyName = partyName,
                warehouseId = warehouseId,
                type = "SALE_RETURN",
                paymentType = if (refundFromCash) "CASH" else "CREDIT",
                subtotal = total,
                discount = 0.0,
                tax = 0.0,
                total = total,
                paidAmount = if (refundFromCash) total else 0.0,
                remainingAmount = if (refundFromCash) 0.0 else total,
                notes = notes,
                originalInvoiceId = originalInvoiceId
            )
            dao.insertInvoice(invoice)

            // Restock items
            var returnCost = 0.0
            items.forEach { item ->
                val currentStock = dao.getStock(item.productId, warehouseId)?.quantity ?: 0.0
                val newStock = currentStock + item.quantity
                dao.insertStockBalance(
                    StockBalanceEntity(
                        id = "${item.productId}_$warehouseId",
                        productId = item.productId,
                        warehouseId = warehouseId,
                        quantity = newStock
                    )
                )

                dao.insertStockMovement(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        movementNumber = "MOV-${System.currentTimeMillis() % 10000}",
                        type = "INBOUND",
                        productId = item.productId,
                        productName = item.productName,
                        toWarehouseId = warehouseId,
                        quantity = item.quantity,
                        unit = item.unit,
                        notes = "توريد مرتجع مبيعات $retNumber"
                    )
                )

                val product = dao.getProductById(item.productId)
                returnCost += (product?.costPrice ?: 0.0) * item.quantity

                dao.insertInvoiceItems(listOf(item.copy(id = UUID.randomUUID().toString(), invoiceId = returnInvId)))
            }

            // Financial adjustments
            if (refundFromCash) {
                dao.adjustAccountBalance("acc_cash", -total)
                dao.insertVoucher(
                    PaymentVoucherEntity(
                        id = UUID.randomUUID().toString(),
                        voucherNumber = "PAY-${System.currentTimeMillis() % 10000}",
                        type = "PAYMENT",
                        partyId = partyId,
                        partyName = partyName,
                        accountId = "acc_cash",
                        amount = total,
                        notes = "رد نقدي مقابل مرتجع مبيعات $retNumber"
                    )
                )
            } else {
                dao.adjustPartyBalance(partyId, -total)
                dao.adjustAccountBalance("acc_ar", -total)
            }
            dao.adjustAccountBalance("acc_sales", -total)

            // Journal Entry
            val entryId = UUID.randomUUID().toString()
            val entryNumber = "JV-${System.currentTimeMillis() % 100000}"
            dao.insertJournalEntry(
                JournalEntryEntity(
                    id = entryId,
                    entryNumber = entryNumber,
                    description = "قيد مرتجع مبيعات $retNumber للعميل $partyName",
                    referenceId = returnInvId,
                    referenceType = "SALE_RETURN"
                )
            )

            val lines = mutableListOf(
                JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_sales", "مردودات ومسموحات المبيعات", debit = total, credit = 0.0),
                if (refundFromCash) {
                    JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cash", "الصندوق الرئيسي", debit = 0.0, credit = total)
                } else {
                    JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_ar", "العملاء (المدينون)", debit = 0.0, credit = total)
                }
            )

            if (returnCost > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_inv", "مخزون البضاعة", debit = returnCost, credit = 0.0))
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cogs", "تكلفة البضاعة المباعة", debit = 0.0, credit = returnCost))
                dao.adjustAccountBalance("acc_inv", returnCost)
                dao.adjustAccountBalance("acc_cogs", -returnCost)
            }
            dao.insertJournalLines(lines)

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "SALE_RETURN",
                    entityType = "INVOICE",
                    entityId = returnInvId,
                    details = "تسجيل مرتجع مبيعات $retNumber بقيمة $total ريال"
                )
            )

            Result.success(retNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. PURCHASE INVOICE (فاتورة مشتريات)
    suspend fun createPurchaseInvoice(
        supplierId: String,
        supplierName: String,
        warehouseId: String,
        paymentType: String,
        items: List<InvoiceItemEntity>,
        discount: Double,
        paidAmount: Double,
        dueDate: Long?,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val subtotal = items.sumOf { it.totalPrice }
            val total = (subtotal - discount).coerceAtLeast(0.0)
            val actualPaid = when (paymentType) {
                "CASH" -> total
                "CREDIT" -> 0.0
                else -> paidAmount.coerceIn(0.0, total)
            }
            val remaining = total - actualPaid
            val invoiceId = UUID.randomUUID().toString()
            val invNumber = "PUR-" + (System.currentTimeMillis() % 100000)

            val invoice = InvoiceEntity(
                id = invoiceId,
                invoiceNumber = invNumber,
                partyId = supplierId,
                partyName = supplierName,
                warehouseId = warehouseId,
                type = "PURCHASE",
                paymentType = paymentType,
                subtotal = subtotal,
                discount = discount,
                total = total,
                paidAmount = actualPaid,
                remainingAmount = remaining,
                dueDate = dueDate,
                notes = notes
            )
            dao.insertInvoice(invoice)

            // Stock addition
            items.forEach { item ->
                val currentStock = dao.getStock(item.productId, warehouseId)?.quantity ?: 0.0
                val newStock = currentStock + item.quantity
                dao.insertStockBalance(
                    StockBalanceEntity(
                        id = "${item.productId}_$warehouseId",
                        productId = item.productId,
                        warehouseId = warehouseId,
                        quantity = newStock
                    )
                )

                dao.insertStockMovement(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        movementNumber = "MOV-${System.currentTimeMillis() % 10000}",
                        type = "INBOUND",
                        productId = item.productId,
                        productName = item.productName,
                        toWarehouseId = warehouseId,
                        quantity = item.quantity,
                        unit = item.unit,
                        notes = "توريد مشتريات فاتورة $invNumber"
                    )
                )

                dao.insertInvoiceItems(listOf(item.copy(id = UUID.randomUUID().toString(), invoiceId = invoiceId)))
            }

            // Adjust balances
            if (actualPaid > 0) {
                dao.adjustAccountBalance("acc_cash", -actualPaid)
                dao.insertVoucher(
                    PaymentVoucherEntity(
                        id = UUID.randomUUID().toString(),
                        voucherNumber = "PAY-${System.currentTimeMillis() % 10000}",
                        type = "PAYMENT",
                        partyId = supplierId,
                        partyName = supplierName,
                        accountId = "acc_cash",
                        amount = actualPaid,
                        invoiceId = invoiceId,
                        notes = "سداد نقدي لفاتورة مشتريات $invNumber"
                    )
                )
            }

            if (remaining > 0) {
                dao.adjustPartyBalance(supplierId, remaining)
                dao.adjustAccountBalance("acc_ap", remaining)
            }

            dao.adjustAccountBalance("acc_inv", total)

            // Journal Entry
            val entryId = UUID.randomUUID().toString()
            val entryNumber = "JV-${System.currentTimeMillis() % 100000}"
            dao.insertJournalEntry(
                JournalEntryEntity(
                    id = entryId,
                    entryNumber = entryNumber,
                    description = "قيد فاتورة مشتريات $invNumber من المورد $supplierName",
                    referenceId = invoiceId,
                    referenceType = "PURCHASE_INVOICE"
                )
            )

            val lines = mutableListOf(
                JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_inv", "مخزون البضاعة", debit = total, credit = 0.0)
            )
            if (actualPaid > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cash", "الصندوق الرئيسي", debit = 0.0, credit = actualPaid))
            }
            if (remaining > 0) {
                lines.add(JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_ap", "الموردون (الدائنون) - $supplierName", debit = 0.0, credit = remaining))
            }
            dao.insertJournalLines(lines)

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "CREATE_PURCHASE",
                    entityType = "INVOICE",
                    entityId = invoiceId,
                    details = "تسجيل فاتورة مشتريات $invNumber بمبلغ $total ريال"
                )
            )

            Result.success(invNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. CASH RECEIPT (سند قبض)
    suspend fun createReceiptVoucher(
        partyId: String?,
        partyName: String,
        amount: Double,
        invoiceId: String?,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val voucherNumber = "RCV-" + (System.currentTimeMillis() % 100000)
            val voucherId = UUID.randomUUID().toString()

            val voucher = PaymentVoucherEntity(
                id = voucherId,
                voucherNumber = voucherNumber,
                type = "RECEIPT",
                partyId = partyId,
                partyName = partyName,
                accountId = "acc_cash",
                amount = amount,
                invoiceId = invoiceId,
                notes = notes
            )
            dao.insertVoucher(voucher)

            dao.adjustAccountBalance("acc_cash", amount)

            if (partyId != null) {
                dao.adjustPartyBalance(partyId, -amount)
                dao.adjustAccountBalance("acc_ar", -amount)

                // If linked invoice, update invoice remaining
                if (invoiceId != null) {
                    dao.applyPaymentToInvoice(invoiceId, amount)
                }

                // Check if customer balance is 0 or less, resolve pending appointments
                val party = dao.getPartyById(partyId)
                if ((party?.balance ?: 0.0) <= 0.0) {
                    // All debts paid
                    dao.getPendingAppointments().collect { list ->
                        list.filter { it.partyId == partyId }.forEach {
                            dao.updateAppointmentStatus(it.id, "PAID")
                        }
                    }
                }
            }

            // Journal Entry
            val entryId = UUID.randomUUID().toString()
            dao.insertJournalEntry(
                JournalEntryEntity(
                    id = entryId,
                    entryNumber = "JV-${System.currentTimeMillis() % 100000}",
                    description = "سند قبض $voucherNumber من $partyName",
                    referenceId = voucherId,
                    referenceType = "RECEIPT"
                )
            )

            dao.insertJournalLines(listOf(
                JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cash", "الصندوق الرئيسي", debit = amount, credit = 0.0),
                JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_ar", "العملاء (المدينون) - $partyName", debit = 0.0, credit = amount)
            ))

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "RECEIPT",
                    entityType = "VOUCHER",
                    entityId = voucherId,
                    details = "إصدار سند قبض $voucherNumber بمبلغ $amount ريال من $partyName"
                )
            )

            Result.success(voucherNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. CASH PAYMENT (سند صرف)
    suspend fun createPaymentVoucher(
        partyId: String?,
        partyName: String,
        amount: Double,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val voucherNumber = "PAY-" + (System.currentTimeMillis() % 100000)
            val voucherId = UUID.randomUUID().toString()

            val voucher = PaymentVoucherEntity(
                id = voucherId,
                voucherNumber = voucherNumber,
                type = "PAYMENT",
                partyId = partyId,
                partyName = partyName,
                accountId = "acc_cash",
                amount = amount,
                notes = notes
            )
            dao.insertVoucher(voucher)

            dao.adjustAccountBalance("acc_cash", -amount)

            if (partyId != null) {
                dao.adjustPartyBalance(partyId, -amount)
                dao.adjustAccountBalance("acc_ap", -amount)
            } else {
                dao.adjustAccountBalance("acc_expenses", amount)
            }

            // Journal Entry
            val entryId = UUID.randomUUID().toString()
            dao.insertJournalEntry(
                JournalEntryEntity(
                    id = entryId,
                    entryNumber = "JV-${System.currentTimeMillis() % 100000}",
                    description = "سند صرف $voucherNumber إلى $partyName",
                    referenceId = voucherId,
                    referenceType = "PAYMENT"
                )
            )

            val targetAccount = if (partyId != null) "acc_ap" else "acc_expenses"
            val targetName = if (partyId != null) "الموردون (الدائنون) - $partyName" else "المصروفات العمومية"
            dao.insertJournalLines(listOf(
                JournalLineEntity(UUID.randomUUID().toString(), entryId, targetAccount, targetName, debit = amount, credit = 0.0),
                JournalLineEntity(UUID.randomUUID().toString(), entryId, "acc_cash", "الصندوق الرئيسي", debit = 0.0, credit = amount)
            ))

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "PAYMENT",
                    entityType = "VOUCHER",
                    entityId = voucherId,
                    details = "إصدار سند صرف $voucherNumber بمبلغ $amount ريال إلى $partyName"
                )
            )

            Result.success(voucherNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. WAREHOUSE TRANSFER (تحويل بين مستودعين)
    suspend fun transferStock(
        productId: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        quantity: Double,
        notes: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val product = dao.getProductById(productId) ?: return@withContext Result.failure(Exception("المنتج غير موجود"))
            val fromBalance = dao.getStock(productId, fromWarehouseId)?.quantity ?: 0.0
            if (fromBalance < quantity) {
                return@withContext Result.failure(Exception("الكمية المتوفرة في المستودع المصدر ($fromBalance) أقل من الكمية المراد تحويلها ($quantity)"))
            }

            // Deduct from source
            dao.insertStockBalance(
                StockBalanceEntity(
                    id = "${productId}_$fromWarehouseId",
                    productId = productId,
                    warehouseId = fromWarehouseId,
                    quantity = fromBalance - quantity
                )
            )

            // Add to destination
            val toBalance = dao.getStock(productId, toWarehouseId)?.quantity ?: 0.0
            dao.insertStockBalance(
                StockBalanceEntity(
                    id = "${productId}_$toWarehouseId",
                    productId = productId,
                    warehouseId = toWarehouseId,
                    quantity = toBalance + quantity
                )
            )

            dao.insertStockMovement(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    movementNumber = "TRF-${System.currentTimeMillis() % 10000}",
                    type = "TRANSFER",
                    productId = productId,
                    productName = product.name,
                    fromWarehouseId = fromWarehouseId,
                    toWarehouseId = toWarehouseId,
                    quantity = quantity,
                    unit = product.unit,
                    notes = notes
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_TRANSFER",
                    entityType = "INVENTORY",
                    entityId = productId,
                    details = "تحويل كمية $quantity ${product.unit} من ${product.name} بين المستودعات"
                )
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. STOCK ADJUSTMENT / AUDIT (جرد وتسوية مخزنية)
    suspend fun reconcileStock(
        productId: String,
        warehouseId: String,
        actualCount: Double,
        reason: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val product = dao.getProductById(productId) ?: return@withContext Result.failure(Exception("المنتج غير موجود"))
            val systemCount = dao.getStock(productId, warehouseId)?.quantity ?: 0.0
            val variance = actualCount - systemCount

            dao.insertStockBalance(
                StockBalanceEntity(
                    id = "${productId}_$warehouseId",
                    productId = productId,
                    warehouseId = warehouseId,
                    quantity = actualCount
                )
            )

            dao.insertStockMovement(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    movementNumber = "ADJ-${System.currentTimeMillis() % 10000}",
                    type = "ADJUSTMENT",
                    productId = productId,
                    productName = product.name,
                    fromWarehouseId = if (variance < 0) warehouseId else null,
                    toWarehouseId = if (variance > 0) warehouseId else null,
                    quantity = Math.abs(variance),
                    unit = product.unit,
                    notes = "تسوية جردية: الدفتري ($systemCount) - الفعلي ($actualCount). السبب: $reason"
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "RECONCILIATION",
                    entityType = "INVENTORY",
                    entityId = productId,
                    details = "تسوية جردية للصنف ${product.name}: فارق $variance ${product.unit}"
                )
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. ADD/EDIT PARTY
    suspend fun saveParty(party: PartyEntity) = withContext(Dispatchers.IO) {
        dao.insertParty(party)
    }

    // 9. ADD/EDIT PRODUCT
    suspend fun saveProduct(product: ProductEntity, initialStock: Double = 0.0, defaultWarehouseId: String = "wh_main") = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
        if (initialStock > 0) {
            val existing = dao.getStock(product.id, defaultWarehouseId)
            if (existing == null) {
                dao.insertStockBalance(
                    StockBalanceEntity(
                        id = "${product.id}_$defaultWarehouseId",
                        productId = product.id,
                        warehouseId = defaultWarehouseId,
                        quantity = initialStock
                    )
                )
            }
        }
    }

    // 10. EXPENSE / REVENUE
    suspend fun recordExpenseRevenue(item: ExpenseRevenueEntity) = withContext(Dispatchers.IO) {
        dao.insertExpenseRevenue(item)
        if (item.type == "EXPENSE") {
            dao.adjustAccountBalance("acc_cash", -item.amount)
            dao.adjustAccountBalance("acc_expenses", item.amount)
        } else {
            dao.adjustAccountBalance("acc_cash", item.amount)
            dao.adjustAccountBalance("acc_sales", item.amount)
        }
    }

    // 11. RESCHEDULE APPOINTMENT
    suspend fun rescheduleAppointment(id: String, newDate: Long, reason: String) = withContext(Dispatchers.IO) {
        dao.rescheduleAppointment(id, newDate, reason)
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "RESCHEDULE",
                entityType = "APPOINTMENT",
                entityId = id,
                details = "تأجيل موعد سداد إلى ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(newDate))}. السبب: $reason"
            )
        )
    }

    // 12. WHATSAPP MESSAGE BUILDER
    fun buildDebtReminderWhatsAppMessage(
        companyName: String,
        customerName: String,
        amount: Double,
        dueDate: Long
    ): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(dueDate))
        return """
            الأخ الكريم / $customerName المحترم
            تحية طيبة وبعد،،
            
            نود تذكيركم بموعد استحقاق المبلغ المتبقي بذمتكم:
            المبلغ المستحق: ${String.format(Locale.US, "%,.0f", amount)} ريال
            تاريخ الاستحقاق: $dateStr
            
            شاكرين لكم حسن تعاونكم الدائم.
            صادر عن: $companyName
        """.trimIndent()
    }

    // 13. RESET TO DEMO DATA
    suspend fun resetDatabase(): Unit = withContext(Dispatchers.IO) {
        dao.clearInvoiceItems()
        dao.clearInvoices()
        dao.clearPaymentVouchers()
        dao.clearJournalLines()
        dao.clearJournalEntries()
        dao.clearAppointments()
        dao.clearStockMovements()
        dao.clearExpensesRevenues()
        dao.clearAuditLogs()
        AppDatabase.populateDatabase(dao)
    }
}
