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

data class Tarea(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val completada: Boolean = false,
    val prioridad: String = "Media"
)

// ViewModel que gestiona las tareas del usuario autenticado.
// Marcada como open para permitir su uso en tests mediante clases fake.
// También programa y cancela notificaciones locales mediante WorkManager.
open class TareasViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workManager = WorkManager.getInstance(application)

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    open val tareas: StateFlow<List<Tarea>> = _tareas

    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error

    private val _aviso = MutableStateFlow<String?>(null)
    open val aviso: StateFlow<String?> = _aviso

    private var tareasListener: ListenerRegistration? = null

    fun clearError() { _error.value = null }
    fun clearAviso() { _aviso.value = null }

    private fun requireUid(): String? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _error.value = "Usuario no autenticado. Por favor, inicia sesión de nuevo."
            return null
        }
        return uid
    }

    private fun tareasRef(uid: String) =
        db.collection("usuarios").document(uid).collection("tareas")

    fun cargarTareas() {
        val uid = requireUid() ?: return
        tareasListener?.remove()
        tareasListener = tareasRef(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error al cargar tareas: ${e.message}"
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.map { doc ->
                    Tarea(
                        id          = doc.id,
                        titulo      = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        fecha       = doc.getString("fecha") ?: "",
                        completada  = doc.getBoolean("completada") ?: false,
                        prioridad   = doc.getString("prioridad") ?: "Media"
                    )
                } ?: emptyList()

                _tareas.value = result.sortedWith(
                    compareBy<Tarea> { it.completada }
                        .thenBy {
                            when (it.prioridad) {
                                "Alta"  -> 0
                                "Media" -> 1
                                "Baja"  -> 2
                                else    -> 3
                            }
                        }
                )
            }
    }

    fun añadirTarea(titulo: String, descripcion: String, fecha: String, prioridad: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "titulo"      to titulo,
                    "descripcion" to descripcion,
                    "fecha"       to fecha,
                    "completada"  to false,
                    "prioridad"   to prioridad
                )
                val docRef = tareasRef(uid).add(data).await()
                programarNotificacion(docRef.id, titulo, descripcion, fecha)
            } catch (e: Exception) {
                _error.value = "Error al añadir tarea: ${e.message}"
            }
        }
    }

    fun editarTarea(tareaId: String, titulo: String, descripcion: String, fecha: String, prioridad: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef(uid).document(tareaId).update(
                    mapOf(
                        "titulo"      to titulo,
                        "descripcion" to descripcion,
                        "fecha"       to fecha,
                        "prioridad"   to prioridad
                    )
                ).await()
                cancelarNotificacion(tareaId)
                programarNotificacion(tareaId, titulo, descripcion, fecha)
            } catch (e: Exception) {
                _error.value = "Error al editar tarea: ${e.message}"
            }
        }
    }

    fun toggleTarea(tareaId: String, completada: Boolean) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef(uid).document(tareaId).update("completada", completada).await()
                if (completada) {
                    cancelarNotificacion(tareaId)
                } else {
                    val doc = tareasRef(uid).document(tareaId).get().await()
                    val titulo = doc.getString("titulo") ?: ""
                    val descripcion = doc.getString("descripcion") ?: ""
                    val fecha = doc.getString("fecha") ?: ""
                    programarNotificacion(tareaId, titulo, descripcion, fecha)
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar tarea: ${e.message}"
            }
        }
    }

    fun eliminarTarea(tareaId: String) {
        val uid = requireUid() ?: return
        viewModelScope.launch {
            try {
                tareasRef(uid).document(tareaId).delete().await()
                cancelarNotificacion(tareaId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar tarea: ${e.message}"
            }
        }
    }

    private fun programarNotificacion(tareaId: String, titulo: String, descripcion: String, fecha: String) {
        if (fecha.isBlank()) return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        val fechaTarea = runCatching { sdf.parse(fecha) }.getOrNull() ?: return
        val calendar = Calendar.getInstance().apply {
            time = fechaTarea
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay <= 0) {
            _aviso.value = "La fecha ya ha pasado — no se programará notificación"
            return
        }
        val inputData = workDataOf(
            TareaNotificationWorker.KEY_TITULO      to titulo,
            TareaNotificationWorker.KEY_DESCRIPCION to descripcion,
            TareaNotificationWorker.KEY_TAREA_ID    to tareaId
        )
        val workRequest = OneTimeWorkRequestBuilder<TareaNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tareaId)
            .build()
        workManager.enqueueUniqueWork(tareaId, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun cancelarNotificacion(tareaId: String) {
        workManager.cancelUniqueWork(tareaId)
    }

    override fun onCleared() {
        super.onCleared()
        tareasListener?.remove()
    }
}