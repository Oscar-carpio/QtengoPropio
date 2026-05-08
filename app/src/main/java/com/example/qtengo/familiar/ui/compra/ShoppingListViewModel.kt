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

/**
 * Representa una lista de la compra (el contenedor), no los productos que contiene.
 *
 * @param id        ID único del documento en Firestore (asignado automáticamente).
 * @param name      Nombre de la lista definido por el usuario.
 * @param itemCount Número de productos en la lista. Se mantiene sincronizado manualmente
 *                  mediante [actualizarContador]; Firestore no lo calcula solo.
 * @param date      Fecha de creación en formato "dd/MM/yyyy".
 */
data class ShoppingList(
    val id: String = "",
    val name: String = "",
    val itemCount: Int = 0,
    val date: String = ""
)

/**
 * Representa un producto dentro de una lista de la compra.
 *
 * @param quantity  Cantidad como texto libre (ej: "2 kg", "1 bote") para mayor flexibilidad.
 * @param price     Precio en Double. Anteriormente era String, lo que impedía realizar
 *                  sumas correctas. Cualquier parseo debe hacerse en la UI antes de llegar aquí.
 * @param isChecked Indica si el producto ya ha sido introducido en el carrito.
 */
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val price: Double = 0.0,
    val isChecked: Boolean = false
)

/**
 * Representa un producto favorito del usuario para añadir rápidamente a cualquier lista.
 *
 * @param price Precio en Double, consistente con [ShoppingItem] para evitar conversiones.
 */
data class FavoriteItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val price: Double = 0.0
)

/**
 * ViewModel para la gestión de listas de la compra, productos y favoritos del usuario.
 *
 * Responsabilidades:
 *  - Leer/escribir listas, productos dentro de cada lista y favoritos en Firestore en tiempo real.
 *  - Mantener sincronizado el contador de productos de cada lista.
 *  - Gestionar el ciclo de vida de tres listeners de Firestore para evitar fugas de memoria.
 *  - Exponer errores a la UI a través de [error] StateFlow.
 *
 * SEGURIDAD: Todas las operaciones verifican la autenticación mediante [requireUid].
 */
open class ShoppingListViewModel : ViewModel() {

    // ─── Dependencias ────────────────────────────────────────────────────────

    /** Instancia de Firestore. Punto único de acceso a la base de datos. */
    private val db = FirebaseFirestore.getInstance()

    /**
     * UID del usuario autenticado, capturado al crear el ViewModel.
     * IMPORTANTE: Si la sesión se cierra mientras el ViewModel está activo,
     * este valor quedará obsoleto. Ver [requireUid].
     */
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ─── Estado observable ───────────────────────────────────────────────────

    /** Lista de listas de la compra del usuario. Solo lectura desde la UI. */
    private val _lists = MutableStateFlow<List<ShoppingList>>(emptyList())
    open val lists: StateFlow<List<ShoppingList>> = _lists

    /**
     * Productos de la lista actualmente abierta.
     * IMPORTANTE: Solo puede haber una lista activa a la vez en este StateFlow.
     * Cada llamada a [cargarItems] reemplaza el listener anterior, cambiando
     * qué lista se está observando.
     */
    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    open val items: StateFlow<List<ShoppingItem>> = _items

    /** Productos favoritos del usuario para añadir rápidamente a listas. */
    private val _favoritos = MutableStateFlow<List<FavoriteItem>>(emptyList())
    open val favoritos: StateFlow<List<FavoriteItem>> = _favoritos

    /**
     * Canal de errores para la UI. La UI debe llamar a [clearError] tras mostrar
     * el mensaje para evitar que se repita en recomposiciones.
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Referencias a los tres listeners activos de Firestore.
     * CRÍTICO: Los tres deben cancelarse en [onCleared] para evitar
     * fugas de memoria y callbacks huérfanos.
     */
    private var listasListener: ListenerRegistration? = null
    private var itemsListener: ListenerRegistration? = null
    private var favoritosListener: ListenerRegistration? = null

    /** Limpia el error actual. Llamar desde la UI tras mostrar el mensaje. */
    fun clearError() { _error.value = null }

    /**
     * Indicador de carga para operaciones de escritura.
     * La UI debe deshabilitar los botones de acción mientras sea true
     * para evitar envíos duplicados.
     */
    private val _isLoading = MutableStateFlow(false)
    open val isLoading: StateFlow<Boolean> = _isLoading

    // ─── Seguridad / Autenticación ───────────────────────────────────────────

    /**
     * Guard centralizado de autenticación.
     *
     * SEGURIDAD: Ninguna operación de lectura/escritura debe ejecutarse sin
     * pasar por aquí. Devuelve null y emite error en [_error] si el UID está vacío,
     * impidiendo accesos no autorizados a colecciones de Firestore.
     *
     * @return UID del usuario autenticado, o null si no hay sesión activa.
     */
    private fun requireUid(): String? {
        if (uid.isBlank()) {
            _error.value = "Usuario no autenticado. Por favor, inicia sesión de nuevo."
            return null
        }
        return uid
    }

    /**
     * Referencias a las colecciones del usuario en Firestore:
     *  - listas:    usuarios/{uid}/listas
     *  - favoritos: usuarios/{uid}/favoritos
     *
     * Los productos de cada lista se acceden como subcolección:
     *  usuarios/{uid}/listas/{listaId}/productos
     */
    private fun listasRef() = db.collection("usuarios").document(uid).collection("listas")
    private fun favoritosRef() = db.collection("usuarios").document(uid).collection("favoritos")

    // ─── Carga de datos ──────────────────────────────────────────────────────

    /**
     * Suscribe un listener en tiempo real a la colección de listas del usuario.
     *
     * Cancela el listener previo antes de crear uno nuevo para evitar duplicados
     * en caso de llamadas repetidas (p.ej. por rotación de pantalla).
     */
    fun cargarListas() {
        val uid = requireUid() ?: return
        listasListener?.remove()
        listasListener = db.collection("usuarios").document(uid).collection("listas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar listas: ${e.message}"
                    return@addSnapshotListener
                }
                // getLong() es necesario porque Firestore almacena enteros como Long
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

    /**
     * Suscribe un listener en tiempo real a los productos de una lista concreta.
     *
     * IMPORTANTE: Solo puede haber un listener de productos activo a la vez.
     * Al llamar a este método con una nueva [listaId], el listener anterior
     * se cancela automáticamente, dejando de observar la lista previa.
     *
     * @param listaId ID de la lista cuyos productos se quieren observar.
     */
    fun cargarItems(listaId: String) {
        val uid = requireUid() ?: return
        itemsListener?.remove()
        itemsListener = db.collection("usuarios").document(uid)
            .collection("listas").document(listaId).collection("productos")
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

    /**
     * Suscribe un listener en tiempo real a la colección de favoritos del usuario.
     */
    fun cargarFavoritos() {
        val uid = requireUid() ?: return
        favoritosListener?.remove()
        favoritosListener = db.collection("usuarios").document(uid).collection("favoritos")
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

    /**
     * Guarda un producto como favorito del usuario, comprobando duplicados previamente.
     *
     * IMPORTANTE — Deduplicación:
     *  - Antes de guardar se realiza una consulta a Firestore para verificar que no
     *    existe ya un favorito con el mismo nombre (tras aplicar trim()).
     *  - El trim() evita falsos duplicados por espacios extra al inicio o al final.
     *  - Si ya existe, se emite un mensaje informativo en [_error] y se cancela la operación.
     *
     * @param nombre   Nombre del producto. Se aplica trim() antes de guardar y comparar.
     * @param cantidad Cantidad en texto libre.
     * @param precio   Precio como Double, parseado previamente en la UI.
     */
    fun guardarFavorito(nombre: String, cantidad: String, precio: Double) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Comprobamos si ya existe un favorito con ese nombre antes de insertar
                val existente = favoritosRef()
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
                favoritosRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al guardar favorito: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina permanentemente un favorito del usuario.
     *
     * ADVERTENCIA: Operación irreversible. Confirmar con el usuario en la UI.
     *
     * @param favoritoId ID del documento a eliminar.
     */
    fun eliminarFavorito(favoritoId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                favoritosRef().document(favoritoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar favorito: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Añade un favorito a una lista de la compra como si fuera un producto nuevo.
     *
     * Tras insertar el producto en la subcolección, actualiza el contador [itemCount]
     * del documento de la lista mediante [actualizarContador].
     *
     * NOTA: Si [favorito.quantity] está vacío se sustituye por "1" para evitar
     * dejar el campo en blanco en Firestore.
     *
     * @param listaId  ID de la lista destino.
     * @param favorito Producto favorito que se va a añadir.
     */
    fun añadirFavoritoALista(listaId: String, favorito: FavoriteItem) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"      to favorito.name,
                    "quantity"  to favorito.quantity.ifBlank { "1" },
                    "price"     to favorito.price,
                    "isChecked" to false
                )
                listasRef().document(listaId).collection("productos").add(data).await()
                actualizarContador(listaId, delta = 1)
            } catch (e: Exception) {
                _error.value = "Error al añadir favorito a la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Listas ──────────────────────────────────────────────────────────────

    /**
     * Crea una nueva lista de la compra con contador inicial a 0 y fecha del día actual.
     *
     * @param nombre Nombre de la lista (no debe estar vacío — validar en UI).
     */
    fun crearLista(nombre: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())
                val data = mapOf(
                    "name"      to nombre,
                    "itemCount" to 0,
                    "date"      to fecha
                )
                listasRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al crear la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Productos ───────────────────────────────────────────────────────────

    /**
     * Añade un nuevo producto a una lista y actualiza su contador.
     *
     * Si [cantidad] está vacía se sustituye por "1" para no dejar el campo en blanco.
     * El precio llega como Double porque el parseo se realiza en la UI antes de llamar aquí.
     *
     * @param listaId  ID de la lista destino.
     * @param nombre   Nombre del producto.
     * @param cantidad Cantidad en texto libre.
     * @param precio   Precio ya parseado como Double.
     */
    fun añadirItem(listaId: String, nombre: String, cantidad: String, precio: Double) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"      to nombre,
                    "quantity"  to cantidad.ifBlank { "1" },
                    "price"     to precio,
                    "isChecked" to false
                )
                listasRef().document(listaId).collection("productos").add(data).await()
                actualizarContador(listaId, delta = 1)
            } catch (e: Exception) {
                _error.value = "Error al añadir producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza nombre, cantidad y precio de un producto existente.
     *
     * NOTA: No modifica [isChecked] porque cambiar el estado de marcado
     * es responsabilidad exclusiva de [toggleItem].
     *
     * @param listaId  ID de la lista que contiene el producto.
     * @param itemId   ID del documento del producto a editar.
     */
    fun editarItem(listaId: String, itemId: String, nombre: String, cantidad: String, precio: Double) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name"     to nombre,
                    "quantity" to cantidad,
                    "price"    to precio
                )
                listasRef().document(listaId)
                    .collection("productos").document(itemId)
                    .update(data).await()
            } catch (e: Exception) {
                _error.value = "Error al editar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alterna el estado marcado/desmarcado de un producto (en carrito o pendiente).
     *
     * @param listaId  ID de la lista que contiene el producto.
     * @param itemId   ID del documento del producto.
     * @param checked  Nuevo estado: true = en carrito, false = pendiente.
     */
    fun toggleItem(listaId: String, itemId: String, checked: Boolean) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listasRef().document(listaId)
                    .collection("productos").document(itemId)
                    .update("isChecked", checked).await()
            } catch (e: Exception) {
                _error.value = "Error al actualizar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina una lista y todos sus productos en una única operación atómica (batch).
     *
     * IMPORTANTE — Por qué se usa batch:
     *  Firestore no elimina subcolecciones automáticamente al borrar un documento padre.
     *  Los productos deben borrarse explícitamente. Se usa [db.batch] para agrupar
     *  todos los borrados en una sola operación: si alguno falla, ninguno se aplica,
     *  evitando dejar datos huérfanos en la subcolección "productos".
     *
     * ADVERTENCIA: Operación irreversible. Confirmar con el usuario en la UI.
     *
     * @param listaId ID de la lista a eliminar junto con todos sus productos.
     */
    fun eliminarLista(listaId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtenemos todos los productos de la lista para incluirlos en el batch
                val productos = listasRef().document(listaId)
                    .collection("productos").get().await()

                val batch = db.batch()
                // Añadimos el borrado de cada producto al batch
                productos.documents.forEach { batch.delete(it.reference) }
                // Añadimos también el borrado del documento de la lista
                batch.delete(listasRef().document(listaId))
                // Ejecutamos todos los borrados a la vez
                batch.commit().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar la lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina un producto de una lista y decrementa el contador de la lista.
     *
     * ADVERTENCIA: Operación irreversible. Confirmar con el usuario en la UI.
     *
     * @param listaId ID de la lista que contiene el producto.
     * @param itemId  ID del documento del producto a eliminar.
     */
    fun eliminarItem(listaId: String, itemId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listasRef().document(listaId)
                    .collection("productos").document(itemId)
                    .delete().await()
                actualizarContador(listaId, delta = -1)
            } catch (e: Exception) {
                _error.value = "Error al eliminar producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Incrementa o decrementa atómicamente el campo [itemCount] de una lista.
     *
     * IMPORTANTE — Por qué se usa FieldValue.increment():
     *  Usar increment() en lugar de leer el valor, modificarlo y escribirlo
     *  evita condiciones de carrera cuando varios usuarios o dispositivos
     *  modifican la misma lista simultáneamente. Firestore garantiza que
     *  la operación es atómica en el servidor.
     *
     * @param listaId ID de la lista cuyo contador se actualiza.
     * @param delta   Valor a sumar al contador: +1 al añadir, -1 al eliminar.
     */
    private suspend fun actualizarContador(listaId: String, delta: Int) {
        try {
            listasRef().document(listaId)
                .update("itemCount", FieldValue.increment(delta.toLong()))
                .await()
        } catch (e: Exception) {
            _error.value = "Error al actualizar contador: ${e.message}"
        }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    /**
     * Se llama cuando el ViewModel va a ser destruido.
     *
     * CRÍTICO — Prevención de fugas de memoria:
     * Se cancelan los tres listeners activos de Firestore. Sin esto, seguirían
     * consumiendo red y memoria, y sus callbacks intentarían actualizar
     * StateFlows de un ViewModel ya destruido, pudiendo causar crashes.
     */
    override fun onCleared() {
        super.onCleared()
        listasListener?.remove()
        itemsListener?.remove()
        favoritosListener?.remove()
    }
}