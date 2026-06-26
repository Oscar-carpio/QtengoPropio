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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.qtengo.core.ui.theme.QtengoTheme
import com.example.qtengo.familiar.ui.FamiliarHomeScreen
import com.example.qtengo.familiar.ui.compra.ShoppingListScreen
import com.example.qtengo.familiar.ui.compra.ShoppingListDetailScreen
import com.example.qtengo.familiar.ui.gastos.GastosScreen
import com.example.qtengo.familiar.ui.gastos.AddGastoScreen
import com.example.qtengo.familiar.ui.inventario.InventarioScreen
import com.example.qtengo.familiar.ui.inventario.AddInventarioScreen
import com.example.qtengo.familiar.ui.tareas.TareasScreen
import com.example.qtengo.login.ui.LoginScreen
import com.example.qtengo.login.ui.RegisterScreen
import com.example.qtengo.pyme.ui.PymeInicioPantalla
import com.example.qtengo.pyme.ui.proveedores.ProveedoresPantalla
import com.example.qtengo.pyme.ui.productos.ProductosPantalla
import com.example.qtengo.pyme.ui.tareas.TareasPantalla
import com.example.qtengo.pyme.ui.finanzas.FinanzasPantalla
import com.example.qtengo.pyme.ui.empleados.EmpleadosPantalla
import com.example.qtengo.restauracion.ui.carta.CartaScreen
import com.example.qtengo.restauracion.ui.stock.StockCocinaScreen
import com.example.qtengo.restauracion.ui.reservas.ReservasScreen
import com.example.qtengo.restauracion.ui.proveedores.ProveedoresRestauracionScreen
import com.example.qtengo.restauracion.ui.home.RestauracionHomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            QtengoTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val session: SessionViewModel = viewModel()

                    // Permiso de notificaciones (Android 13+)
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

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && !permisoConcedido.value
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Verificar sesión activa al arrancar y navegar al destino correcto
                    LaunchedEffect(Unit) {
                        session.verificarSesionActiva()
                    }

                    // Reaccionar a cambios de sesión para navegar automáticamente
                    LaunchedEffect(session.uid, session.perfilActivo) {
                        when {
                            session.uid == null -> {
                                // Sin sesión: ir al login limpiando todo el historial
                                navController.navigate(Rutas.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            session.perfilActivo == null -> {
                                // Sesión activa pero sin perfil: ir al selector
                                navController.navigate(Rutas.SELECTOR_PERFIL) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            session.perfilActivo == Rutas.FAMILIAR -> {
                                navController.navigate(Rutas.FAMILIAR_HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            session.perfilActivo == Rutas.PYME -> {
                                navController.navigate(Rutas.PYME_HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            session.perfilActivo == Rutas.RESTAURACION -> {
                                navController.navigate(Rutas.RESTAURACION_HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Rutas.LOGIN
                    ) {

                        // --- Auth ---
                        composable(Rutas.LOGIN) {
                            LoginScreen(
                                onLoginExitoso = { nuevoUid, nuevosPerfiles ->
                                    session.onLoginExitoso(nuevoUid, nuevosPerfiles)
                                },
                                onIrARegistro = {
                                    navController.navigate(Rutas.REGISTRO)
                                }
                            )
                        }

                        composable(Rutas.REGISTRO) {
                            RegisterScreen(
                                onRegistroExitoso = { nuevoUid, nuevosPerfiles ->
                                    session.onRegistroExitoso(nuevoUid, nuevosPerfiles)
                                },
                                onIrALogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Rutas.SELECTOR_PERFIL) {
                            SelectorPerfilScreen(
                                perfiles = session.perfiles,
                                onPerfilSeleccionado = { perfil ->
                                    session.seleccionarPerfil(perfil)
                                },
                                onCerrarSesion = {
                                    session.cerrarSesion()
                                }
                            )
                        }

                        // --- Familiar ---
                        composable(Rutas.FAMILIAR_HOME) {
                            FamiliarHomeScreen(
                                onMenuSelected = { ruta ->
                                    navController.navigate(ruta)
                                },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.LISTA_COMPRA) {
                            ShoppingListScreen(
                                onListSelected = { listaId ->
                                    navController.navigate("${Rutas.DETALLE_LISTA}/$listaId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "${Rutas.DETALLE_LISTA}/{listaId}",
                            arguments = listOf(
                                navArgument("listaId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val listaId = backStackEntry.arguments?.getString("listaId") ?: ""
                            ShoppingListDetailScreen(
                                listaId = listaId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.CONTROL_GASTOS) {
                            GastosScreen(
                                onAddGasto = { navController.navigate(Rutas.ADD_GASTO) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.ADD_GASTO) {
                            AddGastoScreen(
                                onGastoGuardado = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.INVENTARIO_HOGAR) {
                            InventarioScreen(
                                onAddItem = { navController.navigate(Rutas.ADD_INVENTARIO) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.ADD_INVENTARIO) {
                            AddInventarioScreen(
                                onItemGuardado = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.TAREAS_FAMILIAR) {
                            TareasScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // --- Pyme ---
                        composable(Rutas.PYME_HOME) {
                            PymeInicioPantalla(
                                onMenuSelected = { ruta ->
                                    navController.navigate(ruta)
                                },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.PRODUCTOS_STOCK) {
                            ProductosPantalla(
                                profile = Rutas.PYME,
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.GASTOS_INGRESOS) {
                            FinanzasPantalla(
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.PROVEEDORES_PYME) {
                            ProveedoresPantalla(
                                profile = Rutas.PYME,
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.EMPLEADOS) {
                            EmpleadosPantalla(
                                profile = Rutas.PYME,
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.AGENDA_TAREAS) {
                            TareasPantalla(
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        // --- Restauración ---
                        composable(Rutas.RESTAURACION_HOME) {
                            RestauracionHomeScreen(
                                onMenuSelected = { ruta ->
                                    navController.navigate(ruta)
                                },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.CARTA_MENU) {
                            CartaScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.STOCK_COCINA) {
                            StockCocinaScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        composable(Rutas.RESERVAS) {
                            ReservasScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Rutas.PROVEEDORES_RESTAURACION) {
                            ProveedoresRestauracionScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = { session.cerrarSesion() },
                                onChangeProfile = { session.cambiarPerfil() }
                            )
                        }

                        // Perfil no reconocido
                        composable("error") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.perfil_no_reconocido))
                            }
                        }
                    }
                }
            }
        }
    }
}
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
        Text(
            text = stringResource(R.string.selector_perfil_titulo),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.selector_perfil_subtitulo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
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
        TextButton(onClick = onCerrarSesion) {
            Text(stringResource(R.string.cerrar_sesion))
        }
    }
}