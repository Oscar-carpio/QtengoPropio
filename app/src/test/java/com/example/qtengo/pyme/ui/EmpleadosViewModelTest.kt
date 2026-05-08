package com.example.qtengo.pyme.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.qtengo.core.data.repositories.EmployeeRepository
import com.example.qtengo.core.domain.models.Employee
import com.example.qtengo.pyme.ui.empleados.EmpleadosViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

/**
 * Tests unitarios para [EmpleadosViewModel].
 */
@ExperimentalCoroutinesApi
class EmpleadosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val employeeRepository = mockk<EmployeeRepository>(relaxed = true)
    private lateinit var viewModel: EmpleadosViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock inicial para evitar que el switchMap falle al iniciar el ViewModel
        every { employeeRepository.getByProfileFlow(any()) } returns flowOf(emptyList())
        viewModel = EmpleadosViewModel(employeeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Valida que al cambiar el perfil de trabajo, se actualice reactivamente
     * la lista de empleados mostrada en la UI.
     */
    @Test
    fun loadProfile_actualizaLaListaDeEmpleados() = runTest {
        // Given
        val listaMock = listOf(Employee(id = "1", name = "Test", profile = "PYME"))
        every { employeeRepository.getByProfileFlow("PYME") } returns flowOf(listaMock)

        val observer = mockk<Observer<List<Employee>>>(relaxed = true)
        viewModel.employees.observeForever(observer)

        // When
        viewModel.cargarPerfil("PYME")
        advanceUntilIdle()

        // Then
        verify { observer.onChanged(listaMock) }
        Assert.assertEquals("La lista de empleados debe coincidir con el mock", listaMock, viewModel.employees.value)
        
        viewModel.employees.removeObserver(observer)
    }

    /**
     * Verifica que al insertar un empleado se guarde correctamente en el repositorio.
     */
    @Test
    fun insert_registraEmpleadoCorrectamente() = runTest {
        // Given
        val nombre = "Carlos"
        val salario = 1500.0

        // When
        viewModel.insertar(nombre, "Gerente", salario, "123", "test@test.com", "Notas")
        advanceUntilIdle()

        // Then: Verificar persistencia del empleado
        coVerify { employeeRepository.insert(match { it.name == nombre && it.salary == salario }) }
    }

    /**
     * Asegura que la orden de eliminación se propague correctamente al repositorio.
     */
    @Test
    fun delete_llamaAlRepositorioDeEmpleados() = runTest {
        val id = "emp_123"
        viewModel.eliminar(id)
        advanceUntilIdle()
        coVerify { employeeRepository.delete(id) }
    }

    /**
     * Asegura que la actualización de datos del empleado se propague correctamente al repositorio.
     */
    @Test
    fun update_llamaAlRepositorioDeEmpleados() = runTest {
        val emp = Employee(id = "1", name = "Editado")
        viewModel.actualizar(emp)
        advanceUntilIdle()
        coVerify { employeeRepository.update(emp) }
    }
}
