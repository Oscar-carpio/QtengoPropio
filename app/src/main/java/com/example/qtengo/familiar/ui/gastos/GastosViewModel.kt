package com.example.qtengo.familiar.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import kotlinx.coroutines.flow.map
import java.util.*

// Modelo de un movimiento económico (gasto o ingreso)
// tipo: "GASTO" o "INGRESO" — cualquier otro valor rompe los cálculos de totales
// origen: "manual" o "lista_compra" — permite distinguir gastos automáticos de manuales
data class Gasto(
    val id: String = "",
    val descripcion: String = "",
    val cantidad: Double = 0.0,
    val categoria: String = "",
    val tipo: String = "GASTO",
    val fecha: String = "", // Formato dd/MM/yyyy
    val origen: String = "manual",
    val listaId: String = "" // Solo relevante cuando origen = "lista_compra"
)

// Modelo de un gasto fijo mensual (suscripciones, alquiler, etc.)
// Son informativos — no se registran automáticamente en gastos cada mes
data class GastoRecurrente(
    val id: String = "",
    val descripcion: String = "",
    val cantidad: Double = 0.0,
    val categoria: String = "",
    val fechaCobro: String = "" // Texto libre: "día 1", "día 5", etc.
)

// ViewModel que gestiona gastos, ingresos, recurrentes y presupuesto del usuario.
// Todas las operaciones requieren usuario autenticado (ver requireUid).
class GastosViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // SimpleDateFormat con Locale fijo — solo usar en hilo principal
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos: StateFlow<List<Gasto>> = _gastos

    // null si el usuario aún no ha configurado un presupuesto mensual
    private val _presupuesto = MutableStateFlow<Double?>(null)
    val presupuesto: StateFlow<Double?> = _presupuesto

    // true mientras hay una operación de escritura en curso — la UI debe bloquear botones
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _gastosRecurrentes = MutableStateFlow<List<GastoRecurrente>>(emptyList())
    val gastosRecurrentes: StateFlow<List<GastoRecurrente>> = _gastosRecurrentes

    // Mapa derivado: categoría → suma total de gastos del mes
    // Se recalcula automáticamente cada vez que cambia _gastos
    val gastosPorCategoria: StateFlow<Map<String, Double>> = _gastos
        .map { lista ->
            lista.filter { it.tipo == "GASTO" }
                .groupBy { it.categoria.ifBlank { "Sin categoría" } }
                .mapValues { (_, items) -> items.sumOf { it.cantidad } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Los tres listeners deben cancelarse en onCleared() para evitar fugas de memoria
    private var gastosListener: ListenerRegistration? = null
    private var recurrentesListener: ListenerRegistration? = null
    private var presupuestoListener: ListenerRegistration? = null

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

    // ─── Filtrado por fechas ──────────────────────────────────────────────────

    // null = sin límite en ese extremo del rango
    private val _fechaInicio = MutableStateFlow<Date?>(null)
    val fechaInicio: StateFlow<Date?> = _fechaInicio

    private val _fechaFin = MutableStateFlow<Date?>(null)
    val fechaFin: StateFlow<Date?> = _fechaFin

    // Lista filtrada por rango de fechas — se recalcula automáticamente al cambiar gastos o fechas
    // Si ambas fechas son null devuelve la lista completa
    val gastosFiltrados: StateFlow<List<Gasto>> = combine(
        _gastos, _fechaInicio, _fechaFin
    ) { gastos, inicio, fin ->
        if (inicio == null && fin == null) {
            gastos
        } else {
            gastos.filter { gasto ->
                // runCatching ignora gastos con formato de fecha incorrecto sin lanzar excepción
                val fechaGasto = runCatching { sdf.parse(gasto.fecha) }.getOrNull()
                    ?: return@filter false
                val despuesDeInicio = inicio == null || !fechaGasto.before(inicio)
                val antesDeEnd = fin == null || !fechaGasto.after(fin)
                despuesDeInicio && antesDeEnd
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filtrarPorFechas(inicio: Date?, fin: Date?) {
        _fechaInicio.value = inicio
        _fechaFin.value = fin
    }

    fun limpiarFiltro() {
        _fechaInicio.value = null
        _fechaFin.value = null
    }

    // ─── Referencias Firestore ───────────────────────────────────────────────

    // usuarios/{uid}/gastos
    private fun gastosRef(uid: String) =
        db.collection("usuarios").document(uid).collection("gastos")

    // usuarios/{uid}/gastosRecurrentes
    private fun recurrentesRef(uid: String) =
        db.collection("usuarios").document(uid).collection("gastosRecurrentes")

    // Documento raíz del usuario — el presupuesto se guarda aquí con merge()
    private fun configRef(uid: String) =
        db.collection("usuarios").document(uid)

    // ─── Carga de datos ──────────────────────────────────────────────────────

    // Escucha gastos en tiempo real ordenados por fecha descendente (más reciente primero)
    fun cargarGastos() {
        val uid = requireUid() ?: return
        gastosListener?.remove()
        gastosListener = gastosRef(uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar gastos: ${e.message}"
                    return@addSnapshotListener
                }
                _gastos.value = snapshot?.documents?.map { doc ->
                    Gasto(
                        id          = doc.id,
                        descripcion = doc.getString("descripcion") ?: "",
                        cantidad    = doc.getDouble("cantidad") ?: 0.0,
                        categoria   = doc.getString("categoria") ?: "",
                        tipo        = doc.getString("tipo") ?: "GASTO",
                        fecha       = doc.getString("fecha") ?: "",
                        origen      = doc.getString("origen") ?: "manual",
                        listaId     = doc.getString("listaId") ?: ""
                    )
                } ?: emptyList()
            }
    }

    fun cargarGastosRecurrentes() {
        val uid = requireUid() ?: return
        recurrentesListener?.remove()
        recurrentesListener = recurrentesRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar gastos recurrentes: ${e.message}"
                    return@addSnapshotListener
                }
                _gastosRecurrentes.value = snapshot?.documents?.map { doc ->
                    GastoRecurrente(
                        id          = doc.id,
                        descripcion = doc.getString("descripcion") ?: "",
                        cantidad    = doc.getDouble("cantidad") ?: 0.0,
                        categoria   = doc.getString("categoria") ?: "",
                        fechaCobro  = doc.getString("fechaCobro") ?: ""
                    )
                }?.sortedBy { it.fechaCobro } ?: emptyList()
            }
    }

    // El presupuesto se escucha desde el documento raíz del usuario
    // NOTA: este listener se dispara ante cualquier cambio en el documento,
    // no solo en el campo presupuestoMensual
    fun cargarPresupuesto() {
        val uid = requireUid() ?: return
        presupuestoListener?.remove()
        presupuestoListener = configRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar presupuesto: ${e.message}"
                    return@addSnapshotListener
                }
                _presupuesto.value = snapshot?.getDouble("presupuestoMensual")
            }
    }

    // ─── Escritura ───────────────────────────────────────────────────────────

    // merge() actualiza solo el campo presupuestoMensual sin sobreescribir el resto del documento
    fun guardarPresupuesto(cantidad: Double) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                configRef(uid).set(
                    mapOf("presupuestoMensual" to cantidad),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                _error.value = "Error al guardar presupuesto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun añadirGasto(descripcion: String, cantidad: Double, categoria: String, tipo: String) {
        val uid = requireUid() ?: return
        // Validamos el tipo para evitar valores incorrectos que romperían los cálculos de totales
        if (tipo != "GASTO" && tipo != "INGRESO") {
            _error.value = "Tipo de movimiento no válido. Debe ser GASTO o INGRESO."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())
                val data = mapOf(
                    "descripcion" to descripcion,
                    "cantidad"    to cantidad,
                    "categoria"   to categoria,
                    "tipo"        to tipo,
                    "fecha"       to fecha,
                    "origen"      to "manual",
                    "listaId"     to ""
                )
                gastosRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir gasto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun añadirGastoRecurrente(descripcion: String, cantidad: Double, categoria: String, fechaCobro: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "descripcion" to descripcion,
                    "cantidad"    to cantidad,
                    "categoria"   to categoria,
                    "fechaCobro"  to fechaCobro
                )
                recurrentesRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir gasto recurrente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Solo permite editar descripción, cantidad y categoría — no el tipo ni la fecha
    fun editarGasto(gastoId: String, descripcion: String, cantidad: Double, categoria: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                gastosRef(uid).document(gastoId).update(
                    mapOf(
                        "descripcion" to descripcion,
                        "cantidad"    to cantidad,
                        "categoria"   to categoria
                    )
                ).await()
            } catch (e: Exception) {
                _error.value = "Error al editar gasto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarGastoRecurrente(gastoId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recurrentesRef(uid).document(gastoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar gasto recurrente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editarGastoRecurrente(
        gastoId: String,
        descripcion: String,
        cantidad: Double,
        categoria: String,
        fechaCobro: String
    ) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recurrentesRef(uid).document(gastoId).update(
                    mapOf(
                        "descripcion" to descripcion,
                        "cantidad"    to cantidad,
                        "categoria"   to categoria,
                        "fechaCobro"  to fechaCobro
                    )
                ).await()
            } catch (e: Exception) {
                _error.value = "Error al editar gasto recurrente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Registra automáticamente un gasto vinculado a una lista de compra completada
    // El campo listaId permite rastrear qué lista generó el gasto y evitar duplicados
    fun registrarGastoDesdeLista(listaId: String, nombreLista: String, cantidad: Double) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())
                val data = mapOf(
                    "descripcion" to "Compra: $nombreLista",
                    "cantidad"    to cantidad,
                    "categoria"   to "Alimentación",
                    "tipo"        to "GASTO",
                    "fecha"       to fecha,
                    "origen"      to "lista_compra",
                    "listaId"     to listaId
                )
                gastosRef(uid).add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al registrar gasto desde lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarGasto(gastoId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                gastosRef(uid).document(gastoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar gasto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Totales ─────────────────────────────────────────────────────────────

    // Total de gastos del mes en curso
    // Depende del formato dd/MM/yyyy — gastos con otro formato quedan excluidos silenciosamente
    fun totalGastos(): Double {
        val mesActual = SimpleDateFormat("MM/yyyy", Locale("es", "ES")).format(Date())
        return _gastos.value
            .filter { it.tipo == "GASTO" && it.fecha.endsWith(mesActual) }
            .sumOf { it.cantidad }
    }

    fun totalRecurrentes(): Double = _gastosRecurrentes.value.sumOf { it.cantidad }

    // Total de ingresos del mes en curso — filtrado igual que totalGastos()
    // para que el balance sea consistente y no mezcle ingresos históricos con gastos actuales
    fun totalIngresos(): Double {
        val mesActual = SimpleDateFormat("MM/yyyy", Locale("es", "ES")).format(Date())
        return _gastos.value
            .filter { it.tipo == "INGRESO" && it.fecha.endsWith(mesActual) }
            .sumOf { it.cantidad }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    // Cancelamos los tres listeners al destruir el ViewModel para evitar fugas de memoria
    override fun onCleared() {
        super.onCleared()
        gastosListener?.remove()
        recurrentesListener?.remove()
        presupuestoListener?.remove()
    }
}