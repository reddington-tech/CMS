package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymond.cms.model.DailyTransaction
import com.raymond.cms.util.FirestoreCollections
import com.raymond.cms.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TransactionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val transactionCollection = firestore.collection("transactions") // Legacy

    fun getTransactions(): Flow<Resource<List<DailyTransaction>>> = callbackFlow {
        trySend(Resource.Loading)
        val subscription = transactionCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error("Firestore Error: ${error.localizedMessage}"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.toObjects(DailyTransaction::class.java)
                    trySend(Resource.Success(transactions))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getTransactionByIdSync(id: String): DailyTransaction? {
        val doc = transactionCollection.document(id).get().await()
        return if (doc.exists()) doc.toObject(DailyTransaction::class.java) else null
    }

    suspend fun addTransaction(transaction: DailyTransaction) {
        val docId = transaction.id.ifEmpty { DailyTransaction.createId(transaction.timestamp) }
        transactionCollection.document(docId).set(transaction).await()
    }

    suspend fun deleteTransaction(id: String) {
        // Keeping this internal for now, but UI will remove the button
        transactionCollection.document(id).delete().await()
    }

    suspend fun clearAll() {
        val snapshot = transactionCollection.get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
