package com.example.data

import kotlinx.coroutines.flow.Flow

class RaffleRepository(private val raffleDao: RaffleDao) {
    val allRaffles: Flow<List<Raffle>> = raffleDao.getAllRaffles()
    val activeRaffle: Flow<Raffle?> = raffleDao.getActiveRaffle()

    fun getTicketsForRaffle(raffleId: Int): Flow<List<Ticket>> {
        return raffleDao.getTicketsForRaffle(raffleId)
    }

    suspend fun createRaffle(raffle: Raffle): Int {
        return raffleDao.createNewRaffleWithTickets(raffle)
    }

    suspend fun insertTickets(tickets: List<Ticket>) {
        raffleDao.insertTickets(tickets)
    }

    suspend fun updateRaffle(raffle: Raffle) {
        raffleDao.updateRaffle(raffle)
    }

    suspend fun updateTicket(ticket: Ticket) {
        raffleDao.updateTicket(ticket)
    }

    suspend fun selectRaffle(id: Int) {
        raffleDao.selectRaffle(id)
    }
    
    suspend fun getRaffleByIdOnce(id: Int): Flow<Raffle?> {
        return raffleDao.getRaffleById(id)
    }
}
