package com.example.qtengo.familiar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qtengo.Rutas
import com.example.qtengo.core.ui.components.QtengoTopBar

data class FamiliarMenuOption(
    val title: String,  // Texto visible en la tarjeta
    val ruta: String,   // Ruta de navegación interna
    val icon: String,
    val color: Color
)

@Composable
fun FamiliarHomeScreen(
    onMenuSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onChangeProfile: () -> Unit
) {
    val menuOptions = listOf(
        FamiliarMenuOption(Rutas.MENU_LISTA_COMPRA,        Rutas.LISTA_COMPRA,     "🛒", Color(0xFF1565C0)),
        FamiliarMenuOption(Rutas.MENU_CONTROL_GASTOS,      Rutas.CONTROL_GASTOS,   "💰", Color(0xFF1976D2)),
        FamiliarMenuOption(Rutas.MENU_INVENTARIO_HOGAR,    Rutas.INVENTARIO_HOGAR, "📦", Color(0xFF1E88E5)),
        FamiliarMenuOption(Rutas.MENU_TAREAS_RECORDATORIOS,Rutas.TAREAS_FAMILIAR,  "✅", Color(0xFF2196F3))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        QtengoTopBar(
            title = "Perfil Familiar",
            onLogout = onLogout,
            onChangeProfile = onChangeProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¿Qué quieres gestionar?",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(menuOptions) { option ->
                FamiliarMenuCard(
                    option = option,
                    onClick = { onMenuSelected(option.ruta) }
                )
            }
        }
    }
}

@Composable
fun FamiliarMenuCard(option: FamiliarMenuOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = option.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = option.icon, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = option.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}