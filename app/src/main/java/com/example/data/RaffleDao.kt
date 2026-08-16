package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RaffleDao {
    @Query("SELECT * FROM raffles ORDER BY createdAt DESC")
    fun getAllRaffles(): Flow<List<Raffle>>

    @Query("SELECT * FROM raffles WHERE isActive = 1 LIMIT 1")
    fun getActiveRaffle(): Flow<Raffle?>

    @Query("SELECT * FROM raffles WHERE id = :id")
    fun getRaffleById(id: Int): Flow<Raffle?>

    @Query("SELECT * FROM tickets WHERE raffleId = :raffleId ORDER BY number ASC")
    fun getTicketsForRaffle(raffleId: Int): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: String): Ticket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaffle(raffle: Raffle): Long

    @Update
    suspend fun updateRaffle(raffle: Raffle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<Ticket>)

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Query("UPDATE raffles SET isActive = 0")
    suspend fun deactivateAllRaffles()

    @Query("UPDATE raffles SET isActive = 1 WHERE id = :id")
    suspend fun activateRaffle(id: Int)

    @Transaction
    suspend fun createNewRaffleWithTickets(raffle: Raffle): Int {
        deactivateAllRaffles()
        val raffleId = insertRaffle(raffle.copy(isActive = true)).toInt()
        val tickets = (0..99).map { number ->
            val formattedNumber = String.format("%02d", number)
            Ticket(
                id = "${raffleId}_$formattedNumber",
                raffleId = raffleId,
                number = formattedNumber,
                status = "DISPONIVEL"
            )
        }
        insertTickets(tickets)
        return raffleId
    }

    @Transaction
    suspend fun selectRaffle(id: Int) {
        deactivateAllRaffles()
        activateRaffle(id)
    }
}
