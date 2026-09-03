package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.raymond.cms.model.InventoryItem
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InventoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val inventoryCollection = firestore.collection(FirestoreCollections.INVENTORY)

    fun getInventory(): Flow<List<InventoryItem>> = callbackFlow {
        val subscription = inventoryCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(InventoryItem::class.java))
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getInventorySync(): List<InventoryItem> {
        val snapshot = inventoryCollection.get().await()
        return snapshot.toObjects(InventoryItem::class.java)
    }

    suspend fun updateStock(id: String, newStock: Double) {
        inventoryCollection.document(id).update("currentStock", newStock).await()
    }

    suspend fun addItem(item: InventoryItem) {
        inventoryCollection.add(item).await()
    }
}
