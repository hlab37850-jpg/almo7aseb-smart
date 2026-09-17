package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        PartyEntity::class,
        WarehouseEntity::class,
        ProductEntity::class,
        StockBalanceEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentVoucherEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        StockMovementEntity::class,
        AppointmentEntity::class,
        ExpenseRevenueEntity::class,
        AuditLogEntity::class,
        CompanySettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_accountant.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDatabase(database.appDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            // 1. Chart of Accounts
            val standardAccounts = listOf(
                AccountEntity("acc_cash", "101", "الصندوق الرئيسي", "ASSET", balance = 350000.0),
                AccountEntity("acc_bank", "102", "بنك التضامن الإسلامي", "ASSET", balance = 800000.0),
                AccountEntity("acc_ar", "103", "العملاء (المدينون)", "ASSET", balance = 390000.0),
                AccountEntity("acc_inv", "104", "مخزون البضاعة", "ASSET", balance = 1250000.0),
                AccountEntity("acc_ap", "201", "الموردون (الدائنون)", "LIABILITY", balance = 450000.0),
                AccountEntity("acc_capital", "301", "رأس المال", "EQUITY", balance = 2340000.0),
                AccountEntity("acc_sales", "401", "إيراد المبيعات", "REVENUE", balance = 0.0),
                AccountEntity("acc_cogs", "501", "تكلفة البضاعة المباعة", "EXPENSE", balance = 0.0),
                AccountEntity("acc_purchases", "502", "المشتريات", "EXPENSE", balance = 0.0),
                AccountEntity("acc_expenses", "503", "المصروفات العمومية والإدارية", "EXPENSE", balance = 0.0),
                AccountEntity("acc_rent", "504", "مصروف الإيجار", "EXPENSE", balance = 0.0),
                AccountEntity("acc_salaries", "505", "مصروف الرواتب والأجور", "EXPENSE", balance = 0.0)
            )
            dao.insertAccounts(standardAccounts)

            // 2. Default Warehouse
            val mainWarehouse = WarehouseEntity(
                id = "wh_main",
                name = "المستودع الرئيسي",
                location = "صنعاء - المركز الرئيسي"
            )
            val subWarehouse = WarehouseEntity(
                id = "wh_branch",
                name = "مستودع فرع الستين",
                location = "صنعاء - فرع الستين"
            )
            dao.insertWarehouse(mainWarehouse)
            dao.insertWarehouse(subWarehouse)

            // 3. Sample Products (Notice names with numbers like "50 لتر" - quantity must NOT be extracted from name!)
            val sampleProducts = listOf(
                ProductEntity(
                    id = "prod_pipe_50",
                    code = "PRD-101",
                    name = "بوالد 50 لتر بلاستيك مقوى",
                    category = "مواد بناء",
                    unit = "حبة",
                    costPrice = 4500.0,
                    salePrice = 6000.0,
                    minLimit = 10.0
                ),
                ProductEntity(
                    id = "prod_cement",
                    code = "PRD-102",
                    name = "أسمنت عمران كيس 50 كجم",
                    category = "مواد بناء",
                    unit = "كيس",
                    costPrice = 3800.0,
                    salePrice = 4400.0,
                    minLimit = 25.0
                ),
                ProductEntity(
                    id = "prod_paint_1",
                    code = "PRD-103",
                    name = "طلاء جوتن فاخر 1 هـ يمني",
                    category = "دهانات",
                    unit = "سطل",
                    costPrice = 12000.0,
                    salePrice = 15500.0,
                    minLimit = 5.0
                ),
                ProductEntity(
                    id = "prod_cable_6",
                    code = "PRD-104",
                    name = "كيبل كهربائي نحاس 6 ملم",
                    category = "كهربائيات",
                    unit = "لفة",
                    costPrice = 28000.0,
                    salePrice = 34000.0,
                    minLimit = 8.0
                ),
                ProductEntity(
                    id = "prod_valve_1",
                    code = "PRD-105",
                    name = "محبس نحاس إيطالي 1 بوصة",
                    category = "سباكة",
                    unit = "حبة",
                    costPrice = 2200.0,
                    salePrice = 3000.0,
                    minLimit = 15.0
                )
            )
            dao.insertProducts(sampleProducts)

            // Stock balances
            dao.insertStockBalance(StockBalanceEntity(UUID.randomUUID().toString(), "prod_pipe_50", "wh_main", 40.0))
            dao.insertStockBalance(StockBalanceEntity(UUID.randomUUID().toString(), "prod_cement", "wh_main", 150.0))
            dao.insertStockBalance(StockBalanceEntity(UUID.randomUUID().toString(), "prod_paint_1", "wh_main", 18.0))
            dao.insertStockBalance(StockBalanceEntity(UUID.randomUUID().toString(), "prod_cable_6", "wh_main", 12.0))
            dao.insertStockBalance(StockBalanceEntity(UUID.randomUUID().toString(), "prod_valve_1", "wh_main", 65.0))

            // 4. Sample Customers
            val c1 = PartyEntity(
                id = "cust_1",
                accountId = "acc_ar",
                name = "شركة الأمل للمقاولات العامة",
                type = "CUSTOMER",
                phone = "777123456",
                address = "صنعاء - حي الأصبحي",
                creditLimit = 1000000.0,
                balance = 240000.0,
                notes = "عميل مميز - سداد شهري"
            )
            val c2 = PartyEntity(
                id = "cust_2",
                accountId = "acc_ar",
                name = "المهندس وليد الصلاحي",
                type = "CUSTOMER",
                phone = "771987654",
                address = "صنعاء - حدة",
                creditLimit = 500000.0,
                balance = 150000.0,
                notes = "مشروع فيلا سكنية"
            )
            val c3 = PartyEntity(
                id = "cust_3",
                accountId = "acc_ar",
                name = "مؤسسة النور للإعمار",
                type = "CUSTOMER",
                phone = "773456789",
                address = "صنعاء - بيت بوس",
                creditLimit = 800000.0,
                balance = 0.0,
                notes = "حساب مسدد بالكامل"
            )
            dao.insertParties(listOf(c1, c2, c3))

            // 5. Sample Suppliers
            val s1 = PartyEntity(
                id = "supp_1",
                accountId = "acc_ap",
                name = "مؤسسة البركة للاستيراد ومواد البناء",
                type = "SUPPLIER",
                phone = "775555111",
                address = "الحديدة - شارع الميناء",
                balance = 300000.0,
                notes = "مورد رئيسي للأسمنت والحديد"
            )
            val s2 = PartyEntity(
                id = "supp_2",
                accountId = "acc_ap",
                name = "شركة الشرق للأدوات الصحية والكهربائية",
                type = "SUPPLIER",
                phone = "772222333",
                address = "صنعاء - شعوب",
                balance = 150000.0,
                notes = "توريد أدوات سباكة وكهرباء"
            )
            dao.insertParties(listOf(s1, s2))

            // 6. Sample Initial Invoices & Debts
            val sampleInvoiceId = UUID.randomUUID().toString()
            val sampleInvoice = InvoiceEntity(
                id = sampleInvoiceId,
                invoiceNumber = "INV-1001",
                partyId = "cust_1",
                partyName = "شركة الأمل للمقاولات العامة",
                warehouseId = "wh_main",
                type = "SALE",
                paymentType = "PARTIAL",
                subtotal = 340000.0,
                discount = 10000.0,
                tax = 0.0,
                total = 330000.0,
                paidAmount = 90000.0,
                remainingAmount = 240000.0,
                dueDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
                notes = "فاتورة مواد بناء مرحلة التأسيس",
                createdAt = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)
            )
            dao.insertInvoice(sampleInvoice)

            dao.insertInvoiceItems(listOf(
                InvoiceItemEntity(
                    id = UUID.randomUUID().toString(),
                    invoiceId = sampleInvoiceId,
                    productId = "prod_cement",
                    productName = "أسمنت عمران كيس 50 كجم",
                    unit = "كيس",
                    quantity = 50.0,
                    unitPrice = 4400.0,
                    totalPrice = 220000.0
                ),
                InvoiceItemEntity(
                    id = UUID.randomUUID().toString(),
                    invoiceId = sampleInvoiceId,
                    productId = "prod_pipe_50",
                    productName = "بوالد 50 لتر بلاستيك مقوى",
                    unit = "حبة",
                    quantity = 20.0,
                    unitPrice = 6000.0,
                    totalPrice = 120000.0
                )
            ))

            // Appointments / Reminders for Debt
            val appt = AppointmentEntity(
                id = UUID.randomUUID().toString(),
                partyId = "cust_1",
                partyName = "شركة الأمل للمقاولات العامة",
                phone = "777123456",
                amount = 240000.0,
                dueDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000),
                notes = "استحقاق فاتورة INV-1001"
            )
            dao.insertAppointment(appt)

            // Company Settings
            dao.insertCompanySettings(
                CompanySettingsEntity(
                    id = 1,
                    companyName = "المحاسب الذكي - محلات العالمية للتجارة",
                    companyPhone = "777000123",
                    companyAddress = "صنعاء - شارع الستين - المركز التجاري",
                    baseCurrency = "YER"
                )
            )

            // Audit log
            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "INITIALIZE",
                    entityType = "SYSTEM",
                    entityId = "SETUP",
                    details = "تم تهيئة النظام المحاسبي وإعداد دليل الحسابات والمستودع الأولي بنجاح"
                )
            )
        }
    }
}
