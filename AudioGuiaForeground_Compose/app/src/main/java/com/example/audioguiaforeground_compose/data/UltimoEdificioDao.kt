package com.example.audioguiaforeground_compose.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UltimoEdificioDao {

    /** Inserta o reemplaza el último edificio reproducido. */
    @Upsert
    suspend fun guardar(edificio: UltimoEdificio)

    @Query("SELECT * FROM ultimo_edificio WHERE id = 0")
    fun observar(): Flow<UltimoEdificio?>
}
