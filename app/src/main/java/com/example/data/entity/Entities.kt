package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val type: String, // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    val currency: String = "YER",
    val balance: Double = 0.0
)

@Entity(
    tableName = "parties",
    indices = [Index(value = ["type"])]
)
data class PartyEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val type: String, // CUSTOMER, SUPPLIER
    val phone: String,
    val address: String = "",
    val creditLimit: Double = 0.0,
    val currency: String = "YER",
    val balance: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["code"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val unit: String = "حبة",
    val category: String = "عام",
    val costPrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val minLimit: Double = 5.0,
    val notes: String = ""
)

@Entity(
    tableName = "stock_balances",
    indices = [Index(value = ["productId", "warehouseId"], unique = true)]
)
data class StockBalanceEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val warehouseId: String,
    val quantity: Double = 0.0
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["type"]),
        Index(value = ["partyId"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val partyId: String,
    val partyName: String,
    val warehouseId: String,
    val type: String, // SALE, PURCHASE, SALE_RETURN, PURCHASE_RETURN
    val paymentType: String, // CASH, CREDIT, PARTIAL
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val dueDate: Long? = null,
    val currency: String = "YER",
    val notes: String = "",
    val status: String = "ACTIVE", // ACTIVE, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "admin",
    val originalInvoiceId: String? = null
)

@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(
    tableName = "payment_vouchers",
    indices = [Index(value = ["type"]), Index(value = ["partyId"])]
)
data class PaymentVoucherEntity(
    @PrimaryKey val id: String,
    val voucherNumber: String,
    val type: String, // RECEIPT (سند قبض), PAYMENT (سند صرف)
    val partyId: String?,
    val partyName: String,
    val accountId: String,
    val amount: Double,
    val currency: String = "YER",
    val invoiceId: String? = null,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val userId: String = "admin"
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val entryNumber: String,
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    indices = [Index(value = ["entryId"]), Index(value = ["accountId"])]
)
data class JournalLineEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val accountId: String,
    val accountName: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val currency: String = "YER",
    val notes: String = ""
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"])]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val movementNumber: String,
    val type: String, // INBOUND, OUTBOUND, TRANSFER, ADJUSTMENT
    val productId: String,
    val productName: String,
    val fromWarehouseId: String? = null,
    val toWarehouseId: String? = null,
    val quantity: Double,
    val unit: String,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "appointments",
    indices = [Index(value = ["partyId"]), Index(value = ["status"])]
)
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val partyName: String,
    val phone: String = "",
    val amount: Double,
    val dueDate: Long,
    val originalDueDate: Long = dueDate,
    val rescheduleReason: String? = null,
    val status: String = "PENDING", // PENDING, PAID, POSTPONED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses_revenues")
data class ExpenseRevenueEntity(
    @PrimaryKey val id: String,
    val type: String, // EXPENSE, REVENUE
    val category: String,
    val amount: Double,
    val currency: String = "YER",
    val date: Long = System.currentTimeMillis(),
    val accountId: String,
    val notes: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val userId: String = "admin",
    val userName: String = "مدير النظام",
    val action: String, // CREATE, UPDATE, RETURN, CANCEL
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_settings")
data class CompanySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "محلات العالمية للتجارة والمقاولات",
    val companyPhone: String = "777000123",
    val companyAddress: String = "صنعاء - شارع الستين - المركز التجاري",
    val baseCurrency: String = "YER",
    val vatPercentage: Double = 0.0,
    val invoiceFooter: String = "شكراً لتعاملكم معنا - البضاعة المباعة ترد وتستبدل خلال ٣ أيام"
)
