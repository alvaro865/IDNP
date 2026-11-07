package com.example.sincronizacionservicio_compose10

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sincronizacionservicio_compose10.ui.theme.SincronizacionServicio_Compose10Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------- Constantes de los broadcasts ----------

const val ACCION_SYNC_LISTA = "com.example.sincronizacionservicio_compose.ACCION_SYNC_LISTA"
const val ACCION_SYNC_ERROR = "com.example.sincronizacionservicio_compose.ACCION_SYNC_ERROR"
const val EXTRA_EDIFICIOS = "edificios_actualizados"
const val EXTRA_MENSAJE_ERROR = "mensaje_error"

// ---------- Room (basado en el Laboratorio 09) ----------

// Se agrega el campo "sincronizado" para poder marcar los edificios que el Service actualiza
@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val sincronizado: Boolean = false
)

@Dao
interface EdificioDao {
    @Query("SELECT * FROM EdificioEntity")
    fun observarEdificios(): Flow<List<EdificioEntity>>

    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodos(): List<EdificioEntity>

    @Update
    suspend fun actualizarEdificio(edificio: EdificioEntity)

    // Para poder repetir la prueba: vuelve a dejar todos los edificios como pendientes
    @Query("UPDATE EdificioEntity SET sincronizado = 0")
    suspend fun reiniciarSincronizacion()
}

@Database(entities = [EdificioEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}

private val datosIniciales = listOf(
    "INSERT INTO EdificioEntity (id, nombre, categoria, sincronizado) VALUES " +
            "(1, 'Biblioteca Central', 'Servicios', 0), " +
            "(2, 'Facultad de Ingeniería', 'Académico', 0), " +
            "(3, 'Rectorado', 'Administrativo', 0), " +
            "(4, 'Comedor Universitario', 'Servicios', 0), " +
            "(5, 'Facultad de Derecho', 'Académico', 0), " +
            "(6, 'Coliseo Cerrado', 'Deporte', 0), " +
            "(7, 'Oficina de Bienestar', 'Administrativo', 0)"
)

// Singleton: el Service y la Activity deben usar LA MISMA instancia de la base de datos,
// así el Flow de la pantalla se entera de los cambios que hace el Service.
object BaseDatos {
    @Volatile
    private var instancia: AppDatabase? = null

    fun obtener(context: Context): AppDatabase {
        return instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sync_app.db"
            )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        datosIniciales.forEach(db::execSQL)
                    }
                })
                .build()
                .also { instancia = it }
        }
    }
}

// ---------- Service ----------

class SincronizacionService : Service() {

    // Scope propio del Service: se cancela en onDestroy() para detener la tarea al llamar stopService()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tarea: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SYNC", "1. Servicio iniciado")
        tarea?.cancel()
        tarea = scope.launch {
            delay(3000) // simula el tiempo de la sincronización

            // Problema 3: error simulado con 30% de probabilidad
            if (Random.nextInt(100) < 30) {
                val error = Intent(ACCION_SYNC_ERROR)
                error.setPackage(packageName)
                error.putExtra(EXTRA_MENSAJE_ERROR, "No se pudo conectar con el servidor")
                sendBroadcast(error)
                Log.d("SYNC", "2. Error simulado enviado")
                stopSelf()
                return@launch
            }

            // Problema 2: sincronización real con Room
            // Condición: se sincronizan los edificios que aún están pendientes (sincronizado = false)
            val dao = BaseDatos.obtener(applicationContext).edificioDao()
            val pendientes = dao.obtenerTodos().filter { !it.sincronizado }
            pendientes.forEach { dao.actualizarEdificio(it.copy(sincronizado = true)) }
            val cantidad = pendientes.size

            val resultado = Intent(ACCION_SYNC_LISTA)
            resultado.setPackage(packageName)
            resultado.putExtra(EXTRA_EDIFICIOS, cantidad)
            sendBroadcast(resultado)
            Log.d("SYNC", "2. Aviso enviado: $cantidad edificios")

            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Si el usuario detuvo el servicio a mitad de la tarea, la coroutine se cancela aquí
        scope.cancel()
        Log.d("SYNC", "Servicio destruido")
        super.onDestroy()
    }
}

// ---------- Estado de la pantalla ----------

sealed interface EstadoSync {
    data object Inactivo : EstadoSync
    data object Sincronizando : EstadoSync
    data class Completo(val cantidad: Int) : EstadoSync
    data class Error(val mensaje: String) : EstadoSync
    data object Detenido : EstadoSync
}

// ---------- UI ----------

@Composable
fun PantallaSincronizacion() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { BaseDatos.obtener(context).edificioDao() }
    val edificios by dao.observarEdificios().collectAsState(initial = emptyList())
    var estado by remember { mutableStateOf<EstadoSync>(EstadoSync.Inactivo) }

    DisposableEffect(Unit) {
        // Receiver 1: sincronización exitosa
        val receiverOk = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Aviso recibido en la pantalla")
                val cantidad = intent?.getIntExtra(EXTRA_EDIFICIOS, 0) ?: 0
                estado = EstadoSync.Completo(cantidad)
            }
        }
        // Receiver 2: error en la sincronización
        val receiverError = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Error recibido en la pantalla")
                val mensaje = intent?.getStringExtra(EXTRA_MENSAJE_ERROR) ?: "Error desconocido"
                estado = EstadoSync.Error(mensaje)
            }
        }
        ContextCompat.registerReceiver(
            context, receiverOk, IntentFilter(ACCION_SYNC_LISTA), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            context, receiverError, IntentFilter(ACCION_SYNC_ERROR), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiverOk)
            context.unregisterReceiver(receiverError)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = estado != EstadoSync.Sincronizando,
                onClick = {
                    estado = EstadoSync.Sincronizando
                    context.startService(Intent(context, SincronizacionService::class.java))
                }
            ) {
                Text("Sincronizar")
            }
            // Problema 1: detener la sincronización mientras la tarea está en curso
            OutlinedButton(
                enabled = estado == EstadoSync.Sincronizando,
                onClick = {
                    context.stopService(Intent(context, SincronizacionService::class.java))
                    estado = EstadoSync.Detenido
                }
            ) {
                Text("Detener sincronización")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val e = estado) {
            EstadoSync.Inactivo -> Text("Pulsa \"Sincronizar\" para comenzar")
            EstadoSync.Sincronizando -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sincronizando...")
            }
            is EstadoSync.Completo ->
                Text("Sincronización completa: ${e.cantidad} edificios actualizados")
            is EstadoSync.Error ->
                Text("Error de sincronización: ${e.mensaje}", color = MaterialTheme.colorScheme.error)
            EstadoSync.Detenido ->
                Text("Sincronización detenida por el usuario")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = { scope.launch { dao.reiniciarSincronizacion() } }) {
            Text("Reiniciar datos (todos pendientes)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(edificios, key = { it.id }) { edificio ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(edificio.nombre, fontWeight = FontWeight.Bold)
                            Text(edificio.categoria, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(if (edificio.sincronizado) "✓ Sincronizado" else "Pendiente")
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SincronizacionServicio_Compose10Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaSincronizacion()
                }
            }
        }
    }
}