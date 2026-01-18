package com.vorragun.yailek.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
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

        val dbHelper = ProductDbHelper(requireContext())
        if (dbHelper.getAllProducts().isEmpty()) {
            addSampleProducts(dbHelper)
        }
        allProducts = dbHelper.getAllProducts()

        productAdapter = ProductAdapter(allProducts)
        binding.productsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productsRecyclerView.adapter = productAdapter

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
                        imageResId = product.imageResId // Pass the imageResId
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

        exitSelectionMode()
        (requireActivity() as MainActivity).selectSalesTab()
    }

    private fun addSampleProducts(dbHelper: ProductDbHelper) {
        val titles = listOf(
            "เลย์", "น้ำเปล่า", "โอริโอ", "โออิชิ",
            "เอลเซ", "ลีโอ", "เบียร์", "น้ำแข็ง",
            "จูปาจุป", "โชกี้", "คอนเน่", "มาชิตะ"
        )

        val prices = listOf(
            5.0, 10.0, 10.0, 20.0,
            10.0, 62.0, 65.0, 10.0,
            2.0, 2.0, 10.0, 5.0
        )

        val images = listOf(
            R.drawable.lay,
            R.drawable.water,
            R.drawable.oreo,
            R.drawable.oshi,
            R.drawable.ellse,
            R.drawable.leo,
            R.drawable.singhabeer,
            R.drawable.ice,
            R.drawable.chupachups,
            R.drawable.choki,
            R.drawable.conne,
            R.drawable.mashita
        )

        for (i in titles.indices) {
            val product = Product(
                id = 0,
                name = titles[i],
                description = "",
                price = prices[i],
                quantity = 0,
                imageResId = images[i]
            )
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