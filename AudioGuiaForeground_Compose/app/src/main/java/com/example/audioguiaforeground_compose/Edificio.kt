package com.example.audioguiaforeground_compose

import android.content.ContentResolver
import android.content.Context
import androidx.annotation.RawRes

/** Edificio de la audioguía con su narración guardada en res/raw. */
data class Edificio(val nombre: String, @param:RawRes val audio: Int)

/** Edificios incluidos en la app. Para agregar otro, copia su audio a res/raw y añádelo aquí. */
val EDIFICIOS = listOf(
    Edificio("Biblioteca Central", R.raw.edificio_biblioteca)
)

/**
 * URI del audio en la forma android.resource://paquete/raw/nombre.
 * Se usa el nombre y no el id numérico porque el id puede cambiar entre compilaciones
 * y esta URI se guarda en Room.
 */
fun Edificio.uriAudio(context: Context): String =
    "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/" +
        context.resources.getResourceEntryName(audio)
