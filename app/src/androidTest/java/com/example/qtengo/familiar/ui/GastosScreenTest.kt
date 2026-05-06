package com.example.qtengo.familiar.ui.gastos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import java.util.Date

// ─── Fake ViewModels (sin MockK) ──────────────────────────────────────────────

class FakeGastosViewModel(
    gastosList: List<Gasto> = emptyList(),
    gastosFiltradosList: List<Gasto> = emptyList(),
    presupuestoValor: Double? = null,
    gastosRecurrentesList: List<GastoRecurrente> = emptyList(),
    fechaInicioValor: Date? = null,
    fechaFinValor: Date? = null,
    gastosPorCategoriaMap: Map<String, Double> = emptyMap()
) : GastosViewModel() {

    override val gastos: StateFlow<List<Gasto>> = MutableStateFlow(gastosList)
    override val gastosFiltrados: StateFlow<List<Gasto>> = MutableStateFlow(gastosFiltradosList)
    override val presupuesto: StateFlow<Double?> = MutableStateFlow(presupuestoValor)
    override val gastosRecurrentes: StateFlow<List<GastoRecurrente>> = MutableStateFlow(gastosRecurrentesList)
    override val fechaInicio: StateFlow<Date?> = MutableStateFlow(fechaInicioValor)
    override val fechaFin: StateFlow<Date?> = MutableStateFlow(fechaFinValor)
    override val gastosPorCategoria: StateFlow<Map<String, Double>> = MutableStateFlow(gastosPorCategoriaMap)
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow(null)
}

// ─── Tests GastosScreen ───────────────────────────────────────────────────────

class GastosScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            GastosScreen(
                onAddGasto = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Control de gastos").assertIsDisplayed()
    }

    @Test
    fun elBotonAnadirGastoEsVisible() {
        composeTestRule.setContent {
            GastosScreen(
                onAddGasto = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("+ Añadir gasto").assertIsDisplayed()
    }

    @Test
    fun cuandoHayFiltroActivoSeMuestraElBanner() {
        val fechaInicio = Date()
        val fechaFin = Date()

        composeTestRule.setContent {
            GastosScreen(
                onAddGasto = {},
                onBack = {},
                viewModel = FakeGastosViewModel(
                    fechaInicioValor = fechaInicio,
                    fechaFinValor = fechaFin
                )
            )
        }

        composeTestRule.onNodeWithText("Limpiar", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun cuandoNoHayFiltroActivoNoSeMuestraElBanner() {
        composeTestRule.setContent {
            GastosScreen(
                onAddGasto = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Limpiar").assertDoesNotExist()
    }

    @Test
    fun losGastosSeMuestranEnLaLista() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Supermercado", cantidad = 45.0, categoria = "Alimentación", tipo = "GASTO", fecha = "01/05/2026"),
            Gasto(id = "2", descripcion = "Gasolina", cantidad = 60.0, categoria = "Transporte", tipo = "GASTO", fecha = "02/05/2026")
        )

        composeTestRule.setContent {
            GastosScreen(
                onAddGasto = {},
                onBack = {},
                viewModel = FakeGastosViewModel(
                    gastosList = gastos,
                    gastosFiltradosList = gastos
                )
            )
        }

        composeTestRule.onNodeWithText("Supermercado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gasolina").assertIsDisplayed()
    }
}

// ─── Tests AddGastoScreen ─────────────────────────────────────────────────────

class AddGastoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Añadir gasto").assertIsDisplayed()
    }

    @Test
    fun elCampoDescripcionEsVisible() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Descripción").assertIsDisplayed()
    }

    @Test
    fun elCampoCantidadEsVisible() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Cantidad (€)").assertIsDisplayed()
    }

    @Test
    fun lasCategoriasSeMuestranCorrectamente() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Alimentación").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suministros").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ocio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transporte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salud").assertIsDisplayed()
        composeTestRule.onNodeWithText("Otros").assertIsDisplayed()
    }

    @Test
    fun elBotonGuardarGastoEsVisible() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        composeTestRule.onNodeWithText("Guardar gasto").assertIsDisplayed()
    }

    @Test
    fun cuandoDescripcionEstaVaciaAlGuardarMuestraError() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        // Intentamos guardar sin rellenar nada
        composeTestRule.onNodeWithText("Guardar gasto").performClick()

        composeTestRule.onNodeWithText("La descripción no puede estar vacía").assertIsDisplayed()
    }

    @Test
    fun cuandoCantidadEsInvalidaAlGuardarMuestraError() {
        composeTestRule.setContent {
            AddGastoScreen(
                onGastoGuardado = {},
                onBack = {},
                viewModel = FakeGastosViewModel()
            )
        }

        // Rellenamos descripción pero dejamos cantidad vacía
        composeTestRule.onNodeWithText("Descripción").performClick()
        composeTestRule.onNode(hasSetTextAction() and hasText("")).performTextInput("Luz")
        composeTestRule.onNodeWithText("Guardar gasto").performClick()

        composeTestRule.onNodeWithText("Introduce un importe válido y mayor que 0").assertIsDisplayed()
    }
}
