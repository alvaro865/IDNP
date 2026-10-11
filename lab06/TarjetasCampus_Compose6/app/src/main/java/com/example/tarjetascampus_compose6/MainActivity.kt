package com.example.tarjetascampus_compose6

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tarjetascampus_compose6.ui.theme.TarjetasCampus_Compose6Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


// ---------------------------------------------------------
// MODELO DE DATOS (se reutilizan las mismas imágenes del Paso 3)
// ---------------------------------------------------------

data class Lugar(
    val nombre: String,
    val descripcion: String,
    @DrawableRes val imagen: Int,
    val colorFondo: Color
)

val listaLugares = listOf(
    Lugar(
        "Biblioteca Central",
        "Zona de estudio y préstamo de libros",
        R.drawable.biblioteca,
        Color(0xFFE3F2FD)
    ),
    Lugar(
        "Comedor Universitario",
        "Servicio de alimentación para estudiantes",
        R.drawable.comedor,
        Color(0xFFFFF3E0)
    ),
    Lugar(
        "Pabellón A",
        "Aulas y laboratorios de la facultad",
        R.drawable.biblioteca,
        Color(0xFFE8F5E9)
    ),
    Lugar(
        "Pabellón B",
        "Oficinas administrativas y salas de reunión",
        R.drawable.comedor,
        Color(0xFFF3E5F5)
    ),
    Lugar(
        "Auditorio",
        "Eventos, conferencias y ceremonias",
        R.drawable.biblioteca,
        Color(0xFFFFEBEE)
    )
)


// ---------------------------------------------------------
// VIEWMODEL COMPARTIDO (Laboratorio 05)
// ---------------------------------------------------------

class SeleccionViewModel : ViewModel() {
    private val _edificioSeleccionado = MutableStateFlow("Ninguno")
    val edificioSeleccionado: StateFlow<String> = _edificioSeleccionado.asStateFlow()

    fun actualizarEdificio(nombre: String) {
        _edificioSeleccionado.value = nombre
    }
}


// ---------------------------------------------------------
// PANTALLAS
// ---------------------------------------------------------

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Edificios : Screen("edificios", "Edificios")
    object Mapa : Screen("mapa", "Mapa")
    object Perfil : Screen("perfil", "Perfil")
}

const val RUTA_DETALLE = "detalle"


// ---------------------------------------------------------
// MAIN ACTIVITY
// ---------------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TarjetasCampus_Compose6Theme {
                MainScreen()
            }
        }
    }
}


// ---------------------------------------------------------
// MAIN SCREEN: Scaffold + NavigationBar + NavHost
// ---------------------------------------------------------

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val seleccionViewModel: SeleccionViewModel = viewModel()

    val items = listOf(Screen.Home, Screen.Edificios, Screen.Mapa, Screen.Perfil)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val icon = when (screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.Edificios -> Icons.Default.List
                        Screen.Mapa -> Icons.Default.Place
                        Screen.Perfil -> Icons.Default.Person
                    }

                    // La pestaña Edificios sigue marcada mientras se ve el detalle
                    val seleccionada = currentRoute == screen.route ||
                            (screen == Screen.Edificios &&
                                    currentRoute?.startsWith(RUTA_DETALLE) == true)

                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = seleccionViewModel)
            }

            composable(Screen.Edificios.route) {
                EdificiosScreen(
                    viewModel = seleccionViewModel,
                    onLugarClick = { indice ->
                        navController.navigate("$RUTA_DETALLE/$indice")
                    }
                )
            }

            // Ejercicio 2: destino de detalle con argumento (índice del lugar)
            composable(
                route = "$RUTA_DETALLE/{indice}",
                arguments = listOf(navArgument("indice") { type = NavType.IntType })
            ) { backStackEntry ->
                val indice = backStackEntry.arguments?.getInt("indice") ?: 0
                val lugar = listaLugares[indice]

                DetalleLugarScreen(
                    nombre = lugar.nombre,
                    descripcion = lugar.descripcion,
                    imagen = painterResource(id = lugar.imagen),
                    colorFondo = lugar.colorFondo,
                    onVolver = { navController.popBackStack() }
                )
            }

            composable(Screen.Mapa.route) { MapaScreen() }
            composable(Screen.Perfil.route) { PerfilScreen() }
        }
    }
}


// ---------------------------------------------------------
// HOME
// ---------------------------------------------------------

@Composable
fun HomeScreen(viewModel: SeleccionViewModel) {
    val edificioSeleccionado by viewModel.edificioSeleccionado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bienvenido a la app de Edificios")
        Text(text = "Último edificio consultado: $edificioSeleccionado")
    }
}


// ---------------------------------------------------------
// EDIFICIOS (Ejercicio 3): lista de tarjetas en vez de nombre + botón
// ---------------------------------------------------------

@Composable
fun EdificiosScreen(
    viewModel: SeleccionViewModel,
    onLugarClick: (Int) -> Unit
) {
    // Permite alternar entre los dos estilos para las capturas del informe
    var estiloCompacto by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            onClick = { estiloCompacto = !estiloCompacto },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(if (estiloCompacto) "Cambiar a estilo Row (TarjetaLugar)" else "Cambiar a estilo Box (TarjetaCompacta)")
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(listaLugares) { indice, lugar ->
                // Misma acción para ambos estilos: actualizar ViewModel y navegar
                val alPulsar = {
                    viewModel.actualizarEdificio(lugar.nombre)
                    onLugarClick(indice)
                }

                if (estiloCompacto) {
                    TarjetaCompacta(
                        nombre = lugar.nombre,
                        imagen = painterResource(id = lugar.imagen),
                        onClick = alPulsar
                    )
                } else {
                    TarjetaLugar(
                        nombre = lugar.nombre,
                        descripcion = lugar.descripcion,
                        imagen = painterResource(id = lugar.imagen),
                        colorFondo = lugar.colorFondo,
                        onClick = alPulsar
                    )
                }
            }
        }
    }
}


// ---------------------------------------------------------
// DETALLE (Ejercicio 1): reutiliza el estilo de TarjetaLugar a tamaño completo
// ---------------------------------------------------------

@Composable
fun DetalleLugarScreen(
    nombre: String,
    descripcion: String,
    imagen: Painter,
    colorFondo: Color = Color(0xFFE3F2FD),
    onVolver: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TarjetaLugar(
            nombre = nombre,
            descripcion = descripcion,
            imagen = imagen,
            colorFondo = colorFondo,
            tamanoImagen = 140.dp,
            tamanoTitulo = 26.sp,
            tamanoDescripcion = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}


// ---------------------------------------------------------
// ESTILO 1: TarjetaLugar (Row + Column, imagen circular)
// ---------------------------------------------------------

@Composable
fun TarjetaLugar(
    nombre: String,
    descripcion: String,
    imagen: Painter,
    colorFondo: Color,
    modifier: Modifier = Modifier,
    tamanoImagen: Dp = 90.dp,
    tamanoTitulo: androidx.compose.ui.unit.TextUnit = 20.sp,
    tamanoDescripcion: androidx.compose.ui.unit.TextUnit = 14.sp,
    onClick: (() -> Unit)? = null
) {
    val forma = RoundedCornerShape(24.dp)

    Surface(
        shape = forma,
        color = colorFondo,
        // clip antes de clickable para que el ripple respete las esquinas redondeadas
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = imagen,
                contentDescription = nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(tamanoImagen)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = tamanoTitulo
                )

                Text(
                    text = descripcion,
                    fontSize = tamanoDescripcion
                )
            }
        }
    }
}


// ---------------------------------------------------------
// ESTILO 2: TarjetaCompacta (Box apilado: imagen + scrim + texto)
// ---------------------------------------------------------

@Composable
fun TarjetaCompacta(
    nombre: String,
    imagen: Painter,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Image(
            painter = imagen,
            contentDescription = nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Text(
            text = nombre,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}


// ---------------------------------------------------------
// MAPA y PERFIL (sin cambios respecto al Laboratorio 05)
// ---------------------------------------------------------

@Composable
fun MapaScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Mapa de ubicaciones (pendiente de integrar)")
    }
}

@Composable
fun PerfilScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Perfil de Usuario - Cuarta Pestaña")
    }
}


// ---------------------------------------------------------
// PREVIEWS
// ---------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DetalleLugarPreview() {
    TarjetasCampus_Compose6Theme {
        DetalleLugarScreen(
            nombre = "Biblioteca Central",
            descripcion = "Zona de estudio y préstamo de libros",
            imagen = painterResource(id = R.drawable.biblioteca)
        )
    }
}