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
import com.example.qtengo.core.ui.components.QtengoTopBar

// Modelo de cada opción del menú principal del perfil Familiar
data class FamiliarMenuOption(
    val title: String, // Título visible y clave de navegación
    val icon: String,  // Emoji representativo de la sección
    val color: Color   // Color de fondo de la tarjeta
)

// Pantalla de inicio del perfil Familiar.
// Muestra una cuadrícula 2x2 con acceso a las cuatro secciones del módulo.
@Composable
fun FamiliarHomeScreen(
    onMenuSelected: (String) -> Unit, // Recibe el título de la opción seleccionada para navegar
    onLogout: () -> Unit,
    onChangeProfile: () -> Unit
) {
    // Las cuatro secciones del módulo Familiar con sus colores en escala de azul
    val menuOptions = listOf(
        FamiliarMenuOption("Lista de la compra",     "🛒", Color(0xFF1565C0)),
        FamiliarMenuOption("Control de gastos",      "💰", Color(0xFF1976D2)),
        FamiliarMenuOption("Inventario del hogar",   "📦", Color(0xFF1E88E5)),
        FamiliarMenuOption("Tareas y recordatorios", "✅", Color(0xFF2196F3))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        // Barra superior compartida con logout y cambio de perfil
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

        // Cuadrícula 2x2 con las tarjetas de cada sección
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(menuOptions) { option ->
                FamiliarMenuCard(
                    option = option,
                    onClick = { onMenuSelected(option.title) }
                )
            }
        }
    }
}

// Tarjeta cuadrada con emoji e icono para cada sección del menú Familiar.
// El color de fondo viene definido en FamiliarMenuOption.
@Composable
fun FamiliarMenuCard(option: FamiliarMenuOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Tarjeta siempre cuadrada independientemente del ancho
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