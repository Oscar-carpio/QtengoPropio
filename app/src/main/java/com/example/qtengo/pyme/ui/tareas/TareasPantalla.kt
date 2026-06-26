/**
 * Pantalla principal de la Agenda de Tareas.
 * 
 * Gestiona el ciclo de vida de la UI para la visualización, filtrado y gestión 
 * de tareas diarias. Permite organizar el flujo de trabajo mediante prioridades 
 * y estados de cumplimiento.
 */
package com.example.qtengo.pyme.ui.tareas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qtengo.core.domain.models.Task
import com.example.qtengo.pyme.ui.DialogoConfirmarEliminar
import com.example.qtengo.pyme.ui.TarjetaEstadisticaPyme
import com.example.qtengo.core.ui.components.QtengoTopBar
import com.example.qtengo.core.ui.viewmodels.TaskViewModel
import com.example.qtengo.pyme.ui.filtros.FiltrosTareas
import java.text.SimpleDateFormat
import java.util.*

/**
 * Composable principal del módulo de Tareas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasPantalla(
    viewModel: TaskViewModel = viewModel(),
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangeProfile: () -> Unit
) {
    val allTasks by viewModel.allTasks.observeAsState(emptyList())
    val selectedDate by viewModel.selectedDate.observeAsState("")
    
    val showAddDialog = remember { mutableStateOf(false) }
    val taskToEdit = remember { mutableStateOf<Task?>(null) }
    val taskToDelete = remember { mutableStateOf<Task?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var statusFilter by remember { mutableStateOf("Todas") } 
    var dateFilterEnabled by remember { mutableStateOf(false) }
    var filterMonth by remember { mutableIntStateOf(0) }
    var filterYear by remember { mutableStateOf("Todos") }
    var sortBy by remember { mutableStateOf("") }
    var isAscending by remember { mutableStateOf(true) }
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val today = Date()

    val filteredTasks = allTasks.filter { task ->
        val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                           task.description.contains(searchQuery, ignoreCase = true)
        
        val matchesStatus = when (statusFilter) {
            "Pendientes" -> !task.isCompleted
            "Realizadas" -> task.isCompleted
            else -> true
        }

        val matchesDate = if (dateFilterEnabled) task.date == selectedDate else true

        val matchesScheduledPeriod = run {
            val scheduledDate = try { sdf.parse(task.date) } catch (_: Exception) { null }
            if (scheduledDate == null) {
                filterMonth == 0 && filterYear == "Todos"
            } else {
                val cal = Calendar.getInstance().apply { time = scheduledDate }
                val mMatch = if (filterMonth == 0) true else cal.get(Calendar.MONTH) == (filterMonth - 1)
                val yMatch = if (filterYear == "Todos") true else cal.get(Calendar.YEAR).toString() == filterYear
                mMatch && yMatch
            }
        }

        matchesSearch && matchesStatus && matchesDate && matchesScheduledPeriod
    }.let { list ->
        if (sortBy == "") {
            list.sortedWith(
                compareByDescending<Task> { 
                    when (it.priority.uppercase()) {
                        "ALTA" -> 3
                        "MEDIA" -> 2
                        "BAJA" -> 1
                        else -> 0
                    }
                }.thenByDescending { it.timestamp }
            )
        } else {
            list.sortedWith { t1, t2 ->
                val res = when (sortBy) {
                    "Nombre" -> t1.title.compareTo(t2.title, ignoreCase = true)
                    else -> 0
                }
                if (res != 0) {
                    if (isAscending) res else -res
                } else {
                    compareBy<Task> { it.date.isEmpty() }
                        .thenByDescending { 
                            when (it.priority.uppercase()) {
                                "ALTA" -> 3
                                "MEDIA" -> 2
                                "BAJA" -> 1
                                else -> 0
                            }
                        }
                        .thenByDescending { it.timestamp }
                        .compare(t1, t2)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FB))) {
        QtengoTopBar(
            title = "Agenda de Tareas",
            onBack = onBack,
            onLogout = onLogout,
            onChangeProfile = onChangeProfile
        )

        FiltrosTareas(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            statusFilter = statusFilter,
            onStatusChange = { statusFilter = it },
            filterMonth = filterMonth,
            onMonthChange = { filterMonth = it },
            filterYear = filterYear,
            onYearChange = { filterYear = it },
            dateFilterEnabled = dateFilterEnabled,
            onDateFilterToggle = { dateFilterEnabled = it },
            selectedDate = selectedDate,
            onDateSelected = { viewModel.seleccionarFecha(it) },
            sortBy = sortBy,
            isAscending = isAscending,
            onSortChange = { s, a -> sortBy = s; isAscending = a }
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaEstadisticaPyme(
                titulo = "Disponibles", 
                valor = "${allTasks.count { !it.isCompleted }}", 
                color = Color(0xFF1565C0), 
                modifier = Modifier.weight(1f)
            )
            TarjetaEstadisticaPyme(
                titulo = "Realizadas", 
                valor = "${allTasks.count { it.isCompleted }}", 
                color = Color(0xFF2E7D32), 
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No hay tareas coincidentes", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTasks) { tarea ->
                    ElementoTarjetaTarea(
                        tarea = tarea,
                        hoy = today,
                        onAlternarEstado = { viewModel.actualizarTarea(tarea.copy(isCompleted = !tarea.isCompleted)) },
                        onEditar = { taskToEdit.value = tarea },
                        onEliminar = { taskToDelete.value = tarea }
                    )
                }
            }
        }

        Button(
            onClick = { showAddDialog.value = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B), contentColor = Color.White)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Añadir Tarea", color = Color.White)
        }
    }

    if (showAddDialog.value) {
        DialogoTarea(
            titulo = "Nueva Tarea",
            onDismiss = { showAddDialog.value = false },
            onConfirm = { title, desc, priority, date ->
                viewModel.insertarTarea(title, desc, priority, date)
                showAddDialog.value = false
            }
        )
    }

    val tEdit = taskToEdit.value
    if (tEdit != null) {
        DialogoTarea(
            titulo = "Editar Tarea",
            task = tEdit,
            onDismiss = { taskToEdit.value = null },
            onConfirm = { title, desc, priority, date ->
                viewModel.actualizarTarea(tEdit.copy(title = title, description = desc, priority = priority, date = date))
                taskToEdit.value = null
            }
        )
    }

    val tDelete = taskToDelete.value
    if (tDelete != null) {
        DialogoConfirmarEliminar(
            titulo = "Confirmar eliminación",
            mensaje = "¿Estás seguro de que deseas eliminar la tarea '${tDelete.title}'?",
            onConfirmar = {
                viewModel.eliminarTarea(tDelete)
                taskToDelete.value = null
            },
            onDescartar = { taskToDelete.value = null }
        )
    }
}
