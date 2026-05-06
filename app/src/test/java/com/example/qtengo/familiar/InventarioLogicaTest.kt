package com.example.qtengo.familiar.ui.inventario

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pruebas unitarias de la lógica de negocio del módulo de Inventario del hogar.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 */
class InventarioLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Replica la lógica de detección de artículos bajo mínimos de InventarioScreen.
     */
    private fun contarArticulosBajoMinimos(items: List<InventarioItem>): Int =
        items.count { it.cantidad <= it.minStock }

    /**
     * Replica el cálculo del total de unidades en stock.
     */
    private fun calcularTotalUnidades(items: List<InventarioItem>): Int =
        items.sumOf { it.cantidad }

    /**
     * Replica el conteo de artículos con fecha de caducidad.
     */
    private fun contarArticulosConFecha(items: List<InventarioItem>): Int =
        items.count { it.fechaCaducidad != null }

    /**
     * Replica la validación del formato de fecha de caducidad de AddInventarioScreen.
     */
    private fun esFechaValida(fecha: String): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).apply { isLenient = false }
        return runCatching { sdf.parse(fecha) }.isSuccess
    }

    /**
     * Replica la validación de cantidad de AddInventarioScreen.
     */
    private fun esCantidadValida(cantidad: String): Boolean {
        val num = cantidad.toIntOrNull() ?: return false
        return num > 0
    }

    /**
     * Replica la validación de stock mínimo de AddInventarioScreen.
     */
    private fun esMinStockValido(minStock: String): Boolean {
        val num = minStock.toIntOrNull() ?: return false
        return num >= 0
    }

    // ─── Tests contarArticulosBajoMinimos ─────────────────────────────────────

    @Test
    fun `contarArticulosBajoMinimos devuelve 0 cuando no hay articulos`() {
        assertEquals(0, contarArticulosBajoMinimos(emptyList()))
    }

    @Test
    fun `contarArticulosBajoMinimos detecta articulos con cantidad igual al minimo`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 1, minStock = 1),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 5, minStock = 2)
        )
        assertEquals(1, contarArticulosBajoMinimos(items))
    }

    @Test
    fun `contarArticulosBajoMinimos detecta articulos con cantidad menor al minimo`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 0, minStock = 2),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 1, minStock = 3)
        )
        assertEquals(2, contarArticulosBajoMinimos(items))
    }

    @Test
    fun `contarArticulosBajoMinimos devuelve 0 cuando todos tienen stock suficiente`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 5, minStock = 2),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 10, minStock = 3)
        )
        assertEquals(0, contarArticulosBajoMinimos(items))
    }

    @Test
    fun `contarArticulosBajoMinimos con un solo articulo bajo minimos`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 0, minStock = 5)
        )
        assertEquals(1, contarArticulosBajoMinimos(items))
    }

    // ─── Tests calcularTotalUnidades ──────────────────────────────────────────

    @Test
    fun `calcularTotalUnidades devuelve 0 cuando no hay articulos`() {
        assertEquals(0, calcularTotalUnidades(emptyList()))
    }

    @Test
    fun `calcularTotalUnidades suma correctamente todas las cantidades`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 3),
            InventarioItem(id = "2", nombre = "Aceite", cantidad = 2),
            InventarioItem(id = "3", nombre = "Leche", cantidad = 6)
        )
        assertEquals(11, calcularTotalUnidades(items))
    }

    @Test
    fun `calcularTotalUnidades con un solo articulo`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", cantidad = 5)
        )
        assertEquals(5, calcularTotalUnidades(items))
    }

    // ─── Tests contarArticulosConFecha ────────────────────────────────────────

    @Test
    fun `contarArticulosConFecha devuelve 0 cuando ningun articulo tiene fecha`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Arroz", fechaCaducidad = null),
            InventarioItem(id = "2", nombre = "Aceite", fechaCaducidad = null)
        )
        assertEquals(0, contarArticulosConFecha(items))
    }

    @Test
    fun `contarArticulosConFecha cuenta correctamente los articulos con fecha`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Yogur", fechaCaducidad = "15/05/2026"),
            InventarioItem(id = "2", nombre = "Arroz", fechaCaducidad = null),
            InventarioItem(id = "3", nombre = "Leche", fechaCaducidad = "20/05/2026")
        )
        assertEquals(2, contarArticulosConFecha(items))
    }

    @Test
    fun `contarArticulosConFecha con todos los articulos con fecha`() {
        val items = listOf(
            InventarioItem(id = "1", nombre = "Yogur", fechaCaducidad = "15/05/2026"),
            InventarioItem(id = "2", nombre = "Leche", fechaCaducidad = "20/05/2026")
        )
        assertEquals(2, contarArticulosConFecha(items))
    }

    // ─── Tests esFechaValida ──────────────────────────────────────────────────

    @Test
    fun `esFechaValida devuelve true con formato correcto`() {
        assertTrue(esFechaValida("15/05/2026"))
    }

    @Test
    fun `esFechaValida devuelve false con formato incorrecto`() {
        assertFalse(esFechaValida("2026-05-15"))
    }

    @Test
    fun `esFechaValida devuelve false con fecha imposible`() {
        assertFalse(esFechaValida("32/13/2026"))
    }

    @Test
    fun `esFechaValida devuelve false con texto sin formato de fecha`() {
        assertFalse(esFechaValida("no es una fecha"))
    }

    @Test
    fun `esFechaValida devuelve false con cadena vacia`() {
        assertFalse(esFechaValida(""))
    }

    // ─── Tests esCantidadValida ───────────────────────────────────────────────

    @Test
    fun `esCantidadValida devuelve true con numero positivo`() {
        assertTrue(esCantidadValida("5"))
    }

    @Test
    fun `esCantidadValida devuelve false con cero`() {
        assertFalse(esCantidadValida("0"))
    }

    @Test
    fun `esCantidadValida devuelve false con numero negativo`() {
        assertFalse(esCantidadValida("-1"))
    }

    @Test
    fun `esCantidadValida devuelve false con texto no numerico`() {
        assertFalse(esCantidadValida("abc"))
    }

    @Test
    fun `esCantidadValida devuelve false con cadena vacia`() {
        assertFalse(esCantidadValida(""))
    }

    // ─── Tests esMinStockValido ───────────────────────────────────────────────

    @Test
    fun `esMinStockValido devuelve true con cero`() {
        assertTrue(esMinStockValido("0"))
    }

    @Test
    fun `esMinStockValido devuelve true con numero positivo`() {
        assertTrue(esMinStockValido("3"))
    }

    @Test
    fun `esMinStockValido devuelve false con numero negativo`() {
        assertFalse(esMinStockValido("-1"))
    }

    @Test
    fun `esMinStockValido devuelve false con texto no numerico`() {
        assertFalse(esMinStockValido("abc"))
    }
}