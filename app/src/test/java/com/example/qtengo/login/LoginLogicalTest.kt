package com.example.qtengo.login.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de la lógica de validación del módulo de Login y Registro.
 * No requieren emulador ni conexión a Firebase.
 * Se ejecutan directamente en el ordenador con JUnit.
 */
class LoginLogicaTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Replica la validación de formato de email de AuthViewModel.
     */
    private fun esEmailValido(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }

    /**
     * Replica la validación de longitud de contraseña de AuthViewModel.
     */
    private fun tieneMinimoCochCaracteres(password: String): Boolean =
        password.length >= 8

    /**
     * Replica la validación de mayúscula en contraseña de AuthViewModel.
     */
    private fun tieneMayuscula(password: String): Boolean =
        password.any { it.isUpperCase() }

    /**
     * Replica la validación de número en contraseña de AuthViewModel.
     */
    private fun tieneNumero(password: String): Boolean =
        password.any { it.isDigit() }

    /**
     * Replica la validación de contraseña completa de AuthViewModel.
     */
    private fun esPasswordValida(password: String): Boolean =
        tieneMinimoCochCaracteres(password) &&
                tieneMayuscula(password) &&
                tieneNumero(password)

    /**
     * Replica la validación de campos obligatorios del registro de RegisterScreen.
     */
    private fun sonCamposRegistroValidos(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        perfiles: List<String>
    ): Pair<Boolean, String> {
        if (nombre.trim().isEmpty()) return Pair(false, "El nombre no puede estar vacío")
        if (apellidos.trim().isEmpty()) return Pair(false, "Los apellidos no pueden estar vacíos")
        if (email.trim().isEmpty()) return Pair(false, "El email no puede estar vacío")
        if (password.trim().isEmpty()) return Pair(false, "La contraseña no puede estar vacía")
        if (perfiles.isEmpty()) return Pair(false, "Debes seleccionar al menos un perfil")
        return Pair(true, "")
    }

    // ─── Tests esEmailValido ──────────────────────────────────────────────────

    @Test
    fun `esEmailValido devuelve true con email correcto`() {
        assertTrue(esEmailValido("usuario@ejemplo.com"))
    }

    @Test
    fun `esEmailValido devuelve true con email con subdominio`() {
        assertTrue(esEmailValido("usuario@mail.ejemplo.com"))
    }

    @Test
    fun `esEmailValido devuelve false sin arroba`() {
        assertFalse(esEmailValido("usuarioejemplo.com"))
    }

    @Test
    fun `esEmailValido devuelve false sin dominio`() {
        assertFalse(esEmailValido("usuario@"))
    }

    @Test
    fun `esEmailValido devuelve false con cadena vacia`() {
        assertFalse(esEmailValido(""))
    }

    @Test
    fun `esEmailValido devuelve false con solo texto`() {
        assertFalse(esEmailValido("noesuncorreo"))
    }

    // ─── Tests tieneMinimoCochCaracteres ──────────────────────────────────────

    @Test
    fun `tieneMinimoCochCaracteres devuelve true con 8 caracteres exactos`() {
        assertTrue(tieneMinimoCochCaracteres("Abcde12!"))
    }

    @Test
    fun `tieneMinimoCochCaracteres devuelve true con mas de 8 caracteres`() {
        assertTrue(tieneMinimoCochCaracteres("Abcde1234!"))
    }

    @Test
    fun `tieneMinimoCochCaracteres devuelve false con menos de 8 caracteres`() {
        assertFalse(tieneMinimoCochCaracteres("Abc123"))
    }

    @Test
    fun `tieneMinimoCochCaracteres devuelve false con cadena vacia`() {
        assertFalse(tieneMinimoCochCaracteres(""))
    }

    // ─── Tests tieneMayuscula ─────────────────────────────────────────────────

    @Test
    fun `tieneMayuscula devuelve true con al menos una mayuscula`() {
        assertTrue(tieneMayuscula("abcDef123"))
    }

    @Test
    fun `tieneMayuscula devuelve true con varias mayusculas`() {
        assertTrue(tieneMayuscula("ABCdef123"))
    }

    @Test
    fun `tieneMayuscula devuelve false sin mayusculas`() {
        assertFalse(tieneMayuscula("abcdef123"))
    }

    @Test
    fun `tieneMayuscula devuelve false con cadena vacia`() {
        assertFalse(tieneMayuscula(""))
    }

    // ─── Tests tieneNumero ────────────────────────────────────────────────────

    @Test
    fun `tieneNumero devuelve true con al menos un numero`() {
        assertTrue(tieneNumero("Abcdef1"))
    }

    @Test
    fun `tieneNumero devuelve true con varios numeros`() {
        assertTrue(tieneNumero("Abcdef123"))
    }

    @Test
    fun `tieneNumero devuelve false sin numeros`() {
        assertFalse(tieneNumero("Abcdefgh"))
    }

    @Test
    fun `tieneNumero devuelve false con cadena vacia`() {
        assertFalse(tieneNumero(""))
    }

    // ─── Tests esPasswordValida ───────────────────────────────────────────────

    @Test
    fun `esPasswordValida devuelve true con password correcta`() {
        assertTrue(esPasswordValida("Password1"))
    }

    @Test
    fun `esPasswordValida devuelve false sin mayuscula`() {
        assertFalse(esPasswordValida("password1"))
    }

    @Test
    fun `esPasswordValida devuelve false sin numero`() {
        assertFalse(esPasswordValida("Password"))
    }

    @Test
    fun `esPasswordValida devuelve false con menos de 8 caracteres`() {
        assertFalse(esPasswordValida("Pass1"))
    }

    @Test
    fun `esPasswordValida devuelve false con cadena vacia`() {
        assertFalse(esPasswordValida(""))
    }

    // ─── Tests sonCamposRegistroValidos ───────────────────────────────────────

    @Test
    fun `sonCamposRegistroValidos devuelve true con todos los campos correctos`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "García",
            email = "juan@test.com",
            password = "Password1",
            perfiles = listOf("Familiar")
        )
        assertTrue(resultado.first)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando nombre esta vacio`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "",
            apellidos = "García",
            email = "juan@test.com",
            password = "Password1",
            perfiles = listOf("Familiar")
        )
        assertFalse(resultado.first)
        assertEquals("El nombre no puede estar vacío", resultado.second)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando apellidos esta vacio`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "",
            email = "juan@test.com",
            password = "Password1",
            perfiles = listOf("Familiar")
        )
        assertFalse(resultado.first)
        assertEquals("Los apellidos no pueden estar vacíos", resultado.second)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando email esta vacio`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "García",
            email = "",
            password = "Password1",
            perfiles = listOf("Familiar")
        )
        assertFalse(resultado.first)
        assertEquals("El email no puede estar vacío", resultado.second)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando password esta vacia`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "García",
            email = "juan@test.com",
            password = "",
            perfiles = listOf("Familiar")
        )
        assertFalse(resultado.first)
        assertEquals("La contraseña no puede estar vacía", resultado.second)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando no hay perfiles seleccionados`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "García",
            email = "juan@test.com",
            password = "Password1",
            perfiles = emptyList()
        )
        assertFalse(resultado.first)
        assertEquals("Debes seleccionar al menos un perfil", resultado.second)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve true con multiples perfiles seleccionados`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "Juan",
            apellidos = "García",
            email = "juan@test.com",
            password = "Password1",
            perfiles = listOf("Familiar", "Pyme")
        )
        assertTrue(resultado.first)
    }

    @Test
    fun `sonCamposRegistroValidos devuelve error cuando nombre tiene solo espacios`() {
        val resultado = sonCamposRegistroValidos(
            nombre = "   ",
            apellidos = "García",
            email = "juan@test.com",
            password = "Password1",
            perfiles = listOf("Familiar")
        )
        assertFalse(resultado.first)
        assertEquals("El nombre no puede estar vacío", resultado.second)
    }
}