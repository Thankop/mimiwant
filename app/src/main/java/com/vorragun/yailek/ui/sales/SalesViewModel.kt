package com.vorragun.yailek.ui.sales

import android.app.Application
import androidx.lifecycle.*
import com.vorragun.productmanagement.ProductDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ProductDbHelper(application)

    private val _salesRecords = MutableLiveData<List<SalesRecord>>()
    val salesRecords: LiveData<List<SalesRecord>> = _salesRecords

    private val _dailyTotal = MutableLiveData<Double>()
    val dailyTotal: LiveData<Double> = _dailyTotal

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _saleItems = MutableLiveData<List<SaleItem>>()
    val saleItems: LiveData<List<SaleItem>> = _saleItems

    private val _pendingSaleData =
        MutableLiveData<Pair<SalesRecord, List<SaleItem>>?>()
    val pendingSaleData: LiveData<Pair<SalesRecord, List<SaleItem>>?> = _pendingSaleData

    private var lastSavedToken: String? = null

    init {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        loadSalesForDate(today)
    }

    // 🔥 ปลอดภัย 100%
    fun loadSalesForDate(date: String) {
        _selectedDate.postValue(date)
        lastSavedToken = null

        viewModelScope.launch(Dispatchers.IO) {
            val records = dbHelper.getSalesForDate(date)

            // ✅ รวมยอดเฉพาะที่จ่ายแล้ว
            val totalPaid = records
                .filter { it.paymentStatus == "PAID" }
                .sumOf { it.totalAmount }

            _salesRecords.postValue(records)
            _dailyTotal.postValue(totalPaid)
        }
    }

    // STEP 1: เตรียมข้อมูล → เปิด dialog
    fun saveSaleIfNeeded(
        saleToken: String,
        totalAmount: Double,
        totalItems: Int,
        items: List<SaleItem>
    ) {
        if (saleToken == lastSavedToken || totalAmount <= 0) return
        val date = _selectedDate.value ?: return

        val record = SalesRecord(
            id = 0,
            date = date,
            time = "",
            totalAmount = totalAmount,
            itemCount = totalItems,
            dailyNumber = 0,
            paymentStatus = ""
        )

        lastSavedToken = saleToken
        _pendingSaleData.postValue(record to items)
    }

    // STEP 2: กดยืนยันจาก dialog
    fun finalizeSaleWithStatus(paymentStatus: String) {
        val pending = _pendingSaleData.value ?: return
        val record = pending.first.copy(paymentStatus = paymentStatus)
        val items = pending.second

        viewModelScope.launch(Dispatchers.IO) {
            val saleId = dbHelper.addSaleRecordAndReturnId(record)
            dbHelper.addSaleItems(saleId, items)
            loadSalesForDate(record.date)
        }

        _pendingSaleData.postValue(null)
    }

    fun salePendingDialogCancelled() {
        _pendingSaleData.postValue(null)
        lastSavedToken = null
    }

    fun loadSaleItems(saleId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _saleItems.postValue(dbHelper.getSaleItems(saleId))
        }
    }

    fun updateSalePaymentStatus(saleId: Int, isPaid: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateSalePaymentStatus(saleId, isPaid)
            _selectedDate.value?.let { loadSalesForDate(it) }
        }
    }

    fun deleteSale(saleId: Int, saleDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteSale(saleId)
            loadSalesForDate(saleDate)
        }
    }
}
