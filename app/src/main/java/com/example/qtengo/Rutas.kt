package com.example.qtengo

object Rutas {

    // --- Rutas de navegación (identificadores internos) ---
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val SELECTOR_PERFIL = "selector_perfil"

    // Familiar
    const val FAMILIAR_HOME = "familiar_home"
    const val LISTA_COMPRA = "lista_compra"
    const val DETALLE_LISTA = "detalle_lista"         // uso: "detalle_lista/{listaId}"
    const val CONTROL_GASTOS = "control_gastos"
    const val ADD_GASTO = "add_gasto"
    const val INVENTARIO_HOGAR = "inventario_hogar"
    const val ADD_INVENTARIO = "add_inventario"
    const val TAREAS_FAMILIAR = "tareas_familiar"

    // Pyme
    const val PYME_HOME = "pyme_home"
    const val PRODUCTOS_STOCK = "productos_stock"
    const val GASTOS_INGRESOS = "gastos_ingresos"
    const val PROVEEDORES_PYME = "proveedores_pyme"
    const val EMPLEADOS = "empleados"
    const val AGENDA_TAREAS = "agenda_tareas"

    // Restauración
    const val RESTAURACION_HOME = "restauracion_home"
    const val CARTA_MENU = "carta_menu"
    const val STOCK_COCINA = "stock_cocina"
    const val RESERVAS = "reservas"
    const val PROVEEDORES_RESTAURACION = "proveedores_restauracion"

    // --- Nombres de menú (texto visible para el usuario) ---
    const val MENU_LISTA_COMPRA = "Lista de la compra"
    const val MENU_CONTROL_GASTOS = "Control de gastos"
    const val MENU_INVENTARIO_HOGAR = "Inventario del hogar"
    const val MENU_TAREAS_RECORDATORIOS = "Tareas y recordatorios"

    const val MENU_PRODUCTOS_STOCK = "Productos / Stock"
    const val MENU_GASTOS_INGRESOS = "Gastos e ingresos"
    const val MENU_PROVEEDORES = "Proveedores"
    const val MENU_EMPLEADOS = "Empleados"
    const val MENU_AGENDA_TAREAS = "Agenda de Tareas"

    const val MENU_CARTA_MENU = "Carta / Menú del día"
    const val MENU_STOCK_COCINA = "Stock de cocina"
    const val MENU_RESERVAS = "Reservas"

    // --- Nombres de perfil ---
    const val FAMILIAR = "Familiar"
    const val PYME = "Pyme"
    const val RESTAURACION = "Restauración"
}