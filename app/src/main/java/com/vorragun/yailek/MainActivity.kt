package com.vorragun.yailek

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.vorragun.productmanagement.Product
import com.vorragun.productmanagement.ProductAdapter
import com.vorragun.productmanagement.ProductDbHelper
import com.vorragun.yailek.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: ProductDbHelper
    private lateinit var productAdapter: ProductAdapter
    private var allProducts = listOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "สินค้า"

        dbHelper = ProductDbHelper(this)

        // Add sample data if the database is empty
        if (dbHelper.getAllProducts().isEmpty()) {
            addSampleProducts()
        }

        allProducts = dbHelper.getAllProducts()

        productAdapter = ProductAdapter(allProducts)
        binding.productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.productsRecyclerView.adapter = productAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })

        return true
    }

    private fun addSampleProducts() {
        dbHelper.addProduct(Product(0, "Iced Latte", "", 85.00, 1, ""))
        dbHelper.addProduct(Product(0, "Croissant", "", 60.00, 0, ""))
        dbHelper.addProduct(Product(0, "Matcha Latte", "", 95.00, 2, ""))
        dbHelper.addProduct(Product(0, "B-Berry Muffin", "", 75.00, 0, ""))
        dbHelper.addProduct(Product(0, "Cold Brew", "", 110.00, 1, ""))
        dbHelper.addProduct(Product(0, "Avocado Toast", "", 165.00, 0, ""))
    }

    private fun filterProducts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, ignoreCase = true) }
        }
        productAdapter.updateProducts(filteredList)
    }
}