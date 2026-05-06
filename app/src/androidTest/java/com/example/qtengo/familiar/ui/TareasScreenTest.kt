package com.example.qtengo.familiar.ui.tareas

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

// ─── Fake ViewModel (sin MockK) ───────────────────────────────────────────────

class FakeTareasViewModel(
    tareasList: List<Tarea> = emptyList()
) : TareasViewModel(ApplicationProvider.getApplicationContext<Application>()) {

    override val tareas: StateFlow<List<Tarea>> = MutableStateFlow(tareasList)
    override val error: StateFlow<String?> = MutableStateFlow(null)
}

// ─── Tests TareasScreen ───────────────────────────────────────────────────────

class TareasScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel()
            )
        }

        composeTestRule.onNodeWithText("Tareas y recordatorios").assertIsDisplayed()
    }

    @Test
    fun cuandoNoHayTareasSeMuestraMensajeVacio() {
        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel()
            )
        }

        composeTestRule.onNodeWithText("No hay tareas. ¡Añade una!").assertIsDisplayed()
    }

    @Test
    fun elBotonNuevaTareaEsVisible() {
        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel()
            )
        }

        composeTestRule.onNodeWithText("+ Nueva tarea").assertIsDisplayed()
    }

    @Test
    fun losContadoresDeResumenSeMuestranCorrectamente() {
        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel()
            )
        }

        composeTestRule.onNodeWithText("Pendientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completadas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Urgentes").assertIsDisplayed()
    }

    @Test
    fun lasAreasPendientesSeMuestranEnLaLista() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Revisar médico", completada = false, prioridad = "Alta"),
            Tarea(id = "2", titulo = "Pagar factura", completada = false, prioridad = "Media")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        composeTestRule.onNodeWithText("Revisar médico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pagar factura").assertIsDisplayed()
    }

    @Test
    fun lasTasksCompletadasSeMuestranEnSuSeccion() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Comprar pan", completada = true, prioridad = "Baja")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        composeTestRule.onNodeWithText("Comprar pan").assertIsDisplayed()
    }

    @Test
    fun elContadorPendientesMuestraElNumeroCorreecto() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Tarea 1", completada = false, prioridad = "Media"),
            Tarea(id = "2", titulo = "Tarea 2", completada = false, prioridad = "Baja"),
            Tarea(id = "3", titulo = "Tarea 3", completada = true, prioridad = "Alta")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        // Header: "2 pendientes · 1 completadas"
        composeTestRule.onNodeWithText("2 pendientes · 1 completadas").assertIsDisplayed()
    }

    @Test
    fun elContadorUrgentesMuestraLasTareasDeAltaPrioridad() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Urgente 1", completada = false, prioridad = "Alta"),
            Tarea(id = "2", titulo = "Urgente 2", completada = false, prioridad = "Alta"),
            Tarea(id = "3", titulo = "Normal", completada = false, prioridad = "Media")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        // La card de urgentes debe mostrar "2"
        composeTestRule.onAllNodesWithText("2").onFirst().assertIsDisplayed()
    }

    @Test
    fun cuandoHayTareasPendientesSeMuestraLaSeccionPendientes() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Tarea pendiente", completada = false, prioridad = "Media")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        composeTestRule.onAllNodesWithText("Pendientes").onFirst().assertIsDisplayed()
    }

    @Test
    fun cuandoHayTareasCompletadasSeMuestraLaSeccionCompletadas() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Tarea completada", completada = true, prioridad = "Baja")
        )

        composeTestRule.setContent {
            TareasScreen(
                onBack = {},
                viewModel = FakeTareasViewModel(tareasList = tareas)
            )
        }

        composeTestRule.onAllNodesWithText("Completadas").onFirst().assertIsDisplayed()
    }
}
