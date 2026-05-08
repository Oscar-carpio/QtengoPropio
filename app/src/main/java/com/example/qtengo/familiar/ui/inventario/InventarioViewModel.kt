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

/**
 * Modelo de datos que representa un artículo del inventario familiar.
 *
 * IMPORTANTE: Todos los campos tienen valores por defecto para que Firestore
 * pueda deserializar documentos con campos faltantes sin lanzar excepciones.
 *
 * @param id             ID único del documento en Firestore (asignado automáticamente).
 * @param nombre         Nombre del artículo (obligatorio en UI).
 * @param cantidad       Stock actual. Siempre >= 0; la UI debe impedirlo negativamente.
 * @param minStock       Umbral mínimo de stock. Cuando cantidad < minStock el artículo
 *                       debería marcarse como "bajo stock" en la UI.
 * @param fechaCaducidad Nullable: si el artículo no caduca, este campo no existe en
 *                       Firestore. Ver [editarItem] para la gestión del borrado del campo.
 */
data class InventarioItem(
    val id: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val ubicacion: String = "",
    val minStock: Int = 1,
    val notas: String = "",
    val fechaCaducidad: String? = null
)

/**
 * ViewModel para la gestión del inventario del usuario autenticado.
 *
 * Responsabilidades:
 *  - Leer artículos de Firestore en tiempo real mediante un listener activo.
 *  - Crear, editar, eliminar artículos y actualizar cantidades.
 *  - Gestionar el ciclo de vida del listener para evitar fugas de memoria.
 *  - Exponer errores a la UI a través de [error] StateFlow.
 *
 * SEGURIDAD: Todas las operaciones pasan por [requireUid] para garantizar
 * que el usuario esté autenticado antes de acceder a Firestore.
 */
open class InventarioViewModel : ViewModel() {

    // ─── Dependencias ────────────────────────────────────────────────────────

    /** Instancia de Firestore. Punto único de acceso a la base de datos. */
    private val db = FirebaseFirestore.getInstance()

    /**
     * UID del usuario autenticado, capturado al crear el ViewModel.
     * IMPORTANTE: Si la sesión se cierra mientras el ViewModel está activo,
     * este valor quedará obsoleto. [requireUid] protege contra uid vacío,
     * pero no detecta revocación de sesión en tiempo de ejecución.
     */
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ─── Estado observable ───────────────────────────────────────────────────

    /** Lista de artículos del inventario. Solo lectura desde la UI. */
    private val _items = MutableStateFlow<List<InventarioItem>>(emptyList())
    open val items: StateFlow<List<InventarioItem>> = _items

    /**
     * Canal de errores para la UI. Emite mensajes legibles cuando ocurre
     * cualquier fallo (Firestore, autenticación, etc.).
     * La UI debe llamar a [clearError] tras mostrar el mensaje para evitar
     * que se repita en recomposiciones.
     */
    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error

    /**
     * Referencia al listener activo de Firestore.
     * CRÍTICO: Debe cancelarse en [onCleared] para evitar fugas de memoria
     * y callbacks que lleguen sobre un ViewModel ya destruido.
     */
    private var itemsListener: ListenerRegistration? = null

    /** Limpia el error actual. Llamar desde la UI tras mostrar el mensaje. */
    fun clearError() { _error.value = null }

    // ─── Seguridad / Autenticación ───────────────────────────────────────────

    /**
     * Guard centralizado de autenticación.
     *
     * SEGURIDAD: Ninguna operación de lectura/escritura debe ejecutarse sin
     * pasar por aquí. Devuelve null y emite error en [_error] si el UID está
     * vacío, impidiendo accesos no autorizados a colecciones de Firestore.
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
     * Referencia a la subcolección "inventario" del usuario en Firestore.
     * Ruta resultante: usuarios/{uid}/inventario
     *
     * NOTA: Los callers deben haber comprobado el UID antes de usar esta referencia.
     */
    private fun inventarioRef() = db.collection("usuarios").document(uid).collection("inventario")

    // ─── Carga de datos ──────────────────────────────────────────────────────

    /**
     * Suscribe un listener en tiempo real a la colección de inventario del usuario.
     *
     * IMPORTANTE — Gestión del listener:
     *  - Cancela el listener previo antes de crear uno nuevo para evitar duplicados
     *    si se llama varias veces (p.ej. por rotación de pantalla).
     *  - El listener permanece activo hasta que se llame a [onCleared].
     *
     * NOTA: La lista resultante no tiene un orden explícito definido aquí.
     * Si se requiere ordenación (por nombre, caducidad, etc.) debe añadirse
     * un comparador igual que en TareasViewModel.
     */
    fun cargarItems() {
        val uid = requireUid() ?: return

        // Cancelamos el listener anterior antes de crear uno nuevo
        itemsListener?.remove()
        itemsListener = db.collection("usuarios").document(uid).collection("inventario")
            .addSnapshotListener { snapshot, e ->
                // Si Firestore devuelve un error, lo propagamos a la UI y salimos
                if (e != null) {
                    _error.value = "Error al cargar inventario: ${e.message}"
                    return@addSnapshotListener
                }

                // Mapeamos cada documento Firestore a un objeto InventarioItem.
                // getLong() es necesario porque Firestore almacena números enteros como Long,
                // por lo que se hace la conversión explícita a Int.
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

    /**
     * Crea un nuevo artículo en el inventario de Firestore.
     *
     * IMPORTANTE — Gestión de fechaCaducidad:
     *  - Si [fechaCaducidad] es null o está en blanco, el campo NO se incluye
     *    en el documento (a diferencia de [editarItem], donde sí hay que borrarlo
     *    explícitamente con FieldValue.delete() si ya existía antes).
     *  - Se usa [mutableMapOf] porque la clave "fechaCaducidad" se añade condicionalmente.
     *
     * @param nombre         Nombre del artículo (no debe estar vacío — validar en UI).
     * @param cantidad       Stock inicial (>= 0).
     * @param minStock       Umbral mínimo de stock para alertas (>= 1 recomendado).
     * @param fechaCaducidad Opcional. Formato esperado definido por la UI (p.ej. "dd/MM/yyyy").
     */
    fun añadirItem(
        nombre: String,
        cantidad: Int,
        ubicacion: String,
        minStock: Int,
        notas: String,
        fechaCaducidad: String?
    ) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                // Construimos el mapa de forma mutable para añadir fechaCaducidad solo si tiene valor
                val data = mutableMapOf<String, Any>(
                    "nombre"    to nombre,
                    "cantidad"  to cantidad,
                    "ubicacion" to ubicacion,
                    "minStock"  to minStock,
                    "notas"     to notas
                )
                // Solo incluimos el campo si la fecha tiene contenido real
                if (!fechaCaducidad.isNullOrBlank()) {
                    data["fechaCaducidad"] = fechaCaducidad
                }
                inventarioRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir artículo: ${e.message}"
            }
        }
    }

    /**
     * Actualiza todos los campos de un artículo existente en Firestore.
     *
     * IMPORTANTE — Borrado de fechaCaducidad:
     *  - Si [fechaCaducidad] es null, se usa [FieldValue.delete()] para eliminar
     *    el campo del documento de Firestore. Esto es necesario porque un update()
     *    con valor null dejaría el campo con valor nulo en lugar de borrarlo,
     *    lo que puede causar problemas en la lógica de lectura.
     *  - Si [fechaCaducidad] tiene valor, simplemente se sobreescribe.
     *
     * @param itemId ID del documento en Firestore a actualizar.
     */
    fun editarItem(
        itemId: String,
        nombre: String,
        cantidad: Int,
        ubicacion: String,
        minStock: Int,
        notas: String,
        fechaCaducidad: String?
    ) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                val data = hashMapOf<String, Any>(
                    "nombre"         to nombre,
                    "cantidad"       to cantidad,
                    "ubicacion"      to ubicacion,
                    "minStock"       to minStock,
                    "notas"         to notas,
                    // FieldValue.delete() elimina el campo de Firestore si fechaCaducidad es null
                    "fechaCaducidad" to (fechaCaducidad ?: FieldValue.delete())
                )
                inventarioRef().document(itemId).update(data).await()
            } catch (e: Exception) {
                _error.value = "Error al editar artículo: ${e.message}"
            }
        }
    }

    /**
     * Elimina permanentemente un artículo del inventario en Firestore.
     *
     * ADVERTENCIA: Operación irreversible. No hay papelera de reciclaje.
     * Pedir confirmación al usuario en la UI antes de llamar este método.
     *
     * @param itemId ID del documento a eliminar.
     */
    fun eliminarItem(itemId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                inventarioRef().document(itemId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar artículo: ${e.message}"
            }
        }
    }

    /**
     * Actualiza únicamente el campo "cantidad" de un artículo.
     *
     * Se usa para operaciones rápidas de ajuste de stock (incrementar/decrementar)
     * sin necesidad de editar el resto de campos del artículo.
     *
     * NOTA: No valida que [nuevaCantidad] sea >= 0. La UI debe impedir
     * valores negativos antes de llamar este método.
     *
     * @param itemId       ID del documento en Firestore.
     * @param nuevaCantidad Nuevo valor de stock a guardar.
     */
    fun actualizarCantidad(itemId: String, nuevaCantidad: Int) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                inventarioRef().document(itemId).update("cantidad", nuevaCantidad).await()
            } catch (e: Exception) {
                _error.value = "Error al actualizar cantidad: ${e.message}"
            }
        }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    /**
     * Se llama cuando el ViewModel va a ser destruido.
     *
     * CRÍTICO — Prevención de fugas de memoria:
     * Eliminamos el listener de Firestore aquí. Sin esto, el listener seguiría
     * activo en segundo plano, consumiendo red y memoria, y sus callbacks
     * intentarían actualizar StateFlows de un ViewModel ya destruido.
     */
    override fun onCleared() {
        super.onCleared()
        itemsListener?.remove()
    }
}