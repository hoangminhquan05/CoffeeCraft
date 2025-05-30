package com.example.dacs3.Model

data class Product(
    var id: String = "",
    val imageUrl: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var description: String = "",
    val category: String = ""
) {
    fun getFormattedPrice(): String {
        return "%,.0f VND".format(price).replace(",", ".")
    }
}
