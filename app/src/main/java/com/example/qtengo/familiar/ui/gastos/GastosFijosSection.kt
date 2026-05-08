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

// Sección que muestra la lista de gastos fijos mensuales del usuario.
// Cada tarjeta muestra un emoji según la categoría, descripción, fecha de cobro e importe.
// Permite añadir, editar y eliminar gastos fijos.
@Composable
fun GastosFijosSection(
    gastosRecurrentes: List<GastoRecurrente>,
    onAdd: () -> Unit,
    onEdit: (GastoRecurrente) -> Unit,
    onDelete: (String) -> Unit  // Recibe el ID del gasto a eliminar
) {
    // Cabecera con título y botón de añadir
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Gastos fijos mensuales",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A3A6B)
        )
        TextButton(onClick = onAdd) {
            Text("+ Añadir", color = Color(0xFF1A3A6B), fontSize = 13.sp)
        }
    }

    if (gastosRecurrentes.isEmpty()) {
        // Estado vacío
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay gastos fijos. ¡Añade uno!", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        gastosRecurrentes.forEach { recurrente ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
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
                            .background(Color(0xFFE1BEE7), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (recurrente.categoria) {
                                "Alimentación" -> "🛒"
                                "Ocio"         -> "🎬"
                                "Transporte"   -> "🚗"
                                "Salud"        -> "💊"
                                "Suministros"  -> "💡"
                                else           -> "🔄"
                            },
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Descripción y detalle del gasto
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recurrente.descripcion,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3A6B)
                        )
                        Text(
                            text = "${recurrente.categoria} · 📅 Cobro: ${recurrente.fechaCobro}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Importe en negativo para indicar que es un gasto
                    Text(
                        text = "-%.2f €".format(recurrente.cantidad),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )

                    // Botón editar
                    IconButton(onClick = { onEdit(recurrente) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar gasto fijo",
                            tint = Color(0xFF1565C0)
                        )
                    }

                    // Botón eliminar
                    IconButton(onClick = { onDelete(recurrente.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar gasto fijo",
                            tint = Color(0xFF1A3A6B)
                        )
                    }
                }
            }
        }
    }
}