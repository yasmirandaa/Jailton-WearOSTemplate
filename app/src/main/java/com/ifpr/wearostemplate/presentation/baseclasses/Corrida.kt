package com.ifpr.wearostemplate.presentation.baseclasses

data class Corrida(
    val distanciaKm: Double = 0.0,
    val dataHora: String = "",
    var id: String = "",
    var tempoSegundos: Long = 0L,
    var ritmoMedio: Double = 0.0,
    var velocidadeMedia: Double = 0.0,
    var calorias: Double = 0.0,
    var data: Long = 0L
)