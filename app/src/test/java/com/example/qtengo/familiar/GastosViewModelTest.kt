package com.example.qtengo.familiar.ui.gastos

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pruebas unitarias de la lógica de negocio del módulo de gastos.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 *
 * Dado que GastosViewModel inicializa Firebase en su constructor,
 * las pruebas se aplican directamente sobre la lógica de negocio
 * mediante funciones auxiliares que replican los cálculos del ViewModel.
 */
class GastosLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    private fun fechaMesActual(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())

    private fun fechaMesAnterior(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(cal.time)
    }

    /**
     * Replica la lógica de GastosViewModel.totalGastos():
     * suma los gastos de tipo "GASTO" del mes actual.
     */
    private fun calcularTotalGastos(gastos: List<Gasto>): Double {
        val mesActual = SimpleDateFormat("MM/yyyy", Locale("es", "ES")).format(Date())
        return gastos
            .filter { it.tipo == "GASTO" && it.fecha.endsWith(mesActual) }
            .sumOf { it.cantidad }
    }

    /**
     * Replica la lógica de GastosViewModel.totalIngresos():
     * suma todos los movimientos de tipo "INGRESO".
     */
    private fun calcularTotalIngresos(gastos: List<Gasto>): Double =
        gastos.filter { it.tipo == "INGRESO" }.sumOf { it.cantidad }

    /**
     * Replica la lógica de GastosViewModel.totalRecurrentes():
     * suma todos los gastos fijos recurrentes.
     */
    private fun calcularTotalRecurrentes(recurrentes: List<GastoRecurrente>): Double =
        recurrentes.sumOf { it.cantidad }

    /**
     * Replica la lógica de GastosViewModel.gastosPorCategoria:
     * agrupa los gastos por categoría y suma su importe.
     */
    private fun calcularGastosPorCategoria(gastos: List<Gasto>): Map<String, Double> =
        gastos.filter { it.tipo == "GASTO" }
            .groupBy { it.categoria.ifBlank { "Sin categoría" } }
            .mapValues { (_, items) -> items.sumOf { it.cantidad } }

    // ─── Tests totalGastos ────────────────────────────────────────────────────

    @Test
    fun `totalGastos devuelve 0 cuando no hay gastos`() {
        assertEquals(0.0, calcularTotalGastos(emptyList()), 0.001)
    }

    @Test
    fun `totalGastos suma solo los gastos del mes actual`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Luz", cantidad = 50.0, tipo = "GASTO", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Gas", cantidad = 30.0, tipo = "GASTO", fecha = fechaMesActual()),
            Gasto(id = "3", descripcion = "Factura antigua", cantidad = 100.0, tipo = "GASTO", fecha = fechaMesAnterior())
        )
        assertEquals(80.0, calcularTotalGastos(gastos), 0.001)
    }

    @Test
    fun `totalGastos no incluye los ingresos`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Sueldo", cantidad = 1500.0, tipo = "INGRESO", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Luz", cantidad = 60.0, tipo = "GASTO", fecha = fechaMesActual())
        )
        assertEquals(60.0, calcularTotalGastos(gastos), 0.001)
    }

    @Test
    fun `totalGastos no incluye gastos de meses anteriores`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Factura vieja", cantidad = 200.0, tipo = "GASTO", fecha = fechaMesAnterior())
        )
        assertEquals(0.0, calcularTotalGastos(gastos), 0.001)
    }

    @Test
    fun `totalGastos con lista de un solo gasto del mes actual`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Supermercado", cantidad = 45.50, tipo = "GASTO", fecha = fechaMesActual())
        )
        assertEquals(45.50, calcularTotalGastos(gastos), 0.001)
    }

    // ─── Tests totalIngresos ──────────────────────────────────────────────────

    @Test
    fun `totalIngresos devuelve 0 cuando no hay ingresos`() {
        assertEquals(0.0, calcularTotalIngresos(emptyList()), 0.001)
    }

    @Test
    fun `totalIngresos suma todos los ingresos sin filtrar por mes`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Sueldo", cantidad = 1500.0, tipo = "INGRESO", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Extra", cantidad = 300.0, tipo = "INGRESO", fecha = fechaMesAnterior())
        )
        assertEquals(1800.0, calcularTotalIngresos(gastos), 0.001)
    }

    @Test
    fun `totalIngresos no incluye los gastos`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Luz", cantidad = 60.0, tipo = "GASTO", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Agua", cantidad = 30.0, tipo = "GASTO", fecha = fechaMesActual())
        )
        assertEquals(0.0, calcularTotalIngresos(gastos), 0.001)
    }

    // ─── Tests totalRecurrentes ───────────────────────────────────────────────

    @Test
    fun `totalRecurrentes devuelve 0 cuando no hay gastos recurrentes`() {
        assertEquals(0.0, calcularTotalRecurrentes(emptyList()), 0.001)
    }

    @Test
    fun `totalRecurrentes suma todos los gastos fijos`() {
        val recurrentes = listOf(
            GastoRecurrente(id = "1", descripcion = "Netflix", cantidad = 15.0),
            GastoRecurrente(id = "2", descripcion = "Gym", cantidad = 40.0),
            GastoRecurrente(id = "3", descripcion = "Alquiler", cantidad = 800.0)
        )
        assertEquals(855.0, calcularTotalRecurrentes(recurrentes), 0.001)
    }

    @Test
    fun `totalRecurrentes con un solo gasto fijo`() {
        val recurrentes = listOf(
            GastoRecurrente(id = "1", descripcion = "Netflix", cantidad = 15.99)
        )
        assertEquals(15.99, calcularTotalRecurrentes(recurrentes), 0.001)
    }

    // ─── Tests gastosPorCategoria ─────────────────────────────────────────────

    @Test
    fun `gastosPorCategoria agrupa correctamente por categoria`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Mercadona", cantidad = 50.0, tipo = "GASTO", categoria = "Alimentación", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Carrefour", cantidad = 30.0, tipo = "GASTO", categoria = "Alimentación", fecha = fechaMesActual()),
            Gasto(id = "3", descripcion = "Bus", cantidad = 20.0, tipo = "GASTO", categoria = "Transporte", fecha = fechaMesActual())
        )
        val resultado = calcularGastosPorCategoria(gastos)
        assertEquals(80.0, resultado["Alimentación"] ?: 0.0, 0.001)
        assertEquals(20.0, resultado["Transporte"] ?: 0.0, 0.001)
    }

    @Test
    fun `gastosPorCategoria no incluye ingresos`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Sueldo", cantidad = 1500.0, tipo = "INGRESO", categoria = "Trabajo", fecha = fechaMesActual()),
            Gasto(id = "2", descripcion = "Luz", cantidad = 60.0, tipo = "GASTO", categoria = "Suministros", fecha = fechaMesActual())
        )
        val resultado = calcularGastosPorCategoria(gastos)
        assertNull(resultado["Trabajo"])
        assertEquals(60.0, resultado["Suministros"] ?: 0.0, 0.001)
    }

    @Test
    fun `gastosPorCategoria agrupa sin categoria como Sin categoria`() {
        val gastos = listOf(
            Gasto(id = "1", descripcion = "Varios", cantidad = 25.0, tipo = "GASTO", categoria = "", fecha = fechaMesActual())
        )
        val resultado = calcularGastosPorCategoria(gastos)
        assertEquals(25.0, resultado["Sin categoría"] ?: 0.0, 0.001)
    }

    @Test
    fun `gastosPorCategoria devuelve mapa vacio sin gastos`() {
        val resultado = calcularGastosPorCategoria(emptyList())
        assertTrue(resultado.isEmpty())
    }
}