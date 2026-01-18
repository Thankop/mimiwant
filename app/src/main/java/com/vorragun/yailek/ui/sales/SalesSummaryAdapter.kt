package com.vorragun.yailek.ui.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vorragun.yailek.R

class SalesSummaryAdapter(
    private var salesRecords: List<SalesRecord>,
    private val onItemClick: (SalesRecord) -> Unit
) : RecyclerView.Adapter<SalesSummaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val billNumberTextView: TextView = view.findViewById(R.id.item_id)
        val dateTextView: TextView = view.findViewById(R.id.item_date)
        val amountTextView: TextView = view.findViewById(R.id.item_amount)
        val countTextView: TextView = view.findViewById(R.id.item_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sales_summary_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = salesRecords[position]

        // ⭐ ใช้เลขบิลรายวัน ไม่ใช่ id
        holder.billNumberTextView.text = "${record.dailyNumber}"

        holder.dateTextView.text = record.date
        holder.amountTextView.text = String.format("%.2f", record.totalAmount)
        holder.countTextView.text = "${record.itemCount} ชิ้น"

        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount(): Int = salesRecords.size

    fun updateData(newRecords: List<SalesRecord>) {
        salesRecords = newRecords
        notifyDataSetChanged()
    }
}
