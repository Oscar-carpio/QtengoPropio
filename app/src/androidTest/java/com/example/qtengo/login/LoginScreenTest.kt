package com.example.qtengo.login.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

// ─── Fake ViewModel (sin MockK) ───────────────────────────────────────────────

class FakeAuthViewModel(
    estadoInicial: AuthState = AuthState.Idle
) : AuthViewModel() {

    private val _fakeAuthState = MutableStateFlow<AuthState>(estadoInicial)
    override val authState: StateFlow<AuthState> = _fakeAuthState

    fun setEstado(estado: AuthState) {
        _fakeAuthState.value = estado
    }
}

// ─── Tests LoginScreen ────────────────────────────────────────────────────────

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }

    @Test
    fun elCampoEmailEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun elCampoContrasenaEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test
    fun elCheckboxRecordarContrasenaEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Recordar contraseña").assertIsDisplayed()
    }

    @Test
    fun elBotonEntrarEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Entrar").assertIsDisplayed()
    }

    @Test
    fun elEnlaceRegistroEsVisible() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("¿No tienes cuenta? Regístrate").assertIsDisplayed()
    }

    @Test
    fun cuandoHayErrorSeMuestraElMensaje() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel(
                    estadoInicial = AuthState.Error("Email o contraseña incorrectos")
                )
            )
        }

        composeTestRule.onNodeWithText("Email o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun cuandoEstaLoadingElBotonEstaDeshabilitado() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginExitoso = { _, _ -> },
                onIrARegistro = {},
                authViewModel = FakeAuthViewModel(estadoInicial = AuthState.Loading)
            )
        }

        composeTestRule.onNodeWithText("Entrar").assertDoesNotExist()
    }
}

// ─── Tests RegisterScreen ─────────────────────────────────────────────────────

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
    }

    @Test
    fun losCamposDelFormularioSonVisibles() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Nombre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apellidos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test
    fun losPerfilesDisponiblesSonVisibles() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Familiar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restauración").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pyme").assertIsDisplayed()
    }

    @Test
    fun elBotonRegistrarseEsVisible() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Registrarse").assertIsDisplayed()
    }

    @Test
    fun elEnlaceIniciarSesionEsVisible() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("¿Ya tienes cuenta? Inicia sesión").assertIsDisplayed()
    }

    @Test
    fun cuandoCamposVaciosAlRegistrarMuestraErrorNombre() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Registrarse").performClick()
        composeTestRule.onNodeWithText("El nombre no puede estar vacío").assertIsDisplayed()
    }

    @Test
    fun cuandoNoSeSeleccionaPerfilMuestraError() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        // Rellenamos todos los campos menos el perfil
        composeTestRule.onNodeWithText("Nombre").performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("Juan")
        composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("García")
        composeTestRule.onAllNodes(hasSetTextAction())[2].performTextInput("juan@test.com")
        composeTestRule.onAllNodes(hasSetTextAction())[3].performTextInput("Password1")

        composeTestRule.onNodeWithText("Registrarse").performClick()

        composeTestRule.onNodeWithText("Debes seleccionar al menos un perfil").assertIsDisplayed()
    }

    @Test
    fun cuandoHayErrorDeAuthSeMuestraElMensaje() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel(
                    estadoInicial = AuthState.Error("El email no tiene un formato válido")
                )
            )
        }

        composeTestRule.onNodeWithText("El email no tiene un formato válido").assertIsDisplayed()
    }

    @Test
    fun elTextoDeRequisitosContrasenaEsVisible() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegistroExitoso = { _, _ -> },
                onIrALogin = {},
                authViewModel = FakeAuthViewModel()
            )
        }

        composeTestRule.onNodeWithText("Mínimo 8 caracteres, una mayúscula y un número").assertIsDisplayed()
    }
}
