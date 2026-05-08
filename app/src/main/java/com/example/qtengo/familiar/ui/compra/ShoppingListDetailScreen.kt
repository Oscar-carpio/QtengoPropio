package com.example.qtengo.familiar.ui.compra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qtengo.familiar.ui.gastos.GastosViewModel

// Pantalla de detalle de una lista de la compra.
// Muestra los productos de la lista, permite añadir, editar, eliminar y marcar productos.
// Cuando todos los productos están marcados muestra un banner para registrar el gasto total.
@Composable
fun ShoppingListDetailScreen(
    shoppingList: ShoppingList,
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = viewModel(),
    gastosViewModel: GastosViewModel = viewModel()
) {
    // Control de visibilidad de los tres diálogos
    var showDialog by remember { mutableStateOf(false) }
    var showGastoDialog by remember { mutableStateOf(false) }
    var showFavoritosDialog by remember { mutableStateOf(false) }

    // Importe introducido en el diálogo de registrar gasto
    var gastoTotal by remember { mutableStateOf("") }

    val items by viewModel.items.collectAsState()
    val favoritos by viewModel.favoritos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // true cuando la lista tiene productos y todos están marcados
    val listaCompleta = items.isNotEmpty() && items.all { it.isChecked }

    // Suma de los precios de todos los productos de la lista
    val precioTotal = items.sumOf { it.price }

    // Cargamos los productos y favoritos cuando cambia la lista activa
    LaunchedEffect(shoppingList.id) {
        viewModel.cargarItems(shoppingList.id)
        viewModel.cargarFavoritos()
    }

    // Diálogo para añadir un nuevo producto a la lista
    if (showDialog) {
        NuevoItemDialog(
            onConfirm = { nombre, cantidad, precio ->
                viewModel.añadirItem(shoppingList.id, nombre, cantidad, precio)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    // Diálogo para ver y gestionar los productos favoritos del usuario
    if (showFavoritosDialog) {
        FavoritosDialog(
            favoritos = favoritos,
            onAñadir = { favorito ->
                viewModel.añadirFavoritoALista(shoppingList.id, favorito)
            },
            onEliminar = { favoritoId ->
                viewModel.eliminarFavorito(favoritoId)
            },
            onDismiss = { showFavoritosDialog = false }
        )
    }

    // Diálogo para registrar el gasto total de la compra en el módulo de gastos
    if (showGastoDialog) {
        AlertDialog(
            onDismissRequest = {
                showGastoDialog = false
                gastoTotal = ""
            },
            title = { Text("Registrar gasto de la compra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Cuánto has gastado en \"${shoppingList.name}\"?",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = gastoTotal,
                        onValueChange = { gastoTotal = it },
                        label = { Text("Total (€)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Solo registramos si el importe es un número mayor que 0
                    val cantidad = gastoTotal.toDoubleOrNull()
                    if (cantidad != null && cantidad > 0) {
                        gastosViewModel.registrarGastoDesdeLista(
                            listaId = shoppingList.id,
                            nombreLista = shoppingList.name,
                            cantidad = cantidad
                        )
                        showGastoDialog = false
                        gastoTotal = ""
                    }
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGastoDialog = false
                    gastoTotal = ""
                }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        // Cabecera con nombre de la lista, progreso y precio total
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
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = shoppingList.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // Contador de productos marcados sobre el total
                Text(
                    text = "${items.count { it.isChecked }}/${items.size} productos",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                // Suma de precios de todos los productos
                Text(
                    text = "Total: ${"%.2f".format(precioTotal)} €",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Barra de progreso — avanza según los productos marcados
        val progress = if (items.isEmpty()) 0f else items.count { it.isChecked }.toFloat() / items.size
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2196F3),
            trackColor = Color(0xFFBBDEFB)
        )

        // Banner que aparece cuando todos los productos están marcados
        // Ofrece registrar el gasto en el módulo de gastos
        if (listaCompleta) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "✅ ¡Lista completada!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "¿Quieres registrar el gasto?",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = { showGastoDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "Registrar",
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Lista de productos con scroll
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items) { item ->
                ShoppingItemCard(
                    item = item,
                    // Comprobamos si el producto ya está en favoritos por nombre
                    esFavorito = favoritos.any { it.name == item.name },
                    onToggle = { checked ->
                        viewModel.toggleItem(shoppingList.id, item.id, checked)
                    },
                    onDelete = {
                        viewModel.eliminarItem(shoppingList.id, item.id)
                    },
                    onEdit = { nombre, cantidad, precio ->
                        viewModel.editarItem(shoppingList.id, item.id, nombre, cantidad, precio)
                    },
                    onFavorito = {
                        viewModel.guardarFavorito(item.name, item.quantity, item.price)
                    }
                )
            }
        }

        // Botón para abrir el diálogo de favoritos
        Button(
            onClick = { showFavoritosDialog = true },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Text(text = "⭐ Ver favoritos", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón para añadir un nuevo producto a la lista
        Button(
            onClick = { showDialog = true },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A6B))
        ) {
            Text(text = "+ Añadir producto", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
        }
    }
}