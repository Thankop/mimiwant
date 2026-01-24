package com.vorragun.productmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.vorragun.yailek.R

class ProductAdapter(private var productList: List<Product>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var isInSelectionMode = false
    private val selectedQuantities = mutableMapOf<Int, Int>()

    fun setSelectionMode(enabled: Boolean) {
        isInSelectionMode = enabled
        if (!enabled) {
            selectedQuantities.clear()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = productList[position]
        val productId = currentItem.id

        holder.productNameTextView.text = currentItem.name
        holder.productPriceTextView.text = "฿%.2f".format(currentItem.price)
        holder.productImageView.setImageResource(currentItem.imageResId)

        if (isInSelectionMode) {
            holder.quantityControls.visibility = View.VISIBLE
            val quantity = selectedQuantities.getOrDefault(productId, 0)
            holder.quantityTextView.text = quantity.toString()

            holder.buttonPlus.setOnClickListener {
                val newQuantity = selectedQuantities.getOrDefault(productId, 0) + 1
                selectedQuantities[productId] = newQuantity
                holder.quantityTextView.text = newQuantity.toString()
            }

            holder.buttonMinus.setOnClickListener {
                var newQuantity = selectedQuantities.getOrDefault(productId, 0) - 1
                if (newQuantity < 0) newQuantity = 0
                selectedQuantities[productId] = newQuantity
                holder.quantityTextView.text = newQuantity.toString()
            }
        } else {
            holder.quantityControls.visibility = View.GONE
        }
    }

    override fun getItemCount() = productList.size

    fun updateProducts(newProductList: List<Product>) {
        productList = newProductList
        notifyDataSetChanged()
    }

    fun getSelectedProducts(): Map<Int, Int> {
        return selectedQuantities.filter { it.value > 0 }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        val productImageView: ImageView = itemView.findViewById(R.id.productImageView)

        val quantityControls: LinearLayout = itemView.findViewById(R.id.quantity_controls)
        val buttonMinus: MaterialButton = itemView.findViewById(R.id.button_minus)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantity_text_view)
        val buttonPlus: MaterialButton = itemView.findViewById(R.id.button_plus)
    }
}