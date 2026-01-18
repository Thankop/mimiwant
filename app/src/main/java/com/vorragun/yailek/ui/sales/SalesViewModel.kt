package com.vorragun.yailek.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vorragun.productmanagement.ProductDbHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ProductDbHelper(application)

    private val _salesRecords = MutableLiveData<List<SalesRecord>>()
    val salesRecords: LiveData<List<SalesRecord>> = _salesRecords

    private val _dailyTotal = MutableLiveData<Double>()
    val dailyTotal: LiveData<Double> = _dailyTotal

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private var lastSavedToken: String? = null

    init {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        loadSalesForDate(today)
    }

    fun loadSalesForDate(date: String) {
        _selectedDate.value = date

        // 🔥 สำคัญมาก รีเซ็ต token ทุกครั้งที่เปลี่ยนวัน
        lastSavedToken = null

        val records = dbHelper.getSalesForDate(date)
        _salesRecords.value = records
        _dailyTotal.value = records.sumOf { it.totalAmount }
    }

    fun saveSaleIfNeeded(
        saleToken: String,
        totalAmount: Double,
        totalItems: Int,
        items: List<SaleItem>
    ) {
        if (saleToken == lastSavedToken || totalAmount <= 0) return

        val dateToSave = _selectedDate.value ?: return

        // ❌ ไม่ต้องนับเลขบิลที่นี่
        // ✅ DB จะคำนวณ daily_number จาก sale_date ให้เอง
        val record = SalesRecord(
            id = 0,
            date = dateToSave,
            totalAmount = totalAmount,
            itemCount = totalItems,
            dailyNumber = 0
        )

        val saleId = dbHelper.addSaleRecordAndReturnId(record)
        dbHelper.addSaleItems(saleId, items)

        lastSavedToken = saleToken
        loadSalesForDate(dateToSave)
    }

    fun getSaleItems(saleId: Int): List<SaleItem> {
        return dbHelper.getSaleItems(saleId)
    }

    fun deleteSale(saleId: Int, saleDate: String) {
        dbHelper.deleteSale(saleId)
        loadSalesForDate(saleDate)
    }
}
