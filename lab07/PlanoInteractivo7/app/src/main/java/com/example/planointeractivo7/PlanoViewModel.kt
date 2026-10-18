package com.example.planointeractivo7

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel que mantiene el estado del plano (ambientes, marcadores y el marcador
// seleccionado), siguiendo el mismo patrón que SeleccionViewModel del Laboratorio 05:
// MutableStateFlow privado expuesto como StateFlow de solo lectura.
class PlanoViewModel(application: Application) : AndroidViewModel(application) {

    private val _ambientes = MutableStateFlow<List<Ambiente>>(emptyList())
    val ambientes: StateFlow<List<Ambiente>> = _ambientes.asStateFlow()

    private val _marcadores = MutableStateFlow<List<Marcador>>(emptyList())
    val marcadores: StateFlow<List<Marcador>> = _marcadores.asStateFlow()

    private val _marcadorSeleccionado = MutableStateFlow<Marcador?>(null)
    val marcadorSeleccionado: StateFlow<Marcador?> = _marcadorSeleccionado.asStateFlow()

    init {
        cargarPlano()
    }

    // Carga los ambientes desde assets/ambientes.json (vía AmbienteRepository) y arma
    // los rectángulos y los marcadores táctiles correspondientes.
    private fun cargarPlano() {
        val info = AmbienteRepository.cargarAmbientes(getApplication())

        _ambientes.value = info.map { dato ->
            Ambiente(
                topLeft = Offset(dato.x, dato.y),
                size = Size(dato.ancho, dato.alto),
                color = Color(android.graphics.Color.parseColor(dato.color)),
                etiqueta = dato.etiqueta
            )
        }

        _marcadores.value = info.map { dato ->
            Marcador(
                offset = Offset(dato.x + dato.ancho - 25f, dato.y + dato.alto - 25f),
                radio = 18f,
                titulo = dato.etiqueta,
                descripcion = dato.descripcion
            )
        }
    }

    // Busca si el punto tocado cae dentro de algún marcador y, de ser así, lo selecciona.
    fun onTapPlano(offset: Offset) {
        val marcadorTocado = _marcadores.value.firstOrNull { marcador ->
            (offset - marcador.offset).getDistance() <= marcador.radio
        }
        if (marcadorTocado != null) {
            _marcadorSeleccionado.value = marcadorTocado
        }
    }

    // Cierra el diálogo de información del marcador seleccionado.
    fun cerrarDialogo() {
        _marcadorSeleccionado.value = null
    }
}
