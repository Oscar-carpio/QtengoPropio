package com.example.qtengo.restauracion.ui

import com.example.qtengo.restauracion.ui.carta.Plato
import com.example.qtengo.restauracion.ui.proveedores.Proveedor
import com.example.qtengo.restauracion.ui.reservas.RestauracionReserva
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de la lógica de negocio del módulo de Restauración.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 */
class RestauracionLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Replica la validación de precio positivo de CartaViewModel.
     */
    private fun esPrecioValido(precio: Double): Boolean = precio > 0

    /**
     * Replica la validación de nombre no vacío.
     */
    private fun esNombreValido(nombre: String): Boolean = nombre.isNotBlank()

    /**
     * Replica la lógica de stock no negativo de InventarioRestauracionViewModel.
     */
    private fun calcularNuevoStock(stockActual: Int, cambio: Int): Int =
        (stockActual + cambio).coerceAtLeast(0)

    /**
     * Replica la detección de productos bajo mínimos de InventarioRestauracion.
     */
    private fun esBajoMinimos(stock: Int, stockMinimo: Int): Boolean = stock <= stockMinimo

    /**
     * Replica el filtrado de platos del menú de MenuViewModel.
     */
    private fun filtrarPlatos(platos: List<Plato>, filtro: String): List<Plato> {
        val filtroLimpio = filtro.trim()
        if (filtroLimpio.isBlank()) return platos
        return platos.filter { plato ->
            plato.nombre.contains(filtroLimpio, ignoreCase = true) ||
                    plato.precio.toString().contains(filtroLimpio)
        }
    }

    /**
     * Replica el filtrado de proveedores de ProveedoresRestauracionViewModel.
     */
    private fun filtrarProveedores(proveedores: List<Proveedor>, filtro: String): List<Proveedor> {
        val filtroLimpio = filtro.trim()
        if (filtroLimpio.isBlank()) return proveedores
        return proveedores.filter { proveedor ->
            proveedor.nombre.contains(filtroLimpio, ignoreCase = true) ||
                    proveedor.telefono.contains(filtroLimpio, ignoreCase = true) ||
                    proveedor.email.contains(filtroLimpio, ignoreCase = true) ||
                    proveedor.direccion.contains(filtroLimpio, ignoreCase = true) ||
                    proveedor.notas.contains(filtroLimpio, ignoreCase = true)
        }
    }

    /**
     * Replica el filtrado de reservas de ReservasViewModel.
     */
    private fun filtrarReservas(reservas: List<RestauracionReserva>, filtro: String): List<RestauracionReserva> {
        val filtroLimpio = filtro.trim()
        if (filtroLimpio.isBlank()) return reservas
        return reservas.filter { reserva ->
            reserva.nombreCliente.contains(filtroLimpio, ignoreCase = true) ||
                    reserva.notas.contains(filtroLimpio, ignoreCase = true) ||
                    reserva.comensales.toString().contains(filtroLimpio) ||
                    reserva.estado.contains(filtroLimpio, ignoreCase = true)
        }
    }

    /**
     * Replica la ordenación de reservas por fecha de ReservasViewModel.
     */
    private fun ordenarReservasPorFecha(reservas: List<RestauracionReserva>): List<RestauracionReserva> =
        reservas.sortedBy { it.fecha }

    // ─── Tests esPrecioValido ─────────────────────────────────────────────────

    @Test
    fun `esPrecioValido devuelve true con precio positivo`() {
        assertTrue(esPrecioValido(10.0))
    }

    @Test
    fun `esPrecioValido devuelve false con precio cero`() {
        assertFalse(esPrecioValido(0.0))
    }

    @Test
    fun `esPrecioValido devuelve false con precio negativo`() {
        assertFalse(esPrecioValido(-5.0))
    }

    @Test
    fun `esPrecioValido devuelve true con precio decimal`() {
        assertTrue(esPrecioValido(9.99))
    }

    // ─── Tests esNombreValido ─────────────────────────────────────────────────

    @Test
    fun `esNombreValido devuelve true con nombre con texto`() {
        assertTrue(esNombreValido("Paella valenciana"))
    }

    @Test
    fun `esNombreValido devuelve false con nombre vacio`() {
        assertFalse(esNombreValido(""))
    }

    @Test
    fun `esNombreValido devuelve false con solo espacios`() {
        assertFalse(esNombreValido("   "))
    }

    // ─── Tests calcularNuevoStock ─────────────────────────────────────────────

    @Test
    fun `calcularNuevoStock incrementa correctamente`() {
        assertEquals(6, calcularNuevoStock(5, 1))
    }

    @Test
    fun `calcularNuevoStock decrementa correctamente`() {
        assertEquals(4, calcularNuevoStock(5, -1))
    }

    @Test
    fun `calcularNuevoStock no baja de cero`() {
        assertEquals(0, calcularNuevoStock(0, -1))
    }

    @Test
    fun `calcularNuevoStock con stock en cero y decremento devuelve cero`() {
        assertEquals(0, calcularNuevoStock(1, -5))
    }

    // ─── Tests esBajoMinimos ──────────────────────────────────────────────────

    @Test
    fun `esBajoMinimos devuelve true cuando stock es igual al minimo`() {
        assertTrue(esBajoMinimos(2, 2))
    }

    @Test
    fun `esBajoMinimos devuelve true cuando stock es menor al minimo`() {
        assertTrue(esBajoMinimos(1, 3))
    }

    @Test
    fun `esBajoMinimos devuelve false cuando stock es mayor al minimo`() {
        assertFalse(esBajoMinimos(5, 2))
    }

    @Test
    fun `esBajoMinimos devuelve true cuando stock es cero`() {
        assertTrue(esBajoMinimos(0, 1))
    }

    // ─── Tests filtrarPlatos ──────────────────────────────────────────────────

    @Test
    fun `filtrarPlatos devuelve todos los platos con filtro vacio`() {
        val platos = listOf(
            Plato(id = "1", nombre = "Paella", precio = 12.0),
            Plato(id = "2", nombre = "Gazpacho", precio = 6.0)
        )
        assertEquals(2, filtrarPlatos(platos, "").size)
    }

    @Test
    fun `filtrarPlatos filtra correctamente por nombre`() {
        val platos = listOf(
            Plato(id = "1", nombre = "Paella", precio = 12.0),
            Plato(id = "2", nombre = "Gazpacho", precio = 6.0)
        )
        val resultado = filtrarPlatos(platos, "Paella")
        assertEquals(1, resultado.size)
        assertEquals("Paella", resultado[0].nombre)
    }

    @Test
    fun `filtrarPlatos es insensible a mayusculas`() {
        val platos = listOf(
            Plato(id = "1", nombre = "Paella", precio = 12.0)
        )
        val resultado = filtrarPlatos(platos, "paella")
        assertEquals(1, resultado.size)
    }

    @Test
    fun `filtrarPlatos devuelve lista vacia sin coincidencias`() {
        val platos = listOf(
            Plato(id = "1", nombre = "Paella", precio = 12.0)
        )
        val resultado = filtrarPlatos(platos, "zzz")
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `filtrarPlatos filtra por precio`() {
        val platos = listOf(
            Plato(id = "1", nombre = "Paella", precio = 12.0),
            Plato(id = "2", nombre = "Gazpacho", precio = 6.0)
        )
        val resultado = filtrarPlatos(platos, "12.0")
        assertEquals(1, resultado.size)
        assertEquals("Paella", resultado[0].nombre)
    }

    // ─── Tests filtrarProveedores ─────────────────────────────────────────────

    @Test
    fun `filtrarProveedores devuelve todos con filtro vacio`() {
        val proveedores = listOf(
            Proveedor(id = "1", nombre = "Frutas García", telefono = "600111222", email = "garcia@test.com"),
            Proveedor(id = "2", nombre = "Carnes López", telefono = "600333444", email = "lopez@test.com")
        )
        assertEquals(2, filtrarProveedores(proveedores, "").size)
    }

    @Test
    fun `filtrarProveedores filtra por nombre`() {
        val proveedores = listOf(
            Proveedor(id = "1", nombre = "Frutas García"),
            Proveedor(id = "2", nombre = "Carnes López")
        )
        val resultado = filtrarProveedores(proveedores, "García")
        assertEquals(1, resultado.size)
        assertEquals("Frutas García", resultado[0].nombre)
    }

    @Test
    fun `filtrarProveedores filtra por telefono`() {
        val proveedores = listOf(
            Proveedor(id = "1", nombre = "Frutas García", telefono = "600111222"),
            Proveedor(id = "2", nombre = "Carnes López", telefono = "600333444")
        )
        val resultado = filtrarProveedores(proveedores, "600111222")
        assertEquals(1, resultado.size)
    }

    @Test
    fun `filtrarProveedores filtra por email`() {
        val proveedores = listOf(
            Proveedor(id = "1", nombre = "Frutas García", email = "garcia@test.com"),
            Proveedor(id = "2", nombre = "Carnes López", email = "lopez@test.com")
        )
        val resultado = filtrarProveedores(proveedores, "garcia")
        assertEquals(1, resultado.size)
    }

    @Test
    fun `filtrarProveedores devuelve lista vacia sin coincidencias`() {
        val proveedores = listOf(
            Proveedor(id = "1", nombre = "Frutas García")
        )
        val resultado = filtrarProveedores(proveedores, "zzz")
        assertTrue(resultado.isEmpty())
    }

    // ─── Tests filtrarReservas ────────────────────────────────────────────────

    @Test
    fun `filtrarReservas devuelve todas con filtro vacio`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan García", comensales = 4, estado = "Pendiente"),
            RestauracionReserva(id = "2", nombreCliente = "María López", comensales = 2, estado = "Confirmada")
        )
        assertEquals(2, filtrarReservas(reservas, "").size)
    }

    @Test
    fun `filtrarReservas filtra por nombre de cliente`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan García", comensales = 4, estado = "Pendiente"),
            RestauracionReserva(id = "2", nombreCliente = "María López", comensales = 2, estado = "Confirmada")
        )
        val resultado = filtrarReservas(reservas, "Juan")
        assertEquals(1, resultado.size)
        assertEquals("Juan García", resultado[0].nombreCliente)
    }

    @Test
    fun `filtrarReservas filtra por estado`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan García", comensales = 4, estado = "Pendiente"),
            RestauracionReserva(id = "2", nombreCliente = "María López", comensales = 2, estado = "Confirmada")
        )
        val resultado = filtrarReservas(reservas, "Confirmada")
        assertEquals(1, resultado.size)
        assertEquals("Confirmada", resultado[0].estado)
    }

    @Test
    fun `filtrarReservas filtra por numero de comensales`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan García", comensales = 4, estado = "Pendiente"),
            RestauracionReserva(id = "2", nombreCliente = "María López", comensales = 2, estado = "Confirmada")
        )
        val resultado = filtrarReservas(reservas, "4")
        assertEquals(1, resultado.size)
        assertEquals(4, resultado[0].comensales)
    }

    @Test
    fun `filtrarReservas devuelve lista vacia sin coincidencias`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan García", comensales = 4, estado = "Pendiente")
        )
        val resultado = filtrarReservas(reservas, "zzz")
        assertTrue(resultado.isEmpty())
    }

    // ─── Tests ordenarReservasPorFecha ────────────────────────────────────────

    @Test
    fun `ordenarReservasPorFecha ordena de menor a mayor fecha`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan", fecha = 9000000L),
            RestauracionReserva(id = "2", nombreCliente = "María", fecha = 1000000L)
        )
        val resultado = ordenarReservasPorFecha(reservas)
        assertEquals("María", resultado[0].nombreCliente)
        assertEquals("Juan", resultado[1].nombreCliente)
    }

    @Test
    fun `ordenarReservasPorFecha con lista vacia devuelve lista vacia`() {
        val resultado = ordenarReservasPorFecha(emptyList())
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `ordenarReservasPorFecha con una sola reserva devuelve la misma`() {
        val reservas = listOf(
            RestauracionReserva(id = "1", nombreCliente = "Juan", fecha = 1000000L)
        )
        val resultado = ordenarReservasPorFecha(reservas)
        assertEquals(1, resultado.size)
        assertEquals("Juan", resultado[0].nombreCliente)
    }
}
