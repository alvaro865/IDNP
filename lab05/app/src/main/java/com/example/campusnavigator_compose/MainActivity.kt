package com.example.campusnavigator_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

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
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.campusnavigator_compose.ui.theme.CampusNavigator_ComposeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


// ---------------------------------------------------------
// VIEWMODEL COMPARTIDO
// ---------------------------------------------------------

class SeleccionViewModel : ViewModel() {
    private val _edificioSeleccionado = MutableStateFlow("Ninguno")
    val edificioSeleccionado: StateFlow<String> = _edificioSeleccionado.asStateFlow()

    fun actualizarEdificio(nombre: String) {
        _edificioSeleccionado.value = nombre
    }
}


// ---------------------------------------------------------
// DEFINICIÓN DE LAS PANTALLAS
// ---------------------------------------------------------

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Edificios : Screen("edificios", "Edificios")
    object Mapa : Screen("mapa", "Mapa")
    object Perfil : Screen("perfil", "Perfil")
}


// ---------------------------------------------------------
// MAIN ACTIVITY
// ---------------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusNavigator_ComposeTheme {
                MainScreen()
            }
        }
    }
}


// ---------------------------------------------------------
// PANTALLA PRINCIPAL
// ---------------------------------------------------------

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    // Instancia compartida del ViewModel usando viewModel()
    val seleccionViewModel: SeleccionViewModel = viewModel()

    val items = listOf(
        Screen.Home,
        Screen.Edificios,
        Screen.Mapa,
        Screen.Perfil
    )

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

                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(screen.label)
                        }
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
            // HOME
            composable(Screen.Home.route) {
                HomeScreen(viewModel = seleccionViewModel)
            }

            // EDIFICIOS
            composable(Screen.Edificios.route) {
                EdificiosScreen(viewModel = seleccionViewModel)
            }

            // MAPA
            composable(Screen.Mapa.route) {
                MapaScreen()
            }

            // PERFIL (Cuarta pestaña)
            composable(Screen.Perfil.route) {
                PerfilScreen()
            }
        }
    }
}


// ---------------------------------------------------------
// HOME SCREEN
// ---------------------------------------------------------

@Composable
fun HomeScreen(viewModel: SeleccionViewModel) {
    // Lectura del valor del ViewModel usando collectAsState()
    val edificioSeleccionado by viewModel.edificioSeleccionado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido a la app de Edificios"
        )
        Text(
            text = "Último edificio consultado: $edificioSeleccionado"
        )
    }
}


// ---------------------------------------------------------
// EDIFICIOS SCREEN
// ---------------------------------------------------------

@Composable
fun EdificiosScreen(viewModel: SeleccionViewModel) {
    val edificios = listOf(
        "Biblioteca Central",
        "Pabellón A",
        "Pabellón B",
        "Auditorio"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(edificios) { nombre ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nombre,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        // Actualización directa en el ViewModel en lugar de invocar lambda
                        viewModel.actualizarEdificio(nombre)
                    }
                ) {
                    Text("Ver")
                }
            }
        }
    }
}


// ---------------------------------------------------------
// MAPA SCREEN
// ---------------------------------------------------------

@Composable
fun MapaScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Mapa de ubicaciones (pendiente de integrar)"
        )
    }
}


// ---------------------------------------------------------
// PERFIL SCREEN (Cuarta Pestaña)
// ---------------------------------------------------------

@Composable
fun PerfilScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Perfil de Usuario - Cuarta Pestaña"
        )
    }
}
