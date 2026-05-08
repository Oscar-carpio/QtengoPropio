package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tarjeta que representa un producto dentro de una lista de la compra.
// Permite marcar/desmarcar, editar, eliminar y guardar como favorito.
// Los productos marcados se muestran en azul claro con texto tachado.
@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    esFavorito: Boolean,           // true si el producto ya está guardado como favorito
    onToggle: (Boolean) -> Unit,   // Marca o desmarca el producto como recogido
    onDelete: () -> Unit,
    onEdit: (nombre: String, cantidad: String, precio: Double) -> Unit,
    onFavorito: () -> Unit         // Guarda o elimina el producto de favoritos
) {
    // Controla la visibilidad del diálogo de edición
    var showEditDialog by remember { mutableStateOf(false) }

    // Mostramos el diálogo de edición solo cuando el usuario pulsa el botón editar
    if (showEditDialog) {
        EditarItemDialog(
            item = item,
            onConfirm = { nombre, cantidad, precio ->
                onEdit(nombre, cantidad, precio)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        // Fondo azul claro si el producto está marcado, blanco si está pendiente
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox para marcar el producto como recogido en el carrito
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1A3A6B))
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Icono del carrito — gris si está marcado, azul si está pendiente
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = if (item.isChecked) Color.Gray else Color(0xFF1A3A6B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Nombre tachado y gris si el producto está marcado
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.isChecked) Color.Gray else Color(0xFF1A3A6B),
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(text = "Cantidad: ${item.quantity}", fontSize = 12.sp, color = Color.Gray)
                // Solo mostramos el precio si es mayor que 0
                if (item.price > 0.0) {
                    Text(
                        text = "Precio: ${"%.2f".format(item.price)} €",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Botón favorito — corazón relleno si ya es favorito, vacío si no
            IconButton(onClick = onFavorito) {
                Icon(
                    imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (esFavorito) "Quitar de favoritos" else "Guardar como favorito",
                    tint = if (esFavorito) Color(0xFFE53935) else Color.Gray
                )
            }

            // Botón editar — abre el diálogo de edición
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar producto",
                    tint = Color(0xFF1A3A6B)
                )
            }

            // Botón eliminar
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar producto",
                    tint = Color(0xFF1A3A6B)
                )
            }
        }
    }
}