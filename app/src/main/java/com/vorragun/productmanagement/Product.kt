package com.vorragun.productmanagement

import androidx.annotation.DrawableRes

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    @DrawableRes val imageResId: Int // Changed from imagePath: String
)
