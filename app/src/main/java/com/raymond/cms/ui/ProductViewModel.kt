package com.raymond.cms.ui

import androidx.lifecycle.viewModelScope
import com.raymond.cms.model.*
import com.raymond.cms.repository.ProductRepository
import com.raymond.cms.repository.FinancialRepository
import com.raymond.cms.util.DateTimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class ProductViewModel : BaseViewModel() {
    private val repository = ProductRepository()
    private val financialRepo = FinancialRepository()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch(exceptionHandler) {
            repository.getProducts()
                .onStart { _isLoading.value = true }
                .catch { e -> 
                    _isLoading.value = false
                    _uiEvent.emit("Failed to load products: ${e.localizedMessage}")
                }
                .collect {
                    _products.value = it
                    _isLoading.value = false
                }
        }
    }

    fun addProduct(name: String, description: String, buyingPrice: Double, sellingPrice: Double, quantity: Int) {
        viewModelScope.launch(exceptionHandler) {
            val product = Product(
                name = name,
                description = description,
                buyingPrice = buyingPrice,
                sellingPrice = sellingPrice,
                quantityBought = quantity,
                currentStock = quantity,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addProduct(product)
            _uiEvent.emit("Product added successfully")
        }
    }

    fun addStock(
        productId: String, 
        additionalQuantity: Int, 
        newBuyingPrice: Double,
        user: UserModel,
        shiftId: String,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch(exceptionHandler) {
            val currentProduct = _products.value.find { it.id == productId } ?: return@launch
            
            val totalCost = additionalQuantity * newBuyingPrice
            val timestamp = System.currentTimeMillis()
            val date = DateTimeUtils.getFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

            // 1. Record the Expense automatically (Requirement: Link stock to financial records)
            val expense = Expense(
                staffId = user.uid,
                staffName = user.name.ifEmpty { user.email },
                shiftId = shiftId,
                category = "Inventory / Stock",
                amount = totalCost,
                description = "Restock: ${currentProduct.name} x $additionalQuantity @ KSh $newBuyingPrice",
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                date = date
            )
            financialRepo.addExpense(expense)

            // 2. Update Product Stock and WAC (Weighted Average Cost)
            val currentTotalCost = currentProduct.currentStock * currentProduct.buyingPrice
            val totalStock = currentProduct.currentStock + additionalQuantity
            val weightedAverageBuyingPrice = if (totalStock > 0) {
                (currentTotalCost + totalCost) / totalStock
            } else {
                newBuyingPrice
            }

            val updatedProduct = currentProduct.copy(
                quantityBought = currentProduct.quantityBought + additionalQuantity,
                currentStock = totalStock,
                buyingPrice = weightedAverageBuyingPrice,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateProduct(updatedProduct)
            _uiEvent.emit("Stock updated & Expense recorded. New Avg: KSh ${String.format("%.2f", weightedAverageBuyingPrice)}")
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteProduct(id)
            _uiEvent.emit("Product removed")
        }
    }
}
