package com.example.qtengo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var uid by mutableStateOf<String?>(null)
        private set
    var perfiles by mutableStateOf<List<String>>(emptyList())
        private set
    var perfilActivo by mutableStateOf<String?>(null)
        private set

    // Se llama al arrancar la app para comprobar si ya hay sesión activa en Firebase
    fun verificarSesionActiva() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("usuarios")
                    .document(currentUser.uid)
                    .get()
                    .await()

                @Suppress("UNCHECKED_CAST")
                val perfilesRecuperados: List<String> = when {
                    doc.get("perfiles") != null ->
                        (doc.get("perfiles") as? List<String>) ?: emptyList()
                    doc.getString("perfil") != null ->
                        listOf(doc.getString("perfil")!!)
                    else -> emptyList()
                }

                if (perfilesRecuperados.isNotEmpty()) {
                    uid = currentUser.uid
                    perfiles = perfilesRecuperados
                    if (perfilesRecuperados.size == 1) {
                        perfilActivo = perfilesRecuperados.first()
                    }
                } else {
                    auth.signOut()
                }
            } catch (e: Exception) {
                auth.signOut()
            }
        }
    }

    fun onLoginExitoso(nuevoUid: String, nuevosPerfiles: List<String>) {
        uid = nuevoUid
        perfiles = nuevosPerfiles
        if (nuevosPerfiles.size == 1) perfilActivo = nuevosPerfiles.first()
    }

    fun onRegistroExitoso(nuevoUid: String, nuevosPerfiles: List<String>) {
        uid = nuevoUid
        perfiles = nuevosPerfiles
        if (nuevosPerfiles.size == 1) perfilActivo = nuevosPerfiles.first()
    }

    fun seleccionarPerfil(perfil: String) {
        perfilActivo = perfil
    }

    fun cambiarPerfil() {
        perfilActivo = null
    }

    fun cerrarSesion() {
        auth.signOut()
        uid = null
        perfiles = emptyList()
        perfilActivo = null
    }
}
