package com.example.qtengo.familiar.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Diálogo para editar un gasto existente.
// Permite modificar descripción, importe y categoría.
// No permite cambiar el tipo (GASTO/INGRESO) ni la fecha.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarGastoDialog(
    gasto: Gasto,
    onConfirm: (descripcion: String, cantidad: Double, categoria: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Inicializamos los campos con los valores actuales del gasto
    var descripcion by remember { mutableStateOf(gasto.descripcion) }
    var cantidad by remember { mutableStateOf(gasto.cantidad.toString()) }

    val categorias = listOf("Alimentación", "Suministros", "Ocio", "Transporte", "Salud", "Otros")
    var categoriaSeleccionada by remember { mutableStateOf(gasto.categoria) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Campo descripción — obligatorio para confirmar
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    singleLine = true
                )

                // Campo importe — debe ser parseable a Double para confirmar
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad (€)") },
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
                // Solo confirmamos si la descripción no está vacía y el importe es válido
                val cantidadDouble = cantidad.toDoubleOrNull()
                if (descripcion.isNotBlank() && cantidadDouble != null) {
                    onConfirm(descripcion, cantidadDouble, categoriaSeleccionada)
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}