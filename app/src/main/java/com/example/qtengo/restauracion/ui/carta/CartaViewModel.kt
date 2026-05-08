package com.example.qtengo.restauracion.ui.carta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// MODELOS DE DATOS
// Usamos data class porque Kotlin genera automáticamente equals(), hashCode()
// y toString(), muy útil para comparar objetos en los tests.
// ---------------------------------------------------------------------------

data class Plato(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val categoria: String = "Principales",
    val disponible: Boolean = true
)

data class MenuDia(
    val id: String = "",
    val primerPlato: String = "",
    val segundoPlato: String = "",
    val postre: String = "",
    val precio: Double = 0.0,
    val fecha: String = ""
)

// ---------------------------------------------------------------------------
// VIEWMODEL
// El ViewModel sobrevive a los cambios de configuración (rotar pantalla, etc.)
// y es el responsable de toda la lógica de negocio. La UI solo observa
// los StateFlow y reacciona a los cambios.
// ---------------------------------------------------------------------------

class CartaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // ✅ CORRECCIÓN BUG 1:
    // Antes: val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // El problema era que uid se capturaba UNA SOLA VEZ al crear el ViewModel.
    // Si el usuario no había iniciado sesión todavía, uid quedaba como ""
    // para siempre, aunque luego iniciara sesión correctamente.
    // Ahora uid se obtiene cada vez que se necesita mediante requireUid(),
    // garantizando que siempre tenemos el valor actualizado.

    // StateFlow de solo lectura que expone la lista de platos a la UI.
    // La UI nunca puede modificar _platos directamente, solo leerlo.
    private val _platos = MutableStateFlow<List<Plato>>(emptyList())
    val platos: StateFlow<List<Plato>> = _platos

    // StateFlow para el menú del día. Puede ser null si no hay menú guardado.
    private val _menuDia = MutableStateFlow<MenuDia?>(null)
    val menuDia: StateFlow<MenuDia?> = _menuDia

    // StateFlow para los errores. La UI lo observa y muestra un mensaje
    // cuando no es null.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Guardamos las referencias a los listeners de Firestore para poder
    // eliminarlos en onCleared() y evitar memory leaks.
    private var platosListener: ListenerRegistration? = null
    private var menuListener: ListenerRegistration? = null

    // Limpia el error una vez que la UI lo ha mostrado al usuario.
    fun clearError() { _error.value = null }

    // ✅ CORRECCIÓN BUG 1:
    // requireUid() ahora consulta FirebaseAuth en cada llamada.
    // Si el usuario no está autenticado, emite un error y devuelve null.
    // El operador ?: return en las funciones que la llaman hace que
    // la función se detenga si uid es null, evitando operaciones sin usuario.
    private fun requireUid(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _error.value = "Usuario no autenticado"
            return null
        }
        return uid
    }

    // Funciones auxiliares que construyen la referencia a la colección
    // de Firestore para el usuario actual. Al usar requireUid() dentro,
    // siempre usamos el uid actualizado.
    private fun platosRef(uid: String) =
        db.collection("usuarios").document(uid).collection("restauracion_platos")

    private fun menuRef(uid: String) =
        db.collection("usuarios").document(uid).collection("restauracion_menu")

    // ---------------------------------------------------------------------------
    // CARGA DE DATOS CON LISTENERS EN TIEMPO REAL
    // addSnapshotListener() registra un listener que se dispara automáticamente
    // cada vez que los datos cambian en Firestore. Esto soluciona el problema
    // de tener que recargar la pantalla manualmente para ver los cambios.
    // ---------------------------------------------------------------------------

    fun cargarPlatos() {
        // Si requireUid() devuelve null, el ?: return detiene la función aquí.
        val uid = requireUid() ?: return

        // Eliminamos el listener anterior si existía, para no acumular
        // múltiples listeners escuchando lo mismo.
        platosListener?.remove()

        platosListener = platosRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar carta: ${e.message}"
                    return@addSnapshotListener
                }
                // Mapeamos cada docv  umento de Firestore a un objeto Plato.
                // El operador ?: emptyList() garantiza que nunca tengamos null.
                _platos.value = snapshot?.documents?.map { doc ->
                    Plato(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        categoria = doc.getString("categoria") ?: "Principales",
                        disponible = doc.getBoolean("disponible") ?: true
                    )
                } ?: emptyList()
            }
    }

    fun cargarMenuDia() {
        val uid = requireUid() ?: return
        menuListener?.remove()

        // ✅ CORRECCIÓN BUG 4:
        // Antes el menú siempre se guardaba y leía con la clave "hoy".
        // El problema es que si el usuario no abría la app un día,
        // el menú de ayer seguía apareciendo como "el menú de hoy".
        // Ahora usamos la fecha real como ID del documento (ej: "2026-05-06"),
        // de forma que cada día tiene su propio documento en Firestore.
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        menuListener = menuRef(uid)
            .document(fechaHoy)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar menú: ${e.message}"
                    return@addSnapshotListener
                }
                _menuDia.value = snapshot?.let { doc ->
                    if (!doc.exists()) return@let null
                    MenuDia(
                        id = doc.id,
                        primerPlato = doc.getString("primerPlato") ?: "",
                        segundoPlato = doc.getString("segundoPlato") ?: "",
                        postre = doc.getString("postre") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        fecha = doc.getString("fecha") ?: ""
                    )
                }
            }
    }

    // ---------------------------------------------------------------------------
    // OPERACIONES DE ESCRITURA
    // Usamos viewModelScope.launch para ejecutar operaciones asíncronas
    // (las llamadas a Firestore) sin bloquear el hilo principal de la UI.
    // Si el ViewModel se destruye, viewModelScope cancela automáticamente
    // todas las corrutinas en curso.
    // ---------------------------------------------------------------------------

    fun añadirPlato(plato: Plato) {
        val uid = requireUid() ?: return

        // ✅ CORRECCIÓN BUG 3:
        // Antes no había ninguna validación, así que se podía guardar
        // un plato con nombre vacío o precio negativo/cero en Firestore.
        // Ahora validamos en el ViewModel antes de hacer ninguna operación.
        // Esto es la "segunda línea de defensa" — la primera debería estar
        // en la UI mostrando errores en los campos del formulario.
        if (plato.nombre.isBlank()) {
            _error.value = "El nombre del plato no puede estar vacío"
            return
        }
        if (plato.precio <= 0) {
            _error.value = "El precio debe ser mayor que 0"
            return
        }

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "nombre" to plato.nombre,
                    "descripcion" to plato.descripcion,
                    "precio" to plato.precio,
                    "categoria" to plato.categoria,
                    "disponible" to plato.disponible
                )
                // add() genera un ID automático en Firestore para el nuevo documento.
                // await() convierte la llamada asíncrona en síncrona dentro
                // de la corrutina, sin bloquear el hilo principal.
                platosRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir plato: ${e.message}"
            }
        }
    }

    fun editarPlato(platoId: String, plato: Plato) {
        val uid = requireUid() ?: return

        // Mismas validaciones que en añadirPlato para mantener consistencia.
        if (plato.nombre.isBlank()) {
            _error.value = "El nombre del plato no puede estar vacío"
            return
        }
        if (plato.precio <= 0) {
            _error.value = "El precio debe ser mayor que 0"
            return
        }

        viewModelScope.launch {
            try {
                // ✅ CORRECCIÓN BUG 2:
                // Antes faltaba el campo "disponible" en el update().
                // Esto provocaba que si editabas un plato y cambiabas
                // su disponibilidad al mismo tiempo, el cambio se perdía
                // porque update() solo modifica los campos que le pasas,
                // dejando "disponible" con su valor anterior en Firestore.
                platosRef(uid).document(platoId).update(
                    "nombre", plato.nombre,
                    "descripcion", plato.descripcion,
                    "precio", plato.precio,
                    "categoria", plato.categoria,
                    "disponible", plato.disponible  // ← campo que faltaba
                ).await()
            } catch (e: Exception) {
                _error.value = "Error al editar plato: ${e.message}"
            }
        }
    }

    // Cambia únicamente la disponibilidad de un plato sin tocar el resto de campos.
    // Útil para activar/desactivar platos rápidamente desde la UI con un switch.
    fun toggleDisponible(platoId: String, disponible: Boolean) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                platosRef(uid).document(platoId).update("disponible", disponible).await()
            } catch (e: Exception) {
                _error.value = "Error al actualizar disponibilidad: ${e.message}"
            }
        }
    }

    fun eliminarPlato(platoId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                platosRef(uid).document(platoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar plato: ${e.message}"
            }
        }
    }

    fun guardarMenuDia(menu: MenuDia) {
        val uid = requireUid() ?: return

        // ✅ CORRECCIÓN BUG 3 (también en guardarMenuDia):
        // Validamos que los campos obligatorios del menú no estén vacíos
        // y que el precio sea válido antes de guardar en Firestore.
        if (menu.primerPlato.isBlank()) {
            _error.value = "El primer plato no puede estar vacío"
            return
        }
        if (menu.precio <= 0) {
            _error.value = "El precio del menú debe ser mayor que 0"
            return
        }

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "primerPlato" to menu.primerPlato,
                    "segundoPlato" to menu.segundoPlato,
                    "postre" to menu.postre,
                    "precio" to menu.precio,
                    "fecha" to menu.fecha
                )
                // ✅ CORRECCIÓN BUG 4:
                // Usamos la fecha real como ID del documento en lugar de "hoy",
                // igual que en cargarMenuDia(). Así lectura y escritura
                // siempre apuntan al mismo documento del día correcto.
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                menuRef(uid).document(fechaHoy).set(data).await()
            } catch (e: Exception) {
                _error.value = "Error al guardar menú: ${e.message}"
            }
        }
    }

    // ---------------------------------------------------------------------------
    // LIMPIEZA DE RECURSOS
    // onCleared() se llama automáticamente cuando el ViewModel va a ser destruido
    // (por ejemplo, cuando el usuario sale de la pantalla definitivamente).
    // Es MUY importante eliminar los listeners de Firestore aquí para evitar
    // memory leaks — si no lo hacemos, Firestore seguiría enviando actualizaciones
    // a un ViewModel que ya no existe.
    // ---------------------------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        platosListener?.remove()
        menuListener?.remove()
    }
}