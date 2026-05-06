package com.example.qtengo.familiar.ui.tareas

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pruebas unitarias de la lógica de negocio del módulo de Tareas y recordatorios.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 */
class TareasLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Replica la lógica de ordenación de tareas de TareasViewModel.cargarTareas():
     * pendientes primero, luego por prioridad Alta → Media → Baja.
     */
    private fun ordenarTareas(tareas: List<Tarea>): List<Tarea> =
        tareas.sortedWith(
            compareBy<Tarea> { it.completada }
                .thenBy {
                    when (it.prioridad) {
                        "Alta"  -> 0
                        "Media" -> 1
                        "Baja"  -> 2
                        else    -> 3
                    }
                }
        )

    /**
     * Replica el conteo de tareas pendientes de TareasScreen.
     */
    private fun contarPendientes(tareas: List<Tarea>): Int =
        tareas.count { !it.completada }

    /**
     * Replica el conteo de tareas completadas de TareasScreen.
     */
    private fun contarCompletadas(tareas: List<Tarea>): Int =
        tareas.count { it.completada }

    /**
     * Replica el conteo de tareas urgentes (prioridad Alta y pendientes).
     */
    private fun contarUrgentes(tareas: List<Tarea>): Int =
        tareas.count { !it.completada && it.prioridad == "Alta" }

    /**
     * Replica la validación de formato de fecha de TareasViewModel.programarNotificacion().
     */
    private fun esFechaValida(fecha: String): Boolean {
        if (fecha.isBlank()) return false
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        return runCatching { sdf.parse(fecha) }.isSuccess
    }

    /**
     * Replica la validación de fecha futura de TareasViewModel.programarNotificacion().
     */
    private fun esFechaFutura(fecha: String): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        val fechaTarea = runCatching { sdf.parse(fecha) }.getOrNull() ?: return false
        val cal = Calendar.getInstance().apply {
            time = fechaTarea
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return cal.timeInMillis - System.currentTimeMillis() > 0
    }

    /**
     * Replica la validación de título no vacío de TareasScreen.
     */
    private fun esTituloValido(titulo: String): Boolean = titulo.isNotBlank()

    // ─── Tests ordenarTareas ──────────────────────────────────────────────────

    @Test
    fun `ordenarTareas coloca las tareas pendientes antes que las completadas`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Completada", completada = true, prioridad = "Media"),
            Tarea(id = "2", titulo = "Pendiente", completada = false, prioridad = "Media")
        )
        val resultado = ordenarTareas(tareas)
        assertFalse(resultado[0].completada)
        assertTrue(resultado[1].completada)
    }

    @Test
    fun `ordenarTareas ordena por prioridad Alta antes que Media`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Media", completada = false, prioridad = "Media"),
            Tarea(id = "2", titulo = "Alta", completada = false, prioridad = "Alta")
        )
        val resultado = ordenarTareas(tareas)
        assertEquals("Alta", resultado[0].prioridad)
        assertEquals("Media", resultado[1].prioridad)
    }

    @Test
    fun `ordenarTareas ordena por prioridad Media antes que Baja`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Baja", completada = false, prioridad = "Baja"),
            Tarea(id = "2", titulo = "Media", completada = false, prioridad = "Media")
        )
        val resultado = ordenarTareas(tareas)
        assertEquals("Media", resultado[0].prioridad)
        assertEquals("Baja", resultado[1].prioridad)
    }

    @Test
    fun `ordenarTareas con lista vacia devuelve lista vacia`() {
        val resultado = ordenarTareas(emptyList())
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `ordenarTareas mantiene el orden correcto con mezcla de estados y prioridades`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "Baja pendiente", completada = false, prioridad = "Baja"),
            Tarea(id = "2", titulo = "Alta completada", completada = true, prioridad = "Alta"),
            Tarea(id = "3", titulo = "Alta pendiente", completada = false, prioridad = "Alta"),
            Tarea(id = "4", titulo = "Media pendiente", completada = false, prioridad = "Media")
        )
        val resultado = ordenarTareas(tareas)
        assertEquals("Alta pendiente", resultado[0].titulo)
        assertEquals("Media pendiente", resultado[1].titulo)
        assertEquals("Baja pendiente", resultado[2].titulo)
        assertEquals("Alta completada", resultado[3].titulo)
    }

    // ─── Tests contarPendientes ───────────────────────────────────────────────

    @Test
    fun `contarPendientes devuelve 0 cuando no hay tareas`() {
        assertEquals(0, contarPendientes(emptyList()))
    }

    @Test
    fun `contarPendientes cuenta correctamente las tareas pendientes`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = false),
            Tarea(id = "2", titulo = "T2", completada = true),
            Tarea(id = "3", titulo = "T3", completada = false)
        )
        assertEquals(2, contarPendientes(tareas))
    }

    @Test
    fun `contarPendientes devuelve 0 cuando todas estan completadas`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = true),
            Tarea(id = "2", titulo = "T2", completada = true)
        )
        assertEquals(0, contarPendientes(tareas))
    }

    // ─── Tests contarCompletadas ──────────────────────────────────────────────

    @Test
    fun `contarCompletadas devuelve 0 cuando no hay tareas`() {
        assertEquals(0, contarCompletadas(emptyList()))
    }

    @Test
    fun `contarCompletadas cuenta correctamente las tareas completadas`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = true),
            Tarea(id = "2", titulo = "T2", completada = false),
            Tarea(id = "3", titulo = "T3", completada = true)
        )
        assertEquals(2, contarCompletadas(tareas))
    }

    @Test
    fun `contarCompletadas devuelve 0 cuando todas estan pendientes`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = false),
            Tarea(id = "2", titulo = "T2", completada = false)
        )
        assertEquals(0, contarCompletadas(tareas))
    }

    // ─── Tests contarUrgentes ─────────────────────────────────────────────────

    @Test
    fun `contarUrgentes devuelve 0 cuando no hay tareas`() {
        assertEquals(0, contarUrgentes(emptyList()))
    }

    @Test
    fun `contarUrgentes cuenta solo tareas pendientes de prioridad Alta`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = false, prioridad = "Alta"),
            Tarea(id = "2", titulo = "T2", completada = true, prioridad = "Alta"),
            Tarea(id = "3", titulo = "T3", completada = false, prioridad = "Media")
        )
        assertEquals(1, contarUrgentes(tareas))
    }

    @Test
    fun `contarUrgentes no cuenta tareas Alta completadas`() {
        val tareas = listOf(
            Tarea(id = "1", titulo = "T1", completada = true, prioridad = "Alta"),
            Tarea(id = "2", titulo = "T2", completada = true, prioridad = "Alta")
        )
        assertEquals(0, contarUrgentes(tareas))
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
    fun `esFechaValida devuelve false con cadena vacia`() {
        assertFalse(esFechaValida(""))
    }

    @Test
    fun `esFechaValida devuelve false con texto sin formato de fecha`() {
        assertFalse(esFechaValida("no es una fecha"))
    }

    // ─── Tests esTituloValido ─────────────────────────────────────────────────

    @Test
    fun `esTituloValido devuelve true con titulo con texto`() {
        assertTrue(esTituloValido("Revisar médico"))
    }

    @Test
    fun `esTituloValido devuelve false con titulo vacio`() {
        assertFalse(esTituloValido(""))
    }

    @Test
    fun `esTituloValido devuelve false con titulo de solo espacios`() {
        assertFalse(esTituloValido("   "))
    }
}
