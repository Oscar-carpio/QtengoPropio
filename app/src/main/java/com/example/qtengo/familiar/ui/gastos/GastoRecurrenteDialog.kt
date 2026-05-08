package com.example.qtengo.familiar.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Diálogo reutilizable para crear o editar un gasto fijo mensual.
// Si gastoRecurrente es null se muestra en modo creación, si tiene valor en modo edición.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GastoRecurrenteDialog(
    gastoRecurrente: GastoRecurrente?, // null = nuevo gasto, no null = editar existente
    onConfirm: (descripcion: String, cantidad: Double, categoria: String, fechaCobro: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Inicializamos los campos con los valores del gasto si estamos editando, o vacíos si es nuevo
    var descripcion by remember { mutableStateOf(gastoRecurrente?.descripcion ?: "") }
    var cantidad by remember { mutableStateOf(gastoRecurrente?.cantidad?.toString() ?: "") }
    var fechaCobro by remember { mutableStateOf(gastoRecurrente?.fechaCobro ?: "") }

    val categorias = listOf("Alimentación", "Suministros", "Ocio", "Transporte", "Salud", "Otros")
    // Suministros como categoría por defecto para gastos fijos (alquiler, luz, agua...)
    var categoriaSeleccionada by remember { mutableStateOf(gastoRecurrente?.categoria ?: "Suministros") }

    AlertDialog(
        onDismissRequest = onDismiss,
        // El título cambia según si estamos creando o editando
        title = { Text(if (gastoRecurrente == null) "Nuevo gasto fijo" else "Editar gasto fijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Campo descripción — obligatorio para confirmar
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (ej: Alquiler, Luz...)") },
                    singleLine = true
                )

                // Campo importe — debe ser parseable a Double para confirmar
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad (€)") },
                    singleLine = true
                )

                // Campo fecha de cobro — texto libre, obligatorio para confirmar
                OutlinedTextField(
                    value = fechaCobro,
                    onValueChange = { fechaCobro = it },
                    label = { Text("Fecha de cobro (dd/MM/yyyy)") },
                    singleLine = true
                )

                Text(
                    text = "Categoría",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A6B)
                )

                // Chips de selección de categoría — solo una puede estar seleccionada
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categorias.forEach { categoria ->
                        FilterChip(
                            selected = categoriaSeleccionada == categoria,
                            onClick = { categoriaSeleccionada = categoria },
                            label = { Text(categoria, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1A3A6B),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Solo confirmamos si descripción, importe y fecha de cobro son válidos
                val cantidadDouble = cantidad.toDoubleOrNull()
                if (descripcion.isNotBlank() && cantidadDouble != null && fechaCobro.isNotBlank()) {
                    onConfirm(descripcion, cantidadDouble, categoriaSeleccionada, fechaCobro)
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}