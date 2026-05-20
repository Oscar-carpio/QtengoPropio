package com.example.qtengo.core.domain.models

data class User(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val perfil: String = "" // FAMILIA | HOSTELERIA | PYME
)