package com.example.listafiltrable_compose8

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.listafiltrable_compose8.ui.theme.ListaFiltrable_Compose8Theme

data class Edificio(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val descripcion: String
)

/**
 * Lee assets/edificios.txt. Formato por línea: id|nombre|categoria|descripcion
 * Ignora líneas vacías, comentarios (#) y líneas mal formadas.
 */
fun cargarEdificios(context: Context): List<Edificio> {
    return try {
        context.assets.open("edificios.txt").bufferedReader().useLines { lineas ->
            lineas
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { linea ->
                    val partes = linea.split("|")
                    val id = partes.getOrNull(0)?.trim()?.toIntOrNull()
                    if (partes.size == 4 && id != null) {
                        Edificio(id, partes[1].trim(), partes[2].trim(), partes[3].trim())
                    } else null
                }
                .toList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun iconoParaCategoria(categoria: String): ImageVector = when (categoria) {
    "Servicios" -> Icons.Default.LocalLibrary
    "Académico" -> Icons.Default.School
    "Deportes" -> Icons.Default.SportsSoccer
    "Eventos" -> Icons.Default.Event
    else -> Icons.Default.Place
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListaFiltrable_Compose8Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListaEdificios()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListaEdificiosPreview() {
    ListaFiltrable_Compose8Theme {
        ListaEdificios()
    }
}

@Composable
fun ListaEdificios() {
    val context = LocalContext.current
    // Ejercicio 1: los datos vienen del archivo de texto, no del código
    val edificios = remember { cargarEdificios(context) }

    var texto by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var menuAbierto by remember { mutableStateOf(false) }

    // Las categorías se generan a partir de los datos cargados
    val categorias = remember(edificios) {
        listOf("Todas") + edificios.map { it.categoria }.distinct()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Buscar edificio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ejercicio 3: menú desplegable para filtrar por categoría
        Box {
            OutlinedButton(
                onClick = { menuAbierto = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Categoría: $categoriaSeleccionada")
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir menú")
            }
            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }
            ) {
                categorias.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria) },
                        onClick = {
                            categoriaSeleccionada = categoria
                            menuAbierto = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filtro combinado: nombre Y categoría
        val edificiosFiltrados = edificios.filter {
            it.nombre.contains(texto, ignoreCase = true) &&
                    (categoriaSeleccionada == "Todas" || it.categoria == categoriaSeleccionada)
        }

        if (edificiosFiltrados.isEmpty()) {
            Text("Sin resultados", color = Color.Gray)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(edificiosFiltrados, key = { it.id }) { edificio ->
                FilaEdificio(edificio)
            }
        }
    }
}

@Composable
fun FilaEdificio(edificio: Edificio) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // Ejercicio 2: ícono según la categoría
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconoParaCategoria(edificio.categoria),
                contentDescription = edificio.categoria,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = edificio.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = edificio.categoria,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = edificio.descripcion,
                    fontSize = 14.sp
                )
            }
        }
    }
}