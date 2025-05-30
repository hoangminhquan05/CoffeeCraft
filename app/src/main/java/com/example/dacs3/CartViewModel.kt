package com.example.dacs3


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.dacs3.Model.Product


class CartViewModel : ViewModel() {
    private val _cartItems = mutableStateOf<List<CartItem>>(emptyList())
    val cartItems: List<CartItem> get() = _cartItems.value


    var totalPrice by mutableStateOf(0.0)
        private set


    // Thêm sản phẩm vào giỏ hàng
    fun addToCart(product: Product, quantity: Int = 1) {
        val existingItem = _cartItems.value.find { it.product.id == product.id }
        _cartItems.value = if (existingItem != null) {
            _cartItems.value.map {
                if (it.product.id == product.id) {
                    it.copy(quantity = it.quantity + quantity) // Add to existing quantity
                } else {
                    it
                }
            }
        } else {
            _cartItems.value + CartItem(product, quantity) 
        }
        updateTotalPrice()
    }


    // Xoá sản phẩm khỏi giỏ hàng
    fun removeFromCart(product: Product) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == product.id }
        updateTotalPrice()
    }


    // Tăng số lượng sản phẩm
    fun increaseQuantity(product: Product) {
        _cartItems.value = _cartItems.value.map {
            if (it.product.id == product.id) {
                it.copy(quantity = it.quantity + 1)
            } else {
                it
            }
        }
        updateTotalPrice()
    }


    // Giảm số lượng sản phẩm
    fun decreaseQuantity(product: Product) {
        _cartItems.value = _cartItems.value.mapNotNull {
            if (it.product.id == product.id) {
                if (it.quantity > 1) {
                    it.copy(quantity = it.quantity - 1)
                } else {
                    null
                }
            } else {
                it
            }
        }
        updateTotalPrice()
    }


    // Xóa tất cả sản phẩm trong giỏ hàng
    fun clearCart() {
        _cartItems.value = emptyList()
        totalPrice = 0.0
    }
    fun processPayment(paymentMethod: String) {
        clearCart() // Xóa giỏ hàng sau khi thanh toán
    }


    // Cập nhật tổng tiền giỏ hàng
    private fun updateTotalPrice() {
        totalPrice = _cartItems.value.sumOf { it.totalPrice }
    }


    // Tổng số sản phẩm trong giỏ hàng
    val totalItems: Int
        get() = _cartItems.value.sumOf { it.quantity }
}


data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = product.price * quantity
}

