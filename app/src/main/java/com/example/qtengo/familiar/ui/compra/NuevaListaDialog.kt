package com.example.qtengo.familiar.ui.compra

import androidx.compose.material3.*
import androidx.compose.runtime.*

// Diálogo simple para crear una nueva lista de la compra.
// Solo pide el nombre — el contador y la fecha los asigna el ViewModel.
@Composable
fun NuevaListaDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            nombre = "" // Limpiamos el campo al cerrar sin confirmar
        },
        title = { Text("Nueva lista") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la lista") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                // Solo confirmamos si el nombre tiene contenido
                if (nombre.isNotBlank()) {
                    onConfirm(nombre)
                    nombre = ""
                }
            }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                nombre = ""
            }) { Text("Cancelar") }
        }
    )
}