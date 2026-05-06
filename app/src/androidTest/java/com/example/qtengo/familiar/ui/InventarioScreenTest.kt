package com.example.qtengo.familiar.ui.inventario

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

// ─── Fake ViewModel (sin MockK) ───────────────────────────────────────────────

class FakeInventarioViewModel(
    itemsList: List<InventarioItem> = emptyList()
) : InventarioViewModel() {

    override val items: StateFlow<List<InventarioItem>> = MutableStateFlow(itemsList)
    override val error: StateFlow<String?> = MutableStateFlow(null)
}

// ─── Tests InventarioScreen ───────────────────────────────────────────────────

class InventarioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Inventario del hogar").assertIsDisplayed()
    }

    @Test
    fun cuandoNoHayArticulosSeMuestraMensajeVacio() {
        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("No hay artículos. ¡Añade uno!").assertIsDisplayed()
    }

    @Test
    fun elBotonAnadirArticuloEsVisible() {
        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("+ Añadir artículo").assertIsDisplayed()
    }

    @Test
    fun losArticulosSeMuestranEnLaLista() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 3, minStock = 1),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 2, minStock = 1)
        )

        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("Arroz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aceite").assertIsDisplayed()
    }

    @Test
    fun cuandoHayArticulosBajoMinimosSeMuestraAviso() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 0, minStock = 2),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 1, minStock = 3)
        )

        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("⚠️ 2 artículos bajo mínimos").assertIsDisplayed()
    }

    @Test
    fun cuandoHayUnArticuloBajoMinimosSeMuestraEnSingular() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 0, minStock = 2),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 5, minStock = 1)
        )

        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("⚠️ 1 artículo bajo mínimos").assertIsDisplayed()
    }

    @Test
    fun laSeccionArticulosEsVisible() {
        composeTestRule.setContent {
            InventarioScreen(
                onAddItem = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Artículos").assertIsDisplayed()
    }
}

// ─── Tests AddInventarioScreen ────────────────────────────────────────────────

class AddInventarioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Añadir artículo").assertIsDisplayed()
    }

    @Test
    fun elCampoNombreEsVisible() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Nombre del artículo").assertIsDisplayed()
    }

    @Test
    fun losChipsDeUbicacionSeMuestran() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Cocina").assertIsDisplayed()
        composeTestRule.onNodeWithText("Despensa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lavadero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trastero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Baño").assertIsDisplayed()
        composeTestRule.onNodeWithText("Otros").assertIsDisplayed()
    }

    @Test
    fun elBotonGuardarArticuloEsVisible() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Guardar artículo").assertIsDisplayed()
    }

    @Test
    fun cuandoNombreEstaVacioAlGuardarMuestraError() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Guardar artículo").performClick()

        composeTestRule.onNodeWithText("El nombre no puede estar vacío").assertIsDisplayed()
    }

    @Test
    fun elSwitchFechaCaducidadEsVisible() {
        composeTestRule.setContent {
            AddInventarioScreen(
                onItemGuardado = {},
                onBack = {},
                viewModel = FakeInventarioViewModel()
            )
        }

        composeTestRule.onNodeWithText("Tiene fecha de caducidad").assertIsDisplayed()
    }
}

