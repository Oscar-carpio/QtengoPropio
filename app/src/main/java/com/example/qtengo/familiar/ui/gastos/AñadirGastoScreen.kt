package com.example.qtengo.familiar.ui.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

// Pantalla para añadir un nuevo gasto manual.
// Pide descripción, importe y categoría. El tipo siempre es "GASTO".
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGastoScreen(
    onGastoGuardado: () -> Unit,
    onBack: () -> Unit,
    viewModel: GastosViewModel = viewModel()
) {
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var errorDescripcion by remember { mutableStateOf(false) }
    var errorCantidad by remember { mutableStateOf(false) }

    // Categorías disponibles — la primera se selecciona por defecto
    val categorias = listOf("Alimentación", "Suministros", "Ocio", "Transporte", "Salud", "Otros")
    var categoriaSeleccionada by remember { mutableStateOf("Alimentación") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        // Cabecera con título y botón de volver
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
            Text(
                text = "Añadir gasto",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Campo descripción — obligatorio
            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                    errorDescripcion = false
                },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = errorDescripcion,
                supportingText = {
                    if (errorDescripcion)
                        Text("La descripción no puede estar vacía", color = MaterialTheme.colorScheme.error)
                }
            )

            // Campo importe — debe ser un número mayor que 0
            OutlinedTextField(
                value = cantidad,
                onValueChange = { input ->
                    cantidad = input
                    // Validamos en tiempo real: debe ser número positivo mayor que 0
                    errorCantidad = input.toDoubleOrNull()?.let { it <= 0 } ?: input.isNotBlank()
                },
                label = { Text("Cantidad (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = errorCantidad,
                supportingText = {
                    if (errorCantidad)
                        Text("Introduce un importe válido y mayor que 0", color = MaterialTheme.colorScheme.error)
                }
            )

            Text(
                text = "Categoría",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A3A6B)
            )

            // Chips de selección de categoría — solo una puede estar seleccionada
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { categoria ->
                    FilterChip(
                        selected = categoriaSeleccionada == categoria,
                        onClick = { categoriaSeleccionada = categoria },
                        label = { Text(categoria, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A3A6B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón guardar — valida todos los campos antes de llamar al ViewModel
            Button(
                onClick = {
                    val cantidadDouble = cantidad.toDoubleOrNull()
                    var valido = true

                    if (descripcion.isBlank()) {
                        errorDescripcion = true; valido = false
                    }
                    // La cantidad debe existir y ser mayor que 0
                    if (cantidadDouble == null || cantidadDouble <= 0) {
                        errorCantidad = true; valido = false
                    }

                    if (valido && cantidadDouble != null) {
                        viewModel.añadirGasto(
                            descripcion = descripcion.trim(),
                            cantidad = cantidadDouble,
                            categoria = categoriaSeleccionada,
                            tipo = "GASTO" // Esta pantalla solo registra gastos, no ingresos
                        )
                        onGastoGuardado()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B))
            ) {
                Text(text = "Guardar gasto", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}