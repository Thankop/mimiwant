package com.vorragun.yailek.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.vorragun.productmanagement.Product
import com.vorragun.productmanagement.ProductAdapter
import com.vorragun.productmanagement.ProductDbHelper
import com.vorragun.yailek.MainActivity
import com.vorragun.yailek.R
import com.vorragun.yailek.databinding.FragmentHomeBinding
import com.vorragun.yailek.ui.sales.SaleItem
import com.vorragun.yailek.ui.sales.SalesViewModel
import com.vorragun.yailek.ui.sales.SalesViewModelFactory
import java.util.UUID

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var dbHelper: ProductDbHelper

    // This list is for display and will be filtered
    private var displayedProducts = listOf<Product>()
    // This is the master list of all products, never filtered. For checkout logic.
    private var masterProductList = listOf<Product>()

    private lateinit var salesViewModel: SalesViewModel
    private var isInSelectionMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val factory = SalesViewModelFactory(requireActivity().application)
        salesViewModel = ViewModelProvider(requireActivity(), factory).get(SalesViewModel::class.java)

        dbHelper = ProductDbHelper(requireContext())
        // Always recreate the sample products for easier development
        addSampleProducts(dbHelper)

        // Populate both lists
        masterProductList = dbHelper.getAllProducts()
        displayedProducts = masterProductList

        productAdapter = ProductAdapter(displayedProducts)
        binding.productsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productsRecyclerView.adapter = productAdapter

        setupCategoryChips()
        setupObservers()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })

        binding.fab.setOnClickListener {
            if (isInSelectionMode) {
                processCheckout()
            } else {
                enterSelectionMode()
            }
        }

        return binding.root
    }

    private fun setupObservers() {
        salesViewModel.pendingSaleData.observe(viewLifecycleOwner) { pendingData ->
            pendingData?.let {
                showPaymentStatusDialog()
            }
        }
    }

    private fun showPaymentStatusDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupStatus)
        val radioPaid = dialogView.findViewById<RadioButton>(R.id.radioPaid)
        val radioPending = dialogView.findViewById<RadioButton>(R.id.radioPending)
        val noteEditText = dialogView.findViewById<EditText>(R.id.edit_text_note)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        radioPaid.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            noteEditText.isVisible = checkedId == R.id.radioPending
        }

        btnConfirm.setOnClickListener {
            val selectedStatus = if (radioPending.isChecked) "PENDING" else "PAID"
            val note = if (selectedStatus == "PENDING") noteEditText.text.toString() else null

            salesViewModel.finalizeSaleWithStatus(selectedStatus, note)
            dialog.dismiss()

            exitSelectionMode()
            (requireActivity() as MainActivity).selectSalesTab()
        }

        btnCancel.setOnClickListener {
            salesViewModel.salePendingDialogCancelled()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupCategoryChips() {
        binding.categoryChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedCategory = when (checkedId) {
                R.id.chip_all -> null
                R.id.chip_drinks -> "เครื่องดื่ม"
                R.id.chip_beer -> "เบียร์"
                R.id.chip_snacks -> "ขนม"
                R.id.chip_others -> "อื่นๆ"
                else -> null
            }
            loadProducts(selectedCategory)
        }
    }

    private fun loadProducts(category: String?) {
        displayedProducts = if (category == null) {
            masterProductList
        } else {
            masterProductList.filter { it.category == category }
        }
        productAdapter.updateProducts(displayedProducts)
    }

    private fun enterSelectionMode() {
        isInSelectionMode = true
        productAdapter.setSelectionMode(true)
        binding.fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_save))
    }

    private fun exitSelectionMode() {
        isInSelectionMode = false
        productAdapter.setSelectionMode(false)
        binding.fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_input_add))
    }

    private fun processCheckout() {
        val selectedProducts = productAdapter.getSelectedProducts()
        if (selectedProducts.isEmpty()) {
            exitSelectionMode()
            return
        }

        var totalAmount = 0.0
        var totalItems = 0
        val saleItems = mutableListOf<SaleItem>()

        for ((productId, quantity) in selectedProducts) {
            // THE FIX: Look in the master list, not the filtered display list.
            val product = masterProductList.find { it.id == productId }
            if (product != null) {
                totalAmount += product.price * quantity
                totalItems += quantity
                saleItems.add(
                    SaleItem(
                        productName = product.name,
                        quantity = quantity,
                        price = product.price,
                        imageResId = product.imageResId
                    )
                )
            }
        }

        val saleToken = UUID.randomUUID().toString()
        salesViewModel.saveSaleIfNeeded(
            saleToken,
            totalAmount,
            totalItems,
            saleItems
        )
    }

    private fun addSampleProducts(dbHelper: ProductDbHelper) {
        val products = listOf(
            Product(0, "เลย์", "", 20.0, 0, R.drawable.lay, "ขนม"),
            Product(0, "เลย์ รสบาร์บีคิว", "", 20.0, 0, R.drawable.laybbq, "ขนม"),
            Product(0, "เลย์ รสโนริสาหร่าย", "", 20.0, 0, R.drawable.laynoeri, "ขนม"),
            Product(0, "เลย์ รสหมึกย่าง", "", 20.0, 0, R.drawable.layopic, "ขนม"),
            Product(0, "โอริโอ", "", 10.0, 0, R.drawable.oreo, "ขนม"),
            Product(0, "โออิชิ", "", 20.0, 0, R.drawable.oshi, "เครื่องดื่ม"),
            Product(0, "เอลเซ", "", 10.0, 0, R.drawable.ellse, "ขนม"),
            Product(0, "ลีโอ", "", 62.0, 0, R.drawable.leo, "เบียร์"),
            Product(0, "เบียร์สิงห์", "", 65.0, 0, R.drawable.singhabeer, "เบียร์"),
            Product(0, "สิงห์กระป๋อง", "", 45.0, 0, R.drawable.singkapom, "เบียร์"),
            Product(0, "สโนวี่", "", 65.0, 0, R.drawable.snowy, "เบียร์"),
            Product(0, "น้ำแข็ง", "", 10.0, 0, R.drawable.ice, "อื่นๆ"),
            Product(0, "จูปาจุป", "", 5.0, 0, R.drawable.chupachups, "ขนม"),
            Product(0, "โชกี้", "", 5.0, 0, R.drawable.choki, "ขนม"),
            Product(0, "คอนเน่", "", 20.0, 0, R.drawable.conne, "ขนม"),
            Product(0, "มาชิตะ", "", 20.0, 0, R.drawable.mashita, "ขนม"),
            Product(0, "ลีโอกระป๋อง", "", 35.0, 0, R.drawable.leokapom, "เบียร์"),
            Product(0, "อิชิตัน", "", 20.0, 0, R.drawable.icitan, "เครื่องดื่ม"),
            Product(0, "อิชิตัน รสข้าวญี่ปุ่น", "", 20.0, 0, R.drawable.ichitan, "เครื่องดื่ม"),
            Product(0, "แป๊ปซี่กระป๋อง", "", 15.0, 0, R.drawable.pepsi, "เครื่องดื่ม"),
            Product(0, "โค้กกระป๋อง", "", 15.0, 0, R.drawable.cokecan, "เครื่องดื่ม"),
            Product(0, "น้ำสิงห์ ขวดเล็ก", "", 10.0, 0, R.drawable.watermini, "เครื่องดื่ม"),
            Product(0, "น้ำสิงห์ ขวดกลาง", "", 15.0, 0, R.drawable.watermid, "เครื่องดื่ม"),
            Product(0, "น้ำสิงห์ ขวดใหญ่", "", 20.0, 0, R.drawable.water, "เครื่องดื่ม")
        )

        dbHelper.recreateSampleProducts(products)
    }

    private fun filterProducts(query: String?) {
        val list = if (query.isNullOrEmpty()) {
            // When search is cleared, show products of the current category
            val selectedChipId = binding.categoryChipGroup.checkedChipId
            val selectedCategory = when (selectedChipId) {
                R.id.chip_all -> null
                R.id.chip_drinks -> "เครื่องดื่ม"
                R.id.chip_beer -> "เบียร์"
                R.id.chip_snacks -> "ขนม"
                R.id.chip_others -> "อื่นๆ"
                else -> null
            }
            if (selectedCategory == null) masterProductList else masterProductList.filter { it.category == selectedCategory }
        } else {
            displayedProducts.filter { it.name.contains(query, true) }
        }
        productAdapter.updateProducts(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
