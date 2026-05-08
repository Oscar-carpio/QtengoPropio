package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Pantalla principal del módulo de compra.
// Muestra todas las listas del usuario con búsqueda en tiempo real.
// Permite crear nuevas listas y navegar al detalle de cada una.
@Composable
fun ShoppingListScreen(
    onListSelected: (ShoppingList) -> Unit, // Navega al detalle de la lista seleccionada
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = viewModel()
) {
    // Control de visibilidad del diálogo de nueva lista
    var showDialog by remember { mutableStateOf(false) }

    // Texto de búsqueda — filtra las listas en tiempo real
    var searchQuery by remember { mutableStateOf("") }

    val lists by viewModel.lists.collectAsState()

    // Filtramos las listas localmente sin necesidad de llamar a Firestore
    val filteredLists = lists.filter { list ->
        list.name.contains(searchQuery, ignoreCase = true)
    }

    // Cargamos las listas una sola vez al entrar en la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarListas()
    }

    // Diálogo para crear una nueva lista
    if (showDialog) {
        NuevaListaDialog(
            onConfirm = { nombre ->
                viewModel.crearLista(nombre)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        // Cabecera con título y botón de volver
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A3A6B))
                .padding(24.dp)
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(text = "←", fontSize = 24.sp, color = Color.White)
            }
            Text(
                text = "Lista de la compra",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de búsqueda — filtra las listas por nombre en tiempo real
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar lista...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A3A6B),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLists.isEmpty()) {
            // Estado vacío — mensaje diferente según si hay búsqueda activa o no
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "No hay listas aún. ¡Crea una!" else "No se encontraron listas",
                    color = Color.Gray
                )
            }
        } else {
            // Lista scrollable de tarjetas
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredLists) { list ->
                    ShoppingListCard(
                        list = list,
                        onClick = { onListSelected(list) },
                        onDelete = { viewModel.eliminarLista(list.id) }
                    )
                }
            }
        }

        // Botón para crear una nueva lista
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B))
        ) {
            Text(text = "+ Nueva lista", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
        }
    }
}