package com.example.qtengo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.qtengo.login.ui.LoginScreen
import com.example.qtengo.login.ui.RegisterScreen
import com.example.qtengo.familiar.ui.FamiliarHomeScreen
import com.example.qtengo.familiar.ui.compra.ShoppingListScreen
import com.example.qtengo.familiar.ui.compra.ShoppingListDetailScreen
import com.example.qtengo.familiar.ui.compra.ShoppingList
import com.example.qtengo.familiar.ui.gastos.GastosScreen
import com.example.qtengo.familiar.ui.gastos.AddGastoScreen
import com.example.qtengo.familiar.ui.inventario.InventarioScreen
import com.example.qtengo.familiar.ui.inventario.AddInventarioScreen
import com.example.qtengo.familiar.ui.tareas.TareasScreen
import com.example.qtengo.pyme.ui.PymeInicioPantalla
import com.example.qtengo.pyme.ui.proveedores.ProveedoresPantalla
import com.example.qtengo.pyme.ui.productos.ProductosPantalla
import com.example.qtengo.pyme.ui.tareas.TareasPantalla
import com.example.qtengo.pyme.ui.finanzas.FinanzasPantalla
import com.example.qtengo.pyme.ui.empleados.EmpleadosPantalla
import com.example.qtengo.core.ui.theme.QtengoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.qtengo.restauracion.ui.carta.CartaScreen
import com.example.qtengo.restauracion.ui.stock.StockCocinaScreen
import com.example.qtengo.restauracion.ui.reservas.ReservasScreen
import com.example.qtengo.restauracion.ui.proveedores.ProveedoresRestauracionScreen
import com.example.qtengo.restauracion.ui.home.RestauracionHomeScreen
import com.example.qtengo.restauracion.ui.reservas.RestauracionReserva
import com.example.qtengo.restauracion.ui.proveedores.Proveedor

/**
 * Actividad principal que gestiona la navegación de la aplicación Q-Tengo.
 * Controla el estado de autenticación y redirige a la pantalla correspondiente
 * según el perfil activo del usuario (Familiar, Pyme o Restauración).
 */
class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitamos el modo de borde a borde
        enableEdgeToEdge()

        setContent {
            QtengoTheme {
                // Surface con safeDrawingPadding para respetar notch y barra de navegación
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Estados principales de sesión y navegación
                    var uid by remember { mutableStateOf<String?>(null) }
                    var perfiles by remember { mutableStateOf<List<String>>(emptyList()) }
                    var perfilActivo by remember { mutableStateOf<String?>(null) }
                    var mostrarRegistro by remember { mutableStateOf(false) }
                    var currentScreen by remember { mutableStateOf("") }
                    val selectedShoppingList = remember { mutableStateOf<ShoppingList?>(null) }
                    var showAddGasto by remember { mutableStateOf(false) }
                    var showAddInventario by remember { mutableStateOf(false) }

                    // --- Gestión de permiso de notificaciones (Android 13+) ---
                    val permisoConcedido = remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            } else true
                        )
                    }
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { concedido -> permisoConcedido.value = concedido }

                    // Solicitar permiso de notificaciones al iniciar si no está concedido
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permisoConcedido.value) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Comprobar si hay sesión activa al iniciar la app
                    LaunchedEffect(Unit) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            try {
                                val doc = firestore.collection("usuarios")
                                    .document(currentUser.uid)
                                    .get()
                                    .await()

                                @Suppress("UNCHECKED_CAST")
                                val perfilesRecuperados: List<String> = when {
                                    doc.get("perfiles") != null ->
                                        (doc.get("perfiles") as? List<String>) ?: emptyList()
                                    doc.getString("perfil") != null ->
                                        listOf(doc.getString("perfil")!!)
                                    else -> emptyList()
                                }

                                if (perfilesRecuperados.isNotEmpty()) {
                                    uid = currentUser.uid
                                    perfiles = perfilesRecuperados
                                    // Si solo hay un perfil, lo activamos directamente
                                    if (perfilesRecuperados.size == 1) {
                                        perfilActivo = perfilesRecuperados.first()
                                    }
                                } else {
                                    auth.signOut()
                                }
                            } catch (e: Exception) {
                                auth.signOut()
                            }
                        }
                    }

                    // Cierra la sesión y resetea todos los estados
                    fun cerrarSesion() {
                        auth.signOut()
                        uid = null
                        perfiles = emptyList()
                        perfilActivo = null
                        currentScreen = ""
                    }

                    // Vuelve al selector de perfiles sin cerrar sesión
                    fun cambiarPerfil() {
                        perfilActivo = null
                        currentScreen = ""
                    }

                    // --- Árbol de navegación principal ---
                    when {
                        // Mostrar pantalla de registro
                        uid == null && mostrarRegistro -> RegisterScreen(
                            onRegistroExitoso = { nuevoUid, nuevosPerfiles ->
                                uid = nuevoUid
                                perfiles = nuevosPerfiles
                                mostrarRegistro = false
                                if (nuevosPerfiles.size == 1) perfilActivo = nuevosPerfiles.first()
                            },
                            onIrALogin = { mostrarRegistro = false }
                        )

                        // Mostrar pantalla de login
                        uid == null -> LoginScreen(
                            onLoginExitoso = { nuevoUid, nuevosPerfiles ->
                                uid = nuevoUid
                                perfiles = nuevosPerfiles
                                if (nuevosPerfiles.size == 1) perfilActivo = nuevosPerfiles.first()
                            },
                            onIrARegistro = { mostrarRegistro = true }
                        )

                        // Mostrar selector de perfil si hay más de uno
                        uid != null && perfilActivo == null -> {
                            SelectorPerfilScreen(
                                perfiles = perfiles,
                                onPerfilSeleccionado = { perfilActivo = it },
                                onCerrarSesion = { cerrarSesion() }
                            )
                        }

                        // --- Navegación perfil Familiar ---
                        perfilActivo == "Familiar" -> {
                            when (currentScreen) {
                                "" -> FamiliarHomeScreen(
                                    onMenuSelected = { currentScreen = it },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Lista de la compra" -> {
                                    if (selectedShoppingList.value == null) {
                                        ShoppingListScreen(
                                            onListSelected = { selectedShoppingList.value = it },
                                            onBack = { currentScreen = "" }
                                        )
                                    } else {
                                        ShoppingListDetailScreen(
                                            shoppingList = selectedShoppingList.value!!,
                                            onBack = { selectedShoppingList.value = null }
                                        )
                                    }
                                }
                                "Control de gastos" -> {
                                    if (!showAddGasto) {
                                        GastosScreen(
                                            onAddGasto = { showAddGasto = true },
                                            onBack = { currentScreen = "" }
                                        )
                                    } else {
                                        AddGastoScreen(
                                            onGastoGuardado = { showAddGasto = false },
                                            onBack = { showAddGasto = false }
                                        )
                                    }
                                }
                                "Inventario del hogar" -> {
                                    if (!showAddInventario) {
                                        InventarioScreen(
                                            onAddItem = { showAddInventario = true },
                                            onBack = { currentScreen = "" }
                                        )
                                    } else {
                                        AddInventarioScreen(
                                            onItemGuardado = { showAddInventario = false },
                                            onBack = { showAddInventario = false }
                                        )
                                    }
                                }
                                "Tareas y recordatorios" -> TareasScreen(onBack = { currentScreen = "" })
                                else -> currentScreen = ""
                            }
                        }

                        // --- Navegación perfil Pyme ---
                        perfilActivo == "Pyme" -> {
                            when (currentScreen) {
                                "" -> PymeInicioPantalla(
                                    onMenuSelected = { currentScreen = it },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Productos / Stock" -> ProductosPantalla(
                                    profile = "PYME",
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Gastos e ingresos" -> FinanzasPantalla(
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Proveedores" -> ProveedoresPantalla(
                                    profile = "PYME",
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Empleados" -> EmpleadosPantalla(
                                    profile = "PYME",
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Agenda de Tareas" -> TareasPantalla(
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                else -> currentScreen = ""
                            }
                        }

                        // --- Navegación perfil Restauración ---
                        perfilActivo == "Restauración" -> {
                            when (currentScreen) {
                                "" -> RestauracionHomeScreen(
                                    onMenuSelected = { currentScreen = it },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Carta / Menú del día" -> CartaScreen(
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Stock de cocina" -> StockCocinaScreen(
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                "Reservas" -> ReservasScreen(onBack = { currentScreen = "" })
                                "Proveedores" -> ProveedoresRestauracionScreen(
                                    onBack = { currentScreen = "" },
                                    onLogout = { cerrarSesion() },
                                    onChangeProfile = { cambiarPerfil() }
                                )
                                else -> currentScreen = ""
                            }
                        }

                        // Perfil no reconocido — caso de seguridad
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.perfil_no_reconocido))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pantalla de selección de perfil cuando el usuario tiene más de uno asignado.
 * Permite elegir con qué perfil entrar o cerrar sesión directamente.
 */
@Composable
fun SelectorPerfilScreen(
    perfiles: List<String>,
    onPerfilSeleccionado: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título del selector
        Text(
            text = stringResource(R.string.selector_perfil_titulo),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Subtítulo informativo
        Text(
            text = stringResource(R.string.selector_perfil_subtitulo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Botón por cada perfil disponible
        perfiles.forEach { perfil ->
            Button(
                onClick = { onPerfilSeleccionado(perfil) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(perfil, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opción para cerrar sesión
        TextButton(onClick = onCerrarSesion) {
            Text(stringResource(R.string.cerrar_sesion))
        }
    }
}