package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Diálogo que muestra los productos favoritos del usuario.
// Permite añadir un favorito directamente a la lista activa o eliminarlo.
@Composable
fun FavoritosDialog(
    favoritos: List<FavoriteItem>,
    onAñadir: (FavoriteItem) -> Unit,   // Añade el favorito a la lista activa
    onEliminar: (String) -> Unit,        // Elimina el favorito por su ID
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Productos favoritos") },
        text = {
            if (favoritos.isEmpty()) {
                // Estado vacío — se muestra cuando el usuario no tiene favoritos guardados
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes favoritos aún.\nGuarda productos desde la lista.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Lista scrollable con altura máxima para no ocupar toda la pantalla
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(favoritos) { favorito ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = favorito.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1A3A6B)
                                    )
                                    // Solo mostramos cantidad y precio si tienen valor
                                    if (favorito.quantity.isNotBlank()) {
                                        Text(
                                            text = "Cantidad: ${favorito.quantity}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    // Formateamos Double a "X.XX €" para evitar notación científica
                                    if (favorito.price > 0.0) {
                                        Text(
                                            text = "Precio: ${"%.2f".format(favorito.price)} €",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Botón para añadir el favorito a la lista activa
                                IconButton(onClick = { onAñadir(favorito) }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Añadir a lista",
                                        tint = Color(0xFF1A3A6B)
                                    )
                                }

                                // Botón para eliminar el favorito permanentemente
                                IconButton(onClick = { onEliminar(favorito.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar favorito",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text("Cerrar") }
        }
    )
}