package com.vorragun.yailek.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.vorragun.yailek.databinding.FragmentSalesSummaryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesSummaryFragment : Fragment() {

    private var _binding: FragmentSalesSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SalesViewModel
    private lateinit var summaryAdapter: SalesSummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Listen for the delete request from the bottom sheet
        parentFragmentManager.setFragmentResultListener(SaleDetailBottomSheet.DELETE_REQUEST_KEY, this) { _, bundle ->
            val saleId = bundle.getInt("sale_id")
            val saleDate = bundle.getString("sale_date", "")
            if (saleId != -1) {
                viewModel.deleteSale(saleId, saleDate)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SalesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), factory).get(SalesViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupUI()
    }

    private fun setupRecyclerView() {
        summaryAdapter = SalesSummaryAdapter(emptyList()) { saleRecord ->
            val bottomSheet = SaleDetailBottomSheet.newInstance(saleRecord.id, saleRecord.date)
            bottomSheet.show(parentFragmentManager, SaleDetailBottomSheet.TAG)
        }
        binding.salesSummaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = summaryAdapter
        }
    }

    private fun setupObservers() {
        viewModel.salesRecords.observe(viewLifecycleOwner) {
            summaryAdapter.updateData(it)
        }

        viewModel.dailyTotal.observe(viewLifecycleOwner) { total ->
            binding.textDailyTotal.text = String.format(Locale.getDefault(), "ยอดรวม: %.2f", total)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            binding.btnSelectDate.text = if (date == today) "วันนี้" else date
        }
    }

    private fun setupUI() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("เลือกวันที่")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selection))
            viewModel.loadSalesForDate(selectedDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}