package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    val isFirebaseAvailable: Boolean by lazy {
        try {
            // Attempt to fetch FirebaseApp instance. This will throw if google-services.json is missing or invalid.
            val app = FirebaseApp.getInstance()
            Log.d(TAG, "Firebase initialized successfully: ${app.name}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase is not configured or google-services.json is missing: ${e.message}")
            false
        }
    }

    val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
                null
            }
        } else null

    val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FirebaseAuth instance: ${e.message}")
                null
            }
        } else null

    // Upload a raffle to Firestore
    suspend fun syncRaffleToFirestore(raffle: Raffle) {
        val db = firestore ?: return
        val authUser = auth?.currentUser ?: return
        
        try {
            val raffleDoc = db.collection("users")
                .document(authUser.uid)
                .collection("raffles")
                .document(raffle.id.toString())

            val data = hashMapOf(
                "id" to raffle.id,
                "title" to raffle.title,
                "drawDate" to raffle.drawDate,
                "ticketPrice" to raffle.ticketPrice,
                "drawType" to raffle.drawType,
                "prize1" to raffle.prize1,
                "prize2" to raffle.prize2,
                "prize3" to raffle.prize3,
                "adminPhone" to raffle.adminPhone,
                "isActive" to raffle.isActive,
                "drawnNumber1" to raffle.drawnNumber1,
                "drawnNumber2" to raffle.drawnNumber2,
                "drawnNumber3" to raffle.drawnNumber3,
                "lastUpdated" to System.currentTimeMillis()
            )
            raffleDoc.set(data).await()
            Log.d(TAG, "Raffle ${raffle.id} synced to Firestore successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing raffle to Firestore: ${e.message}")
        }
    }

    // Upload tickets to Firestore under a specific raffle
    suspend fun syncTicketsToFirestore(raffleId: Int, tickets: List<Ticket>) {
        val db = firestore ?: return
        val authUser = auth?.currentUser ?: return

        try {
            val raffleDoc = db.collection("users")
                .document(authUser.uid)
                .collection("raffles")
                .document(raffleId.toString())

            // Write in batches to be efficient
            val batch = db.batch()
            tickets.forEach { ticket ->
                val ticketDoc = raffleDoc.collection("tickets").document(ticket.number)
                val data = hashMapOf(
                    "id" to ticket.id,
                    "raffleId" to ticket.raffleId,
                    "number" to ticket.number,
                    "status" to ticket.status,
                    "buyerName" to ticket.buyerName,
                    "buyerPhone" to ticket.buyerPhone
                )
                batch.set(ticketDoc, data)
            }
            batch.commit().await()
            Log.d(TAG, "100 tickets for Raffle $raffleId synced to Firestore successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tickets to Firestore: ${e.message}")
        }
    }

    // Sync a single ticket update to Firestore
    suspend fun syncSingleTicketToFirestore(ticket: Ticket) {
        val db = firestore ?: return
        val authUser = auth?.currentUser ?: return

        try {
            val ticketDoc = db.collection("users")
                .document(authUser.uid)
                .collection("raffles")
                .document(ticket.raffleId.toString())
                .collection("tickets")
                .document(ticket.number)

            val data = hashMapOf(
                "id" to ticket.id,
                "raffleId" to ticket.raffleId,
                "number" to ticket.number,
                "status" to ticket.status,
                "buyerName" to ticket.buyerName,
                "buyerPhone" to ticket.buyerPhone
            )
            ticketDoc.set(data).await()
            Log.d(TAG, "Ticket ${ticket.number} for Raffle ${ticket.raffleId} updated in Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing single ticket: ${e.message}")
        }
    }

    // Download user's raffles from Firestore
    suspend fun fetchRafflesFromFirestore(): List<Raffle> {
        val db = firestore ?: return emptyList()
        val authUser = auth?.currentUser ?: return emptyList()

        return try {
            val snapshot = db.collection("users")
                .document(authUser.uid)
                .collection("raffles")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Raffle(
                        id = (doc.get("id") as? Number)?.toInt() ?: doc.id.toInt(),
                        title = doc.getString("title") ?: "",
                        drawDate = doc.getString("drawDate") ?: "",
                        ticketPrice = (doc.get("ticketPrice") as? Number)?.toDouble() ?: 0.0,
                        drawType = doc.getString("drawType") ?: "Loteria Federal",
                        prize1 = doc.getString("prize1") ?: "",
                        prize2 = doc.getString("prize2") ?: "",
                        prize3 = doc.getString("prize3") ?: "",
                        adminPhone = doc.getString("adminPhone") ?: "",
                        isActive = doc.getBoolean("isActive") ?: false,
                        drawnNumber1 = doc.getString("drawnNumber1"),
                        drawnNumber2 = doc.getString("drawnNumber2"),
                        drawnNumber3 = doc.getString("drawnNumber3")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching raffles from Firestore: ${e.message}")
            emptyList()
        }
    }

    // Download tickets for a specific raffle from Firestore
    suspend fun fetchTicketsFromFirestore(raffleId: Int): List<Ticket> {
        val db = firestore ?: return emptyList()
        val authUser = auth?.currentUser ?: return emptyList()

        return try {
            val snapshot = db.collection("users")
                .document(authUser.uid)
                .collection("raffles")
                .document(raffleId.toString())
                .collection("tickets")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Ticket(
                        id = doc.getString("id") ?: "${raffleId}_${doc.id}",
                        raffleId = (doc.get("raffleId") as? Number)?.toInt() ?: raffleId,
                        number = doc.getString("number") ?: doc.id,
                        status = doc.getString("status") ?: "DISPONIVEL",
                        buyerName = doc.getString("buyerName"),
                        buyerPhone = doc.getString("buyerPhone")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tickets from Firestore: ${e.message}")
            emptyList()
        }
    }
}
