package com.vorragun.productmanagement

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vorragun.yailek.ui.sales.SaleItem
import com.vorragun.yailek.ui.sales.SalesRecord
import com.vorragun.yailek.R


class ProductDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val db: SQLiteDatabase by lazy { writableDatabase }

    companion object {
        private const val DATABASE_NAME = "product.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_PRODUCTS = "products"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_QUANTITY = "quantity"
        private const val COLUMN_IMAGE_RES_ID = "image_res_id"

        private const val TABLE_SALES = "sales"
        private const val COLUMN_SALE_ID = "id"
        private const val COLUMN_SALE_DATE = "sale_date"
        private const val COLUMN_SALE_TOTAL_AMOUNT = "total_amount"
        private const val COLUMN_SALE_ITEM_COUNT = "item_count"

        private const val TABLE_SALE_ITEMS = "sale_items"
        private const val COLUMN_SALE_ITEM_ID = "id"
        private const val COLUMN_SALE_ITEM_SALE_ID = "sale_id"
        private const val COLUMN_SALE_ITEM_PRODUCT_NAME = "product_name"
        private const val COLUMN_SALE_ITEM_QUANTITY = "quantity"
        private const val COLUMN_SALE_ITEM_PRICE = "price"

        private const val COLUMN_SALE_ITEM_IMAGE ="image_res_id"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createProductsTable = "CREATE TABLE $TABLE_PRODUCTS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT NOT NULL, " +
                "$COLUMN_DESCRIPTION TEXT, " +
                "$COLUMN_PRICE REAL NOT NULL, " +
                "$COLUMN_QUANTITY INTEGER NOT NULL, " +
                "$COLUMN_IMAGE_RES_ID INTEGER)"
        db?.execSQL(createProductsTable)

        val createSalesTable = "CREATE TABLE $TABLE_SALES ($COLUMN_SALE_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_SALE_DATE TEXT, $COLUMN_SALE_TOTAL_AMOUNT REAL, $COLUMN_SALE_ITEM_COUNT INTEGER)"
        db?.execSQL(createSalesTable)

        val createSaleItemsTable = "CREATE TABLE $TABLE_SALE_ITEMS ($COLUMN_SALE_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_SALE_ITEM_SALE_ID INTEGER, $COLUMN_SALE_ITEM_PRODUCT_NAME TEXT, $COLUMN_SALE_ITEM_QUANTITY INTEGER, $COLUMN_SALE_ITEM_PRICE REAL)"
        db?.execSQL(createSaleItemsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SALES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SALE_ITEMS")
        onCreate(db)
    }

    // --- Product Functions --- //
    fun addProduct(product: Product): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, product.name)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_QUANTITY, product.quantity)
            put(COLUMN_IMAGE_RES_ID, product.imageResId)
        }
        return db.insert(TABLE_PRODUCTS, null, values)
    }

    fun getAllProducts(): List<Product> {
        val productList = mutableListOf<Product>()
        db.query(TABLE_PRODUCTS, null, null, null, null, null, "$COLUMN_ID DESC").use { cursor ->
            while (cursor.moveToNext()) {
                productList.add(
                    Product(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                        imageResId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_RES_ID))
                    )
                )
            }
        }
        return productList
    }

    // --- Sales Functions --- //
    fun addSaleRecordAndReturnId(record: SalesRecord): Long {
        val values = ContentValues().apply {
            put(COLUMN_SALE_DATE, record.date)
            put(COLUMN_SALE_TOTAL_AMOUNT, record.totalAmount)
            put(COLUMN_SALE_ITEM_COUNT, record.itemCount)
        }
        return db.insert(TABLE_SALES, null, values)
    }

    fun addSaleItems(saleId: Long, items: List<SaleItem>) {
        db.beginTransaction()
        try {
            for (item in items) {
                val values = ContentValues().apply {
                    put(COLUMN_SALE_ITEM_SALE_ID, saleId)
                    put(COLUMN_SALE_ITEM_PRODUCT_NAME, item.productName)
                    put(COLUMN_SALE_ITEM_QUANTITY, item.quantity)
                    put(COLUMN_SALE_ITEM_PRICE, item.price)
                }
                db.insert(TABLE_SALE_ITEMS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllSalesRecords(): List<SalesRecord> {
        val salesList = mutableListOf<SalesRecord>()
        var dailyCounter = 1

        db.query(TABLE_SALES, null, null, null, null, null, "$COLUMN_SALE_ID ASC").use { cursor ->
            while (cursor.moveToNext()) {
                salesList.add(
                    SalesRecord(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ID)),
                        date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALE_DATE)),
                        totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SALE_TOTAL_AMOUNT)),
                        itemCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_COUNT)),
                        dailyNumber = dailyCounter
                    )
                )
                dailyCounter++
            }
        }
        return salesList.reversed()
    }


    fun getSalesForDate(date: String): List<SalesRecord> {
        val salesList = mutableListOf<SalesRecord>()
        var dailyCounter = 1

        db.query(
            TABLE_SALES,
            null,
            "$COLUMN_SALE_DATE = ?",
            arrayOf(date),
            null,
            null,
            "$COLUMN_SALE_ID ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                salesList.add(
                    SalesRecord(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ID)),
                        date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALE_DATE)),
                        totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SALE_TOTAL_AMOUNT)),
                        itemCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_COUNT)),
                        dailyNumber = dailyCounter
                    )
                )
                dailyCounter++
            }
        }
        return salesList.reversed()
    }


    fun getSaleItems(saleId: Int): List<SaleItem> {
        val list = mutableListOf<SaleItem>()
        db.rawQuery("SELECT * FROM $TABLE_SALE_ITEMS WHERE $COLUMN_SALE_ITEM_SALE_ID = ?", arrayOf(saleId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    SaleItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_ID)),
                        saleId = saleId,
                        productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_PRODUCT_NAME)),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_QUANTITY)),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_PRICE)),
                        imageResId = cursor.getInt(
                            cursor.getColumnIndexOrThrow(COLUMN_SALE_ITEM_IMAGE)
                        )
                    )
                    )
            }
        }
        return list
    }

    fun deleteSale(saleId: Int) {
        db.beginTransaction()
        try {
            db.delete(TABLE_SALE_ITEMS, "$COLUMN_SALE_ITEM_SALE_ID = ?", arrayOf(saleId.toString()))
            db.delete(TABLE_SALES, "$COLUMN_SALE_ID = ?", arrayOf(saleId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}