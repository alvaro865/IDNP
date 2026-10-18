package com.example.planointeractivo7

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

// Representa un registro "crudo" tal como viene del archivo assets/ambientes.json,
// antes de convertirse en los modelos de UI (Ambiente / Marcador).
data class AmbienteInfo(
    val etiqueta: String,
    val descripcion: String,
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
    val color: String
)

// Repositorio encargado de leer el archivo JSON de assets/ y convertirlo en una
// lista de datos, siguiendo el mismo patrón de persistencia basado en archivos
// usado en el Laboratorio 03 (lectura desde assets con el Context de la app).
object AmbienteRepository {

    private const val NOMBRE_ARCHIVO = "ambientes.json"

    fun cargarAmbientes(context: Context): List<AmbienteInfo> {
        val json = leerArchivoDesdeAssets(context, NOMBRE_ARCHIVO)
        return parsearAmbientes(json)
    }

    private fun leerArchivoDesdeAssets(context: Context, nombreArchivo: String): String {
        context.assets.open(nombreArchivo).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                return reader.readText()
            }
        }
    }

    private fun parsearAmbientes(json: String): List<AmbienteInfo> {
        val arreglo = JSONArray(json)
        val lista = mutableListOf<AmbienteInfo>()
        for (i in 0 until arreglo.length()) {
            val obj = arreglo.getJSONObject(i)
            lista.add(
                AmbienteInfo(
                    etiqueta = obj.getString("etiqueta"),
                    descripcion = obj.getString("descripcion"),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    ancho = obj.getDouble("ancho").toFloat(),
                    alto = obj.getDouble("alto").toFloat(),
                    color = obj.getString("color")
                )
            )
        }
        return lista
    }
}
