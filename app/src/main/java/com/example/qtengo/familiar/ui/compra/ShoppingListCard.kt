package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tarjeta que representa una lista de la compra en la pantalla principal.
// Muestra el nombre, número de productos y fecha de creación.
// Al pulsar navega al detalle de la lista; el botón de papelera la elimina.
@Composable
fun ShoppingListCard(
    list: ShoppingList,
    onClick: () -> Unit,  // Navega al detalle de la lista
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del carrito con fondo circular azul
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1A3A6B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nombre de la lista y resumen: número de productos y fecha
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A6B)
                )
                Text(
                    text = "${list.itemCount} productos · ${list.date}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Botón para eliminar la lista
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar lista",
                    tint = Color(0xFF1A3A6B)
                )
            }

            // Flecha indicadora de navegación al detalle
            Text(text = "→", fontSize = 20.sp, color = Color.Gray)
        }
    }
}