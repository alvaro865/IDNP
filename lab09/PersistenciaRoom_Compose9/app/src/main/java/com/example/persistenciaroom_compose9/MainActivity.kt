package com.example.persistenciaroom_compose9

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.persistenciaroom_compose9.ui.theme.PersistenciaRoom_Compose9Theme

// ---------- Entidades ----------

@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = EdificioEntity::class,
            parentColumns = ["id"],
            childColumns = ["edificioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("edificioId")]
)
data class AmbienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val edificioId: Int,
    val nombre: String
)

// Tercera entidad: un ambiente puede tener varias notas (1:N)
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AmbienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["ambienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ambienteId")]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ambienteId: Int,
    val texto: String
)

// ---------- Relaciones ----------

// Consulta relacional 1 (ejercicio resuelto): edificio con sus ambientes
data class EdificioConAmbientes(
    @Embedded val edificio: EdificioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "edificioId"
    )
    val ambientes: List<AmbienteEntity>
)

// Consulta relacional 2 (nueva): ambiente con sus notas
data class AmbienteConNotas(
    @Embedded val ambiente: AmbienteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ambienteId"
    )
    val notas: List<NotaEntity>
)

// ---------- DAO ----------

@Dao
interface EdificioDao {
    @Insert
    suspend fun insertarEdificio(edificio: EdificioEntity): Long
    @Update
    suspend fun actualizarEdificio(edificio: EdificioEntity)
    @Delete
    suspend fun eliminarEdificio(edificio: EdificioEntity)

    @Insert
    suspend fun insertarAmbiente(ambiente: AmbienteEntity): Long
    @Update
    suspend fun actualizarAmbiente(ambiente: AmbienteEntity)
    @Delete
    suspend fun eliminarAmbiente(ambiente: AmbienteEntity)

    @Insert
    suspend fun insertarNota(nota: NotaEntity): Long
    @Update
    suspend fun actualizarNota(nota: NotaEntity)
    @Delete
    suspend fun eliminarNota(nota: NotaEntity)

    @Transaction
    @Query("SELECT * FROM EdificioEntity")
    fun obtenerConAmbientes(): Flow<List<EdificioConAmbientes>>

    @Transaction
    @Query("SELECT * FROM AmbienteEntity")
    fun obtenerAmbientesConNotas(): Flow<List<AmbienteConNotas>>

    @Transaction
    @Query("SELECT * FROM AmbienteEntity WHERE id = :ambienteId")
    suspend fun obtenerAmbienteConNotas(ambienteId: Int): AmbienteConNotas?
}

// ---------- Base de datos ----------

@Database(
    entities = [EdificioEntity::class, AmbienteEntity::class, NotaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}

// Datos de ejemplo: se insertan solo cuando la base se crea, así los cambios del usuario persisten
private val datosIniciales = listOf(
    "INSERT INTO EdificioEntity (id, nombre, categoria) VALUES " +
        "(1, 'Biblioteca Central', 'Servicios'), " +
        "(2, 'Facultad de Ingeniería', 'Académico'), " +
        "(3, 'Rectorado', 'Administrativo')",
    "INSERT INTO AmbienteEntity (id, edificioId, nombre) VALUES " +
        "(1, 1, 'Sala de lectura'), (2, 1, 'Hemeroteca'), " +
        "(3, 2, 'Laboratorio de cómputo'), (4, 2, 'Aula 201'), " +
        "(5, 3, 'Mesa de partes')",
    "INSERT INTO NotaEntity (ambienteId, texto) VALUES " +
        "(1, 'Abierta de 8:00 a 20:00'), (1, 'Silencio obligatorio'), " +
        "(2, 'Revistas de los últimos 5 años'), " +
        "(3, '30 computadoras con Android Studio'), (3, 'Requiere reserva previa'), " +
        "(4, 'Proyector en mantenimiento')"
)

fun crearBaseDeDatos(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app.db"
    )
        .fallbackToDestructiveMigration(true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                datosIniciales.forEach(db::execSQL)
            }
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                datosIniciales.forEach(db::execSQL)
            }
        })
        .build()
}

// ---------- ViewModel ----------

class EdificiosViewModel(private val dao: EdificioDao) : ViewModel() {
    val edificios: StateFlow<List<EdificioConAmbientes>> = dao.obtenerConAmbientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ambientesConNotas: StateFlow<List<AmbienteConNotas>> = dao.obtenerAmbientesConNotas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarEdificio(edificio: EdificioEntity) = viewModelScope.launch { dao.actualizarEdificio(edificio) }
    fun eliminarEdificio(edificio: EdificioEntity) = viewModelScope.launch { dao.eliminarEdificio(edificio) }
    fun actualizarAmbiente(ambiente: AmbienteEntity) = viewModelScope.launch { dao.actualizarAmbiente(ambiente) }
    fun eliminarAmbiente(ambiente: AmbienteEntity) = viewModelScope.launch { dao.eliminarAmbiente(ambiente) }
}

class EdificiosViewModelFactory(private val dao: EdificioDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EdificiosViewModel(dao) as T
    }
}

// ---------- UI ----------

private sealed interface Edicion {
    data class Edificio(val edificio: EdificioEntity) : Edicion
    data class Ambiente(val ambiente: AmbienteEntity) : Edicion
}

@Composable
fun PantallaPrincipal(viewModel: EdificiosViewModel) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        PrimaryTabRow(selectedTabIndex = pestana) {
            Tab(selected = pestana == 0, onClick = { pestana = 0 },
                text = { Text("Edificios y ambientes") })
            Tab(selected = pestana == 1, onClick = { pestana = 1 },
                text = { Text("Ambientes y notas") })
        }
        when (pestana) {
            0 -> ListaEdificiosConAmbientes(viewModel)
            else -> ListaAmbientesConNotas(viewModel)
        }
    }
}

@Composable
fun ListaEdificiosConAmbientes(viewModel: EdificiosViewModel) {
    val edificios by viewModel.edificios.collectAsState()
    var edicion by remember { mutableStateOf<Edicion?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Text(
                text = "Consulta 1: EdificioConAmbientes",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(edificios, key = { it.edificio.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    FilaConAcciones(
                        onEditar = { edicion = Edicion.Edificio(item.edificio) },
                        onEliminar = { viewModel.eliminarEdificio(item.edificio) }
                    ) {
                        Text(item.edificio.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(item.edificio.categoria, fontSize = 13.sp)
                    }
                    item.ambientes.forEach { ambiente ->
                        FilaConAcciones(
                            onEditar = { edicion = Edicion.Ambiente(ambiente) },
                            onEliminar = { viewModel.eliminarAmbiente(ambiente) },
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text("- ${ambiente.nombre}", fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    when (val e = edicion) {
        is Edicion.Edificio -> DialogoEdicion(
            titulo = "Editar edificio",
            campos = listOf("Nombre" to e.edificio.nombre, "Categoría" to e.edificio.categoria),
            onCancelar = { edicion = null },
            onGuardar = { valores ->
                viewModel.actualizarEdificio(e.edificio.copy(nombre = valores[0], categoria = valores[1]))
                edicion = null
            }
        )
        is Edicion.Ambiente -> DialogoEdicion(
            titulo = "Editar ambiente",
            campos = listOf("Nombre" to e.ambiente.nombre),
            onCancelar = { edicion = null },
            onGuardar = { valores ->
                viewModel.actualizarAmbiente(e.ambiente.copy(nombre = valores[0]))
                edicion = null
            }
        )
        null -> {}
    }
}

@Composable
fun ListaAmbientesConNotas(viewModel: EdificiosViewModel) {
    val ambientes by viewModel.ambientesConNotas.collectAsState()
    val edificios by viewModel.edificios.collectAsState()
    val nombreEdificio = edificios.associate { it.edificio.id to it.edificio.nombre }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Text(
                text = "Consulta 2: AmbienteConNotas",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(ambientes, key = { it.ambiente.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.ambiente.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(nombreEdificio[item.ambiente.edificioId] ?: "", fontSize = 13.sp)
                    if (item.notas.isEmpty()) {
                        Text("(sin notas)", fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp))
                    }
                    item.notas.forEach { nota ->
                        Text("• ${nota.texto}", fontSize = 14.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaConAcciones(
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { contenido() }
        TextButton(onClick = onEditar) { Text("Editar") }
        TextButton(onClick = onEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun DialogoEdicion(
    titulo: String,
    campos: List<Pair<String, String>>,
    onCancelar: () -> Unit,
    onGuardar: (List<String>) -> Unit
) {
    var valores by remember { mutableStateOf(campos.map { it.second }) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                campos.forEachIndexed { i, (etiqueta, _) ->
                    OutlinedTextField(
                        value = valores[i],
                        onValueChange = { nuevo -> valores = valores.toMutableList().also { it[i] = nuevo } },
                        label = { Text(etiqueta) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(valores.map { it.trim() }) },
                enabled = valores.all { it.isNotBlank() }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = crearBaseDeDatos(applicationContext)
        val dao = database.edificioDao()
        val factory = EdificiosViewModelFactory(dao)
        setContent {
            PersistenciaRoom_Compose9Theme {
                val viewModel: EdificiosViewModel = viewModel(factory = factory)
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaPrincipal(viewModel)
                }
            }
        }
    }
}
