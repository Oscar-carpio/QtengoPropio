package com.example.qtengo.pyme.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.qtengo.core.data.repositories.EmployeeRepository
import com.example.qtengo.core.data.repositories.FinanceRepository
import com.example.qtengo.core.domain.models.Employee
import com.example.qtengo.core.domain.models.FinanceMovement
import com.example.qtengo.pyme.ui.finanzas.FinanzasViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests unitarios para [FinanzasViewModel].
 * 
 * Valida la lógica de negocio financiera, incluyendo la combinación de movimientos manuales
 * con nóminas autogeneradas y las reglas de protección de datos.
 */
@ExperimentalCoroutinesApi
class FinanzasViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val financeRepository = mockk<FinanceRepository>(relaxed = true)
    private val employeeRepository = mockk<EmployeeRepository>(relaxed = true)

    // Flujos mutables para simular la base de datos de forma reactiva
    private val financeFlow = MutableStateFlow<List<FinanceMovement>>(emptyList())
    private val employeeFlow = MutableStateFlow<List<Employee>>(emptyList())

    private lateinit var viewModel: FinanzasViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Configuración de los mocks para devolver los flujos controlados
        every { financeRepository.getAllFlow("PYME") } returns financeFlow
        every { employeeRepository.getByProfileFlow("PYME") } returns employeeFlow

        viewModel = FinanzasViewModel(financeRepository, employeeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifica que el ViewModel combine en una única lista los movimientos de caja
     * y las nóminas generadas a partir de la tabla de empleados.
     */
    @Test
    fun movements_combinaCorrectamenteMovimientosDeCajaYNominasDeEmpleados() = runTest {
        // Given
        val movimientosCaja = listOf(
            FinanceMovement(id = "1", concept = "Venta", amount = 100.0, type = "INGRESO", profile = "PYME")
        )
        val empleados = listOf(
            Employee(id = "emp1", name = "Juan", salary = 1200.0, profile = "PYME")
        )

        // Activamos el LiveData para que empiece a recolectar los flujos
        viewModel.movements.observeForever {}

        // When: Simulamos la llegada de datos desde el repositorio
        financeFlow.value = movimientosCaja
        employeeFlow.value = empleados

        // Then
        val result = viewModel.movements.value
        Assert.assertEquals("Debe haber 2 movimientos en total", 2, result?.size)
        Assert.assertEquals("El primer movimiento debe ser la venta", "Venta", result?.get(0)?.concept)
        Assert.assertEquals("El segundo debe ser la nómina autogenerada", "Nómina de Juan", result?.get(1)?.concept)
    }

    /**
     * Valida que el cálculo de ingresos y gastos totales sea preciso y sume correctamente
     * las nóminas como gastos adicionales.
     */
    @Test
    fun calculoDeTotalIngresosYTotalGastosEsCorrecto() = runTest {
        // Given
        val movimientos = listOf(
            FinanceMovement(amount = 500.0, type = "INGRESO"),
            FinanceMovement(amount = 200.0, type = "GASTO")
        )
        val empleados = listOf(
            Employee(salary = 1000.0) // Esto genera un gasto de nómina de 1000.0
        )

        // Observar transformaciones para activarlas
        viewModel.totalIngresos.observeForever {}
        viewModel.totalGastos.observeForever {}

        // When
        financeFlow.value = movimientos
        employeeFlow.value = empleados

        // Then
        Assert.assertEquals("Total ingresos debe ser 500", 500.0, viewModel.totalIngresos.value ?: 0.0, 0.1)
        Assert.assertEquals("Total gastos debe ser 1200 (200 + 1000)", 1200.0, viewModel.totalGastos.value ?: 0.0, 0.1)
    }

    /**
     * Comprueba que la inserción de un nuevo movimiento se delegue correctamente al repositorio.
     */
    @Test
    fun insert_llamaAlRepositorioDeFinanzas() = runTest {
        val movement = FinanceMovement(concept = "Test", amount = 10.0, type = "INGRESO")
        viewModel.insertar(movement)
        coVerify { financeRepository.insert(movement) }
    }

    /**
     * Asegura que los movimientos normales sí puedan ser eliminados.
     */
    @Test
    fun delete_eliminaSiElIDNoEmpiezaPorNomina_() = runTest {
        viewModel.eliminar("mov_123")
        coVerify { financeRepository.delete("mov_123") }
    }
}
