package com.example.qtengo.login.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.qtengo.R

@Composable
fun LoginScreen(
    onLoginExitoso: (uid: String, perfiles: List<String>) -> Unit,
    onIrARegistro: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    val prefs = remember {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "qtengo_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var email by remember { mutableStateOf(prefs.getString("ultimo_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("password_guardada", "") ?: "") }
    var recordarPassword by remember { mutableStateOf(prefs.getBoolean("recordar_password", false)) }

    // Estado del diálogo de recuperación
    var mostrarDialogoRecuperacion by remember { mutableStateOf(false) }
    var emailRecuperacion by remember { mutableStateOf("") }
    var mensajeRecuperacion by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val success = authState as AuthState.Success
            prefs.edit().putString("ultimo_email", email).apply()
            if (recordarPassword) {
                prefs.edit()
                    .putString("password_guardada", password)
                    .putBoolean("recordar_password", true)
                    .apply()
            } else {
                prefs.edit()
                    .remove("password_guardada")
                    .putBoolean("recordar_password", false)
                    .apply()
            }
            onLoginExitoso(success.uid, success.perfiles)
            authViewModel.reset()
        }
    }

    // Diálogo de recuperación de contraseña
    if (mostrarDialogoRecuperacion) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoRecuperacion = false
                emailRecuperacion = ""
                mensajeRecuperacion = null
            },
            title = { Text("Recuperar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Introduce tu email y te enviaremos un enlace para restablecer tu contraseña.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = emailRecuperacion,
                        onValueChange = { emailRecuperacion = it },
                        label = { Text(stringResource(R.string.email)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    mensajeRecuperacion?.let { mensaje ->
                        Text(
                            text = mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.recuperarContrasena(emailRecuperacion) { exito ->
                        mensajeRecuperacion = "Si ese email está registrado, recibirás un enlace en breve."
                    }
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoRecuperacion = false
                    emailRecuperacion = ""
                    mensajeRecuperacion = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.iniciar_sesion), style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.contrasena)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = recordarPassword,
                onCheckedChange = { recordarPassword = it }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.recordar_contrasena), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).mensaje,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { authViewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.entrar))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botón de recuperación de contraseña
        TextButton(onClick = {
            emailRecuperacion = email
            mostrarDialogoRecuperacion = true
        }) {
            Text("¿Olvidaste tu contraseña?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onIrARegistro) {
            Text(stringResource(R.string.no_tienes_cuenta))
        }
    }
}