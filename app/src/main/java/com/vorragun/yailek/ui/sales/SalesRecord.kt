package com.vorragun.yailek.ui.sales

data class SalesRecord(
    val id: Int,
    val dailyNumber: Int, // ⭐ เลขบิลประจำวัน
    val date: String,
    val time: String,
    val totalAmount: Double,
    val itemCount: Int
)
