package com.example.qtengo.familiar.ui.gastos

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

// Tarjeta resumen del mes que muestra el total gastado, el presupuesto
// y una barra de progreso que cambia de color según el porcentaje consumido.
@Composable
fun GastosResumenCard(
    totalGastos: Double,      // Suma de gastos puntuales del mes
    totalRecurrentes: Double, // Suma de gastos fijos mensuales
    presupuesto: Double?,     // null si el usuario aún no ha configurado presupuesto
    onCambiarPresupuesto: () -> Unit
) {
    // Total estimado = gastos puntuales + gastos fijos
    val totalMes = totalGastos + totalRecurrentes

    // Porcentaje del presupuesto consumido — limitado entre 0 y 1 para la barra
    val porcentaje = if ((presupuesto ?: 0.0) > 0)
        (totalMes / presupuesto!!).toFloat().coerceIn(0f, 1f) else 0f

    // Color de la barra según el nivel de gasto:
    // verde < 75%, naranja entre 75% y 100%, rojo al superar el presupuesto
    val colorBarra = when {
        porcentaje >= 1f    -> Color(0xFFD32F2F)
        porcentaje >= 0.75f -> Color(0xFFF57C00)
        else                -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A6B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total estimado este mes",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            // Importe total en grande
            Text(
                text = "%.2f €".format(totalMes),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Desglose: gastos puntuales vs gastos fijos
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Puntuales: %.2f €".format(totalGastos),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Fijos: %.2f €".format(totalRecurrentes),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Sección de presupuesto — solo se muestra si el usuario lo ha configurado
            if (presupuesto != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Presupuesto: %.2f €".format(presupuesto),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Barra de progreso que refleja el porcentaje del presupuesto consumido
                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = colorBarra,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Mensaje de estado según el porcentaje consumido
                Text(
                    text = when {
                        porcentaje >= 1f    -> "⚠️ Has superado el presupuesto"
                        porcentaje >= 0.75f -> "⚠️ Llevas el ${(porcentaje * 100).toInt()}% del presupuesto"
                        else                -> "Llevas el ${(porcentaje * 100).toInt()}% del presupuesto"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para establecer o cambiar el presupuesto mensual
            TextButton(
                onClick = onCambiarPresupuesto,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text(
                    text = if (presupuesto != null) "✏️ Cambiar presupuesto" else "＋ Establecer presupuesto",
                    fontSize = 13.sp
                )
            }
        }
    }
}