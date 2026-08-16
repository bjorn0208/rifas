package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseSyncManager
import com.example.data.Raffle
import com.example.data.RaffleRepository
import com.example.data.Ticket
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RaffleViewModel(private val repository: RaffleRepository) : ViewModel() {

    private val TAG = "RaffleViewModel"

    val allRaffles: StateFlow<List<Raffle>> = repository.allRaffles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRaffleId = MutableStateFlow<Int?>(null)
    val selectedRaffleId = _selectedRaffleId.asStateFlow()

    // Current raffle being viewed/managed
    val currentRaffle: StateFlow<Raffle?> = combine(
        allRaffles,
        _selectedRaffleId
    ) { raffles, selectedId ->
        if (selectedId != null) {
            raffles.find { it.id == selectedId }
        } else {
            raffles.find { it.isActive } ?: raffles.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactive tickets list for current raffle
    val currentTickets: StateFlow<List<Ticket>> = currentRaffle
        .flatMapLatest { raffle ->
            if (raffle != null) {
                repository.getTicketsForRaffle(raffle.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Mode state (true = Admin View, false = Public View)
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode = _isAdminMode.asStateFlow()

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    fun setAdminMode(isAdmin: Boolean) {
        _isAdminMode.value = isAdmin
    }

    // Temporary list of selected numbers in Public View for multi-reservation
    private val _selectedPublicNumbers = MutableStateFlow<Set<String>>(emptySet())
    val selectedPublicNumbers = _selectedPublicNumbers.asStateFlow()

    fun togglePublicNumberSelection(number: String) {
        val current = _selectedPublicNumbers.value
        if (current.contains(number)) {
            _selectedPublicNumbers.value = current - number
        } else {
            _selectedPublicNumbers.value = current + number
        }
    }

    fun clearPublicNumberSelection() {
        _selectedPublicNumbers.value = emptySet()
    }

    // Referral state for discount per referred person
    private val _referrer = MutableStateFlow<String?>(null)
    val referrer = _referrer.asStateFlow()

    fun setReferrer(ref: String?) {
        _referrer.value = ref
    }

    fun clearReferrer() {
        _referrer.value = null
    }

    // ---------------------------------------------------------------------------------
    // FIREBASE SECURITY & FIREBASE CLOUD SYNC ENGINE
    // ---------------------------------------------------------------------------------
    
    val isFirebaseAvailable = FirebaseSyncManager.isFirebaseAvailable

    private val _currentUser = MutableStateFlow<FirebaseUser?>(
        if (FirebaseSyncManager.isFirebaseAvailable) FirebaseSyncManager.auth?.currentUser else null
    )
    val currentUser = _currentUser.asStateFlow()

    init {
        // Listen to Auth State if Firebase is available
        if (isFirebaseAvailable) {
            try {
                FirebaseSyncManager.auth?.addAuthStateListener { auth ->
                    _currentUser.value = auth.currentUser
                    if (auth.currentUser != null) {
                        Log.d(TAG, "User logged in: ${auth.currentUser?.email}. Sinking data...")
                        syncFromFirestoreToRoom()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding auth listener: ${e.message}")
            }
        }
    }

    // Download user's Firestore data and save it locally into Room DB
    fun syncFromFirestoreToRoom() {
        viewModelScope.launch {
            if (!isFirebaseAvailable || currentUser.value == null) return@launch
            try {
                Log.d(TAG, "Starting Firestore to Room synchronization...")
                val remoteRaffles = FirebaseSyncManager.fetchRafflesFromFirestore()
                
                remoteRaffles.forEach { raffle ->
                    // 1. Add/Update Raffle locally in Room
                    val localExists = allRaffles.value.any { it.id == raffle.id }
                    if (!localExists) {
                        repository.createRaffle(raffle)
                    } else {
                        repository.updateRaffle(raffle)
                    }

                    // 2. Add/Update Tickets locally in Room
                    val remoteTickets = FirebaseSyncManager.fetchTicketsFromFirestore(raffle.id)
                    if (remoteTickets.isNotEmpty()) {
                        remoteTickets.forEach { ticket ->
                            repository.updateTicket(ticket)
                        }
                    }
                }
                Log.d(TAG, "Firestore sync complete. ${remoteRaffles.size} raffles synced.")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from Firestore: ${e.message}")
            }
        }
    }

    // Manual Upload Sync of local Room DB to Firestore Cloud
    fun syncFromRoomToFirestore() {
        viewModelScope.launch {
            if (!isFirebaseAvailable || currentUser.value == null) return@launch
            try {
                Log.d(TAG, "Starting Room to Firestore synchronization...")
                val localRaffles = allRaffles.value
                localRaffles.forEach { raffle ->
                    FirebaseSyncManager.syncRaffleToFirestore(raffle)
                    
                    // Fetch local tickets list
                    repository.getTicketsForRaffle(raffle.id).firstOrNull()?.let { tickets ->
                        FirebaseSyncManager.syncTicketsToFirestore(raffle.id, tickets)
                    }
                }
                Log.d(TAG, "Room upload sync complete.")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to Firestore: ${e.message}")
            }
        }
    }

    // Email/Password Signup wrapper
    fun signUpWithEmail(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val auth = FirebaseSyncManager.auth ?: run {
            onFailure("Firebase não está inicializado neste dispositivo.")
            return
        }
        
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                onSuccess()
                syncFromRoomToFirestore() // Immediately upload existing local Room data
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Erro desconhecido ao cadastrar.")
            }
    }

    // Email/Password Signin wrapper
    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val auth = FirebaseSyncManager.auth ?: run {
            onFailure("Firebase não está inicializado neste dispositivo.")
            return
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                onSuccess()
                syncFromFirestoreToRoom() // Immediately download cloud data to Room
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Credenciais inválidas.")
            }
    }

    // Sign out wrapper
    fun signOut(onComplete: () -> Unit) {
        val auth = FirebaseSyncManager.auth ?: return
        auth.signOut()
        _currentUser.value = null
        onComplete()
    }

    // ---------------------------------------------------------------------------------
    // RAFFLE OPERATIONS
    // ---------------------------------------------------------------------------------

    fun createRaffle(
        title: String,
        drawDate: String,
        ticketPrice: Double,
        drawType: String,
        prize1: String,
        prize2: String,
        prize3: String,
        adminPhone: String
    ) {
        viewModelScope.launch {
            val raffle = Raffle(
                title = title,
                drawDate = drawDate,
                ticketPrice = ticketPrice,
                drawType = drawType,
                prize1 = prize1,
                prize2 = prize2,
                prize3 = prize3,
                adminPhone = adminPhone
            )
            val newId = repository.createRaffle(raffle)
            _selectedRaffleId.value = newId

            // Sync to Firestore Cloud if user is authenticated and cloud is active
            if (isFirebaseAvailable && currentUser.value != null) {
                val createdRaffle = raffle.copy(id = newId, isActive = true)
                FirebaseSyncManager.syncRaffleToFirestore(createdRaffle)
                
                // Fetch newly generated tickets to sync to Cloud
                repository.getTicketsForRaffle(newId).firstOrNull()?.let { tickets ->
                    FirebaseSyncManager.syncTicketsToFirestore(newId, tickets)
                }
            }
        }
    }

    fun updateTicket(ticket: Ticket) {
        viewModelScope.launch {
            repository.updateTicket(ticket)
            
            // Sync single update to Firestore Cloud
            if (isFirebaseAvailable && currentUser.value != null) {
                FirebaseSyncManager.syncSingleTicketToFirestore(ticket)
            }
        }
    }

    fun updateTicketStatus(
        ticket: Ticket,
        status: String,
        buyerName: String?,
        buyerPhone: String?
    ) {
        viewModelScope.launch {
            val updated = ticket.copy(
                status = status,
                buyerName = buyerName?.trim()?.ifEmpty { null },
                buyerPhone = buyerPhone?.trim()?.ifEmpty { null },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTicket(updated)

            // Sync single update to Firestore Cloud
            if (isFirebaseAvailable && currentUser.value != null) {
                FirebaseSyncManager.syncSingleTicketToFirestore(updated)
            }
        }
    }

    fun selectRaffle(id: Int) {
        _selectedRaffleId.value = id
    }

    fun makeRaffleActive(id: Int) {
        viewModelScope.launch {
            repository.selectRaffle(id)
            _selectedRaffleId.value = id
            
            // Sync raffle activation state to cloud
            if (isFirebaseAvailable && currentUser.value != null) {
                allRaffles.value.forEach { r ->
                    FirebaseSyncManager.syncRaffleToFirestore(r)
                }
            }
        }
    }

    // Drawing federal lottery results
    fun saveDrawResults(raffle: Raffle, d1: String?, d2: String?, d3: String?) {
        viewModelScope.launch {
            val updated = raffle.copy(
                drawnNumber1 = d1?.padStart(2, '0')?.takeLast(2),
                drawnNumber2 = d2?.padStart(2, '0')?.takeLast(2),
                drawnNumber3 = d3?.padStart(2, '0')?.takeLast(2)
            )
            repository.updateRaffle(updated)

            // Sync updated drawing details to Firestore Cloud
            if (isFirebaseAvailable && currentUser.value != null) {
                FirebaseSyncManager.syncRaffleToFirestore(updated)
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // WHATSAPP RESERVATION & COMMUNICATIONS
    // ---------------------------------------------------------------------------------

    private fun formatPhoneForWhatsApp(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length <= 11 && !digits.startsWith("55") && digits.isNotEmpty()) {
            "55$digits"
        } else {
            digits
        }
    }

    // Admin shares coupon / receipt via WhatsApp with client
    fun shareWithClientViaWhatsApp(
        context: Context,
        raffleTitle: String,
        ticketPrice: Double,
        buyerName: String,
        buyerPhone: String,
        numbers: List<String>
    ) {
        val total = numbers.size * ticketPrice
        val numberListStr = numbers.sorted().joinToString(", ")
        val message = "Olá $buyerName, seus números para a rifa \"$raffleTitle\" são: [$numberListStr]. Total: R$ ${String.format("%.2f", total)}."
        val formattedPhone = formatPhoneForWhatsApp(buyerPhone)

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------------------------------------------------------------------
    // SIMULATOR / QUICK ACTIONS
    // ---------------------------------------------------------------------------------

    fun createRandomRaffle() {
        val titles = listOf(
            "Rifa de iPhone 15 Pro Max 256GB",
            "Fusca TSI de Colecionador",
            "Pix de R$ 10.000,00 na Conta",
            "Rifa Premium PlayStation 5 Slim",
            "Super Moto Honda CB 300F Twister 0km",
            "Kit Churrasco Completo + Cooler Stella",
            "Vale Viagem R$ 5.000 CVC",
            "Mochila Gamer com Acessórios Logitech"
        )
        val prizes = listOf(
            Triple("iPhone 15 Pro Max 256GB", "R$ 500 via Pix", "R$ 200 via Pix"),
            Triple("Fusca TSI completo 2013", "R$ 1.000 via Pix", "R$ 500 via Pix"),
            Triple("Pix de R$ 10.000,00", "Pix de R$ 1.000,00", "Pix de R$ 500,00"),
            Triple("PlayStation 5 Slim + 2 Jogos", "R$ 500 em saldo PSN", "R$ 200 via Pix"),
            Triple("Honda CB 300F Twister 0km", "R$ 1.000 via Pix", "R$ 400 via Pix"),
            Triple("Kit Churrasco + Cooler + Carnes", "Kit Faca do Chefe", "R$ 100 via Pix"),
            Triple("Vale Viagem R$ 5.000 CVC", "Mala de Viagem Samsonite", "R$ 200 via Pix"),
            Triple("Teclado G915 + Mouse G502", "Mousepad Gamer", "R$ 100 via Pix")
        )
        val index = (titles.indices).random()
        val randomTitle = titles[index]
        val randomPrizes = prizes[index]
        val randomPrice = listOf(5.00, 10.00, 15.00, 20.00, 25.00, 50.00).random()
        val randomDrawType = "Loteria Federal"
        
        // Format a nice date, e.g. next Saturday
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 10)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val formattedDate = sdf.format(calendar.time)

        val adminPhone = "11999998888"

        createRaffle(
            title = randomTitle,
            drawDate = formattedDate,
            ticketPrice = randomPrice,
            drawType = randomDrawType,
            prize1 = randomPrizes.first,
            prize2 = randomPrizes.second,
            prize3 = randomPrizes.third,
            adminPhone = adminPhone
        )
    }

    fun fillAllTicketsWithMockData(raffleId: Int) {
        viewModelScope.launch {
            val names = listOf(
                "Ana Silva", "Carlos Souza", "Mariana Costa", "Felipe Santos",
                "Juliana Lima", "Bruno Oliveira", "Beatriz Rocha", "Lucas Martins",
                "Camila Alves", "Rafael Pereira", "Larissa Gomes", "Thiago Ribeiro",
                "Gabriela Melo", "Rodrigo Cardoso", "Amanda Ferreira", "Gustavo Lima",
                "Patrícia Santos", "Daniel Barbosa", "Fernanda Souza", "Leonardo Cruz",
                "Aline Mendes", "Marcos Dias", "Letícia Rocha", "Eduardo Teixeira",
                "Vanessa Nogueira", "Matheus Barros", "Isabela Castro", "Vinícius Carvalho"
            )
            val ddds = listOf("11", "21", "31", "41", "51", "61", "71", "81", "19", "27")
            
            val updatedTickets = (0..99).map { number ->
                val formattedNumber = String.format("%02d", number)
                val randomName = names.random()
                val randomPhone = "(" + ddds.random() + ") 9" + (1000..9999).random().toString() + "-" + (1000..9999).random().toString()
                val status = if (Math.random() < 0.70) "PAGO" else "RESERVADO"
                Ticket(
                    id = "${raffleId}_$formattedNumber",
                    raffleId = raffleId,
                    number = formattedNumber,
                    status = status,
                    buyerName = randomName,
                    buyerPhone = randomPhone,
                    updatedAt = System.currentTimeMillis()
                )
            }
            
            repository.insertTickets(updatedTickets)
            
            // Sync to Cloud Firestore if active
            if (isFirebaseAvailable && currentUser.value != null) {
                FirebaseSyncManager.syncTicketsToFirestore(raffleId, updatedTickets)
            }
        }
    }

    // User submits reservation order to Admin
    fun reserveNumbersAndSubmit(
        context: Context,
        raffle: Raffle,
        buyerName: String,
        buyerPhone: String,
        numbers: List<String>
    ) {
        viewModelScope.launch {
            val hasReferral = !_referrer.value.isNullOrBlank()
            val referral = _referrer.value
            val dbBuyerName = if (hasReferral && !referral.isNullOrBlank()) "$buyerName (Indicado por: $referral)" else buyerName

            // 1. Save in local DB & Sync to Cloud
            numbers.forEach { numStr ->
                val ticketId = "${raffle.id}_$numStr"
                val existing = currentTickets.value.find { it.id == ticketId }
                if (existing != null) {
                    val updated = existing.copy(
                        status = "RESERVADO",
                        buyerName = dbBuyerName,
                        buyerPhone = buyerPhone,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateTicket(updated)

                    // Sync single ticket status changes to Cloud Firestore
                    if (isFirebaseAvailable && currentUser.value != null) {
                        FirebaseSyncManager.syncSingleTicketToFirestore(updated)
                    }
                }
            }

            // 2. Clear selections
            clearPublicNumberSelection()

            // 3. Launch WhatsApp link to notify admin
            val breakdown = calculateDiscount(numbers.size, raffle.ticketPrice, hasReferral)
            val numberListStr = numbers.sorted().joinToString(", ")

            val promoDetails = StringBuilder()
            if (breakdown.volumeDiscountPercent > 0) {
                promoDetails.append(" Combo de Dezenas (${breakdown.volumeDiscountPercent}% OFF)")
            }
            if (hasReferral) {
                if (promoDetails.isNotEmpty()) promoDetails.append(" +")
                promoDetails.append(" Indicação por $referral (10% OFF)")
            }

            val message = if (breakdown.totalDiscount > 0) {
                "Olá! Gostaria de reservar o(s) número(s) [$numberListStr] da rifa \"${raffle.title}\". Meu nome é $buyerName.${if (hasReferral) " Fui indicado por: $referral." else ""}\n\n" +
                "📊 Detalhes da Compra:\n" +
                "• Valor sem desconto: R$ ${String.format(Locale.US, "%.2f", breakdown.originalTotal)}\n" +
                "• Promoções ativas:${promoDetails.toString()}\n" +
                "• Total de Descontos: R$ ${String.format(Locale.US, "%.2f", breakdown.totalDiscount)}\n" +
                "• Valor Final com Desconto: *R$ ${String.format(Locale.US, "%.2f", breakdown.finalTotal)}*"
            } else {
                "Olá! Gostaria de reservar o(s) número(s) [$numberListStr] da rifa \"${raffle.title}\". Meu nome é $buyerName. Total: R$ ${String.format(Locale.US, "%.2f", breakdown.finalTotal)}."
            }
            val formattedPhone = formatPhoneForWhatsApp(raffle.adminPhone)

            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(message)}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Confirm payment and mark selected numbers as PAID (verde)
    fun confirmPaymentAndMarkPaid(
        raffleId: Int,
        numbers: List<String>,
        buyerName: String,
        buyerPhone: String
    ) {
        viewModelScope.launch {
            numbers.forEach { numStr ->
                val ticketId = "${raffleId}_$numStr"
                val existing = currentTickets.value.find { it.id == ticketId }
                if (existing != null) {
                    val updated = existing.copy(
                        status = "PAGO",
                        buyerName = buyerName.trim(),
                        buyerPhone = buyerPhone.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateTicket(updated)

                    // Sync single ticket status changes to Cloud Firestore
                    if (isFirebaseAvailable && currentUser.value != null) {
                        FirebaseSyncManager.syncSingleTicketToFirestore(updated)
                    }
                }
            }
            clearPublicNumberSelection()
        }
    }
}

class RaffleViewModelFactory(private val repository: RaffleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RaffleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RaffleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class DiscountBreakdown(
    val originalTotal: Double,
    val referralDiscount: Double,
    val volumeDiscountPercent: Int,
    val volumeDiscount: Double,
    val totalDiscount: Double,
    val finalTotal: Double
)

fun calculateDiscount(ticketCount: Int, ticketPrice: Double, hasReferrer: Boolean): DiscountBreakdown {
    val originalTotal = ticketCount * ticketPrice
    val referralPercent = if (hasReferrer) 0.10 else 0.0
    val volumePercent = when {
        ticketCount >= 10 -> 0.15
        ticketCount >= 5 -> 0.10
        ticketCount >= 3 -> 0.05
        else -> 0.0
    }
    // Combined limit of 25% max discount
    val combinedPercent = (referralPercent + volumePercent).coerceAtMost(0.25)
    
    val referralDiscountVal = originalTotal * referralPercent
    val volumeDiscountVal = originalTotal * volumePercent
    val totalDiscountVal = originalTotal * combinedPercent
    val finalTotalVal = originalTotal - totalDiscountVal
    
    return DiscountBreakdown(
        originalTotal = originalTotal,
        referralDiscount = referralDiscountVal,
        volumeDiscountPercent = (volumePercent * 100).toInt(),
        volumeDiscount = volumeDiscountVal,
        totalDiscount = totalDiscountVal,
        finalTotal = finalTotalVal
    )
}

