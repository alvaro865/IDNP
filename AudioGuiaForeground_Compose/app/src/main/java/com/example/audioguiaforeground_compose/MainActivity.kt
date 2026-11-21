package com.example.audioguiaforeground_compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audioguiaforeground_compose.data.AudioGuiaDatabase
import com.example.audioguiaforeground_compose.ui.theme.AudioGuiaForeground_ComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioGuiaForeground_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AudioGuiaScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AudioGuiaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Lista de reproducción: por defecto, los edificios con audio en res/raw
    var pistas by rememberSaveable { mutableStateOf(EDIFICIOS.map { it.uriAudio(context) }) }
    var titulos by rememberSaveable { mutableStateOf(EDIFICIOS.map { it.nombre }) }
    var indice by rememberSaveable { mutableIntStateOf(0) }
    var sinPermiso by rememberSaveable { mutableStateOf(false) }

    // Estado publicado por el servicio (también cambia desde los botones de la notificación)
    val estadoAudio by AudioGuiaService.estado.collectAsState()
    LaunchedEffect(estadoAudio) {
        if (estadoAudio.activo) indice = estadoAudio.indice
    }

    // Último edificio reproducido, guardado en Room
    val ultimoEdificioFlow = remember {
        AudioGuiaDatabase.obtener(context).ultimoEdificioDao().observar()
    }
    val ultimoEdificio by ultimoEdificioFlow.collectAsState(initial = null)

    val titulo = titulos.getOrElse(indice) { "Ningún audio seleccionado" }
    val estado = when {
        sinPermiso -> "Sin permiso de notificaciones"
        !estadoAudio.activo -> "Detenido"
        estadoAudio.reproduciendo -> "Reproduciendo"
        else -> "En pausa"
    }

    fun reproducirPista(i: Int) {
        indice = i
        AudioGuiaService.reproducir(context, pistas, titulos, i)
    }

    // Permiso de notificaciones (Android 13+), necesario para ver la notificación del servicio
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        sinPermiso = !concedido
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Selector de archivos de audio del dispositivo (permite elegir varios)
    val selectorAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Conserva el acceso a los archivos para que el servicio pueda leerlos
            uris.forEach {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            // La lista anterior deja de ser válida: se detiene lo que esté sonando
            if (estadoAudio.activo) {
                AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_STOP)
            }
            pistas = uris.map { it.toString() }
            titulos = uris.map { obtenerNombreArchivo(context, it) }
            indice = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Audioguía", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                if (pistas.isNotEmpty()) {
                    Text(
                        text = "Pista ${indice + 1} de ${pistas.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(text = "Estado: $estado", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Retoma el último edificio escuchado (solo si no hay nada sonando)
        ultimoEdificio?.let { edificio ->
            if (!estadoAudio.activo) {
                Button(
                    onClick = {
                        pistas = listOf(edificio.uri)
                        titulos = listOf(edificio.titulo)
                        reproducirPista(0)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continuar: ${edificio.titulo}")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (estadoAudio.activo) {
                        AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_STOP)
                    }
                    pistas = EDIFICIOS.map { it.uriAudio(context) }
                    titulos = EDIFICIOS.map { it.nombre }
                    indice = 0
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Edificios")
            }
            OutlinedButton(
                onClick = { selectorAudio.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Elegir audios")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { reproducirPista(indice) },
                enabled = pistas.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Reproducir")
            }
            Button(
                onClick = {
                    if (estadoAudio.activo) {
                        AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_NEXT)
                    } else {
                        // Al llegar a la última pista vuelve a la primera
                        reproducirPista((indice + 1) % pistas.size)
                    }
                },
                enabled = pistas.size > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Siguiente")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_PAUSE) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Pausar")
            }
            OutlinedButton(
                onClick = { AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_RESUME) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reanudar")
            }
            OutlinedButton(
                onClick = { AudioGuiaService.enviarAccion(context, AudioGuiaService.ACTION_STOP) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Detener")
            }
        }

        Text(
            text = "El audio sigue sonando aunque salgas de la app; contrólalo desde la notificación.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
    return uri.lastPathSegment ?: "Audio"
}

@Preview(showBackground = true)
@Composable
fun AudioGuiaScreenPreview() {
    AudioGuiaForeground_ComposeTheme {
        AudioGuiaScreen()
    }
}
