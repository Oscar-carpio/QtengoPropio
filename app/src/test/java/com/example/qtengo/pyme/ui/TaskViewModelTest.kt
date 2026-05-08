package com.example.qtengo.pyme.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.qtengo.core.data.repositories.FinanceRepository
import com.example.qtengo.core.data.repositories.StockMovementRepository
import com.example.qtengo.core.data.repositories.TaskRepository
import com.example.qtengo.core.domain.models.Task
import com.example.qtengo.core.ui.screens.TaskViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val financeRepository = mockk<FinanceRepository>(relaxed = true)
    private val stockRepository = mockk<StockMovementRepository>(relaxed = true)
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mocks iniciales
        every { taskRepository.getByProfileFlow("PYME") } returns flowOf(emptyList())
        every { taskRepository.getByDate(any(), "PYME") } returns flowOf(emptyList())
        every { financeRepository.getByDate(any(), "PYME") } returns flowOf(emptyList())
        every { stockRepository.getMovementsByDate(any(), "PYME") } returns flowOf(emptyList())

        viewModel = TaskViewModel(taskRepository, financeRepository, stockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectDate_actualizaLaFechaSeleccionadaYDisparaNuevasConsultas() = runTest {
        val nuevaFecha = "25/12/2024"
        val tareasMock = listOf(Task(id = "1", title = "Tarea Navidad", date = nuevaFecha))

        every { taskRepository.getByDate(nuevaFecha, "PYME") } returns flowOf(tareasMock)

        val observer = mockk<Observer<List<Task>>>(relaxed = true)
        viewModel.tasksByDate.observeForever(observer)

        viewModel.seleccionarFecha(nuevaFecha)
        advanceUntilIdle()

        Assert.assertEquals(nuevaFecha, viewModel.selectedDate.value)
        verify { observer.onChanged(tareasMock) }
        
        viewModel.tasksByDate.removeObserver(observer)
    }

    @Test
    fun insertTask_llamaAlRepositorioConLaFechaDeCreacionDeHoy() = runTest {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        viewModel.insertarTarea("Comprar pan", "Descripción", "ALTA", "30/12/2024")
        advanceUntilIdle()

        coVerify {
            taskRepository.insert(match {
                it.title == "Comprar pan" &&
                        it.date == "30/12/2024" &&
                        it.createdAt == today
            })
        }
    }

    @Test
    fun updateTaskYDeleteTask_llamanCorrectamenteAlRepositorio() = runTest {
        val task = Task(id = "task_1", title = "Test")

        viewModel.actualizarTarea(task)
        advanceUntilIdle()
        coVerify { taskRepository.update(task) }

        viewModel.eliminarTarea(task)
        advanceUntilIdle()
        coVerify { taskRepository.delete("task_1") }
    }

    @Test
    fun financeByDateYStockByDate_reaccionanAlCambioDeFecha() = runTest {
        val fecha = "01/01/2024"
        
        // ACTIVAR LOS LIVEDATA para que el switchMap se ejecute
        viewModel.financeByDate.observeForever {}
        viewModel.stockByDate.observeForever {}
        
        viewModel.seleccionarFecha(fecha)
        advanceUntilIdle()

        // Verificar que se llamó a los repositorios correspondientes con esa fecha
        verify { financeRepository.getByDate(fecha, "PYME") }
        verify { stockRepository.getMovementsByDate(fecha, "PYME") }
    }
}
