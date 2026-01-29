package com.vorragun.yailek.ui.sales

data class SalesRecord(
    val id: Int,
    val date: String,
    val time: String,
    val totalAmount: Double,
    val itemCount: Int,
    val dailyNumber: Int,
    val paymentStatus: String // "PAID" หรือ "PENDING"
)

