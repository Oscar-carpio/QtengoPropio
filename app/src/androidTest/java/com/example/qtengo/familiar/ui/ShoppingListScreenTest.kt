package com.example.qtengo.familiar.ui.compra

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

// ─── Fake ViewModels (sin MockK) ──────────────────────────────────────────────

/**
 * ViewModel falso para pruebas de la pantalla de listas.
 * No se conecta a Firestore. Devuelve datos controlados por el test.
 */
class FakeShoppingListViewModel(
    listas: List<ShoppingList> = emptyList(),
    itemsList: List<ShoppingItem> = emptyList(),
    favoritosList: List<FavoriteItem> = emptyList()
) : ShoppingListViewModel() {

    override val lists: StateFlow<List<ShoppingList>> = MutableStateFlow(listas)
    override val items: StateFlow<List<ShoppingItem>> = MutableStateFlow(itemsList)
    override val favoritos: StateFlow<List<FavoriteItem>> = MutableStateFlow(favoritosList)
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
}

// ─── Tests ShoppingListScreen ─────────────────────────────────────────────────

class ShoppingListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cuandoNoHayListasSeMuestraMensajeVacio() {
        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("No hay listas aún. ¡Crea una!").assertIsDisplayed()
    }

    @Test
    fun cuandoHayListasSeMuestranEnPantalla() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal", itemCount = 5, date = "01/05/2026"),
            ShoppingList(id = "2", name = "Mercado", itemCount = 3, date = "02/05/2026")
        )

        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel(listas = listas)
            )
        }

        composeTestRule.onNodeWithText("Compra semanal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mercado").assertIsDisplayed()
    }

    @Test
    fun elTituloDeLaPantallaEsVisible() {
        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("Lista de la compra").assertIsDisplayed()
    }

    @Test
    fun elBotonNuevaListaEsVisible() {
        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("+ Nueva lista").assertIsDisplayed()
    }

    @Test
    fun laBusquedaFiltraLasListasPorNombre() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal", itemCount = 5, date = "01/05/2026"),
            ShoppingList(id = "2", name = "Mercado", itemCount = 3, date = "02/05/2026")
        )

        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel(listas = listas)
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Mercado")

        // Verificamos que la lista que NO coincide desaparece
        composeTestRule.onNodeWithText("Compra semanal").assertDoesNotExist()
    }

    @Test
    fun cuandoBusquedaSinResultadosMuestraMensaje() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal", itemCount = 5, date = "01/05/2026")
        )

        composeTestRule.setContent {
            ShoppingListScreen(
                onListSelected = {},
                onBack = {},
                viewModel = FakeShoppingListViewModel(listas = listas)
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzz")
        composeTestRule.onNodeWithText("No se encontraron listas").assertIsDisplayed()
    }
}

// ─── Tests ShoppingListDetailScreen ──────────────────────────────────────────

class ShoppingListDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val listaEjemplo = ShoppingList(
        id = "lista1",
        name = "Compra semanal",
        itemCount = 2,
        date = "01/05/2026"
    )

    @Test
    fun elNombreDeLaListaApareceEnElHeader() {
        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("Compra semanal").assertIsDisplayed()
    }

    @Test
    fun losProductosDeLaListaSeVisualizanCorrectamente() {
        val items = listOf(
            ShoppingItem(id = "p1", name = "Leche", quantity = "2", price = 1.5, isChecked = false),
            ShoppingItem(id = "p2", name = "Pan", quantity = "1", price = 0.9, isChecked = false)
        )

        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("Leche").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pan").assertIsDisplayed()
    }

    @Test
    fun elContadorDeProductosMuestraElProgreso() {
        val items = listOf(
            ShoppingItem(id = "p1", name = "Leche", quantity = "2", price = 1.5, isChecked = true),
            ShoppingItem(id = "p2", name = "Pan", quantity = "1", price = 0.9, isChecked = false)
        )

        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("1/2 productos").assertIsDisplayed()
    }

    @Test
    fun cuandoTodosLosProductosEstanMarcadosApareceElBannerDeListaCompletada() {
        val items = listOf(
            ShoppingItem(id = "p1", name = "Leche", quantity = "2", price = 1.5, isChecked = true),
            ShoppingItem(id = "p2", name = "Pan", quantity = "1", price = 0.9, isChecked = true)
        )

        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("✅ ¡Lista completada!").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Quieres registrar el gasto?").assertIsDisplayed()
    }

    @Test
    fun elBotonAnadirProductoEsVisible() {
        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("+ Añadir producto").assertIsDisplayed()
    }

    @Test
    fun elBotonVerFavoritosEsVisible() {
        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel()
            )
        }

        composeTestRule.onNodeWithText("⭐ Ver favoritos").assertIsDisplayed()
    }

    @Test
    fun elTotalDePrecioSeMuestraEnElHeader() {
        val items = listOf(
            ShoppingItem(id = "p1", name = "Leche", quantity = "2", price = 1.50, isChecked = false),
            ShoppingItem(id = "p2", name = "Pan", quantity = "1", price = 0.90, isChecked = false)
        )

        composeTestRule.setContent {
            ShoppingListDetailScreen(
                shoppingList = listaEjemplo,
                onBack = {},
                viewModel = FakeShoppingListViewModel(itemsList = items)
            )
        }

        composeTestRule.onNodeWithText("Total: 2,40 €").assertIsDisplayed()
    }
}