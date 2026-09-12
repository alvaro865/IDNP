package com.example.helloworldcompose

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.helloworldcompose.ui.theme.HelloWorldComposeTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    // Controla qué pantalla se muestra: saludo o registro de libro
    var mostrarRegistro by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (mostrarRegistro) {
            RegistroLibroScreen()
        } else {
            Greeting(
                name = "Alvaro gustavo",
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { mostrarRegistro = !mostrarRegistro },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(if (mostrarRegistro) "Volver al saludo" else "Ir a Registro de libro")
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun RegistroLibroScreen() {
    val context = LocalContext.current

    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var paginas by remember { mutableStateOf("") }
    var contenidoLeido by remember { mutableStateOf("") }

    val nombreArchivo = "registro_libro.txt"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Registro de lectura actual", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título del libro") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = autor,
            onValueChange = { autor = it },
            label = { Text("Autor") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = paginas,
            onValueChange = { paginas = it },
            label = { Text("Páginas leídas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                val texto = "Título: $titulo\nAutor: $autor\nPáginas leídas: $paginas\n"
                try {
                    context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE).use { fos ->
                        fos.write(texto.toByteArray())
                    }
                    Log.d("RegistroLibro", "Guardado correctamente: $texto")
                } catch (e: Exception) {
                    Log.e("RegistroLibro", "Error al guardar: ${e.message}")
                }
            }) {
                Text("Guardar")
            }

            Button(onClick = {
                try {
                    val inputStream = context.openFileInput(nombreArchivo)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val texto = reader.readText()
                    reader.close()

                    Log.d("RegistroLibro", "Contenido leído:\n$texto")
                    contenidoLeido = texto
                } catch (e: Exception) {
                    Log.e("RegistroLibro", "Error al leer: ${e.message}")
                    contenidoLeido = "No se encontró ningún registro guardado."
                }
            }) {
                Text("Ver registro")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (contenidoLeido.isNotEmpty()) {
            Text(
                text = "Registro guardado:\n$contenidoLeido",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HelloWorldComposeTheme {
        Greeting("Android")
    }
}