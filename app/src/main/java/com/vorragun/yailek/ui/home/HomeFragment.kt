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
    private var allProducts = listOf<Product>()

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
        if (dbHelper.getAllProducts().isEmpty()) {
            addSampleProducts(dbHelper)
        }
        allProducts = dbHelper.getAllProducts()

        productAdapter = ProductAdapter(allProducts)
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
        allProducts = if (category == null) {
            dbHelper.getAllProducts()
        } else {
            dbHelper.getProductsByCategory(category)
        }
        productAdapter.updateProducts(allProducts)
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
            val product = allProducts.find { it.id == productId }
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
            Product(0, "เลย์", "", 5.0, 0, R.drawable.lay, "ขนม"),
            Product(0, "น้ำเปล่า", "", 10.0, 0, R.drawable.water, "เครื่องดื่ม"),
            Product(0, "โอริโอ", "", 10.0, 0, R.drawable.oreo, "ขนม"),
            Product(0, "โออิชิ", "", 20.0, 0, R.drawable.oshi, "เครื่องดื่ม"),
            Product(0, "เอลเซ", "", 10.0, 0, R.drawable.ellse, "ขนม"),
            Product(0, "ลีโอ", "", 62.0, 0, R.drawable.leo, "เบียร์"),
            Product(0, "เบียร์", "", 65.0, 0, R.drawable.singhabeer, "เบียร์"),
            Product(0, "น้ำแข็ง", "", 10.0, 0, R.drawable.ice, "อื่นๆ"),
            Product(0, "จูปาจุป", "", 2.0, 0, R.drawable.chupachups, "ขนม"),
            Product(0, "โชกี้", "", 2.0, 0, R.drawable.choki, "ขนม"),
            Product(0, "คอนเน่", "", 10.0, 0, R.drawable.conne, "ขนม"),
            Product(0, "มาชิตะ", "", 5.0, 0, R.drawable.mashita, "ขนม")
        )

        for (product in products) {
            dbHelper.addProduct(product)
        }
    }

    private fun filterProducts(query: String?) {
        val list = if (query.isNullOrEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, true) }
        }
        productAdapter.updateProducts(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
