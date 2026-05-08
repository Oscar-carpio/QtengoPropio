package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

// Diálogo para añadir un nuevo producto a una lista de la compra.
// Valida nombre (obligatorio), cantidad y precio (ambos opcionales) antes de confirmar.
@Composable
fun NuevoItemDialog(
    onConfirm: (nombre: String, cantidad: String, precio: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precioTexto by remember { mutableStateOf("") }
    var errorNombre by remember { mutableStateOf(false) }
    var errorCantidad by remember { mutableStateOf(false) }
    var errorPrecio by remember { mutableStateOf(false) }

    // Limpia todos los campos y errores — se llama al confirmar o cancelar
    fun resetear() {
        nombre = ""; cantidad = ""; precioTexto = ""
        errorNombre = false; errorCantidad = false; errorPrecio = false
    }

    // Extrae el número al inicio de textos como "2 kg", "500 ml", "3"
    // Devuelve null si no hay un número válido al principio del texto
    fun extraerNumero(texto: String): Double? {
        val numStr = texto.trim().split(" ", "kg", "Kg", "KG", "ml", "ML", "l", "L",
            "g", "G", "cm", "CM", "m", "M", "litros", "Litros").first().trim()
        return numStr.replace(",", ".").toDoubleOrNull()
    }

    AlertDialog(
        onDismissRequest = { onDismiss(); resetear() },
        title = { Text("Añadir producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Campo nombre — obligatorio
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        // Limpiamos el error en tiempo real si el usuario empieza a escribir
                        if (errorNombre) errorNombre = it.isBlank()
                    },
                    label = { Text("Nombre del producto") },
                    singleLine = true,
                    isError = errorNombre,
                    supportingText = {
                        if (errorNombre) Text("El nombre no puede estar vacío", color = Color.Red)
                    }
                )

                // Campo cantidad — opcional, acepta unidades como "2 kg", "500 ml"
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        cantidad = it
                        // Solo validamos si el campo tiene contenido
                        if (it.isNotBlank()) {
                            val num = extraerNumero(it)
                            errorCantidad = num == null || num < 0
                        } else {
                            errorCantidad = false
                        }
                    },
                    label = { Text("Cantidad (ej: 2, 500 ml, 1 kg)") },
                    singleLine = true,
                    isError = errorCantidad,
                    supportingText = {
                        if (errorCantidad) Text("Introduce una cantidad válida (ej: 2, 1 kg, 500 ml)", color = Color.Red)
                    }
                )

                // Campo precio — opcional, se normaliza la coma a punto antes de parsear
                OutlinedTextField(
                    value = precioTexto,
                    onValueChange = { input ->
                        precioTexto = input
                        val normalizado = input.replace(",", ".")
                        errorPrecio = normalizado.toDoubleOrNull()?.let { it < 0 } ?: (input.isNotBlank())
                    },
                    label = { Text("Precio (€)") },
                    singleLine = true,
                    isError = errorPrecio,
                    supportingText = {
                        if (errorPrecio) Text("Introduce un precio válido y no negativo", color = Color.Red)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validamos todos los campos antes de confirmar
                errorNombre = nombre.isBlank()

                // La cantidad es opcional — solo validamos si tiene contenido
                val cantidadOk = if (cantidad.isBlank()) true else {
                    val num = extraerNumero(cantidad)
                    num != null && num >= 0
                }
                errorCantidad = !cantidadOk

                // El precio es opcional — si está vacío se usa 0.0
                val precioDouble = precioTexto.replace(",", ".").toDoubleOrNull() ?: 0.0
                val precioValido = precioTexto.isBlank() || precioDouble >= 0.0
                errorPrecio = !precioValido && precioTexto.isNotBlank()

                // Solo confirmamos si todos los campos son válidos
                if (!errorNombre && !errorCantidad && !errorPrecio) {
                    onConfirm(nombre.trim(), cantidad.trim(), precioDouble)
                    resetear()
                }
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(); resetear() }) { Text("Cancelar") }
        }
    )
}