package com.example.qtengo.familiar.ui.tareas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Modelo de datos que representa una tarea del usuario.
 *
 * IMPORTANTE: Los valores por defecto permiten que Firestore deserialice
 * correctamente los documentos incluso si faltan campos en la base de datos.
 * No eliminar los valores por defecto sin revisar la lógica de mapeo en [cargarTareas].
 *
 * @param id        ID único del documento en Firestore (asignado automáticamente).
 * @param titulo    Texto principal de la tarea (obligatorio en UI, pero no forzado aquí).
 * @param prioridad Valores esperados: "Alta", "Media", "Baja". Cualquier otro valor
 *                  se trata como prioridad desconocida (orden 3) en el comparador de [cargarTareas].
 */
data class Tarea(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val completada: Boolean = false,
    val prioridad: String = "Media"
)

/**
 * ViewModel principal para la gestión de tareas del usuario autenticado.
 *
 * Responsabilidades:
 *  - Leer/escribir tareas en Firestore en tiempo real.
 *  - Gestionar el ciclo de vida del listener de Firestore para evitar fugas de memoria.
 *  - Programar/cancelar notificaciones locales mediante WorkManager.
 *  - Exponer errores a la UI a través de [error] StateFlow.
 *
 * SEGURIDAD: Todas las operaciones verifican que el usuario esté autenticado
 * mediante [requireUid] antes de acceder a Firestore.
 */
open class TareasViewModel(application: Application) : AndroidViewModel(application) {

    // ─── Dependencias ────────────────────────────────────────────────────────

    /** Instancia de Firestore. Punto único de acceso a la base de datos. */
    private val db = FirebaseFirestore.getInstance()

    /**
     * UID del usuario autenticado, obtenido en el momento de creación del ViewModel.
     * IMPORTANTE: Si el usuario cierra sesión mientras el ViewModel está activo,
     * este valor quedará obsoleto. [requireUid] protege contra el caso de uid vacío,
     * pero no detecta una sesión revocada en tiempo de ejecución.
     */
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    /** WorkManager para programar notificaciones locales diferidas. */
    private val workManager = WorkManager.getInstance(application)

    // ─── Estado observable ───────────────────────────────────────────────────

    /** Lista de tareas del usuario, ordenada por estado y prioridad. Solo lectura desde la UI. */
    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    open val tareas: StateFlow<List<Tarea>> = _tareas

    /**
     * Canal de errores para la UI. Emite un mensaje legible cuando ocurre cualquier
     * fallo (Firestore, autenticación, notificaciones). La UI debe llamar a [clearError]
     * tras mostrar el mensaje para evitar que se repita en recomposiciones.
     */
    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error

    /**
     * Referencia al listener activo de Firestore.
     * CRÍTICO: Debe cancelarse en [onCleared] para evitar fugas de memoria y
     * callbacks huérfanos que intenten actualizar un ViewModel destruido.
     */
    private var tareasListener: ListenerRegistration? = null

    /** Limpia el error actual. Debe llamarse desde la UI tras mostrar el mensaje. */
    fun clearError() { _error.value = null }

    // ─── Seguridad / Autenticación ───────────────────────────────────────────

    /**
     * Guard centralizado de autenticación.
     */
    private fun requireUid(): String? {
        if (uid.isBlank()) {
            _error.value = "Usuario no autenticado. Por favor, inicia sesión de nuevo."
            return null
        }
        return uid
    }

    /**
     * Referencia a la subcolección "tareas" del usuario en Firestore.
     * Ruta resultante: usuarios/{uid}/tareas
     */
    private fun tareasRef() = db.collection("usuarios").document(uid).collection("tareas")

    // ─── Carga de datos ──────────────────────────────────────────────────────

    /**
     * Suscribe un listener en tiempo real a la colección de tareas del usuario.
     *
     * IMPORTANTE — Gestión del listener:
     *  - Cancela el listener previo antes de crear uno nuevo para evitar duplicados
     */
    fun cargarTareas() {
        val uid = requireUid() ?: return

        // Cancelamos el listener anterior antes de crear uno nuevo
        tareasListener?.remove()
        tareasListener = db.collection("usuarios").document(uid).collection("tareas")
            .addSnapshotListener { snapshot, e ->
                // Si Firestore devuelve un error, lo propagamos a la UI y salimos
                if (e != null) {
                    _error.value = "Error al cargar tareas: ${e.message}"
                    return@addSnapshotListener
                }

                // Mapeamos cada documento Firestore a un objeto Tarea
                val result = snapshot?.documents?.map { doc ->
                    Tarea(
                        id = doc.id,
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        fecha = doc.getString("fecha") ?: "",
                        completada = doc.getBoolean("completada") ?: false,
                        prioridad = doc.getString("prioridad") ?: "Media"
                    )
                } ?: emptyList()

                // Ordenamos: pendientes primero, luego por prioridad
                _tareas.value = result.sortedWith(
                    compareBy<Tarea> { it.completada }
                        .thenBy {
                            when (it.prioridad) {
                                "Alta"  -> 0
                                "Media" -> 1
                                "Baja"  -> 2
                                else    -> 3 // Prioridad desconocida va al final
                            }
                        }
                )
            }
    }

    // ─── Escritura ───────────────────────────────────────────────────────────

    /**
     * Crea una nueva tarea en Firestore y programa su notificación.
     *
     * El documento se añade con ID automático generado por Firestore.
     * Tras la escritura exitosa, se llama a [programarNotificacion] usando
     * el ID real del documento para poder cancelarla más tarde.
     *
     * @param titulo      Texto principal (no debe estar vacío — validar en UI).
     * @param descripcion Detalle opcional de la tarea.
     * @param fecha       Fecha en formato "dd/MM/yyyy". Si está vacía, no se programa notificación.
     * @param prioridad   Debe ser "Alta", "Media" o "Baja".
     */
    fun añadirTarea(titulo: String, descripcion: String, fecha: String, prioridad: String) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "titulo"      to titulo,
                    "descripcion" to descripcion,
                    "fecha"       to fecha,
                    "completada"  to false,
                    "prioridad"   to prioridad
                )
                // Usamos .await() para obtener la referencia real del documento creado
                val docRef = tareasRef().add(data).await()
                programarNotificacion(docRef.id, titulo, descripcion, fecha)
            } catch (e: Exception) {
                _error.value = "Error al añadir tarea: ${e.message}"
            }
        }
    }

    /**
     * Actualiza los campos de una tarea existente y reprograma su notificación.
     *
     * IMPORTANTE: La notificación anterior se cancela antes de programar la nueva
     * para evitar notificaciones duplicadas si cambia la fecha.
     *
     * @param tareaId ID del documento en Firestore.
     */
    fun editarTarea(tareaId: String, titulo: String, descripcion: String, fecha: String, prioridad: String) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef().document(tareaId).update(
                    mapOf(
                        "titulo"      to titulo,
                        "descripcion" to descripcion,
                        "fecha"       to fecha,
                        "prioridad"   to prioridad
                    )
                ).await()
                // Cancelamos la notificación antigua antes de reprogramar
                cancelarNotificacion(tareaId)
                programarNotificacion(tareaId, titulo, descripcion, fecha)
            } catch (e: Exception) {
                _error.value = "Error al editar tarea: ${e.message}"
            }
        }
    }

    /**
     * Alterna el estado completado/pendiente de una tarea.
     *
     * IMPORTANTE: Si la tarea se marca como completada, se cancela su notificación
     * pendiente para no molestar al usuario con recordatorios de tareas ya hechas.
     * Al desmarcarla no se reprograma automáticamente — el usuario deberá editar
     * la tarea si quiere recuperar la notificación.
     *
     * @param tareaId   ID del documento en Firestore.
     * @param completada Nuevo estado: true = completada, false = pendiente.
     */
    fun toggleTarea(tareaId: String, completada: Boolean) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef().document(tareaId).update("completada", completada).await()
                // Solo cancelamos la notificación al completar, no al desmarcar
                if (completada) cancelarNotificacion(tareaId)
            } catch (e: Exception) {
                _error.value = "Error al actualizar tarea: ${e.message}"
            }
        }
    }

    /**
     * Elimina permanentemente una tarea de Firestore y cancela su notificación.
     *
     * ADVERTENCIA: Esta operación es irreversible. No existe papelera de reciclaje.
     * Asegurarse de pedir confirmación al usuario en la UI antes de llamar este método.
     *
     * @param tareaId ID del documento a eliminar.
     */
    fun eliminarTarea(tareaId: String) {
        requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef().document(tareaId).delete().await()
                cancelarNotificacion(tareaId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar tarea: ${e.message}"
            }
        }
    }

    // ─── Notificaciones ──────────────────────────────────────────────────────

    /**
     * Programa una notificación local para la tarea usando WorkManager.
     *
     * La notificación se lanza a las 09:00h del día indicado en [fecha].
     *
     * IMPORTANTE — Consideraciones de seguridad y robustez:
     *  - El Locale está fijado a es_ES para garantizar el parseo correcto de la fecha
     *    independientemente de la configuración regional del dispositivo.
     *  - Si el delay calculado es ≤ 0 (fecha pasada), no se programa nada y se avisa
     *    al usuario mediante [_error].
     *  - Se usa [ExistingWorkPolicy.REPLACE] para que una edición de tarea cancele
     *    y reemplace la notificación anterior sin dejar workers huérfanos.
     *  - El [tareaId] se usa como tag único tanto en WorkManager como en la notificación,
     *    permitiendo cancelarla con precisión en [cancelarNotificacion].
     *
     * @param tareaId     Identificador único (= tag en WorkManager).
     * @param titulo      Se pasa al worker como dato de entrada para el título de la notificación.
     * @param descripcion Se pasa al worker para el cuerpo de la notificación.
     * @param fecha       Fecha en formato "dd/MM/yyyy".
     */
    private fun programarNotificacion(tareaId: String, titulo: String, descripcion: String, fecha: String) {
        // Si no hay fecha, no tiene sentido programar una notificación
        if (fecha.isBlank()) return

        // Locale fijo en español para evitar fallos de parseo en dispositivos con otro idioma
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        val fechaTarea = runCatching { sdf.parse(fecha) }.getOrNull() ?: return

        // Fijamos la hora de notificación a las 09:00:00 del día indicado
        val calendar = Calendar.getInstance().apply {
            time = fechaTarea
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay <= 0) {
            // La fecha ya ha pasado — avisamos en lugar de fallar silenciosamente
            _error.value = "La fecha ya ha pasado — no se programará notificación"
            return
        }

        // Datos que se pasan al Worker para construir la notificación
        val inputData = workDataOf(
            TareaNotificationWorker.KEY_TITULO      to titulo,
            TareaNotificationWorker.KEY_DESCRIPCION to descripcion,
            TareaNotificationWorker.KEY_TAREA_ID    to tareaId
        )

        val workRequest = OneTimeWorkRequestBuilder<TareaNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tareaId) // Tag = tareaId para poder cancelar por ID
            .build()

        // enqueueUniqueWork con REPLACE garantiza que no haya duplicados
        workManager.enqueueUniqueWork(
            tareaId,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancela la notificación programada para una tarea concreta.
     *
     * Usa el nombre único de trabajo (= tareaId) para localizar y cancelar
     * el worker correspondiente en WorkManager.
     *
     * @param tareaId ID de la tarea cuya notificación se desea cancelar.
     */
    private fun cancelarNotificacion(tareaId: String) {
        workManager.cancelUniqueWork(tareaId)
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────────

    /**
     * Se llama cuando el ViewModel va a ser destruido (p.ej. el usuario sale de la pantalla).
     *
     * CRÍTICO — Prevención de fugas de memoria:
     * Eliminamos el listener de Firestore aquí. Si no se hace, el listener seguiría
     * activo en segundo plano, consumiendo red, memoria y potencialmente llamando
     * callbacks sobre objetos ya liberados, lo que puede causar crashes.
     */
    override fun onCleared() {
        super.onCleared()
        tareasListener?.remove()
    }
}