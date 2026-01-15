package com.vorragun.productmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vorragun.yailek.R

class ProductAdapter(private var productList: List<Product>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = productList[position]

        holder.productNameTextView.text = currentItem.name
        holder.productPriceTextView.text = "฿%.2f".format(currentItem.price)
        holder.productQuantityTextView.text = currentItem.quantity.toString()

        // TODO: Load image using a library like Glide or Coil
        // Glide.with(holder.itemView.context).load(currentItem.imagePath).into(holder.productImageView)
    }

    override fun getItemCount() = productList.size

    fun updateProducts(newProductList: List<Product>) {
        productList = newProductList
        notifyDataSetChanged()
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        val productQuantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
    }
}