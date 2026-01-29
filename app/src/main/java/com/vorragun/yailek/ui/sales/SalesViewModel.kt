package com.vorragun.yailek.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vorragun.productmanagement.ProductDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ProductDbHelper(application)

    // Holds the currently displayed records (can be filtered or unfiltered)
    private val _salesRecords = MutableLiveData<List<SalesRecord>>()
    val salesRecords: LiveData<List<SalesRecord>> = _salesRecords

    // Holds ALL sales records for the selected date, unfiltered.
    private var _allSalesForDay = listOf<SalesRecord>()

    private val _dailyTotal = MutableLiveData<Double>()
    val dailyTotal: LiveData<Double> = _dailyTotal

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _saleItems = MutableLiveData<List<SaleItem>>()
    val saleItems: LiveData<List<SaleItem>> = _saleItems

    private val _pendingSaleData = MutableLiveData<Pair<SalesRecord, List<SaleItem>>?>()
    val pendingSaleData: LiveData<Pair<SalesRecord, List<SaleItem>>?> = _pendingSaleData

    private val _isFilterPendingOnly = MutableLiveData(false)
    val isFilterPendingOnly: LiveData<Boolean> = _isFilterPendingOnly

    private var lastSavedToken: String? = null

    init {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        loadSalesForDate(today)
    }

    fun loadSalesForDate(date: String) {
        _selectedDate.postValue(date)

        viewModelScope.launch(Dispatchers.IO) {
            // THE FIX IS HERE:
            // Step 1: Fetch all records and store in the master list.
            _allSalesForDay = dbHelper.getSalesForDate(date)

            // Step 2: Calculate total based on the complete, unfiltered list.
            val totalPaid = _allSalesForDay
                .filter { it.paymentStatus == "PAID" }
                .sumOf { it.totalAmount }
            _dailyTotal.postValue(totalPaid)

            // Step 3: Apply the current filter and post the visible list to the UI.
            applyFilter()
        }
    }

    fun setFilterPendingOnly(isFiltered: Boolean) {
        if (_isFilterPendingOnly.value != isFiltered) {
            _isFilterPendingOnly.value = isFiltered
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filteredList = if (_isFilterPendingOnly.value == true) {
            _allSalesForDay.filter { it.paymentStatus == "PENDING" }
        } else {
            _allSalesForDay
        }
        _salesRecords.postValue(filteredList)
    }

    fun saveSaleIfNeeded(
        saleToken: String,
        totalAmount: Double,
        totalItems: Int,
        items: List<SaleItem>
    ) {
        if (saleToken == lastSavedToken || totalAmount <= 0) return
        val date = _selectedDate.value ?: return

        val record = SalesRecord(
            id = 0, date = date, time = "",
            totalAmount = totalAmount, itemCount = totalItems, dailyNumber = 0,
            paymentStatus = ""
        )

        lastSavedToken = saleToken
        _pendingSaleData.value = record to items
    }

    fun finalizeSaleWithStatus(paymentStatus: String, note: String?) {
        val pending = _pendingSaleData.value ?: return
        val record = pending.first.copy(paymentStatus = paymentStatus, paymentNote = note)
        val items = pending.second

        viewModelScope.launch(Dispatchers.IO) {
            val saleId = dbHelper.addSaleRecordAndReturnId(record)
            dbHelper.addSaleItems(saleId, items)
            // Reload all data for the date to reflect the new sale
            loadSalesForDate(record.date)
        }

        _pendingSaleData.value = null
    }

    fun salePendingDialogCancelled() {
        _pendingSaleData.value = null
        lastSavedToken = null
    }

    fun loadSaleItems(saleId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _saleItems.postValue(dbHelper.getSaleItems(saleId))
        }
    }

    fun updateSalePaymentStatus(saleId: Int, isPaid: Boolean, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateSalePaymentStatus(saleId, isPaid, note)
            _selectedDate.value?.let { loadSalesForDate(it) }
        }
    }

    fun deleteSale(saleId: Int, saleDate: String) {
        // Launch as a coroutine for consistency
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteSale(saleId)
            loadSalesForDate(saleDate)
        }
    }
}
