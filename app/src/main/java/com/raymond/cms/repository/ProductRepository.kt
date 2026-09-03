package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymond.cms.model.Product
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productCollection = firestore.collection(FirestoreCollections.PRODUCTS)

    fun getProducts(): Flow<List<Product>> = callbackFlow {
        // Simple query first to ensure data visibility
        val subscription = productCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log error to console for debugging
                    android.util.Log.e("ProductRepo", "Listen failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.toObjects(Product::class.java)
                    // Order manually in memory if needed, but let's see if data shows up first
                    trySend(products.sortedByDescending { it.lastUpdated })
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addProduct(product: Product) {
        productCollection.add(product).await()
    }

    suspend fun updateProduct(product: Product) {
        productCollection.document(product.id).set(product).await()
    }

    suspend fun deleteProduct(id: String) {
        productCollection.document(id).delete().await()
    }

    suspend fun deductStock(productId: String, quantity: Int) {
        firestore.runTransaction { transaction ->
            val docRef = productCollection.document(productId)
            val snapshot = transaction.get(docRef)
            val currentStock = snapshot.getLong("currentStock") ?: 0L
            val newStock = currentStock - quantity
            transaction.update(docRef, "currentStock", newStock)
        }.await()
    }
}
