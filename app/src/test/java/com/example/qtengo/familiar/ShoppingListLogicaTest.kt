package com.example.qtengo.familiar.ui.compra

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de la lógica de negocio del módulo de Lista de la compra.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 *
 * Dado que ShoppingListViewModel inicializa Firebase en su constructor,
 * las pruebas se aplican directamente sobre la lógica de negocio
 * mediante funciones auxiliares que replican los cálculos del ViewModel.
 */
class ShoppingListLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Replica la lógica de filtrado de listas por nombre de ShoppingListScreen.
     */
    private fun filtrarListas(listas: List<ShoppingList>, query: String): List<ShoppingList> =
        listas.filter { it.name.contains(query, ignoreCase = true) }

    /**
     * Replica el cálculo del precio total de los productos de una lista.
     */
    private fun calcularPrecioTotal(items: List<ShoppingItem>): Double =
        items.sumOf { it.price }

    /**
     * Replica la lógica de detección de lista completada.
     */
    private fun esListaCompletada(items: List<ShoppingItem>): Boolean =
        items.isNotEmpty() && items.all { it.isChecked }

    /**
     * Replica el cálculo del progreso de la lista.
     */
    private fun contarProductosMarcados(items: List<ShoppingItem>): Int =
        items.count { it.isChecked }

    /**
     * Replica la lógica de deduplicación de favoritos por nombre.
     */
    private fun existeFavoritoDuplicado(favoritos: List<FavoriteItem>, nombre: String): Boolean =
        favoritos.any { it.name.trim() == nombre.trim() }

    // ─── Tests filtrarListas ──────────────────────────────────────────────────

    @Test
    fun `filtrarListas devuelve todas las listas cuando la query esta vacia`() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal"),
            ShoppingList(id = "2", name = "Mercado"),
            ShoppingList(id = "3", name = "Farmacia")
        )
        val resultado = filtrarListas(listas, "")
        assertEquals(3, resultado.size)
    }

    @Test
    fun `filtrarListas devuelve solo las listas que coinciden con la query`() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal"),
            ShoppingList(id = "2", name = "Mercado"),
            ShoppingList(id = "3", name = "Compra mensual")
        )
        val resultado = filtrarListas(listas, "Compra")
        assertEquals(2, resultado.size)
        assertTrue(resultado.any { it.name == "Compra semanal" })
        assertTrue(resultado.any { it.name == "Compra mensual" })
    }

    @Test
    fun `filtrarListas es insensible a mayusculas`() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Mercado"),
            ShoppingList(id = "2", name = "Compra")
        )
        val resultado = filtrarListas(listas, "mercado")
        assertEquals(1, resultado.size)
        assertEquals("Mercado", resultado[0].name)
    }

    @Test
    fun `filtrarListas devuelve lista vacia cuando no hay coincidencias`() {
        val listas = listOf(
            ShoppingList(id = "1", name = "Compra semanal"),
            ShoppingList(id = "2", name = "Mercado")
        )
        val resultado = filtrarListas(listas, "zzz")
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `filtrarListas con lista vacia devuelve lista vacia`() {
        val resultado = filtrarListas(emptyList(), "Compra")
        assertTrue(resultado.isEmpty())
    }

    // ─── Tests calcularPrecioTotal ────────────────────────────────────────────

    @Test
    fun `calcularPrecioTotal devuelve 0 cuando no hay productos`() {
        assertEquals(0.0, calcularPrecioTotal(emptyList()), 0.001)
    }

    @Test
    fun `calcularPrecioTotal suma correctamente los precios`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", price = 1.50),
            ShoppingItem(id = "2", name = "Pan", price = 0.90),
            ShoppingItem(id = "3", name = "Aceite", price = 3.20)
        )
        assertEquals(5.60, calcularPrecioTotal(items), 0.001)
    }

    @Test
    fun `calcularPrecioTotal incluye productos marcados y no marcados`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", price = 1.50, isChecked = true),
            ShoppingItem(id = "2", name = "Pan", price = 0.90, isChecked = false)
        )
        assertEquals(2.40, calcularPrecioTotal(items), 0.001)
    }

    @Test
    fun `calcularPrecioTotal con un solo producto`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", price = 1.50)
        )
        assertEquals(1.50, calcularPrecioTotal(items), 0.001)
    }

    // ─── Tests esListaCompletada ──────────────────────────────────────────────

    @Test
    fun `esListaCompletada devuelve false cuando la lista esta vacia`() {
        assertFalse(esListaCompletada(emptyList()))
    }

    @Test
    fun `esListaCompletada devuelve true cuando todos los productos estan marcados`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = true),
            ShoppingItem(id = "2", name = "Pan", isChecked = true)
        )
        assertTrue(esListaCompletada(items))
    }

    @Test
    fun `esListaCompletada devuelve false cuando hay productos sin marcar`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = true),
            ShoppingItem(id = "2", name = "Pan", isChecked = false)
        )
        assertFalse(esListaCompletada(items))
    }

    @Test
    fun `esListaCompletada devuelve false cuando ningun producto esta marcado`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = false),
            ShoppingItem(id = "2", name = "Pan", isChecked = false)
        )
        assertFalse(esListaCompletada(items))
    }

    // ─── Tests contarProductosMarcados ────────────────────────────────────────

    @Test
    fun `contarProductosMarcados devuelve 0 cuando no hay productos marcados`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = false),
            ShoppingItem(id = "2", name = "Pan", isChecked = false)
        )
        assertEquals(0, contarProductosMarcados(items))
    }

    @Test
    fun `contarProductosMarcados cuenta correctamente los productos marcados`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = true),
            ShoppingItem(id = "2", name = "Pan", isChecked = false),
            ShoppingItem(id = "3", name = "Aceite", isChecked = true)
        )
        assertEquals(2, contarProductosMarcados(items))
    }

    @Test
    fun `contarProductosMarcados devuelve el total cuando todos estan marcados`() {
        val items = listOf(
            ShoppingItem(id = "1", name = "Leche", isChecked = true),
            ShoppingItem(id = "2", name = "Pan", isChecked = true),
            ShoppingItem(id = "3", name = "Aceite", isChecked = true)
        )
        assertEquals(3, contarProductosMarcados(items))
    }

    // ─── Tests existeFavoritoDuplicado ────────────────────────────────────────

    @Test
    fun `existeFavoritoDuplicado devuelve false cuando no hay favoritos`() {
        assertFalse(existeFavoritoDuplicado(emptyList(), "Pan"))
    }

    @Test
    fun `existeFavoritoDuplicado devuelve true cuando el favorito ya existe`() {
        val favoritos = listOf(
            FavoriteItem(id = "1", name = "Pan"),
            FavoriteItem(id = "2", name = "Leche")
        )
        assertTrue(existeFavoritoDuplicado(favoritos, "Pan"))
    }

    @Test
    fun `existeFavoritoDuplicado devuelve false cuando el favorito no existe`() {
        val favoritos = listOf(
            FavoriteItem(id = "1", name = "Pan"),
            FavoriteItem(id = "2", name = "Leche")
        )
        assertFalse(existeFavoritoDuplicado(favoritos, "Aceite"))
    }

    @Test
    fun `existeFavoritoDuplicado ignora espacios extra en el nombre`() {
        val favoritos = listOf(
            FavoriteItem(id = "1", name = "Pan")
        )
        assertTrue(existeFavoritoDuplicado(favoritos, "  Pan  "))
    }
}
