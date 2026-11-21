package com.example.audioguiaforeground_compose.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UltimoEdificio::class], version = 1, exportSchema = false)
abstract class AudioGuiaDatabase : RoomDatabase() {

    abstract fun ultimoEdificioDao(): UltimoEdificioDao

    companion object {
        @Volatile
        private var instancia: AudioGuiaDatabase? = null

        fun obtener(context: Context): AudioGuiaDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AudioGuiaDatabase::class.java,
                    "audioguia.db"
                ).build().also { instancia = it }
            }
    }
}
