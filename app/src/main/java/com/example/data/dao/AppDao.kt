package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

data class StockProductItem(
    val productId: String,
    val productCode: String,
    val productName: String,
    val unit: String,
    val category: String,
    val costPrice: Double,
    val salePrice: Double,
    val minLimit: Double,
    val totalStock: Double
)

@Dao
interface AppDao {
    // 1. ACCOUNTS
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: String, amount: Double)

    // 2. PARTIES (Customers & Suppliers)
    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun getAllParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE type = :type ORDER BY name ASC")
    fun getPartiesByType(type: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyById(id: String): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParties(parties: List<PartyEntity>)

    @Query("UPDATE parties SET balance = balance + :amount WHERE id = :partyId")
    suspend fun adjustPartyBalance(partyId: String, amount: Double)

    @Query("SELECT * FROM parties WHERE type = :type AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')")
    fun searchParties(type: String, query: String): Flow<List<PartyEntity>>

    // 3. WAREHOUSES
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<WarehouseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: WarehouseEntity)

    // 4. PRODUCTS
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // 5. STOCK BALANCES
    @Query("SELECT * FROM stock_balances")
    fun getAllStockBalances(): Flow<List<StockBalanceEntity>>

    @Query("SELECT * FROM stock_balances WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun getStock(productId: String, warehouseId: String): StockBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockBalance(stockBalance: StockBalanceEntity)

    @Query("""
        SELECT p.id AS productId, p.code AS productCode, p.name AS productName,
               p.unit AS unit, p.category AS category, p.costPrice AS costPrice,
               p.salePrice AS salePrice, p.minLimit AS minLimit,
               COALESCE(SUM(sb.quantity), 0.0) AS totalStock
        FROM products p
        LEFT JOIN stock_balances sb ON p.id = sb.productId
        GROUP BY p.id
        ORDER BY p.name ASC
    """)
    fun getProductsWithStock(): Flow<List<StockProductItem>>

    // 6. INVOICES
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY createdAt DESC")
    fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE partyId = :partyId ORDER BY createdAt DESC")
    fun getInvoicesByParty(partyId: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET paidAmount = paidAmount + :amount, remainingAmount = remainingAmount - :amount WHERE id = :invoiceId")
    suspend fun applyPaymentToInvoice(invoiceId: String, amount: Double)

    @Query("UPDATE invoices SET status = :status WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: String, status: String)

    // 7. INVOICE ITEMS
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItems(invoiceId: String): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItemsSync(invoiceId: String): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    // 8. PAYMENT VOUCHERS
    @Query("SELECT * FROM payment_vouchers ORDER BY date DESC")
    fun getAllVouchers(): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_vouchers WHERE type = :type ORDER BY date DESC")
    fun getVouchersByType(type: String): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_vouchers WHERE partyId = :partyId ORDER BY date DESC")
    fun getVouchersByParty(partyId: String): Flow<List<PaymentVoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: PaymentVoucherEntity)

    // 9. JOURNAL ENTRIES & LINES
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_lines WHERE entryId = :entryId")
    fun getJournalLines(entryId: String): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM journal_lines ORDER BY entryId DESC")
    fun getAllJournalLines(): Flow<List<JournalLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    // 10. STOCK MOVEMENTS
    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    // 11. APPOINTMENTS & REMINDERS
    @Query("SELECT * FROM appointments ORDER BY dueDate ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE status = 'PENDING' ORDER BY dueDate ASC")
    fun getPendingAppointments(): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateAppointmentStatus(id: String, status: String)

    @Query("UPDATE appointments SET dueDate = :newDate, status = 'POSTPONED', rescheduleReason = :reason WHERE id = :id")
    suspend fun rescheduleAppointment(id: String, newDate: Long, reason: String)

    // 12. EXPENSES & REVENUES
    @Query("SELECT * FROM expenses_revenues ORDER BY date DESC")
    fun getAllExpensesRevenues(): Flow<List<ExpenseRevenueEntity>>

    @Query("SELECT * FROM expenses_revenues WHERE type = :type ORDER BY date DESC")
    fun getExpensesRevenuesByType(type: String): Flow<List<ExpenseRevenueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseRevenue(item: ExpenseRevenueEntity)

    // 13. AUDIT LOGS
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // 14. COMPANY SETTINGS
    @Query("SELECT * FROM company_settings WHERE id = 1")
    fun getCompanySettings(): Flow<CompanySettingsEntity?>

    @Query("SELECT * FROM company_settings WHERE id = 1")
    suspend fun getCompanySettingsSync(): CompanySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanySettings(settings: CompanySettingsEntity)

    // CLEAR ALL FOR RESET/DEMO
    @Query("DELETE FROM invoice_items")
    suspend fun clearInvoiceItems()

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()

    @Query("DELETE FROM payment_vouchers")
    suspend fun clearPaymentVouchers()

    @Query("DELETE FROM journal_lines")
    suspend fun clearJournalLines()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("DELETE FROM appointments")
    suspend fun clearAppointments()

    @Query("DELETE FROM stock_movements")
    suspend fun clearStockMovements()

    @Query("DELETE FROM expenses_revenues")
    suspend fun clearExpensesRevenues()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}
