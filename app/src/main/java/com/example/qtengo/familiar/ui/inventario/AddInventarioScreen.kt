package com.example.qtengo.familiar.ui.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

// Pantalla para añadir un nuevo artículo al inventario del hogar.
// Valida nombre, cantidad, stock mínimo y fecha de caducidad antes de guardar.
@Composable
fun AddInventarioScreen(
    onItemGuardado: () -> Unit,
    onBack: () -> Unit,
    viewModel: InventarioViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("1") }
    var notas by remember { mutableStateOf("") }
    var fechaCaducidad by remember { mutableStateOf("") }

    // Controla si se muestra el campo de fecha de caducidad
    var tieneFechaCaducidad by remember { mutableStateOf(false) }

    // Mensajes de error por campo — cadena vacía = sin error
    var errorNombre by remember { mutableStateOf("") }
    var errorCantidad by remember { mutableStateOf("") }
    var errorMinStock by remember { mutableStateOf("") }
    var errorFecha by remember { mutableStateOf("") }

    val ubicaciones = listOf("Cocina", "Despensa", "Lavadero", "Trastero", "Baño", "Otros")
    var ubicacionSeleccionada by remember { mutableStateOf("Cocina") }

    // isLenient = false para rechazar fechas inválidas como "32/01/2025"
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).apply { isLenient = false } }

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
                text = "Añadir artículo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Campo nombre — obligatorio
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errorNombre = "" },
                label = { Text("Nombre del artículo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = errorNombre.isNotEmpty(),
                supportingText = {
                    if (errorNombre.isNotEmpty())
                        Text(errorNombre, color = MaterialTheme.colorScheme.error)
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Campo cantidad — debe ser un entero mayor que 0
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        cantidad = it
                        errorCantidad = when {
                            it.isBlank()            -> ""
                            it.toIntOrNull() == null -> "Solo números enteros"
                            it.toInt() <= 0          -> "Debe ser mayor que 0"
                            else                     -> ""
                        }
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorCantidad.isNotEmpty(),
                    supportingText = {
                        if (errorCantidad.isNotEmpty())
                            Text(errorCantidad, color = MaterialTheme.colorScheme.error)
                    }
                )

                // Campo stock mínimo — debe ser un entero no negativo
                OutlinedTextField(
                    value = minStock,
                    onValueChange = {
                        minStock = it
                        errorMinStock = when {
                            it.isBlank()            -> ""
                            it.toIntOrNull() == null -> "Solo números enteros"
                            it.toInt() < 0           -> "No puede ser negativo"
                            else                     -> ""
                        }
                    },
                    label = { Text("Stock Mín.") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMinStock.isNotEmpty(),
                    supportingText = {
                        if (errorMinStock.isNotEmpty())
                            Text(errorMinStock, color = MaterialTheme.colorScheme.error)
                    }
                )
            }

            // Campo notas — opcional
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Switch para activar el campo de fecha de caducidad
            // Al desactivarlo limpiamos la fecha y el error
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tiene fecha de caducidad",
                    fontSize = 14.sp,
                    color = Color(0xFF1A3A6B),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = tieneFechaCaducidad,
                    onCheckedChange = {
                        tieneFechaCaducidad = it
                        if (!it) { fechaCaducidad = ""; errorFecha = "" }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1A3A6B))
                )
            }

            // Campo fecha de caducidad — solo visible si el switch está activado
            // Valida el formato dd/MM/yyyy en tiempo real con isLenient = false
            if (tieneFechaCaducidad) {
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = {
                        fechaCaducidad = it
                        errorFecha = if (it.isNotBlank()) {
                            runCatching { sdf.parse(it) }.fold(
                                onSuccess = { "" },
                                onFailure = { "Formato inválido — usa dd/MM/yyyy" }
                            )
                        } else ""
                    },
                    label = { Text("Fecha caducidad (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorFecha.isNotEmpty(),
                    supportingText = {
                        if (errorFecha.isNotEmpty())
                            Text(errorFecha, color = MaterialTheme.colorScheme.error)
                    }
                )
            }

            Text(
                text = "Ubicación / Categoría",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A3A6B)
            )

            // Chips de ubicación organizados en filas de 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ubicaciones.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ubicacion ->
                            FilterChip(
                                selected = ubicacionSeleccionada == ubicacion,
                                onClick = { ubicacionSeleccionada = ubicacion },
                                label = { Text(ubicacion, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1A3A6B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón guardar — valida todos los campos antes de llamar al ViewModel
            Button(
                onClick = {
                    var valido = true

                    if (nombre.isBlank()) {
                        errorNombre = "El nombre no puede estar vacío"; valido = false
                    }

                    val cantidadInt = cantidad.toIntOrNull()
                    if (cantidadInt == null || cantidadInt <= 0) {
                        errorCantidad = "Introduce una cantidad válida mayor que 0"; valido = false
                    }

                    val minStockInt = minStock.toIntOrNull()
                    if (minStockInt == null || minStockInt < 0) {
                        errorMinStock = "Introduce un stock mínimo válido"; valido = false
                    }

                    // Solo validamos la fecha si el switch está activo y tiene contenido
                    if (tieneFechaCaducidad && fechaCaducidad.isNotBlank()) {
                        val fechaOk = runCatching { sdf.parse(fechaCaducidad) }.isSuccess
                        if (!fechaOk) {
                            errorFecha = "Formato inválido — usa dd/MM/yyyy"; valido = false
                        }
                    }

                    if (valido && cantidadInt != null && minStockInt != null) {
                        viewModel.añadirItem(
                            nombre = nombre.trim(),
                            cantidad = cantidadInt,
                            ubicacion = ubicacionSeleccionada,
                            minStock = minStockInt,
                            notas = notas.trim(),
                            // Si el campo está vacío guardamos null — el ViewModel no incluye el campo en Firestore
                            fechaCaducidad = fechaCaducidad.ifBlank { null }
                        )
                        onItemGuardado()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B))
            ) {
                Text(text = "Guardar artículo", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}