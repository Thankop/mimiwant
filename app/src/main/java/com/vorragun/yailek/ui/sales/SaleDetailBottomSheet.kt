package com.vorragun.yailek.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vorragun.yailek.databinding.BottomSheetSaleDetailBinding

class SaleDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSaleDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SalesViewModel
    private var saleId: Int = -1
    private var saleDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            saleId = it.getInt(ARG_SALE_ID)
            saleDate = it.getString(ARG_SALE_DATE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSaleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the ViewModel scoped to the Activity
        val factory = SalesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), factory).get(SalesViewModel::class.java)

        // Setup RecyclerView
        val saleItems = viewModel.getSaleItems(saleId)
        val adapter = SaleItemAdapter(saleItems)
        binding.saleItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.saleItemsRecyclerView.adapter = adapter

        // When delete is clicked, send a result back to the parent fragment
        binding.btnDeleteSale.setOnClickListener {
            val resultBundle = bundleOf(
                ARG_SALE_ID to saleId,
                ARG_SALE_DATE to saleDate
            )
            setFragmentResult(DELETE_REQUEST_KEY, resultBundle)
            dismiss() // Close the bottom sheet
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SaleDetailBottomSheet"
        const val DELETE_REQUEST_KEY = "delete_request"
        private const val ARG_SALE_ID = "sale_id"
        private const val ARG_SALE_DATE = "sale_date"

        fun newInstance(saleId: Int, saleDate: String): SaleDetailBottomSheet {
            return SaleDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SALE_ID, saleId)
                    putString(ARG_SALE_DATE, saleDate)
                }
            }
        }
    }
}