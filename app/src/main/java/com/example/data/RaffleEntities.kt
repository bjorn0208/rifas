package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raffles")
data class Raffle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val drawDate: String,
    val ticketPrice: Double,
    val drawType: String, // ex: Loteria Federal
    val prize1: String,
    val prize2: String,
    val prize3: String,
    val adminPhone: String = "", // WhatsApp do administrador para receber pedidos
    val createdAt: Long = System.currentTimeMillis(),
    val drawnNumber1: String? = null,
    val drawnNumber2: String? = null,
    val drawnNumber3: String? = null,
    val isActive: Boolean = false
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey val id: String, // format: "${raffleId}_${number}" (e.g. "1_05")
    val raffleId: Int,
    val number: String, // "00" to "99"
    val status: String, // "DISPONIVEL", "RESERVADO", "PAGO"
    val buyerName: String? = null,
    val buyerPhone: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
