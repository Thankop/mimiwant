package com.vorragun.yailek.ui.sales

data class SaleItem(
    val id: Int = 0,
    val saleId: Int = 0,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val imageResId: Int
)



