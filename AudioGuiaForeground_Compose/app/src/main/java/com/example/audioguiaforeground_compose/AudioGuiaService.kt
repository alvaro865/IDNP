package com.example.audioguiaforeground_compose

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.audioguiaforeground_compose.data.AudioGuiaDatabase
import com.example.audioguiaforeground_compose.data.UltimoEdificio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de la reproducción que la interfaz observa. */
data class EstadoAudio(
    val activo: Boolean = false,
    val reproduciendo: Boolean = false,
    val indice: Int = 0
)

class AudioGuiaService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var pistas: List<String> = emptyList()
    private var titulos: List<String> = emptyList()
    private var indice = 0
    private var reproduciendo = false

    // Corrutinas para escribir en Room fuera del hilo principal
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val titulo: String
        get() = titulos.getOrElse(indice) { "Audioguía" }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Tras startForegroundService() hay que llamar a startForeground() en pocos segundos
        iniciarPrimerPlano()

        when (intent?.action) {
            ACTION_PLAY -> {
                pistas = intent.getStringArrayListExtra(EXTRA_PISTAS).orEmpty()
                titulos = intent.getStringArrayListExtra(EXTRA_TITULOS).orEmpty()
                indice = intent.getIntExtra(EXTRA_INDICE, 0)
                if (indice in pistas.indices) reproducirActual() else detener()
            }
            ACTION_NEXT -> siguiente()
            ACTION_PAUSE -> pausar()
            ACTION_RESUME -> reanudar()
            ACTION_STOP -> detener()
        }
        // Sin audio cargado no tiene sentido mantener el servicio vivo
        if (mediaPlayer == null) detener()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        liberarReproductor()
        publicarEstado(activo = false)
        super.onDestroy()
    }

    private fun reproducirActual() {
        liberarReproductor()
        guardarUltimoEdificio()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            try {
                setDataSource(applicationContext, Uri.parse(pistas[indice]))
            } catch (e: Exception) {
                // Por ejemplo, si el archivo guardado ya no existe
                Log.e(TAG, "No se pudo abrir el audio", e)
                release()
                detener()
                return
            }
            setOnPreparedListener {
                it.start()
                reproduciendo = true
                actualizarNotificacion()
            }
            setOnCompletionListener { detener() }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Error en MediaPlayer: what=$what extra=$extra")
                detener()
                true
            }
            prepareAsync()
        }
        actualizarNotificacion()
    }

    private fun guardarUltimoEdificio() {
        val edificio = UltimoEdificio(uri = pistas[indice], titulo = titulo)
        scope.launch {
            AudioGuiaDatabase.obtener(applicationContext).ultimoEdificioDao().guardar(edificio)
        }
    }

    private fun siguiente() {
        if (pistas.size < 2) return
        // Al llegar a la última pista vuelve a la primera
        indice = (indice + 1) % pistas.size
        reproducirActual()
    }

    private fun pausar() {
        mediaPlayer?.takeIf { it.isPlaying }?.let {
            it.pause()
            reproduciendo = false
            actualizarNotificacion()
        }
    }

    private fun reanudar() {
        mediaPlayer?.takeIf { !it.isPlaying }?.let {
            it.start()
            reproduciendo = true
            actualizarNotificacion()
        }
    }

    private fun detener() {
        liberarReproductor()
        publicarEstado(activo = false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun liberarReproductor() {
        mediaPlayer?.release()
        mediaPlayer = null
        reproduciendo = false
    }

    private fun publicarEstado(activo: Boolean = mediaPlayer != null) {
        _estado.value = EstadoAudio(activo, reproduciendo, indice)
    }

    // --- Notificación ---

    private fun iniciarPrimerPlano() {
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, construirNotificacion(), tipo)
    }

    private fun actualizarNotificacion() {
        publicarEstado()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, construirNotificacion())
    }

    private fun construirNotificacion() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(titulo)
            .setContentText(
                (if (reproduciendo) "Reproduciendo" else "En pausa") +
                    if (pistas.isNotEmpty()) " · Pista ${indice + 1} de ${pistas.size}" else ""
            )
            .setContentIntent(intentAbrirApp())
            .setOngoing(reproduciendo)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(
                if (reproduciendo) {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_pause, "Pausar", intentAccion(ACTION_PAUSE)
                    )
                } else {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_play, "Reanudar", intentAccion(ACTION_RESUME)
                    )
                }
            )
            .apply {
                if (pistas.size > 1) {
                    addAction(
                        NotificationCompat.Action(
                            android.R.drawable.ic_media_next, "Siguiente", intentAccion(ACTION_NEXT)
                        )
                    )
                }
            }
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel, "Detener", intentAccion(ACTION_STOP)
                )
            )
            .build()

    private fun intentAccion(accion: String): PendingIntent =
        PendingIntent.getService(
            this,
            accion.hashCode(),
            Intent(this, AudioGuiaService::class.java).setAction(accion),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun intentAbrirApp(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de audioguía",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    companion object {
        private const val TAG = "AudioGuiaService"
        private const val CHANNEL_ID = "audioguia_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "com.example.audioguiaforeground_compose.action.PLAY"
        const val ACTION_NEXT = "com.example.audioguiaforeground_compose.action.NEXT"
        const val ACTION_PAUSE = "com.example.audioguiaforeground_compose.action.PAUSE"
        const val ACTION_RESUME = "com.example.audioguiaforeground_compose.action.RESUME"
        const val ACTION_STOP = "com.example.audioguiaforeground_compose.action.STOP"

        const val EXTRA_PISTAS = "extra_pistas"
        const val EXTRA_TITULOS = "extra_titulos"
        const val EXTRA_INDICE = "extra_indice"

        private val _estado = MutableStateFlow(EstadoAudio())

        /** Estado actual del servicio, para que la interfaz se mantenga sincronizada. */
        val estado: StateFlow<EstadoAudio> = _estado.asStateFlow()

        /** Inicia en primer plano la reproducción de la lista, empezando por [indice]. */
        fun reproducir(context: Context, pistas: List<String>, titulos: List<String>, indice: Int) {
            val intent = Intent(context, AudioGuiaService::class.java)
                .setAction(ACTION_PLAY)
                .putStringArrayListExtra(EXTRA_PISTAS, ArrayList(pistas))
                .putStringArrayListExtra(EXTRA_TITULOS, ArrayList(titulos))
                .putExtra(EXTRA_INDICE, indice)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Envía una acción (siguiente, pausar, reanudar, detener) al servicio. */
        fun enviarAccion(context: Context, accion: String) {
            context.startService(Intent(context, AudioGuiaService::class.java).setAction(accion))
        }
    }
}
