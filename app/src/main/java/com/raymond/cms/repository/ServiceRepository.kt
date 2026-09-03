package com.raymond.cms.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.raymond.cms.model.Service
import com.raymond.cms.util.FirestoreCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val serviceCollection = firestore.collection(FirestoreCollections.SERVICES)

    fun getServices(): Flow<List<Service>> = callbackFlow {
        val subscription = serviceCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val services = snapshot.toObjects(Service::class.java)
                trySend(services)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun initializeDefaultServices() {
        val existing = serviceCollection.get().await()
        if (existing.isEmpty) {
            Service.DEFAULT_SERVICES.forEach { name ->
                val service = Service(name = name, price = 0.0, isActive = true)
                serviceCollection.add(service).await()
            }
        }
    }

    suspend fun updateService(service: Service) {
        serviceCollection.document(service.id).set(service).await()
    }

    suspend fun getServiceById(id: String): Service? {
        val doc = serviceCollection.document(id).get().await()
        return doc.toObject(Service::class.java)
    }
}
