package com.vorragun.productmanagement

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProductDbHelper(context: Context) : SQLiteOpenHelper(context,DATABASE_NAME,null,
DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "product.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "products"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_QUANTITY = "quantity"
        private const val COLUMN_IMAGE_PATH = "image_path"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT NOT NULL, " +
                "$COLUMN_DESCRIPTION TEXT, " +
                "$COLUMN_PRICE REAL NOT NULL, " +
                "$COLUMN_QUANTITY INTEGER NOT NULL, " +
                "$COLUMN_IMAGE_PATH TEXT)"

        db?.execSQL(createTable)
    }
    override fun onUpgrade(db:SQLiteDatabase?,oldVersion:Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    //เพิ่มสินค้า
    fun addProduct(product: Product): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, product.name)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_QUANTITY, product.quantity)
            put(COLUMN_IMAGE_PATH, product.imagePath)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    // อ่านสินค้าทั้งหมด
    fun getAllProducts(): List<Product> {
        val productList = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val name = getString(getColumnIndexOrThrow(COLUMN_NAME))
                val description = getString(getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val price = getDouble(getColumnIndexOrThrow(COLUMN_PRICE))
                val quantity = getInt(getColumnIndexOrThrow(COLUMN_QUANTITY))
                val imagePath = getString(getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                val product = Product(id, name, description, price, quantity, imagePath)
                productList.add(product)
            }
        }
        cursor.close()
        db.close()
        return productList
    }

    // อ่านสินค้าตาม ID
    fun getProductById(id: Int): Product? {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, "$COLUMN_ID=?", arrayOf(id.toString()), null, null, null)

        return if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE))
            val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY))
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))

            cursor.close()
            db.close()
            Product(id, name, description, price, quantity, imagePath)
        } else {
            cursor.close()
            db.close()
            null
        }
    }
    // อัพเดทสินค้า
    fun updateProduct(product: Product): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, product.name)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_QUANTITY, product.quantity)
            put(COLUMN_IMAGE_PATH, product.imagePath)
        }

        val rowsAffected = db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(product.id.toString()))
        db.close()
        return rowsAffected
    }

    // ลบสินค้า
    fun deleteProduct(id: Int): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return rowsDeleted
    }
}