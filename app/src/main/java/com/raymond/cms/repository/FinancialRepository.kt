package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymond.cms.model.Expense
import com.raymond.cms.model.Investment
import com.raymond.cms.model.Transaction
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FinancialRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val transactionCollection = firestore.collection(FirestoreCollections.TRANSACTIONS)
    private val expenseCollection = firestore.collection(FirestoreCollections.EXPENSES)
    private val investmentCollection = firestore.collection(FirestoreCollections.INVESTMENTS)

    fun getTransactionsForShift(shiftId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = transactionCollection
            .whereEqualTo("shiftId", shiftId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Transaction::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getExpensesForShift(shiftId: String): Flow<List<Expense>> = callbackFlow {
        val subscription = expenseCollection
            .whereEqualTo("shiftId", shiftId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Expense::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val subscription = transactionCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Transaction::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getAllExpenses(): Flow<List<Expense>> = callbackFlow {
        val subscription = expenseCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Expense::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addTransaction(transaction: Transaction) {
        transactionCollection.add(transaction).await()
    }

    suspend fun addExpense(expense: Expense) {
        expenseCollection.add(expense).await()
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionCollection.document(transaction.id).set(transaction).await()
    }

    suspend fun updateExpense(expense: Expense) {
        expenseCollection.document(expense.id).set(expense).await()
    }

    suspend fun getTransactionById(id: String): Transaction? {
        val doc = transactionCollection.document(id).get().await()
        return doc.toObject(Transaction::class.java)
    }

    suspend fun getExpenseById(id: String): Expense? {
        val doc = expenseCollection.document(id).get().await()
        return doc.toObject(Expense::class.java)
    }

    suspend fun addInvestment(investment: Investment) {
        investmentCollection.add(investment).await()
    }

    fun getInvestmentsForShift(shiftId: String): Flow<List<Investment>> = callbackFlow {
        val subscription = investmentCollection
            .whereEqualTo("shiftId", shiftId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Investment::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }
}
