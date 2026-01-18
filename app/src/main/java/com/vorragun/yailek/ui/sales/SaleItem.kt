package com.vorragun.yailek.ui.sales

import androidx.annotation.DrawableRes

data class SaleItem(
    val id: Int = 0,
    val saleId: Int = 0,
    val productName: String,
    val quantity: Int,
    val price: Double,
    @DrawableRes val imageResId: Int = 0
)
