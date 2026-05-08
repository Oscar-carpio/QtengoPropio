package com.example.qtengo.pyme.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.qtengo.core.data.repositories.ProductRepository
import com.example.qtengo.core.data.repositories.StockMovementRepository
import com.example.qtengo.core.domain.models.Product
import com.example.qtengo.pyme.ui.productos.ProductosViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ProductosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val stockRepository = mockk<StockMovementRepository>(relaxed = true)
    private lateinit var viewModel: ProductosViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { productRepository.getByProfileFlow(any()) } returns flowOf(emptyList())
        viewModel = ProductosViewModel(productRepository, stockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun lowStockProducts_filtraCorrectamenteProductosConPocoStock() = runTest {
        val productos = listOf(
            Product(id = "1", name = "Mucho Stock", quantity = 10.0, minStock = 5.0),
            Product(id = "2", name = "Poco Stock", quantity = 2.0, minStock = 5.0)
        )
        every { productRepository.getByProfileFlow("PYME") } returns flowOf(productos)

        val observer = mockk<Observer<List<Product>>>(relaxed = true)
        viewModel.lowStockProducts.observeForever(observer)

        advanceUntilIdle()

        val result = viewModel.lowStockProducts.value
        Assert.assertEquals(1, result?.size)
        Assert.assertEquals("Poco Stock", result?.get(0)?.name)
    }

    @Test
    fun updateQuantity_registraUnMovimientoDeStockSiLaCantidadCambia() = runTest {
        val product = Product(id = "prod1", name = "Martillo", quantity = 10.0, profile = "PYME")

        viewModel.actualizarCantidad(product, 15.0)
        advanceUntilIdle()

        // Verifica que se actualiza el producto
        coVerify { productRepository.update(match { it.quantity == 15.0 }, "prod1") }

        // Verifica que se registra el movimiento (+5.0)
        coVerify {
            stockRepository.insert(match {
                it.productName == "Martillo" && it.quantityChanged == 5.0 && it.newQuantity == 15.0
            })
        }
    }

    @Test
    fun insert_registraProductoYMovimientoInicial() = runTest {
        val product = Product(id = "new", name = "Nuevo", quantity = 20.0, profile = "PYME")

        viewModel.insertar(product)
        advanceUntilIdle()

        // Verificamos que se insertó un producto con el mismo nombre y cantidad
        // sin comprobar el customId generado automáticamente
        coVerify {
            productRepository.insert(match {
                it.name == product.name && it.quantity == product.quantity
            })
        }
        coVerify { stockRepository.insert(match { it.quantityChanged == 20.0 }) }
    }
}
