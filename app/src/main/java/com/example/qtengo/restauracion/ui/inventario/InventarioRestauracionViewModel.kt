package com.example.qtengo.restauracion.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RestauracionProducto(
    val id_producto: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val stock: Int = 0,
    val stock_minimo: Int = 0,
    val precio: Double = 0.0
)

class InventarioRestauracionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _items = MutableStateFlow<List<RestauracionProducto>>(emptyList())
    val items: StateFlow<List<RestauracionProducto>> = _items.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() { _error.value = null }

    private fun requireUid(): String? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _error.value = "Usuario no autenticado"
            return null
        }
        return uid
    }

    private fun inventarioRef(uid: String) =
        db.collection("usuarios")
            .document(uid)
            .collection("inventarioRestauracion")

    fun cargarItems() {
        val uid = requireUid() ?: return
        inventarioRef(uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                _error.value = "Error al cargar inventario: ${e.message}"
                return@addSnapshotListener
            }
            _items.value = snapshot?.documents?.map { doc ->
                RestauracionProducto(
                    id_producto = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    categoria = doc.getString("categoria") ?: "",
                    stock = (doc.getLong("stock") ?: 0L).toInt(),
                    stock_minimo = (doc.getLong("stock_minimo") ?: 0L).toInt(),
                    precio = doc.getDouble("precio") ?: 0.0
                )
            } ?: emptyList()
        }
    }

    fun agregarItem(nombre: String, categoria: String, stock: Int, stockMinimo: Int, precio: Double) {
        val uid = requireUid() ?: return
        if (nombre.isBlank()) { _error.value = "El nombre no puede estar vacío"; return }
        if (precio < 0) { _error.value = "El precio no puede ser negativo"; return }
        viewModelScope.launch {
            try {
                val data = hashMapOf("nombre" to nombre.trim(), "categoria" to categoria.trim(), "stock" to stock, "stock_minimo" to stockMinimo, "precio" to precio)
                inventarioRef(uid).add(data).await()
            } catch (e: Exception) { _error.value = "Error al agregar producto: ${e.message}" }
        }
    }

    fun eliminarItem(idProducto: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try { inventarioRef(uid).document(idProducto).delete().await() }
            catch (e: Exception) { _error.value = "Error al eliminar: ${e.message}" }
        }
    }

    fun actualizarStock(idProducto: String, nuevoStock: Int) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try { inventarioRef(uid).document(idProducto).update("stock", nuevoStock.coerceAtLeast(0)).await() }
            catch (e: Exception) { _error.value = "Error al actualizar stock: ${e.message}" }
        }
    }

    fun aumentarStock(producto: RestauracionProducto) = actualizarStock(producto.id_producto, producto.stock + 1)
    fun disminuirStock(producto: RestauracionProducto) = actualizarStock(producto.id_producto, (producto.stock - 1).coerceAtLeast(0))
}
