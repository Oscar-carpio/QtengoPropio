package com.example.qtengo.core.data.repositories

import com.example.qtengo.core.domain.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar la persistencia y recuperación de usuarios usando Firebase Firestore.
 * Los datos de usuario se guardan bajo /usuarios/{uid}/ para cumplir las reglas de seguridad.
 */
class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun collection() = firestore
        .collection("usuarios")
        .document(auth.currentUser?.uid ?: "")
        .collection("users")

    suspend fun registrar(user: User): Boolean {
        val snapshot = collection().whereEqualTo("email", user.email).get().await()
        if (!snapshot.isEmpty) return false
        val docRef = collection().document()
        val userWithId = user.copy(id = docRef.id)
        docRef.set(userWithId).await()
        return true
    }

    suspend fun login(email: String): User? {
        val snapshot = collection()
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    suspend fun getUserByEmail(email: String): User? {
        val snapshot = collection()
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }
}