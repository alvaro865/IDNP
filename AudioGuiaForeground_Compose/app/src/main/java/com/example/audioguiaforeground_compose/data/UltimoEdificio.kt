package com.example.audioguiaforeground_compose.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Último edificio (pista de la audioguía) reproducido.
 * La tabla guarda una sola fila: siempre se usa id = 0 y se sobrescribe.
 */
@Entity(tableName = "ultimo_edificio")
data class UltimoEdificio(
    @PrimaryKey val id: Int = 0,
    val uri: String,
    val titulo: String
)
