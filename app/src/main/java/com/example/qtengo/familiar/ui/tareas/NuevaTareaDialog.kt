package com.example.qtengo.familiar.ui.tareas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// Diálogo para crear una nueva tarea.
// La prioridad por defecto es "Media".
// Las fechas pasadas muestran aviso naranja pero no bloquean el guardado —
// el ViewModel se encargará de no programar la notificación en ese caso.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuevaTareaDialog(
    onConfirm: (titulo: String, descripcion: String, fecha: String, prioridad: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    val prioridades = listOf("Alta", "Media", "Baja")
    var prioridadSeleccionada by remember { mutableStateOf("Media") }

    // Mensajes de error por campo
    var errorTitulo by remember { mutableStateOf("") }
    var errorFecha by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }

    // Valida el formato de la fecha y si es futura.
    // Devuelve cadena vacía si es válida, mensaje de error si el formato es incorrecto,
    // o aviso informativo si la fecha ya ha pasado (no bloquea el guardado).
    fun validarFecha(input: String): String {
        if (input.isBlank()) return "La fecha es obligatoria"
        sdf.isLenient = false
        val fechaParsed = runCatching { sdf.parse(input) }.getOrNull()
            ?: return "Formato inválido — usa dd/MM/yyyy"
        if (fechaParsed.before(Date())) return "La fecha ya ha pasado — no se programará notificación"
        return ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Campo título — obligatorio
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        errorTitulo = ""
                    },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorTitulo.isNotEmpty(),
                    supportingText = {
                        if (errorTitulo.isNotEmpty())
                            Text(errorTitulo, color = MaterialTheme.colorScheme.error)
                    }
                )

                // Campo descripción — opcional
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo fecha — obligatorio, formato dd/MM/yyyy
                // Aviso naranja si la fecha ya pasó, error rojo si el formato es inválido
                OutlinedTextField(
                    value = fecha,
                    onValueChange = {
                        fecha = it
                        errorFecha = ""
                    },
                    label = { Text("Fecha (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorFecha.isNotEmpty(),
                    supportingText = {
                        if (errorFecha.isNotEmpty()) {
                            val esPasada = errorFecha.contains("pasado")
                            Text(
                                text = errorFecha,
                                color = if (esPasada) Color(0xFFF57C00)
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Text(
                    text = "Prioridad",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A6B)
                )

                // Chips de prioridad — color según nivel: rojo Alta, naranja Media, verde Baja
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    prioridades.forEach { prioridad ->
                        FilterChip(
                            selected = prioridadSeleccionada == prioridad,
                            onClick = { prioridadSeleccionada = prioridad },
                            label = { Text(prioridad) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (prioridad) {
                                    "Alta"  -> Color(0xFFD32F2F)
                                    "Media" -> Color(0xFFF57C00)
                                    else    -> Color(0xFF388E3C)
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var valido = true

                if (titulo.isBlank()) {
                    errorTitulo = "El título no puede estar vacío"
                    valido = false
                }

                val errorFechaActual = validarFecha(fecha)
                if (errorFechaActual.isNotEmpty()) {
                    errorFecha = errorFechaActual
                    // Solo bloqueamos si es un error de formato, no si la fecha ya pasó
                    if (!errorFechaActual.contains("pasado")) valido = false
                }

                if (valido) {
                    onConfirm(titulo.trim(), descripcion.trim(), fecha.trim(), prioridadSeleccionada)
                }
            }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}