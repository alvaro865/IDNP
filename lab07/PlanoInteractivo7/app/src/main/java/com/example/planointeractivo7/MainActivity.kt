package com.example.planointeractivo7

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.planointeractivo7.ui.theme.PlanoInteractivo7Theme

// ---------------------------------------------------------
// RUTAS DE NAVEGACIÓN (mismo patrón sealed class del Laboratorio 05)
// ---------------------------------------------------------

sealed class Screen(val route: String) {
    object Mapa : Screen("mapa")
    object Plano : Screen("plano")
}

// Representa un ambiente físico (salón, laboratorio o patio) del plano.
data class Ambiente(
    val topLeft: Offset,
    val size: Size,
    val color: Color,
    val etiqueta: String
)

// Representa el punto táctil asociado a un ambiente, con la info que se muestra en el diálogo.
data class Marcador(
    val offset: Offset,
    val radio: Float,
    val titulo: String,
    val descripcion: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlanoInteractivo7Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

// ---------------------------------------------------------
// NAVEGACIÓN PRINCIPAL: MapaScreen -> Plano interactivo
// ---------------------------------------------------------

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Mapa.route) {
        composable(Screen.Mapa.route) {
            MapaScreen(
                onVerPlano = { navController.navigate(Screen.Plano.route) }
            )
        }
        composable(Screen.Plano.route) {
            PlanoAmbiente()
        }
    }
}

// ---------------------------------------------------------
// MAPA SCREEN: pantalla de entrada con la tarjeta que lleva al plano
// ---------------------------------------------------------

@Composable
fun MapaScreen(onVerPlano: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Mapa del campus")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVerPlano() }
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Pabellón de Ingeniería")
                Text(text = "Ver plano interactivo con los ambientes")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlanoAmbientePreview() {
    PlanoInteractivo7Theme {
        PlanoAmbiente()
    }
}

@Preview(showBackground = true)
@Composable
fun MapaScreenPreview() {
    PlanoInteractivo7Theme {
        MapaScreen(onVerPlano = {})
    }
}

// Plano del Pabellón de Ingeniería: 4 ambientes reales (2 salones, 1 laboratorio, 1 patio interior).
// El estado (ambientes, marcadores, marcador seleccionado) ya no vive en el Composable:
// se obtiene de PlanoViewModel, siguiendo el mismo patrón de ViewModel del Laboratorio 05
// (StateFlow expuesto por el ViewModel + collectAsState() en la UI).
@Composable
fun PlanoAmbiente(viewModel: PlanoViewModel = viewModel()) {
    val ambientes by viewModel.ambientes.collectAsState()
    val marcadores by viewModel.marcadores.collectAsState()
    val marcadorSeleccionado by viewModel.marcadorSeleccionado.collectAsState()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    viewModel.onTapPlano(offset)
                }
            }
    ) {
        // Rectángulos de cada ambiente
        ambientes.forEach { ambiente ->
            drawRect(
                color = ambiente.color,
                topLeft = ambiente.topLeft,
                size = ambiente.size
            )
        }

        // Paredes divisorias entre ambientes
        drawLine(
            color = Color.DarkGray,
            start = Offset(170f, 20f),
            end = Offset(170f, 330f),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.DarkGray,
            start = Offset(20f, 170f),
            end = Offset(330f, 170f),
            strokeWidth = 4f
        )

        // Etiquetas visibles de cada ambiente, dibujadas sobre su rectángulo
        ambientes.forEach { ambiente ->
            dibujarEtiqueta(
                texto = ambiente.etiqueta,
                centro = Offset(
                    ambiente.topLeft.x + ambiente.size.width / 2f,
                    ambiente.topLeft.y + ambiente.size.height / 2f
                )
            )
        }

        // Marcadores táctiles
        marcadores.forEach { marcador ->
            drawCircle(
                color = Color.Red,
                radius = marcador.radio,
                center = marcador.offset
            )
        }
    }

    marcadorSeleccionado?.let { marcador ->
        AlertDialog(
            onDismissRequest = { viewModel.cerrarDialogo() },
            title = { Text(marcador.titulo) },
            text = { Text(marcador.descripcion) },
            confirmButton = {
                TextButton(onClick = { viewModel.cerrarDialogo() }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// Dibuja un texto centrado directamente sobre el Canvas usando el Canvas nativo de Android.
private fun DrawScope.dibujarEtiqueta(texto: String, centro: Offset) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 28f
            isAntiAlias = true
        }
        drawText(texto, centro.x, centro.y, paint)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlanoInteractivo7Theme {
        Greeting("Android")
    }
}
