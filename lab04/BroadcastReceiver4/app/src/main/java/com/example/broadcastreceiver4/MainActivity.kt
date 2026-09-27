
package com.example.broadcastreceiver4

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.broadcastreceiver4.ui.theme.BroadcastReceiver4Theme


const val ACCION_ACTUALIZAR_BATERIA =
    "com.tuapp.ACTUALIZAR_BATERIA"


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BroadcastReceiver4Theme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) {
                    BatteryScreen()
                }
            }
        }
    }
}


@Composable
fun BatteryScreen() {

    // =========================================================
    // ESTADOS
    // =========================================================

    var porcentaje by remember {
        mutableStateOf(0)
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current


    // =========================================================
    // RECEIVER AUTOMÁTICO
    // =========================================================

    DisposableEffect(Unit) {

        val receiverAutomatico = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                actualizarBateria(
                    intent,
                    onPorcentaje = {
                        porcentaje = it
                    },
                    onCargando = {
                        cargando = it
                    }
                )
            }
        }


        context.registerReceiver(
            receiverAutomatico,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            Context.RECEIVER_EXPORTED
        )


        Log.d(
            "BatteryScreen",
            "Receiver automático registrado"
        )


        onDispose {

            context.unregisterReceiver(
                receiverAutomatico
            )
        }
    }


    // =========================================================
    // RECEIVER MANUAL
    // =========================================================

    DisposableEffect(Unit) {

        val receiverManual = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                Log.d(
                    "BatteryScreen",
                    "Broadcast manual recibido"
                )


                if (context != null) {

                    // BatteryManager
                    val batteryManager =
                        context.getSystemService(
                            Context.BATTERY_SERVICE
                        ) as BatteryManager


                    // Consultar porcentaje actual
                    val nivel =
                        batteryManager.getIntProperty(
                            BatteryManager.BATTERY_PROPERTY_CAPACITY
                        )


                    if (nivel >= 0) {

                        porcentaje = nivel
                    }


                    // Obtener Intent actual de batería
                    val batteryIntent =
                        context.registerReceiver(
                            null,
                            IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                            )
                        )


                    // Obtener EXTRA_STATUS
                    val status =
                        batteryIntent?.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            -1
                        ) ?: -1


                    // Determinar estado
                    cargando =
                        status ==
                                BatteryManager.BATTERY_STATUS_CHARGING ||
                                status ==
                                BatteryManager.BATTERY_STATUS_FULL


                    Log.d(
                        "BatteryScreen",
                        "Actualización manual: $nivel%"
                    )
                }
            }
        }


        // Registrar receiver manual
        context.registerReceiver(
            receiverManual,
            IntentFilter(
                ACCION_ACTUALIZAR_BATERIA
            ),
            Context.RECEIVER_NOT_EXPORTED
        )


        Log.d(
            "BatteryScreen",
            "Receiver manual registrado"
        )


        // Desregistrar receiver
        onDispose {

            context.unregisterReceiver(
                receiverManual
            )

            Log.d(
                "BatteryScreen",
                "Receiver manual desregistrado"
            )
        }
    }


    // =========================================================
    // PENDING INTENT
    // =========================================================

    val intentManual = Intent(
        ACCION_ACTUALIZAR_BATERIA
    ).setPackage(context.packageName)


    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intentManual,
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
    )


    // =========================================================
    // INTERFAZ
    // =========================================================

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Batería: $porcentaje%"
        )


        Text(
            text = if (cargando) {
                "Estado: Cargando"
            } else {
                "Estado: No está cargando"
            }
        )


        Button(
            onClick = {

                try {

                    Log.d(
                        "BatteryScreen",
                        "Solicitando actualización manual..."
                    )

                    pendingIntent.send()

                } catch (e: PendingIntent.CanceledException) {

                    Log.e(
                        "BatteryScreen",
                        "Error enviando broadcast",
                        e
                    )
                }
            }
        ) {

            Text(
                text = "Actualizar manualmente"
            )
        }
    }
}


// =========================================================
// FUNCIÓN PARA ACTUALIZAR BATERÍA AUTOMÁTICAMENTE
// =========================================================

fun actualizarBateria(
    intent: Intent?,
    onPorcentaje: (Int) -> Unit,
    onCargando: (Boolean) -> Unit
) {

    val nivel = intent?.getIntExtra(
        BatteryManager.EXTRA_LEVEL,
        -1
    ) ?: -1


    val escala = intent?.getIntExtra(
        BatteryManager.EXTRA_SCALE,
        -1
    ) ?: -1


    if (nivel != -1 && escala != -1) {

        val porcentaje =
            (nivel * 100) / escala

        onPorcentaje(porcentaje)
    }


    // Obtener EXTRA_STATUS
    val status = intent?.getIntExtra(
        BatteryManager.EXTRA_STATUS,
        -1
    ) ?: -1


    val estaCargando =
        status ==
                BatteryManager.BATTERY_STATUS_CHARGING ||
                status ==
                BatteryManager.BATTERY_STATUS_FULL


    onCargando(estaCargando)
}
