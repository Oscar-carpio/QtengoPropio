package com.example.qtengo.familiar.ui.gastos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

class FakeGastosViewModel(
    gastosList: List<Gasto> = emptyList(),
    gastosFiltradosList: List<Gasto> = emptyList(),
    presupuestoValor: Double? = null,
    gastosRecurrentesList: List<GastoRecurrente> = emptyList(),
    fechaInicioValor: Date? = null,
    fechaFinValor: Date? = null,
    gastosPorCategoriaMap: Map<String, Double> = emptyMap()
) : GastosViewModel() {

    override val gastos: StateFlow<List<Gasto>> = MutableStateFlow(gastosList)
    override val gastosFiltrados: StateFlow<List<Gasto>> = MutableStateFlow(gastosFiltradosList)
    override val presupuesto: StateFlow<Double?> = MutableStateFlow(presupuestoValor)
    override val gastosRecurrentes: StateFlow<List<GastoRecurrente>> = MutableStateFlow(gastosRecurrentesList)
    override val fechaInicio: StateFlow<Date?> = MutableStateFlow(fechaInicioValor)
    override val fechaFin: StateFlow<Date?> = MutableStateFlow(fechaFinValor)
    override val gastosPorCategoria: StateFlow<Map<String, Double>> = MutableStateFlow(gastosPorCategoriaMap)
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow(null)
}
