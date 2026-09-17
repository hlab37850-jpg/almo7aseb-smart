package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseRevenueEntity
import com.example.data.entity.InvoiceItemEntity
import com.example.data.entity.PartyEntity
import com.example.data.entity.ProductEntity
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class ScreenDestination {
    object Home : ScreenDestination()
    object Sales : ScreenDestination()
    object Purchases : ScreenDestination()
    object CashBox : ScreenDestination()
    object Customers : ScreenDestination()
    object Suppliers : ScreenDestination()
    object Inventory : ScreenDestination()
    object AccountsJournal : ScreenDestination()
    object Expenses : ScreenDestination()
    object Appointments : ScreenDestination()
    object Reports : ScreenDestination()
    object Settings : ScreenDestination()
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AccountingRepository(db.appDao())

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Flows from repository
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val warehouses = repository.warehouses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productsWithStock = repository.productsWithStock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allInvoices = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val vouchers = repository.vouchers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val journalEntries = repository.journalEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val appointments = repository.appointments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockMovements = repository.stockMovements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expensesRevenues = repository.expensesRevenues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val companySettings = repository.companySettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanySettingsEntity())

    // Financial Overview Dashboard Computations
    val dashboardStats = combine(
        allInvoices,
        customers,
        suppliers,
        accounts
    ) { invoices, custList, suppList, accList ->
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySales = invoices
            .filter { it.type == "SALE" && it.createdAt >= startOfToday && it.status == "ACTIVE" }
            .sumOf { it.total }

        val totalReceivables = custList.sumOf { it.balance }
        val totalPayables = suppList.sumOf { it.balance }
        val cashBalance = accList.find { it.id == "acc_cash" }?.balance ?: 0.0
        val bankBalance = accList.find { it.id == "acc_bank" }?.balance ?: 0.0

        DashboardData(
            todaySales = todaySales,
            totalReceivables = totalReceivables,
            totalPayables = totalPayables,
            cashBalance = cashBalance,
            bankBalance = bankBalance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardData())

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun showMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }

    // CREATE SALE
    fun createSaleInvoice(
        partyId: String,
        partyName: String,
        warehouseId: String,
        paymentType: String,
        items: List<InvoiceItemEntity>,
        discount: Double,
        paidAmount: Double,
        dueDate: Long?,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.createSalesInvoice(
                partyId, partyName, warehouseId, paymentType, items, discount, paidAmount, dueDate, notes
            )
            if (res.isSuccess) {
                showMessage("تم حفظ فاتورة المبيعات رقم ${res.getOrNull()} بنجاح")
            } else {
                showMessage("فشل في حفظ الفاتورة: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // CREATE SALE RETURN
    fun createSaleReturn(
        originalInvoiceId: String?,
        partyId: String,
        partyName: String,
        warehouseId: String,
        items: List<InvoiceItemEntity>,
        refundFromCash: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.createSalesReturn(
                originalInvoiceId, partyId, partyName, warehouseId, items, refundFromCash, notes
            )
            if (res.isSuccess) {
                showMessage("تم تسجيل مرتجع المبيعات رقم ${res.getOrNull()} بنجاح")
            } else {
                showMessage("فشل في تسجيل المرتجع: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // CREATE PURCHASE
    fun createPurchaseInvoice(
        supplierId: String,
        supplierName: String,
        warehouseId: String,
        paymentType: String,
        items: List<InvoiceItemEntity>,
        discount: Double,
        paidAmount: Double,
        dueDate: Long?,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.createPurchaseInvoice(
                supplierId, supplierName, warehouseId, paymentType, items, discount, paidAmount, dueDate, notes
            )
            if (res.isSuccess) {
                showMessage("تم حفظ فاتورة المشتريات رقم ${res.getOrNull()} بنجاح")
            } else {
                showMessage("فشل في حفظ المشتريات: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // CREATE RECEIPT
    fun createReceipt(partyId: String?, partyName: String, amount: Double, invoiceId: String?, notes: String) {
        viewModelScope.launch {
            val res = repository.createReceiptVoucher(partyId, partyName, amount, invoiceId, notes)
            if (res.isSuccess) {
                showMessage("تم إصدار سند القبض رقم ${res.getOrNull()} بنجاح")
            } else {
                showMessage("فشل إصدار السند: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // CREATE PAYMENT
    fun createPayment(partyId: String?, partyName: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val res = repository.createPaymentVoucher(partyId, partyName, amount, notes)
            if (res.isSuccess) {
                showMessage("تم إصدار سند الصرف رقم ${res.getOrNull()} بنجاح")
            } else {
                showMessage("فشل إصدار السند: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // SAVE PARTY
    fun saveParty(party: PartyEntity) {
        viewModelScope.launch {
            repository.saveParty(party)
            showMessage("تم حفظ بيانات ${if (party.type == "CUSTOMER") "العميل" else "المورد"} بنجاح")
        }
    }

    // SAVE PRODUCT
    fun saveProduct(product: ProductEntity, initialStock: Double) {
        viewModelScope.launch {
            repository.saveProduct(product, initialStock)
            showMessage("تم حفظ الصنف ${product.name} بنجاح")
        }
    }

    // TRANSFER STOCK
    fun transferStock(productId: String, fromWh: String, toWh: String, qty: Double, notes: String) {
        viewModelScope.launch {
            val res = repository.transferStock(productId, fromWh, toWh, qty, notes)
            if (res.isSuccess) {
                showMessage("تم التحويل المخزني بنجاح")
            } else {
                showMessage("خطأ: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // RECONCILE STOCK
    fun reconcileStock(productId: String, wh: String, actualCount: Double, reason: String) {
        viewModelScope.launch {
            val res = repository.reconcileStock(productId, wh, actualCount, reason)
            if (res.isSuccess) {
                showMessage("تم حفظ التسوية الجردية بنجاح")
            } else {
                showMessage("خطأ: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // RECORD EXPENSE/REVENUE
    fun recordExpenseRevenue(item: ExpenseRevenueEntity) {
        viewModelScope.launch {
            repository.recordExpenseRevenue(item)
            showMessage("تم تسجيل ${if (item.type == "EXPENSE") "المصروف" else "الإيراد"} بنجاح")
        }
    }

    // RESCHEDULE APPOINTMENT
    fun rescheduleAppointment(id: String, newDate: Long, reason: String) {
        viewModelScope.launch {
            repository.rescheduleAppointment(id, newDate, reason)
            showMessage("تم تمديد موعد السداد وتحديث السجل")
        }
    }

    // RESET TO DEMO
    fun resetToDemo() {
        viewModelScope.launch {
            repository.resetDatabase()
            showMessage("تم استعادة البيانات التجريبية الشاملة بنجاح")
        }
    }
}

data class DashboardData(
    val todaySales: Double = 0.0,
    val totalReceivables: Double = 0.0,
    val totalPayables: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0
)
