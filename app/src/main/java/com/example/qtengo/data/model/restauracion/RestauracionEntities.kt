package com.example.qtengo.data.model.restauracion

data class RestauracionProducto(
    val id_producto: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val stock: Int = 0,
    val stock_minimo: Int = 0,
    val precio: Double = 0.0
)

data class RestauracionProveedor(
    val id_proveedor: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val email: String = "",
    val telefono: String = ""
)

data class RestauracionPedido(
    val id_pedido: String = "",
    val fecha: Long = 0L,
    val id_proveedor: String = ""
)

data class RestauracionLineaPedido(
    val id_pedido: String = "",
    val id_producto: String = "",
    val precio: Double = 0.0
)

data class RestauracionMovimiento(
    val id_movimiento: String = "",
    val tipo: String = "",
    val fecha: Long = 0L,
    val cantidad: Int = 0
)

data class RestauracionReserva(
    val id: String = "",
    val nombreCliente: String = "",
    val fecha: Long = 0L,
    val comensales: Int = 0,
    val notas: String = ""
)

data class RestauracionPlato(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val disponible: Boolean = true
)
