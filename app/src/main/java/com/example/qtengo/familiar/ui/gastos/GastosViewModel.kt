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

/**
 * Representa un movimiento económico (gasto o ingreso) del usuario.
 *
 * @param tipo     Valores esperados: "GASTO" o "INGRESO". Determina cómo se
 *                 contabiliza en [totalGastos] y [totalIngresos].
 * @param origen   "manual" si lo creó el usuario directamente, "lista_compra"
 *                 si se generó desde una lista de compra. Útil para filtrar y
 *                 evitar duplicados.
 * @param listaId  Solo relevante cuando origen = "lista_compra". Permite
 *                 rastrear qué lista generó el gasto.
 * @param fecha    Formato esperado: "dd/MM/yyyy". Debe ser consistente porque
 *                 [gastosFiltrados] y [totalGastos] parsean esta cadena.
 */
data class Gasto(
    val id: String = "",
    val descripcion: String = "",
    val cantidad: Double = 0.0,
    val categoria: String = "",
    val tipo: String = "GASTO",
    val fecha: String = "",
    val origen: String = "manual",
    val listaId: String = ""
)

/**
 * Representa un gasto fijo que se repite mensualmente (suscripciones, alquiler, etc.).
 *
 * NOTA: Los gastos recurrentes no se registran automáticamente como [Gasto] cada mes.
 * Son únicamente informativos para calcular [totalRecurrentes] y mostrarlos en la UI.
 *
 * @param fechaCobro Día del mes en que se suele cobrar (cadena libre, p.ej. "día 5").
 */
data class GastoRecurrente(
    val id: String = "",
    val descripcion: String = "",
    val cantidad: Double = 0.0,
    val categoria: String = "",
    val fechaCobro: String = ""
)

/**
 * ViewModel principal para la gestión económica del usuario.
 *
 * Responsabilidades:
 *  - Leer/escribir gastos, ingresos y gastos recurrentes en Firestore en tiempo real.
 *  - Mantener y exponer el presupuesto mensual del usuario.
 *  - Proporcionar datos derivados: gastos por categoría, totales y lista filtrada por fechas.
 *  - Gestionar el ciclo de vida de tres listeners de Firestore para evitar fugas de memoria.
 *  - Exponer errores a la UI a través de [error] StateFlow.
 *
 * SEGURIDAD: Todas las operaciones verifican la autenticación mediante [requireUid].
 */
open class GastosViewModel : ViewModel() {

    // ─── Dependencias ────────────────────────────────────────────────────────

    /** Instancia de Firestore. Punto único de acceso a la base de datos. */
    private val db = FirebaseFirestore.getInstance()

    /**
     * UID del usuario autenticado, capturado al crear el ViewModel.
     * IMPORTANTE: Si la sesión se cierra mientras el ViewModel está activo,
     * este valor quedará obsoleto. Ver [requireUid].
     */
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    /**
     * Formateador de fechas compartido con Locale fijo es_ES.
     * IMPORTANTE: SimpleDateFormat NO es thread-safe. Aquí es seguro porque
     * solo se usa en el hilo principal (StateFlow / collect). Si se usase
     * en coroutines paralelas habría que crear instancias locales.
     */
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    // ─── Estado observable ───────────────────────────────────────────────────

    /**
     * Lista completa de gastos e ingresos del usuario (sin filtrar).
     * Privado (escritura solo desde el ViewModel), público en solo lectura.
     */
    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    open val gastos: StateFlow<List<Gasto>> = _gastos

    /**
     * Presupuesto mensual definido por el usuario. Null si aún no se ha configurado.
     * Se almacena en el documento raíz del usuario (no en una subcolección).
     */
    private val _presupuesto = MutableStateFlow<Double?>(null)
    open val presupuesto: StateFlow<Double?> = _presupuesto

    /**
     * Indicador de carga para operaciones de escritura.
     * La UI debe deshabilitar los botones de acción mientras sea true
     * para evitar envíos duplicados.
     */
    private val _isLoading = MutableStateFlow(false)
    open val isLoading: StateFlow<Boolean> = _isLoading

    /** Lista de gastos fijos recurrentes del usuario. */
    private val _gastosRecurrentes = MutableStateFlow<List<GastoRecurrente>>(emptyList())
    open val gastosRecurrentes: StateFlow<List<GastoRecurrente>> = _gastosRecurrentes

    /**
     * Mapa derivado: categoría → suma total de gastos (excluye ingresos).
     *
     * Se recalcula automáticamente cada vez que cambia [_gastos].
     * - Agrupa solo los movimientos de tipo "GASTO".
     * - Las entradas sin categoría se agrupan bajo "Sin categoría".
     * - WhileSubscribed(5000): el flow se mantiene activo 5 segundos después
     *   de que la UI deje de observarlo, evitando recálculos innecesarios en
     *   rotaciones de pantalla rápidas.
     */
    open val gastosPorCategoria: StateFlow<Map<String, Double>> = _gastos
        .map { lista ->
            lista.filter { it.tipo == "GASTO" }
                .groupBy { it.categoria.ifBlank { "Sin categoría" } }
                .mapValues { (_, items) -> items.sumOf { it.cantidad } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Canal de errores para la UI. La UI debe llamar a [clearError] tras mostrar
     * el mensaje para evitar que se repita en recomposiciones.
     */
    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error

    /**
     * Referencias a los tres listeners activos de Firestore.
     * CRÍTICO: Los tres deben cancelarse en [onCleared] para evitar
     * fugas de memoria y callbacks huérfanos.
     */
    private var gastosListener: ListenerRegistration? = null
    private var recurrentesListener: ListenerRegistration? = null
    private var presupuestoListener: ListenerRegistration? = null

    /** Limpia el error actual. Llamar desde la UI tras mostrar el mensaje. */
    fun clearError() { _error.value = null }

    // ─── Seguridad / Autenticación ───────────────────────────────────────────

    /**
     * Guard centralizado de autenticación.
     *
     * SEGURIDAD: Ninguna operación de lectura/escritura debe ejecutarse sin
     * pasar por aquí. Devuelve null y emite error en [_error] si el UID está vacío.
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

    // ─── Filtrado por fechas ──────────────────────────────────────────────────

    /**
     * Fecha de inicio del filtro activo. Null = sin límite inferior.
     * Se combina con [_fechaFin] para calcular [gastosFiltrados].
     */
    private val _fechaInicio = MutableStateFlow<Date?>(null)
    open val fechaInicio: StateFlow<Date?> = _fechaInicio

    /** Fecha de fin del filtro activo. Null = sin límite superior. */
    private val _fechaFin = MutableStateFlow<Date?>(null)
    open val fechaFin: StateFlow<Date?> = _fechaFin

    /**
     * Lista de gastos filtrada por el rango [_fechaInicio] — [_fechaFin].
     *
     * Se recalcula automáticamente cuando cambian [_gastos], [_fechaInicio] o [_fechaFin].
     * Si ambas fechas son null, devuelve la lista completa sin filtrar.
     *
     * NOTA: El parseo de fecha usa runCatching para ignorar entradas con formato
     * incorrecto (return@filter false) sin lanzar excepciones que rompan el flow.
     */
    open val gastosFiltrados: StateFlow<List<Gasto>> = combine(
        _gastos, _fechaInicio, _fechaFin
    ) { gastos, inicio, fin ->
        if (inicio == null && fin == null) {
            gastos // Sin filtro activo → lista completa
        } else {
            gastos.filter { gasto ->
                val fechaGasto = runCatching { sdf.parse(gasto.fecha) }.getOrNull()
                    ?: return@filter false // Ignoramos gastos con fecha inválida
                val despuesDeInicio = inicio == null || !fechaGasto.before(inicio)
                val antesDeEnd = fin == null || !fechaGasto.after(fin)
                despuesDeInicio && antesDeEnd
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Aplica un filtro de fechas a [gastosFiltrados].
     * Cualquier valor null desactiva ese límite del rango.
     */
    fun filtrarPorFechas(inicio: Date?, fin: Date?) {
        _fechaInicio.value = inicio
        _fechaFin.value = fin
    }

    /** Elimina el filtro de fechas y vuelve a mostrar todos los gastos. */
    fun limpiarFiltro() {
        _fechaInicio.value = null
        _fechaFin.value = null
    }

    // ─── Referencias Firestore ───────────────────────────────────────────────

    /**
     * Referencias a las colecciones/documentos del usuario en Firestore:
     *  - gastos:             usuarios/{uid}/gastos
     *  - gastosRecurrentes:  usuarios/{uid}/gastosRecurrentes
     *  - config (presupuesto): usuarios/{uid}  ← documento raíz del usuario
     *
     * NOTA: El presupuesto se guarda directamente en el documento raíz del usuario,
     * no en una subcolección. Esto simplifica la lectura pero significa que
     * [presupuestoListener] escucha cambios en TODO el documento del usuario.
     */
    private fun gastosRef() = db.collection("usuarios").document(uid).collection("gastos")
    private fun recurrentesRef() = db.collection("usuarios").document(uid).collection("gastosRecurrentes")
    private fun configRef() = db.collection("usuarios").document(uid)

    // ─── Carga de datos ──────────────────────────────────────────────────────

    /**
     * Suscribe un listener en tiempo real a la colección de gastos.
     *
     * IMPORTANTE:
     *  - Los gastos se ordenan por fecha descendente directamente en la query de Firestore
     *    (más eficiente que ordenar en cliente).
     *  - Cancela el listener previo antes de crear uno nuevo para evitar duplicados.
     */
    fun cargarGastos() {
        val uid = requireUid() ?: return

        gastosListener?.remove()
        gastosListener = db.collection("usuarios").document(uid).collection("gastos")
            .orderBy("fecha", Query.Direction.DESCENDING) // Orden descendente: más reciente primero
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

    /**
     * Suscribe un listener en tiempo real a la colección de gastos recurrentes.
     *
     * NOTA: El ordenamiento se hace en cliente (sortedBy fechaCobro) porque
     * la colección se espera pequeña. Si creciera mucho, conviene mover el
     * orderBy a la query de Firestore igual que en [cargarGastos].
     */
    fun cargarGastosRecurrentes() {
        val uid = requireUid() ?: return

        recurrentesListener?.remove()
        recurrentesListener = db.collection("usuarios").document(uid).collection("gastosRecurrentes")
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

    /**
     * Suscribe un listener al documento raíz del usuario para leer el presupuesto mensual.
     *
     * ADVERTENCIA: Este listener escucha cambios en TODO el documento del usuario,
     * no solo en el campo "presupuestoMensual". Si en el futuro se añaden más campos
     * al documento raíz, este listener se disparará por esos cambios también.
     * Considerar mover el presupuesto a un documento separado si esto supone un problema.
     */
    fun cargarPresupuesto() {
        val uid = requireUid() ?: return

        presupuestoListener?.remove()
        presupuestoListener = db.collection("usuarios").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar presupuesto: ${e.message}"
                    return@addSnapshotListener
                }
                _presupuesto.value = snapshot?.getDouble("presupuestoMensual")
            }
    }

    // ─── Escritura ───────────────────────────────────────────────────────────

    /**
     * Guarda o actualiza el presupuesto mensual en el documento raíz del usuario.
     *
     * IMPORTANTE — SetOptions.merge():
     *  - Se usa merge() para actualizar SOLO el campo "presupuestoMensual" sin
     *    sobreescribir el resto del documento del usuario (nombre, email, etc.).
     *  - Sin merge(), set() reemplazaría el documento completo con solo este campo.
     *
     * @param cantidad Importe del presupuesto mensual (debe ser > 0, validar en UI).
     */
    fun guardarPresupuesto(cantidad: Double) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                configRef().set(
                    mapOf("presupuestoMensual" to cantidad),
                    SetOptions.merge() // Crucial: no sobreescribe el documento completo
                ).await()
            } catch (e: Exception) {
                _error.value = "Error al guardar presupuesto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Registra un nuevo gasto o ingreso con la fecha actual del sistema.
     *
     * NOTA: La fecha se genera en el momento de la llamada con Locale es_ES.
     * Si el usuario pudiera elegir la fecha manualmente, habría que añadirla
     * como parámetro y validar su formato antes de guardar.
     *
     * @param tipo Debe ser "GASTO" o "INGRESO". Un valor incorrecto no causará
     *             error en Firestore pero romperá los cálculos de [totalGastos]
     *             y [totalIngresos].
     */
    fun añadirGasto(descripcion: String, cantidad: Double, categoria: String, tipo: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fecha del día actual en formato dd/MM/yyyy con Locale fijo
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
                gastosRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir gasto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Registra un nuevo gasto recurrente (suscripción, alquiler, etc.).
     *
     * NOTA: Los gastos recurrentes son informativos; no se añaden automáticamente
     * a [gastos] cada mes. Si se desea automatización habría que implementar
     * un Cloud Function o un Worker periódico.
     *
     * @param fechaCobro Texto libre indicando cuándo se cobra (p.ej. "día 1 de mes").
     */
    fun añadirGastoRecurrente(descripcion: String, cantidad: Double, categoria: String, fechaCobro: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "descripcion" to descripcion,
                    "cantidad"    to cantidad,
                    "categoria"   to categoria,
                    "fechaCobro"  to fechaCobro
                )
                recurrentesRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al añadir gasto recurrente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza descripción, cantidad y categoría de un gasto existente.
     *
     * NOTA: No permite cambiar el tipo (GASTO/INGRESO) ni la fecha del gasto.
     * Si se necesitara editar esos campos, habría que añadirlos al mapa de update.
     */
    fun editarGasto(gastoId: String, descripcion: String, cantidad: Double, categoria: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                gastosRef().document(gastoId).update(
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

    /**
     * Elimina permanentemente un gasto recurrente.
     *
     * ADVERTENCIA: Operación irreversible. Confirmar con el usuario en la UI.
     */
    fun eliminarGastoRecurrente(gastoId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recurrentesRef().document(gastoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar gasto recurrente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Actualiza todos los campos de un gasto recurrente existente. */
    fun editarGastoRecurrente(
        gastoId: String,
        descripcion: String,
        cantidad: Double,
        categoria: String,
        fechaCobro: String
    ) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recurrentesRef().document(gastoId).update(
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

    /**
     * Registra automáticamente un gasto vinculado a una lista de compra completada.
     *
     * IMPORTANTE:
     *  - El campo "origen" se fija a "lista_compra" para distinguirlo de los gastos manuales.
     *  - El campo "listaId" permite rastrear qué lista generó el gasto y evitar duplicados
     *    (la UI debería comprobar si ya existe un gasto con ese listaId antes de llamar aquí).
     *  - La categoría se fija a "Alimentación" de forma automática. Si en el futuro se
     *    permiten listas de otras categorías, habría que pasarla como parámetro.
     *
     * @param listaId     ID de la lista de compra que genera el gasto.
     * @param nombreLista Nombre de la lista, usado como descripción del gasto.
     * @param cantidad    Total gastado en la compra.
     */
    fun registrarGastoDesdeLista(listaId: String, nombreLista: String, cantidad: Double) {
        requireUid() ?: return
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
                    "origen"      to "lista_compra", // Distingue gastos automáticos de manuales
                    "listaId"     to listaId          // Trazabilidad con la lista de compra
                )
                gastosRef().add(data).await()
            } catch (e: Exception) {
                _error.value = "Error al registrar gasto desde lista: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina permanentemente un gasto o ingreso.
     *
     * ADVERTENCIA: Operación irreversible. Confirmar con el usuario en la UI
     * antes de llamar este método.
     */
    fun eliminarGasto(gastoId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                gastosRef().document(gastoId).delete().await()
            } catch (e: Exception) {
                _error.value = "Error al eliminar gasto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Totales ─────────────────────────────────────────────────────────────

    /**
     * Calcula el total de gastos del mes en curso.
     *
     * IMPORTANTE — Filtrado por mes:
     *  - Filtra usando endsWith("MM/yyyy"), lo que significa que depende de que
     *    el formato de fecha almacenado sea estrictamente "dd/MM/yyyy".
     *    Si algún gasto tiene un formato diferente, quedará excluido silenciosamente.
     *  - Solo suma movimientos de tipo "GASTO" (excluye ingresos).
     *
     * NOTA: Este método accede a _gastos.value directamente (snapshot actual),
     * no es reactivo. Para un valor reactivo, usar [gastosPorCategoria] como
     * referencia y crear un StateFlow equivalente.
     */
    fun totalGastos(): Double {
        val mesActual = SimpleDateFormat("MM/yyyy", Locale("es", "ES")).format(Date())
        return _gastos.value
            .filter { it.tipo == "GASTO" && it.fecha.endsWith(mesActual) }
            .sumOf { it.cantidad }
    }

    /**
     * Suma el importe de todos los gastos recurrentes registrados.
     *
     * Representa la carga fija mensual estimada del usuario.
     * No filtra por mes porque los recurrentes no tienen fecha específica de mes.
     */
    fun totalRecurrentes(): Double = _gastosRecurrentes.value.sumOf { it.cantidad }

    /**
     * Suma todos los ingresos registrados (sin filtro de mes).
     *
     * NOTA: A diferencia de [totalGastos], este método no filtra por mes.
     * Si se necesita el total de ingresos del mes actual, aplicar el mismo
     * filtro endsWith("MM/yyyy") que en [totalGastos].
     */
    fun totalIngresos(): Double = _gastos.value
        .filter { it.tipo == "INGRESO" }
        .sumOf { it.cantidad }

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
        gastosListener?.remove()
        recurrentesListener?.remove()
        presupuestoListener?.remove()
    }
}