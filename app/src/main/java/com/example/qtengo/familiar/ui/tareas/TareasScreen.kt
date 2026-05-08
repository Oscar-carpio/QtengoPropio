package com.example.qtengo.familiar.ui.tareas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Pantalla principal del módulo de tareas.
// Muestra un resumen con contadores y la lista dividida en pendientes y completadas.
// Permite crear, editar, completar y eliminar tareas.
@Composable
fun TareasScreen(
    onBack: () -> Unit,
    viewModel: TareasViewModel = viewModel()
) {
    val tareas by viewModel.tareas.collectAsState()

    // Control de visibilidad de los diálogos
    var showNuevaTareaDialog by remember { mutableStateOf(false) }
    var tareaAEditar by remember { mutableStateOf<Tarea?>(null) }

    // Dividimos las tareas en dos grupos para mostrarlas en secciones separadas
    val tareasPendientes = tareas.filter { !it.completada }
    val tareasCompletadas = tareas.filter { it.completada }

    // Cargamos las tareas una sola vez al entrar en la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarTareas()
    }

    // Diálogo para crear una nueva tarea
    if (showNuevaTareaDialog) {
        NuevaTareaDialog(
            onConfirm = { titulo, descripcion, fecha, prioridad ->
                viewModel.añadirTarea(titulo, descripcion, fecha, prioridad)
                showNuevaTareaDialog = false
            },
            onDismiss = { showNuevaTareaDialog = false }
        )
    }

    // Diálogo para editar una tarea existente — solo visible cuando tareaAEditar no es null
    tareaAEditar?.let { tarea ->
        EditarTareaDialog(
            tarea = tarea,
            onConfirm = { titulo, descripcion, fecha, prioridad ->
                viewModel.editarTarea(tarea.id, titulo, descripcion, fecha, prioridad)
                tareaAEditar = null
            },
            onDismiss = { tareaAEditar = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        // Cabecera con título y contadores de pendientes/completadas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A3A6B))
                .padding(24.dp)
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(text = "←", fontSize = 24.sp, color = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = "Tareas y recordatorios",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${tareasPendientes.size} pendientes · ${tareasCompletadas.size} completadas",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Fila de tres tarjetas resumen: pendientes, completadas y urgentes (Alta prioridad)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tarjeta: tareas pendientes
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A6B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${tareasPendientes.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Pendientes",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Tarjeta: tareas completadas
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${tareasCompletadas.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Completadas",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Tarjeta: tareas pendientes con prioridad Alta
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${tareasPendientes.count { it.prioridad == "Alta" }}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Urgentes",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (tareas.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay tareas. ¡Añade una!", color = Color.Gray)
            }
        } else {
            // Lista dividida en secciones: primero pendientes, luego completadas
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Sección tareas pendientes
                if (tareasPendientes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pendientes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3A6B),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(tareasPendientes) { tarea ->
                        TareaCard(
                            tarea = tarea,
                            onToggle = { viewModel.toggleTarea(tarea.id, it) },
                            onEdit = { tareaAEditar = tarea },
                            onDelete = { viewModel.eliminarTarea(tarea.id) }
                        )
                    }
                }

                // Sección tareas completadas
                if (tareasCompletadas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completadas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(tareasCompletadas) { tarea ->
                        TareaCard(
                            tarea = tarea,
                            onToggle = { viewModel.toggleTarea(tarea.id, it) },
                            onEdit = { tareaAEditar = tarea },
                            onDelete = { viewModel.eliminarTarea(tarea.id) }
                        )
                    }
                }
            }
        }

        // Botón para abrir el diálogo de nueva tarea
        Button(
            onClick = { showNuevaTareaDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B))
        ) {
            Text(text = "+ Nueva tarea", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
        }
    }
}