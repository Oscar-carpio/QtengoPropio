package com.example.qtengo.familiar.ui.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sección que muestra los gastos puntuales (no recurrentes) del usuario.
// Los gastos generados desde una lista de la compra se identifican con una etiqueta especial.
// Permite editar y eliminar cada gasto.
@Composable
fun GastosPuntualesSection(
    gastos: List<Gasto>,
    onEdit: (Gasto) -> Unit,
    onDelete: (String) -> Unit  // Recibe el ID del gasto a eliminar
) {
    Text(
        text = "Gastos puntuales",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A3A6B),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    if (gastos.isEmpty()) {
        // Estado vacío
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay gastos registrados.", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        gastos.forEach { gasto ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono emoji según la categoría del gasto
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (gasto.categoria) {
                                "Alimentación" -> "🛒"
                                "Ocio"         -> "🎬"
                                "Transporte"   -> "🚗"
                                "Salud"        -> "💊"
                                "Suministros"  -> "💡"
                                else           -> "💰"
                            },
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = gasto.descripcion,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3A6B)
                        )
                        Text(
                            text = "${gasto.categoria} · ${gasto.fecha}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        // Etiqueta especial para gastos generados automáticamente desde una lista de la compra
                        if (gasto.origen == "lista_compra") {
                            Text(
                                text = "📋 Desde lista de la compra",
                                fontSize = 11.sp,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }

                    // Importe en negativo y rojo para indicar que es un gasto
                    Text(
                        text = "-%.2f €".format(gasto.cantidad),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )

                    // Botón editar
                    IconButton(onClick = { onEdit(gasto) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar gasto",
                            tint = Color(0xFF1565C0)
                        )
                    }

                    // Botón eliminar
                    IconButton(onClick = { onDelete(gasto.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar gasto",
                            tint = Color(0xFF1A3A6B)
                        )
                    }
                }
            }
        }
    }
}