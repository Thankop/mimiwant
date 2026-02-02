package com.vorragun.yailek.ui.sales

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vorragun.yailek.R
import com.vorragun.yailek.databinding.BottomSheetSaleDetailBinding

class SaleDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSaleDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SalesViewModel
    private var saleId: Int = -1
    private var saleDate: String = ""
    private lateinit var saleItemAdapter: SaleItemAdapter

    // To hold the full SalesRecord object
    private var currentSaleRecord: SalesRecord? = null

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
        requireContext().deleteDatabase("products.db")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (saleId == -1) {
            dismiss()
            return
        }

        val factory = SalesViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), factory).get(SalesViewModel::class.java)

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        viewModel.loadSaleItems(saleId)
    }

    private fun observeViewModel() {
        viewModel.saleItems.observe(viewLifecycleOwner) { items ->
            saleItemAdapter.updateData(items)
        }

        // Observe the full list to find our specific record
        viewModel.salesRecords.observe(viewLifecycleOwner) { sales ->
            currentSaleRecord = sales.find { it.id == saleId }
            displaySaleDetails()
        }
    }

    private fun displaySaleDetails() {
        currentSaleRecord?.let { record ->
            if (!record.paymentNote.isNullOrEmpty()) {
                binding.textPaymentNote.text = "โน้ต: ${record.paymentNote}"
                binding.textPaymentNote.isVisible = true
            } else {
                binding.textPaymentNote.isVisible = false
            }
        }
    }

    private fun setupButtons() {
        binding.btnDeleteSale.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnEditStatus.setOnClickListener {
            currentSaleRecord?.let { showEditStatusDialog(it) }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณแน่ใจหรือไม่ว่าต้องการลบบิลนี้?")
            .setNegativeButton("ลบ") { _, _ ->
                val resultBundle = bundleOf(ARG_SALE_ID to saleId, ARG_SALE_DATE to saleDate)
                setFragmentResult(DELETE_REQUEST_KEY, resultBundle)
                dismiss()
            }
            .setPositiveButton("ยกเลิก", null)
            .show()
    }

    private fun showEditStatusDialog(record: SalesRecord) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupStatus)
        val radioPaid = dialogView.findViewById<RadioButton>(R.id.radioPaid)
        val radioPending = dialogView.findViewById<RadioButton>(R.id.radioPending)
        val noteEditText = dialogView.findViewById<EditText>(R.id.edit_text_note)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Pre-fill dialog with existing data
        if (record.paymentStatus == "PENDING") {
            radioPending.isChecked = true
            noteEditText.setText(record.paymentNote)
            noteEditText.isVisible = true
        } else {
            radioPaid.isChecked = true
            noteEditText.isVisible = false
        }

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            noteEditText.isVisible = checkedId == R.id.radioPending
        }

        btnConfirm.setOnClickListener {
            val isPaid = radioGroup.checkedRadioButtonId == R.id.radioPaid
            val note = if (!isPaid) noteEditText.text.toString() else null
            viewModel.updateSalePaymentStatus(saleId, isPaid, note)
            dialog.dismiss()
            this@SaleDetailBottomSheet.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        saleItemAdapter = SaleItemAdapter(emptyList())
        binding.saleItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = saleItemAdapter
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
