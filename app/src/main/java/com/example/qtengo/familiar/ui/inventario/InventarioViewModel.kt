package com.example.qtengo.familiar.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Modelo de un artículo del inventario familiar
// fechaCaducidad es nullable — si el artículo no caduca el campo no existe en Firestore
data class InventarioItem(
    val id: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val ubicacion: String = "",
    val minStock: Int = 1, // Umbral mínimo — cuando cantidad < minStock se marca como stock bajo
    val notas: String = "",
    val fechaCaducidad: String? = null
)

// ViewModel que gestiona el inventario del hogar del usuario autenticado.
// Todas las operaciones requieren usuario autenticado (ver requireUid).
class InventarioViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _items = MutableStateFlow<List<InventarioItem>>(emptyList())
    val items: StateFlow<List<InventarioItem>> = _items

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // El listener debe cancelarse en onCleared() para evitar fugas de memoria
    private var itemsListener: ListenerRegistration? = null

    fun clearError() { _error.value = null }

    // Obtiene el uid del usuario en cada llamada para evitar valores obsoletos
    // si la sesión expira mientras el ViewModel está activo
    private fun requireUid(): String? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _error.value = "Usuario no autenticado. Por favor, inicia sesión de nuevo."
            return null
        }
        return uid
    }

    // usuarios/{uid}/inventario
    private fun inventarioRef(uid: String) =
        db.collection("usuarios").document(uid).collection("inventario")

    // ─── Carga de datos ──────────────────────────────────────────────────────

    // Escucha cambios en tiempo real en el inventario del usuario
    fun cargarItems() {
        val uid = requireUid() ?: return
        itemsListener?.remove()
        itemsListener = inventarioRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar inventario: ${e.message}"
                    return@addSnapshotListener
                }
                // getLong() necesario porque Firestore almacena enteros como Long
                _items.value = snapshot?.documents?.map { doc ->
                    InventarioItem(
                        id             = doc.id,
                        nombre         = doc.getString("nombre") ?: "",
                        cantidad       = (doc.getLong("cantidad") ?: 0).toInt(),
                        ubicacion      = doc.getString("ubicacion") ?: "",
                        minStock       = (doc.getLong("minStock") ?: 1).toInt(),
                        notas          = doc.getString("notas") ?: "",
                        fechaCaducidad = doc.getString("fechaCaducidad") // null si no existe el campo
                    )
                } ?: emptyList()
            }
    }

    // ─── Escritura ───────────────────────────────────────────────────────────

    // fechaCaducidad solo se incluye en el documento si tiene valor
    // así evitamos guardar el campo con valor vacío en Firestore
    fun añadirItem(
        nombre: String,
        cantidad: Int,
        ubicacion: String,
        minStock: Int,
        notas: String,
        fechaCaducidad: String?
    ) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    "nombre"    to nombre,
                    "cantidad"  to cantidad,
                    "ubicacion" to ubicacion,
                    "minStock"  to minStock,
                    "notas"     to notas
                )
                if (!fechaCaducidad.isNullOrBlank()) {
                    data["fechaCaducidad"] = fechaCaducidad
                }
                inventarioRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir artículo: ${e.message}"
            }
        }
    }

    // Si fechaCaducidad es null se usa FieldValue.delete() para eliminar el campo de Firestore
    // Un update() con null dejaría el campo con valor nulo en lugar de borrarlo
    fun editarItem(
        itemId: String,
        nombre: String,
        cantidad: Int,
        ubicacion: String,
        minStock: Int,
        notas: String,
        fechaCaducidad: String?
    ) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                val data = hashMapOf<String, Any>(
                    "nombre"         to nombre,
                    "cantidad"       to cantidad,
                    "ubicacion"      to ubicacion,
                    "minStock"       to minStock,
                    "notas"          to notas,
                    "fechaCaducidad" to (fechaCaducidad ?: FieldValue.delete())
                )
                inventarioRef(uid).document(itemId).update(data).await()
            } catch (e: Exception) {
                _error.value = "Error al editar artículo: ${e.message}"
            }
        }
    }

    fun eliminarItem(itemId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                inventarioRef(uid).document(itemId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar artículo: ${e.message}"
            }
        }
    }

    // coerceAtLeast(0) garantiza que nunca se guarde stock negativo en Firestore
    fun actualizarCantidad(itemId: String, nuevaCantidad: Int) {
        val uid = requireUid() ?: return
        val cantidad = nuevaCantidad.coerceAtLeast(0)
        viewModelScope.launch {
            try {
                inventarioRef(uid).document(itemId).update("cantidad", cantidad).await()
            } catch (e: Exception) {
                _error.value = "Error al actualizar cantidad: ${e.message}"
            }
        }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    // Cancelamos el listener al destruir el ViewModel para evitar fugas de memoria
    override fun onCleared() {
        super.onCleared()
        itemsListener?.remove()
    }
}