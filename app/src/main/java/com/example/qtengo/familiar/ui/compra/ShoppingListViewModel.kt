package com.example.qtengo.familiar.ui.compra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Modelo de una lista de la compra (el contenedor, no los productos)
data class ShoppingList(
    val id: String = "",
    val name: String = "",
    val itemCount: Int = 0, // Se actualiza manualmente con actualizarContador()
    val date: String = ""   // Formato dd/MM/yyyy
)

// Modelo de un producto dentro de una lista
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "", // Texto libre: "2 kg", "1 bote", etc.
    val price: Double = 0.0,
    val isChecked: Boolean = false // true = ya está en el carrito
)

// Modelo de un producto favorito para añadir rápido a cualquier lista
data class FavoriteItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val price: Double = 0.0
)

// ViewModel que gestiona listas de la compra, productos y favoritos del usuario.
// Todas las operaciones requieren usuario autenticado (ver requireUid).
class ShoppingListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _lists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val lists: StateFlow<List<ShoppingList>> = _lists

    // Solo hay una lista activa a la vez — cargarItems() reemplaza el listener anterior
    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val items: StateFlow<List<ShoppingItem>> = _items

    private val _favoritos = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favoritos: StateFlow<List<FavoriteItem>> = _favoritos

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // true mientras hay una operación de escritura en curso — la UI debe bloquear botones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Los tres listeners deben cancelarse en onCleared() para evitar fugas de memoria
    private var listasListener: ListenerRegistration? = null
    private var itemsListener: ListenerRegistration? = null
    private var favoritosListener: ListenerRegistration? = null

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

    // Referencias a las colecciones de Firestore del usuario
    // usuarios/{uid}/listas y usuarios/{uid}/favoritos
    private fun listasRef(uid: String) =
        db.collection("usuarios").document(uid).collection("listas")

    private fun favoritosRef(uid: String) =
        db.collection("usuarios").document(uid).collection("favoritos")

    // ─── Carga de datos ──────────────────────────────────────────────────────

    // Escucha cambios en tiempo real en la colección de listas del usuario
    fun cargarListas() {
        val uid = requireUid() ?: return
        listasListener?.remove()
        listasListener = listasRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar listas: ${e.message}"
                    return@addSnapshotListener
                }
                _lists.value = snapshot?.documents?.map { doc ->
                    ShoppingList(
                        id        = doc.id,
                        name      = doc.getString("name") ?: "",
                        itemCount = (doc.getLong("itemCount") ?: 0).toInt(),
                        date      = doc.getString("date") ?: ""
                    )
                } ?: emptyList()
            }
    }

    // Escucha los productos de una lista concreta en tiempo real
    // Al cambiar de lista, el listener anterior se cancela automáticamente
    fun cargarItems(listaId: String) {
        val uid = requireUid() ?: return
        itemsListener?.remove()
        itemsListener = listasRef(uid).document(listaId).collection("productos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar productos: ${e.message}"
                    return@addSnapshotListener
                }
                _items.value = snapshot?.documents?.map { doc ->
                    ShoppingItem(
                        id        = doc.id,
                        name      = doc.getString("name") ?: "",
                        quantity  = doc.getString("quantity") ?: "",
                        price     = doc.getDouble("price") ?: 0.0,
                        isChecked = doc.getBoolean("isChecked") ?: false
                    )
                } ?: emptyList()
            }
    }

    // Escucha los favoritos del usuario en tiempo real
    fun cargarFavoritos() {
        val uid = requireUid() ?: return
        favoritosListener?.remove()
        favoritosListener = favoritosRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar favoritos: ${e.message}"
                    return@addSnapshotListener
                }
                _favoritos.value = snapshot?.documents?.map { doc ->
                    FavoriteItem(
                        id       = doc.id,
                        name     = doc.getString("name") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        price    = doc.getDouble("price") ?: 0.0
                    )
                } ?: emptyList()
            }
    }

    // ─── Favoritos ───────────────────────────────────────────────────────────

    // Guarda un favorito comprobando antes que no exista uno con el mismo nombre
    fun guardarFavorito(nombre: String, cantidad: String, precio: Double) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existente = favoritosRef(uid)
                    .whereEqualTo("name", nombre.trim())
                    .get()
                    .await()

                if (!existente.isEmpty) {
                    _error.value = "\"$nombre\" ya está en tus favoritos"
                    return@launch
                }

                val data = mapOf(
                    "name"     to nombre.trim(),
                    "quantity" to cantidad,
                    "price"    to precio
                )
                favoritosRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al guardar favorito: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarFavorito(favoritoId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                favoritosRef(uid).document(favoritoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar favorito: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Añade un favorito a una lista como si fuera un producto nuevo
    fun añadirFavoritoALista(listaId: String, favorito: FavoriteItem) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"      to favorito.name,
                    "quantity"  to favorito.quantity.ifBlank { "1" },
                    "price"     to favorito.price,
                    "isChecked" to false
                )
                listasRef(uid).document(listaId).collection("productos").add(data).await()
                actualizarContador(uid, listaId, delta = 1)
            } catch (e: Exception) {
                _error.value = "Error al añadir favorito a la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Listas ──────────────────────────────────────────────────────────────

    fun crearLista(nombre: String) {
        val uid = requireUid() ?: return
        // Validación en el ViewModel, no solo en la UI
        if (nombre.isBlank()) {
            _error.value = "El nombre de la lista no puede estar vacío"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())
                val data = mapOf(
                    "name"      to nombre.trim(),
                    "itemCount" to 0,
                    "date"      to fecha
                )
                listasRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al crear la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Productos ───────────────────────────────────────────────────────────

    fun añadirItem(listaId: String, nombre: String, cantidad: String, precio: Double) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"      to nombre,
                    "quantity"  to cantidad.ifBlank { "1" },
                    "price"     to precio,
                    "isChecked" to false
                )
                listasRef(uid).document(listaId).collection("productos").add(data).await()
                actualizarContador(uid, listaId, delta = 1)
            } catch (e: Exception) {
                _error.value = "Error al añadir producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editarItem(listaId: String, itemId: String, nombre: String, cantidad: String, precio: Double) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"     to nombre,
                    "quantity" to cantidad,
                    "price"    to precio
                )
                listasRef(uid).document(listaId)
                    .collection("productos").document(itemId)
                    .update(data).await()
            } catch (e: Exception) {
                _error.value = "Error al editar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Marca o desmarca un producto como recogido en el carrito
    fun toggleItem(listaId: String, itemId: String, checked: Boolean) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listasRef(uid).document(listaId)
                    .collection("productos").document(itemId)
                    .update("isChecked", checked).await()
            } catch (e: Exception) {
                _error.value = "Error al actualizar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Elimina la lista y todos sus productos en una operación atómica (batch)
    // Firestore no borra subcolecciones automáticamente al borrar el documento padre
    fun eliminarLista(listaId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val productos = listasRef(uid).document(listaId)
                    .collection("productos").get().await()
                val batch = db.batch()
                productos.documents.forEach { batch.delete(it.reference) }
                batch.delete(listasRef(uid).document(listaId))
                batch.commit().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarItem(listaId: String, itemId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listasRef(uid).document(listaId)
                    .collection("productos").document(itemId)
                    .delete().await()
                actualizarContador(uid, listaId, delta = -1)
            } catch (e: Exception) {
                _error.value = "Error al eliminar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Incrementa o decrementa el contador de productos de una lista de forma atómica
    // FieldValue.increment() evita condiciones de carrera en accesos simultáneos
    private suspend fun actualizarContador(uid: String, listaId: String, delta: Int) {
        try {
            listasRef(uid).document(listaId)
                .update("itemCount", FieldValue.increment(delta.toLong()))
                .await()
        } catch (e: Exception) {
            _error.value = "Error al actualizar contador: ${e.message}"
        }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    // Cancelamos los tres listeners al destruir el ViewModel para evitar fugas de memoria
    override fun onCleared() {
        super.onCleared()
        listasListener?.remove()
        itemsListener?.remove()
        favoritosListener?.remove()
    }
}