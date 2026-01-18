package com.vorragun.yailek.ui.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vorragun.yailek.R

class SaleItemAdapter(private var items: List<SaleItem>) : RecyclerView.Adapter<SaleItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.item_product_image)
        val productName: TextView = view.findViewById(R.id.item_product_name)
        val quantity: TextView = view.findViewById(R.id.item_quantity)
        val totalPrice: TextView = view.findViewById(R.id.item_total_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sale_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.productName.text = item.productName
        holder.quantity.text = "x${item.quantity}"
        holder.totalPrice.text = "%.2f".format(item.price * item.quantity)

        if (item.imageResId != 0) {
            holder.productImage.setImageResource(item.imageResId)
        } else {
            // Optional: Set a placeholder image if no image is available
            holder.productImage.setImageResource(R.drawable.ic_launcher_foreground) 
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<SaleItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}