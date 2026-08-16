package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Raffle
import com.example.data.Ticket
import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaffleApp(viewModel: RaffleViewModel) {
    val context = LocalContext.current
    val currentRaffle by viewModel.currentRaffle.collectAsStateWithLifecycle()
    val allRaffles by viewModel.allRaffles.collectAsStateWithLifecycle()
    val tickets by viewModel.currentTickets.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val selectedPublicNumbers by viewModel.selectedPublicNumbers.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTicketForEdit by remember { mutableStateOf<Ticket?>(null) }
    var showDrawDialog by remember { mutableStateOf(false) }
    var showRaffleSelectorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentRaffle != null) currentRaffle!!.title else "Rifa Digital",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentRaffle != null) "Sorteio: ${currentRaffle!!.drawDate} • ${currentRaffle!!.drawType}" else "Gerencie e adquira rifas online",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Admin Mode Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isAdminMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        1.dp,
                                        (if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.15f),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAdminMode) "ADMIN" else "PÚBLICO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleAdminMode() },
                                modifier = Modifier
                                    .testTag("admin_toggle_button")
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdminMode) Icons.Rounded.Visibility else Icons.Rounded.AdminPanelSettings,
                                    contentDescription = if (isAdminMode) "Ver como Público" else "Painel Administrativo",
                                    tint = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (currentRaffle == null) {
                EmptyRaffleState(
                    onCreateNewClick = { showCreateDialog = true },
                    onGenerateRandomClick = {
                        viewModel.createRandomRaffle()
                        Toast.makeText(context, "Nova Rifa Aleatória Gerada!", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                val raffle = currentRaffle!!

                if (isAdminMode) {
                    AdminDashboardContent(
                        raffle = raffle,
                        tickets = tickets,
                        onNewRaffleClick = { showCreateDialog = true },
                        onEditTicketClick = { selectedTicketForEdit = it },
                        onDrawClick = { showDrawDialog = true },
                        viewModel = viewModel,
                        onSelectRaffleClick = { showRaffleSelectorDialog = true }
                    )
                } else {
                    PublicRaffleContent(
                        raffle = raffle,
                        tickets = tickets,
                        selectedNumbers = selectedPublicNumbers,
                        onNumberClick = { num -> viewModel.togglePublicNumberSelection(num) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateRaffleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, date, price, drawType, p1, p2, p3, phone ->
                viewModel.createRaffle(title, date, price, drawType, p1, p2, p3, phone)
                showCreateDialog = false
                Toast.makeText(context, "Nova rifa criada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (selectedTicketForEdit != null) {
        EditTicketDialog(
            ticket = selectedTicketForEdit!!,
            onDismiss = { selectedTicketForEdit = null },
            onConfirm = { status, buyer, phone ->
                viewModel.updateTicketStatus(selectedTicketForEdit!!, status, buyer, phone)
                selectedTicketForEdit = null
                Toast.makeText(context, "Número atualizado!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDrawDialog && currentRaffle != null) {
        DrawLotteryDialog(
            raffle = currentRaffle!!,
            tickets = tickets,
            onDismiss = { showDrawDialog = false },
            onConfirm = { d1, d2, d3 ->
                viewModel.saveDrawResults(currentRaffle!!, d1, d2, d3)
                showDrawDialog = false
                Toast.makeText(context, "Sorteio salvo!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showRaffleSelectorDialog) {
        RaffleSelectorDialog(
            allRaffles = allRaffles,
            currentRaffleId = currentRaffle?.id,
            onDismiss = { showRaffleSelectorDialog = false },
            onSelect = { id ->
                viewModel.selectRaffle(id)
                showRaffleSelectorDialog = false
            },
            onMakeActive = { id ->
                viewModel.makeRaffleActive(id)
                showRaffleSelectorDialog = false
            },
            onCreateNew = {
                showRaffleSelectorDialog = false
                showCreateDialog = true
            }
        )
    }
}

// Helpers
fun getInitials(name: String): String {
    val clean = name.trim()
    if (clean.isEmpty()) return ""
    val parts = clean.split("\\s+".toRegex())
    if (parts.isEmpty()) return ""
    val first = parts[0].take(1).uppercase()
    val second = if (parts.size > 1) parts[1].take(1).uppercase() else ""
    return first + second
}

@Composable
fun getStatusColor(status: String): Color {
    return when (status) {
        "PAGO" -> Color(0xFF2E7D32)
        "RESERVADO" -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
fun EmptyRaffleState(
    onCreateNewClick: () -> Unit,
    onGenerateRandomClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Inbox,
            contentDescription = "Sem rifas",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhuma Rifa Ativa",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Comece criando a sua primeira rifa digital baseada em folha tradicional para gerenciar ou vender números online.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateNewClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("create_first_raffle_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Nova Rifa", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onGenerateRandomClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("generate_random_first_raffle_button")
        ) {
            Icon(Icons.Rounded.Casino, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gerar Rifa Aleatória (Simulador)", fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------------
// PUBLIC VIEW SCREEN
// ---------------------------------------------------------------------------------
@Composable
fun PublicRaffleContent(
    raffle: Raffle,
    tickets: List<Ticket>,
    selectedNumbers: Set<String>,
    onNumberClick: (String) -> Unit,
    viewModel: RaffleViewModel
) {
    val context = LocalContext.current
    val referrer by viewModel.referrer.collectAsStateWithLifecycle()
    val hasReferral = !referrer.isNullOrBlank()
    val breakdown = calculateDiscount(selectedNumbers.size, raffle.ticketPrice, hasReferral)
    val originalTotal = breakdown.originalTotal
    val discountVal = breakdown.totalDiscount
    val finalTotal = breakdown.finalTotal

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }

    // Search, filter and receipt states
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TODOS") }
    var showReceiptDialogByBuyer by remember { mutableStateOf<Triple<String, String, List<String>>?>(null) }

    var selectedUnavailableTicketForView by remember { mutableStateOf<Ticket?>(null) }
    var showPixPaymentDialog by remember { mutableStateOf(false) }
    var showBankGatewayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RaffleHeaderCard(raffle = raffle)

        Spacer(modifier = Modifier.height(8.dp))

        referrer?.let { ref ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0FDF4)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Desconto de Indicação Ativo! 🎉",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "Você foi indicado por $ref. Ganhe 10% de desconto nesta compra!",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearReferrer() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar indicação",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        ShareRaffleCard(raffle = raffle)

        Spacer(modifier = Modifier.height(12.dp))

        // Public-facing Stats Row
        val paidCount = tickets.count { it.status == "PAGO" }
        val reservedCount = tickets.count { it.status == "RESERVADO" }
        val totalCollectedVal = paidCount * raffle.ticketPrice

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsCardCompact(
                title = "Arrecadado",
                value = "R$ ${String.format("%.0f", totalCollectedVal)}",
                color = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            StatsCardCompact(
                title = "Vendidos",
                value = "${paidCount + reservedCount}/100",
                color = if (isSystemInDarkTheme()) Color(0xFF818CF8) else Color(0xFF4F46E5),
                modifier = Modifier.weight(1f)
            )
            StatsCardCompact(
                title = "Valor N°",
                value = "R$ ${String.format("%.2f", raffle.ticketPrice)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrizeHighlightStrip(raffle = raffle, tickets = tickets)

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Selector / Surpresinha panel
        val availableNumbers = remember(tickets) { tickets.filter { it.status == "DISPONIVEL" }.map { it.number } }
        if (availableNumbers.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "🎰 COMPRA RÁPIDA (SURPRESINHA)",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3, 5, 10).forEach { qty ->
                            Button(
                                onClick = {
                                    if (availableNumbers.size < qty) {
                                        Toast.makeText(context, "Apenas ${availableNumbers.size} números disponíveis!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Shuffle and select random available numbers that are not currently selected
                                    val unselectedAvailable = availableNumbers.filter { !selectedNumbers.contains(it) }
                                    if (unselectedAvailable.size < qty) {
                                        Toast.makeText(context, "Não há mais números novos disponíveis para selecionar!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val shuffled = unselectedAvailable.shuffled().take(qty)
                                    shuffled.forEach { num ->
                                        onNumberClick(num)
                                    }
                                    Toast.makeText(context, "Sorteados e selecionados +$qty números livres!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "+$qty N°",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (raffle.drawnNumber1 != null || raffle.drawnNumber2 != null || raffle.drawnNumber3 != null) {
            DrawWinnersHighlightCard(raffle = raffle, tickets = tickets)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Filters and Interactive Search
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nome, WhatsApp ou número...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf(
                        Triple("TODOS", "Todos", MaterialTheme.colorScheme.primary),
                        Triple("LIVRE", "Livres", MaterialTheme.colorScheme.secondary),
                        Triple("RESERVADO", "Reservados", Color(0xFF1565C0)),
                        Triple("PAGO", "Pagos", Color(0xFF2E7D32))
                    )

                    filters.forEach { (key, label, color) ->
                        val isSelected = statusFilter == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { statusFilter = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid
        TraditionalLeafGrid(
            tickets = tickets,
            selectedNumbers = selectedNumbers,
            isAdmin = false,
            searchQuery = searchQuery,
            statusFilter = statusFilter,
            onTicketClick = { ticket ->
                if (ticket.status == "DISPONIVEL") {
                    onNumberClick(ticket.number)
                } else {
                    selectedUnavailableTicketForView = ticket
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = selectedNumbers.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Solicitar Reserva de Números",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Números Selecionados: ${selectedNumbers.sorted().joinToString(", ")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (discountVal > 0 && selectedNumbers.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "De R$ ${String.format(Locale.US, "%.2f", originalTotal)}",
                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "R$ ${String.format(Locale.US, "%.2f", finalTotal)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                        
                        val appliedPromos = mutableListOf<String>()
                        if (breakdown.volumeDiscountPercent > 0) {
                            appliedPromos.add("Combo (${breakdown.volumeDiscountPercent}% OFF)")
                        }
                        if (hasReferral) {
                            appliedPromos.add("Indicação (10% OFF)")
                        }
                        
                        Text(
                            text = "Desconto Ativo: ${appliedPromos.joinToString(" + ")} 🎉",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A)
                        )
                    } else {
                        Text(
                            text = "Valor Total: R$ ${String.format(Locale.US, "%.2f", originalTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = { buyerName = it },
                        label = { Text("Seu Nome") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buyer_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it },
                        label = { Text("Seu WhatsApp (Ex: 11999998888)") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buyer_phone_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Button
                        Button(
                            onClick = {
                                if (buyerName.trim().isEmpty() || buyerPhone.trim().isEmpty()) {
                                    Toast.makeText(context, "Por favor, preencha o Nome e WhatsApp", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.reserveNumbersAndSubmit(
                                    context,
                                    raffle,
                                    buyerName,
                                    buyerPhone,
                                    selectedNumbers.toList()
                                )
                                showReceiptDialogByBuyer = Triple(buyerName, buyerPhone, selectedNumbers.toList())
                                buyerName = ""
                                buyerPhone = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_reservation_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "WhatsApp",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Pix Button (Automático)
                        Button(
                            onClick = {
                                if (buyerName.trim().isEmpty() || buyerPhone.trim().isEmpty()) {
                                    Toast.makeText(context, "Por favor, preencha o Nome e WhatsApp", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                showPixPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("pix_payment_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode,
                                contentDescription = "Pagar Pix",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pagar via Pix (Auto)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReceiptDialogByBuyer != null) {
        val rNumbers = showReceiptDialogByBuyer!!.third
        val breakdownR = calculateDiscount(rNumbers.size, raffle.ticketPrice, hasReferral)
        RaffleReceiptDialog(
            raffle = raffle,
            buyerName = showReceiptDialogByBuyer!!.first,
            buyerPhone = showReceiptDialogByBuyer!!.second,
            numbers = rNumbers,
            originalTotal = breakdownR.originalTotal,
            discountAmount = breakdownR.totalDiscount,
            finalTotal = breakdownR.finalTotal,
            onDismiss = { showReceiptDialogByBuyer = null }
        )
    }

    if (selectedUnavailableTicketForView != null) {
        UnavailableTicketDetailsDialog(
            ticket = selectedUnavailableTicketForView!!,
            onDismiss = { selectedUnavailableTicketForView = null }
        )
    }

    if (showPixPaymentDialog) {
        PixPaymentDialog(
            raffle = raffle,
            selectedNumbers = selectedNumbers,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            originalTotal = originalTotal,
            discountAmount = discountVal,
            finalTotal = finalTotal,
            onDismiss = { showPixPaymentDialog = false },
            onGoToGateway = {
                showPixPaymentDialog = false
                showBankGatewayDialog = true
            }
        )
    }

    if (showBankGatewayDialog) {
        BankGatewayDialog(
            raffle = raffle,
            selectedNumbers = selectedNumbers,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            originalTotal = originalTotal,
            discountAmount = discountVal,
            finalTotal = finalTotal,
            onDismiss = { showBankGatewayDialog = false },
            onConfirmPayment = {
                viewModel.confirmPaymentAndMarkPaid(
                    raffle.id,
                    selectedNumbers.toList(),
                    buyerName,
                    buyerPhone
                )
                buyerName = ""
                buyerPhone = ""
            }
        )
    }
}

// ---------------------------------------------------------------------------------
// ADMIN DASHBOARD SCREEN
// ---------------------------------------------------------------------------------
@Composable
fun AdminDashboardContent(
    raffle: Raffle,
    tickets: List<Ticket>,
    onNewRaffleClick: () -> Unit,
    onEditTicketClick: (Ticket) -> Unit,
    onDrawClick: () -> Unit,
    viewModel: RaffleViewModel,
    onSelectRaffleClick: () -> Unit
) {
    val context = LocalContext.current
    var adminTabSelected by remember { mutableStateOf(0) }
    var sortByMostTickets by remember { mutableStateOf(true) }
    var showReceiptDialogByAdmin by remember { mutableStateOf<Triple<String, String, List<String>>?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = adminTabSelected,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = adminTabSelected == 0,
                onClick = { adminTabSelected = 0 },
                text = { Text("Estatísticas", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.Dashboard, null) }
            )
            Tab(
                selected = adminTabSelected == 1,
                onClick = { adminTabSelected = 1 },
                text = { Text("Tabela (Folha)", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.GridOn, null) }
            )
            Tab(
                selected = adminTabSelected == 2,
                onClick = { adminTabSelected = 2 },
                text = { Text("Compradores", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.People, null) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (adminTabSelected) {
                0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visão Geral",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onSelectRaffleClick, modifier = Modifier.testTag("admin_select_raffle_button")) {
                                Icon(Icons.Rounded.FolderOpen, "Minhas Rifas", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDrawClick, modifier = Modifier.testTag("admin_draw_results_button")) {
                                Icon(Icons.Rounded.EmojiEvents, "Sorteio", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onNewRaffleClick, modifier = Modifier.testTag("admin_new_raffle_button")) {
                                Icon(Icons.Rounded.Add, "Nova Rifa", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FirebaseSyncCard(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(12.dp))

                    RaffleHeaderCard(raffle = raffle)

                    Spacer(modifier = Modifier.height(16.dp))

                    val paidTicketsCount = tickets.count { it.status == "PAGO" }
                    val reservedTicketsCount = tickets.count { it.status == "RESERVADO" }
                    val availableTicketsCount = tickets.count { it.status == "DISPONIVEL" }

                    val totalCollected = paidTicketsCount * raffle.ticketPrice
                    val totalPending = reservedTicketsCount * raffle.ticketPrice

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsCardCompact(
                            title = "Arrecadado",
                            value = "R$ ${String.format("%.0f", totalCollected)}",
                            color = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        StatsCardCompact(
                            title = "Vendidos",
                            value = "${paidTicketsCount + reservedTicketsCount}/100",
                            color = if (isSystemInDarkTheme()) Color(0xFF818CF8) else Color(0xFF4F46E5),
                            modifier = Modifier.weight(1f)
                        )
                        StatsCardCompact(
                            title = "Valor N°",
                            value = "R$ ${String.format("%.2f", raffle.ticketPrice)}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Progresso de Vendas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${paidTicketsCount + reservedTicketsCount} Vendidos / Reservados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$availableTicketsCount Livres",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (paidTicketsCount + reservedTicketsCount).toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    RaffleDonutChart(
                        paid = paidTicketsCount,
                        reserved = reservedTicketsCount,
                        available = availableTicketsCount
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium CRM & Analytics Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ANÁLISE FINANCEIRA & PROJEÇÕES CRM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Projections Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pendente (Reservado)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("R$ ${String.format("%.2f", totalPending)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (isSystemInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF2563EB))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Máximo Potencial", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("R$ ${String.format("%.2f", 100 * raffle.ticketPrice)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Conversion rate and Realistic Forecast
                            val totalReservedAndPaid = paidTicketsCount + reservedTicketsCount
                            val conversionRate = if (totalReservedAndPaid > 0) (paidTicketsCount.toFloat() / totalReservedAndPaid.toFloat() * 100f) else 0f
                            val realisticForecast = totalCollected + (totalPending * 0.70) // Estimating 70% conversion of reserved tickets

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Taxa de Conversão", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.Percent,
                                            contentDescription = null,
                                            tint = if (conversionRate >= 75f) Color(0xFF10B981) else if (conversionRate >= 40f) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("${String.format("%.1f", conversionRate)}%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Faturamento Realista (70% Conv.)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("R$ ${String.format("%.2f", realisticForecast)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ParticipantPaymentDashboard(
                        tickets = tickets,
                        ticketPrice = raffle.ticketPrice,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulator & Quick Actions Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Casino,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SIMULADOR E AÇÕES RÁPIDAS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = "Use as opções rápidas abaixo para simular cenários reais de vendas ou gerar novas rifas de teste instantaneamente.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.createRandomRaffle()
                                        Toast.makeText(context, "Nova rifa aleatória gerada!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nova Rifa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.fillAllTicketsWithMockData(raffle.id)
                                        Toast.makeText(context, "Todos os 100 números foram preenchidos!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preencher Tudo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DrawWinnersHighlightCard(raffle = raffle, tickets = tickets)
                }

                1 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Folha de Números",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Toque em um número para editar",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    RaffleLegend()
                    Spacer(modifier = Modifier.height(12.dp))

                    TraditionalLeafGrid(
                        tickets = tickets,
                        selectedNumbers = emptySet(),
                        isAdmin = true,
                        onTicketClick = { onEditTicketClick(it) }
                    )
                }

                2 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lista de Compradores",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Sort toggle button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                .clickable { sortByMostTickets = !sortByMostTickets }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (sortByMostTickets) Icons.Rounded.Sort else Icons.Rounded.SortByAlpha,
                                contentDescription = "Filtro",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (sortByMostTickets) "Mais Números" else "Alfabética",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val buyers = tickets
                        .filter { it.status != "DISPONIVEL" && !it.buyerName.isNullOrEmpty() }
                        .groupBy { Pair(it.buyerName!!, it.buyerPhone ?: "") }
                        .toList()
                        .let { list ->
                            if (sortByMostTickets) {
                                list.sortedByDescending { it.second.size }
                            } else {
                                list.sortedBy { it.first.first.lowercase() }
                            }
                        }

                    if (buyers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum comprador registrado ainda.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        buyers.forEachIndexed { index, (buyer, numbersList) ->
                            val (name, phone) = buyer
                            val numbersOnly = numbersList.map { it.number }
                            val isTopSupporter = sortByMostTickets && index == 0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isTopSupporter) BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isTopSupporter) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.WorkspacePremium,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFD700),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "TOP 1",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "WhatsApp: $phone",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            numbersList.forEach { ticket ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .background(
                                                            getStatusColor(ticket.status),
                                                            RoundedCornerShape(4.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ticket.number,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // WhatsApp share button
                                        IconButton(
                                            onClick = {
                                                viewModel.shareWithClientViaWhatsApp(
                                                    context,
                                                    raffle.title,
                                                    raffle.ticketPrice,
                                                    name,
                                                    phone,
                                                    numbersOnly
                                                )
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color(0xFF25D366).copy(alpha = 0.1f),
                                                contentColor = Color(0xFF25D366)
                                            ),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = "Enviar WhatsApp Cobrança",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Premium Digital Receipt Viewer
                                        IconButton(
                                            onClick = {
                                                showReceiptDialogByAdmin = Triple(name, phone, numbersOnly)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ReceiptLong,
                                                contentDescription = "Visualizar Recibo",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showReceiptDialogByAdmin != null) {
                        val admNumbers = showReceiptDialogByAdmin!!.third
                        val admTotal = admNumbers.size * raffle.ticketPrice
                        RaffleReceiptDialog(
                            raffle = raffle,
                            buyerName = showReceiptDialogByAdmin!!.first,
                            buyerPhone = showReceiptDialogByAdmin!!.second,
                            numbers = admNumbers,
                            originalTotal = admTotal,
                            discountAmount = 0.0,
                            finalTotal = admTotal,
                            onDismiss = { showReceiptDialogByAdmin = null }
                        )
                    }
                }
            }
        }
    }
}

// Stats Card helper Composable
@Composable
fun StatsCard(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun StatsCardCompact(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun PrizeHighlightStrip(raffle: Raffle, tickets: List<Ticket>) {
    val hasWinner = raffle.drawnNumber1 != null
    val winnerTicket = if (hasWinner) tickets.find { it.number == raffle.drawnNumber1 } else null
    val winnerName = if (winnerTicket != null && winnerTicket.status != "DISPONIVEL" && !winnerTicket.buyerName.isNullOrEmpty()) {
        winnerTicket.buyerName
    } else if (hasWinner) {
        "Sem Comprador"
    } else {
        null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A) // Dark Slate-900
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "1º PRÊMIO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8) // Slate 400
                )
                Text(
                    text = raffle.prize1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFF334155)) // Slate 700
            )
            
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (hasWinner) "Nº SORTEADO: ${raffle.drawnNumber1}" else "GANHADOR 1º",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8) // Slate 400
                )
                Text(
                    text = winnerName ?: "Aguardando...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasWinner) Color(0xFF10B981) else Color(0xFFF59E0B), // Emerald or Yellow
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// SHARED COMPONENTS
// ---------------------------------------------------------------------------------

@Composable
fun RaffleHeaderCard(raffle: Raffle) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Premium Gold Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Premium Gold color
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "EDIÇÃO DE LUXO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSystemInDarkTheme()) Color(0xFFFFD700) else Color(0xFFD4AF37),
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = raffle.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sorteio: ${raffle.drawDate} | Via: ${raffle.drawType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VALOR DO NÚMERO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", raffle.ticketPrice)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (raffle.adminPhone.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Color(0xFF25D366).copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp Rifa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PREMIAÇÃO:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            PrizeRow(icon = Icons.Rounded.Star, place = "1º Prêmio", description = raffle.prize1, tint = Color(0xFFFFD700))
            PrizeRow(icon = Icons.Rounded.StarBorder, place = "2º Prêmio", description = raffle.prize2, tint = Color(0xFFC0C0C0))
            PrizeRow(icon = Icons.Rounded.StarHalf, place = "3º Prêmio", description = raffle.prize3, tint = Color(0xFFCD7F32))
        }
    }
}

@Composable
fun PrizeRow(icon: ImageVector, place: String, description: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$place:",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RaffleLegend() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(label = "Livre", boxColor = MaterialTheme.colorScheme.surface, textColor = MaterialTheme.colorScheme.primary, borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            LegendItem(label = "Reservado", boxColor = Color(0xFF1565C0), textColor = Color.White)
            LegendItem(label = "Pago", boxColor = Color(0xFF2E7D32), textColor = Color.White)
            LegendItem(label = "Selecionado", boxColor = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    boxColor: Color,
    textColor: Color,
    borderStroke: BorderStroke? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(boxColor, RoundedCornerShape(4.dp))
                .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(4.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "00",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun TraditionalLeafGrid(
    tickets: List<Ticket>,
    selectedNumbers: Set<String>,
    isAdmin: Boolean,
    searchQuery: String = "",
    statusFilter: String = "TODOS",
    onTicketClick: (Ticket) -> Unit
) {
    val ticketsMap = remember(tickets) { tickets.associateBy { it.number } }
    val isDark = isSystemInDarkTheme()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // High-density Integrated Header and Legends
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOLHA DE NÚMEROS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyLegendDot(label = "Pago", color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                    TinyLegendDot(label = "Res.", color = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6))
                    TinyLegendDot(label = "Livre", color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                }
            }

            // Grid Layout (10 Rows of 10 Cells)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0..9) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..9) {
                            val numberVal = row * 10 + col
                            val formattedNum = String.format("%02d", numberVal)
                            val ticket = ticketsMap[formattedNum] ?: Ticket(id = "temp_$formattedNum", raffleId = 0, number = formattedNum, status = "DISPONIVEL")

                            val isSelected = selectedNumbers.contains(formattedNum)

                            val matchesSearch = remember(searchQuery, formattedNum, ticket) {
                                searchQuery.isEmpty() ||
                                formattedNum.contains(searchQuery) ||
                                (ticket.buyerName?.contains(searchQuery, ignoreCase = true) == true) ||
                                (ticket.buyerPhone?.contains(searchQuery) == true)
                            }

                            val matchesFilter = remember(statusFilter, ticket) {
                                statusFilter == "TODOS" ||
                                (statusFilter == "LIVRE" && ticket.status == "DISPONIVEL") ||
                                (statusFilter == "RESERVADO" && ticket.status == "RESERVADO") ||
                                (statusFilter == "PAGO" && ticket.status == "PAGO")
                            }

                            val isDimmed = !matchesSearch || !matchesFilter

                            val cellBgColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                ticket.status == "PAGO" -> if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                                ticket.status == "RESERVADO" -> if (isDark) Color(0xFF2563EB) else Color(0xFF3B82F6)
                                else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            }

                            val cellBorderColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                ticket.status == "PAGO" -> if (isDark) Color(0xFF047857) else Color(0xFF059669)
                                ticket.status == "RESERVADO" -> if (isDark) Color(0xFF1D4ED8) else Color(0xFF2563EB)
                                else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            }

                            val cellContentColor = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                ticket.status == "PAGO" || ticket.status == "RESERVADO" -> Color.White
                                else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .alpha(if (isDimmed) 0.15f else 1f)
                                    .background(cellBgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = cellBorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onTicketClick(ticket) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = ticket.number,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cellContentColor
                                    )

                                    if (!isSelected && ticket.status != "DISPONIVEL" && !ticket.buyerName.isNullOrEmpty()) {
                                        val initials = getInitials(ticket.buyerName)
                                        Text(
                                            text = initials,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TinyLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun DrawWinnersHighlightCard(raffle: Raffle, tickets: List<Ticket>) {
    if (raffle.drawnNumber1 == null && raffle.drawnNumber2 == null && raffle.drawnNumber3 == null) {
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winners_highlight_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Ganhadores",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GANHADORES DO SORTEIO",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            WinnerEntry(place = "1º Prêmio", drawnNum = raffle.drawnNumber1, prizeName = raffle.prize1, tickets = tickets)
            Spacer(modifier = Modifier.height(8.dp))
            WinnerEntry(place = "2º Prêmio", drawnNum = raffle.drawnNumber2, prizeName = raffle.prize2, tickets = tickets)
            Spacer(modifier = Modifier.height(8.dp))
            WinnerEntry(place = "3º Prêmio", drawnNum = raffle.drawnNumber3, prizeName = raffle.prize3, tickets = tickets)
        }
    }
}

@Composable
fun WinnerEntry(place: String, drawnNum: String?, prizeName: String?, tickets: List<Ticket>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$place: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = prizeName ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (drawnNum != null) {
                val winnerTicket = tickets.find { it.number == drawnNum }
                if (winnerTicket != null && winnerTicket.status != "DISPONIVEL" && !winnerTicket.buyerName.isNullOrEmpty()) {
                    Text(
                        text = "Ganhador(a): ${winnerTicket.buyerName}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "WhatsApp: ${winnerTicket.buyerPhone ?: "N/A"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Número sem comprador registrado.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                Text(
                    text = "Aguardando sorteio...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        if (drawnNum != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = drawnNum,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// DIALOG COMPOSABLES
// ---------------------------------------------------------------------------------

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RaffleReceiptDialog(
    raffle: Raffle,
    buyerName: String,
    buyerPhone: String,
    numbers: List<String>,
    originalTotal: Double,
    discountAmount: Double,
    finalTotal: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ConfirmationNumber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Recibo de Compra",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Outer coupon container
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Ticket header
                        Text(
                            text = raffle.title.uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "COMPROVANTE DE RESERVA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Buyer details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Apoiador:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(buyerName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Contato:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(buyerPhone, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sorteio:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(raffle.drawDate, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Numbers list
                        Text(
                            text = "BILHETES SELECIONADOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // High-end ticket chips grid using FlowRow
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            numbers.sorted().forEach { number ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(34.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = number,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Financial pricing
                        if (discountAmount > 0) {
                            Text(
                                text = "VALOR ORIGINAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "R$ ${String.format(Locale.US, "%.2f", originalTotal)}",
                                fontSize = 14.sp,
                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DESCONTO DE INDICAÇÃO (10% OFF)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = "- R$ ${String.format(Locale.US, "%.2f", discountAmount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "VALOR TOTAL COM DESCONTO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = "R$ ${String.format(Locale.US, "%.2f", finalTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF16A34A)
                            )
                        } else {
                            Text(
                                text = "VALOR TOTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "R$ ${String.format(Locale.US, "%.2f", finalTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Mock bar code / QR code representation for premium look
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(28.dp)
                                    .background(
                                        if (isSystemInDarkTheme()) Color.White else Color.Black,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            ) {
                                // Draw lines to look like a barcode
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (i in 0..15) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(if (i % 3 == 0) 3.dp else if (i % 2 == 0) 1.dp else 2.dp)
                                                .background(if (isSystemInDarkTheme()) Color.Black else Color.White)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RIFA-DIGITAL-${raffle.id}-${buyerPhone.takeLast(4)}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Apresente este recibo para o administrador realizar a liberação definitiva dos números.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val numbersStr = numbers.sorted().joinToString(", ")
                    val clipData = android.content.ClipData.newPlainText(
                        "Recibo Rifa",
                        "Recibo Rifa: ${raffle.title}\nComprador: $buyerName\nNúmeros: $numbersStr\nTotal: R$ ${String.format("%.2f", numbers.size * raffle.ticketPrice)}"
                    )
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(clipData)
                    Toast.makeText(context, "Texto do recibo copiado!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar Recibo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaffleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var drawDate by remember { mutableStateOf("") }
    var ticketPriceStr by remember { mutableStateOf("") }
    var drawType by remember { mutableStateOf("Loteria Federal") }
    var prize1 by remember { mutableStateOf("") }
    var prize2 by remember { mutableStateOf("") }
    var prize3 by remember { mutableStateOf("") }
    var adminPhone by remember { mutableStateOf("") }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                "Criar Nova Rifa",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Autofill button
                TextButton(
                    onClick = {
                        val titles = listOf(
                            "Rifa de iPhone 15 Pro Max 256GB",
                            "Fusca TSI de Colecionador",
                            "Pix de R$ 10.000,00 na Conta",
                            "Rifa Premium PlayStation 5 Slim",
                            "Super Moto Honda CB 300F Twister 0km",
                            "Kit Churrasco Completo + Cooler Stella"
                        )
                        val prizes = listOf(
                            Triple("iPhone 15 Pro Max 256GB", "R$ 500 via Pix", "R$ 200 via Pix"),
                            Triple("Fusca TSI completo 2013", "R$ 1.000 via Pix", "R$ 500 via Pix"),
                            Triple("Pix de R$ 10.000,00", "Pix de R$ 1.000,00", "Pix de R$ 500,00"),
                            Triple("PlayStation 5 Slim + 2 Jogos", "R$ 500 em saldo PSN", "R$ 200 via Pix"),
                            Triple("Honda CB 300F Twister 0km", "R$ 1.000 via Pix", "R$ 400 via Pix"),
                            Triple("Kit Churrasco + Cooler + Carnes", "Kit Faca do Chefe", "R$ 100 via Pix")
                        )
                        val index = (titles.indices).random()
                        title = titles[index]
                        val randomPrizes = prizes[index]
                        prize1 = randomPrizes.first
                        prize2 = randomPrizes.second
                        prize3 = randomPrizes.third
                        ticketPriceStr = listOf("5.00", "10.00", "15.00", "20.00", "25.00", "50.00").random()
                        drawType = "Loteria Federal"
                        
                        val calendar = java.util.Calendar.getInstance()
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 10)
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        drawDate = sdf.format(calendar.time)
                        adminPhone = "11999998888"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Rounded.Casino, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preencher com Dados Aleatórios", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Rifa") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_raffle_title"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = drawDate,
                        onValueChange = { drawDate = it },
                        label = { Text("Data Sorteio") },
                        placeholder = { Text("Ex: 15/08/2026") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_raffle_date"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ticketPriceStr,
                        onValueChange = { ticketPriceStr = it },
                        label = { Text("Valor Número (R$)") },
                        placeholder = { Text("Ex: 10.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_raffle_price"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = drawType,
                    onValueChange = { drawType = it },
                    label = { Text("Tipo de Sorteio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminPhone,
                    onValueChange = { adminPhone = it },
                    label = { Text("WhatsApp Administrador (Receber reservas)") },
                    placeholder = { Text("Ex: 11999998888") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_raffle_admin_phone"),
                    singleLine = true
                )

                Text(
                    "Prêmios:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = prize1,
                    onValueChange = { prize1 = it },
                    label = { Text("1º Lugar (Prêmio Principal)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_raffle_prize1"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prize2,
                    onValueChange = { prize2 = it },
                    label = { Text("2º Lugar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prize3,
                    onValueChange = { prize3 = it },
                    label = { Text("3º Lugar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val price = ticketPriceStr.toDoubleOrNull()
                    if (title.trim().isEmpty() || drawDate.trim().isEmpty() || price == null || prize1.trim().isEmpty() || adminPhone.trim().isEmpty()) {
                        Toast.makeText(context, "Por favor, preencha todos os campos obrigatórios e coloque um valor válido para o preço.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    onConfirm(title, drawDate, price, drawType, prize1, prize2, prize3, adminPhone)
                },
                modifier = Modifier.testTag("confirm_create_raffle_button")
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTicketDialog(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var status by remember { mutableStateOf(ticket.status) }
    var buyerName by remember { mutableStateOf(ticket.buyerName ?: "") }
    var buyerPhone by remember { mutableStateOf(ticket.buyerPhone ?: "") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                "Gerenciar Número ${ticket.number}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Status do Número:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusButton(
                        label = "Livre",
                        isSelected = status == "DISPONIVEL",
                        activeColor = MaterialTheme.colorScheme.outline,
                        onClick = {
                            status = "DISPONIVEL"
                            buyerName = ""
                            buyerPhone = ""
                        },
                        modifier = Modifier.weight(1f).testTag("status_free_button")
                    )
                    StatusButton(
                        label = "Reservado",
                        isSelected = status == "RESERVADO",
                        activeColor = Color(0xFF1565C0),
                        onClick = { status = "RESERVADO" },
                        modifier = Modifier.weight(1f).testTag("status_reserved_button")
                    )
                    StatusButton(
                        label = "Pago",
                        isSelected = status == "PAGO",
                        activeColor = Color(0xFF2E7D32),
                        onClick = { status = "PAGO" },
                        modifier = Modifier.weight(1f).testTag("status_paid_button")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedVisibility(visible = status != "DISPONIVEL") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text("Nome do Comprador") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_buyer_name"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = buyerPhone,
                            onValueChange = { buyerPhone = it },
                            label = { Text("WhatsApp do Comprador") },
                            placeholder = { Text("Ex: 11999998888") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_buyer_phone"),
                            singleLine = true
                        )

                        if (ticket.status != "DISPONIVEL") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Última atualização: ${formatDateTime(ticket.updatedAt)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onConfirm(status, buyerName, buyerPhone)
                },
                modifier = Modifier.testTag("confirm_edit_ticket_button")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StatusButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawLotteryDialog(
    raffle: Raffle,
    tickets: List<Ticket>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var d1 by remember { mutableStateOf(raffle.drawnNumber1 ?: "") }
    var d2 by remember { mutableStateOf(raffle.drawnNumber2 ?: "") }
    var d3 by remember { mutableStateOf(raffle.drawnNumber3 ?: "") }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                "Inserir Resultados Sorteio",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section for automatic drawing suggestions
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "✨ Sorteador Automático",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = "Gere números da sorte instantaneamente de forma aleatória:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Paid numbers draw button
                            Button(
                                onClick = {
                                    val paidTickets = tickets.filter { it.status == "PAGO" }.map { it.number }
                                    if (paidTickets.isEmpty()) {
                                        Toast.makeText(context, "Nenhum número pago ainda! Realizando sorteio geral...", Toast.LENGTH_SHORT).show()
                                        // General draw fallback
                                        val randomNumbers = (0..99).map { String.format(Locale.US, "%02d", it) }.shuffled().take(3)
                                        d1 = randomNumbers.getOrElse(0) { "" }
                                        d2 = randomNumbers.getOrElse(1) { "" }
                                        d3 = randomNumbers.getOrElse(2) { "" }
                                    } else {
                                        val shuffled = paidTickets.shuffled()
                                        val winners = mutableListOf<String>()
                                        winners.addAll(shuffled.take(3))
                                        
                                        // Fill remaining slots up to 3 if fewer than 3 paid tickets
                                        while (winners.size < 3) {
                                            val extraNum = String.format(Locale.US, "%02d", (0..99).random())
                                            if (!winners.contains(extraNum)) {
                                                winners.add(extraNum)
                                            }
                                        }
                                        
                                        d1 = winners[0]
                                        d2 = winners[1]
                                        d3 = winners[2]
                                        Toast.makeText(context, "Sorteado a partir dos números pagos! 🎉", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("Apenas Pagos 🎟️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Completely random draw button
                            Button(
                                onClick = {
                                    val randomNumbers = (0..99).map { String.format(Locale.US, "%02d", it) }.shuffled().take(3)
                                    d1 = randomNumbers[0]
                                    d2 = randomNumbers[1]
                                    d3 = randomNumbers[2]
                                    Toast.makeText(context, "Sorteio geral aleatório gerado! 🎲", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Qualquer Número 🎲", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Text(
                    text = "Ou digite os números manualmente abaixo:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = d1,
                    onValueChange = { d1 = it.take(2) },
                    label = { Text("1º Prêmio (Número de 00 a 99)") },
                    placeholder = { Text("Ex: 44") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawn_num_1"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = d2,
                    onValueChange = { d2 = it.take(2) },
                    label = { Text("2º Prêmio (Número de 00 a 99)") },
                    placeholder = { Text("Ex: 89") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawn_num_2"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = d3,
                    onValueChange = { d3 = it.take(2) },
                    label = { Text("3º Prêmio (Número de 00 a 99)") },
                    placeholder = { Text("Ex: 05") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawn_num_3"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (d1.trim().isEmpty() || d2.trim().isEmpty() || d3.trim().isEmpty()) {
                        Toast.makeText(context, "Por favor, digite todos os três números.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onConfirm(d1, d2, d3)
                },
                modifier = Modifier.testTag("confirm_draw_button")
            ) {
                Text("Sortear")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RaffleSelectorDialog(
    allRaffles: List<Raffle>,
    currentRaffleId: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onMakeActive: (Int) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Gerenciar Minhas Rifas",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Button(
                    onClick = onCreateNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Nova Rifa", fontWeight = FontWeight.Bold)
                }

                Divider()

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(allRaffles) { raffle ->
                        val isCurrent = raffle.id == currentRaffleId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(raffle.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = raffle.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (raffle.isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        Color(0xFF2E7D32),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "ATIVA",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Sorteio: ${raffle.drawDate} | R$ ${String.format("%.2f", raffle.ticketPrice)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                if (!raffle.isActive) {
                                    TextButton(
                                        onClick = { onMakeActive(raffle.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun FirebaseSyncCard(viewModel: RaffleViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isFirebaseAvailable = viewModel.isFirebaseAvailable

    var expanded by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (currentUser != null) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp, 
            if (currentUser != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firebase_sync_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!isFirebaseAvailable) Icons.Rounded.CloudOff 
                                      else if (currentUser != null) Icons.Rounded.CloudQueue 
                                      else Icons.Rounded.Cloud,
                        contentDescription = "Nuvem",
                        tint = if (!isFirebaseAvailable) MaterialTheme.colorScheme.error 
                               else if (currentUser != null) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sincronização em Nuvem",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (!isFirebaseAvailable) "Firebase pendente (Toque para ver)"
                                   else if (currentUser != null) "Conectado como ${currentUser?.email}"
                                   else "Desconectado (Apenas local ativo)",
                            fontSize = 11.sp,
                            color = if (!isFirebaseAvailable) MaterialTheme.colorScheme.error 
                                    else if (currentUser != null) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Fechar" else "Abrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isFirebaseAvailable) {
                        Text(
                            text = "Para ativar o salvamento automático na nuvem (Firestore) e login seguro com múltiplos dispositivos:\n\n1. Adicione o arquivo 'google-services.json' na pasta '/app/'.\n2. Crie um projeto no console do Firebase e ative Authentication (Email) e Firestore Database.\n\nAtualmente o app está operando em Modo Offline Local (Room DB) 100% funcional.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    } else if (currentUser != null) {
                        Text(
                            text = "Sua conta está integrada e todos os dados de rifas criadas ou atualizadas serão automaticamente espelhados na nuvem em tempo real.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    viewModel.syncFromRoomToFirestore()
                                    Toast.makeText(context, "Sincronização enviada!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enviar Nuvem", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { 
                                    viewModel.syncFromFirestoreToRoom()
                                    Toast.makeText(context, "Baixando da nuvem...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Baixar Nuvem", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { 
                                viewModel.signOut {
                                    Toast.makeText(context, "Sessão encerrada.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sair da Conta")
                        }
                    } else {
                        Text(
                            text = if (isRegistering) "Crie uma conta para salvar suas rifas na nuvem de forma segura:" else "Acesse com sua conta do organizador para sincronizar suas rifas:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (email.trim().isEmpty() || password.isEmpty()) {
                                        Toast.makeText(context, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (isRegistering) {
                                        viewModel.signUpWithEmail(email, password, {
                                            Toast.makeText(context, "Cadastro realizado!", Toast.LENGTH_SHORT).show()
                                            expanded = false
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    } else {
                                        viewModel.signInWithEmail(email, password, {
                                            Toast.makeText(context, "Login efetuado!", Toast.LENGTH_SHORT).show()
                                            expanded = false
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                    email = ""
                                    password = ""
                                },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(if (isRegistering) "Confirmar Cadastro" else "Acessar Conta")
                            }

                            TextButton(
                                onClick = { isRegistering = !isRegistering },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isRegistering) "Já tenho conta" else "Criar Conta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RaffleDonutChart(
    paid: Int,
    reserved: Int,
    available: Int,
    modifier: Modifier = Modifier
) {
    val total = paid + reserved + available
    if (total == 0) return

    val paidAngle = (paid.toFloat() / total) * 360f
    val reservedAngle = (reserved.toFloat() / total) * 360f
    val availableAngle = (available.toFloat() / total) * 360f

    val colorPaid = if (isSystemInDarkTheme()) Color(0xFF10B981) else Color(0xFF059669)
    val colorReserved = if (isSystemInDarkTheme()) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val colorAvailable = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribuição de Números",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 26f
                        val sizeMinusStroke = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        if (paid == 0 && reserved == 0) {
                            drawArc(
                                color = colorAvailable.copy(alpha = 0.25f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            // Draw Available
                            if (availableAngle > 0f) {
                                drawArc(
                                    color = colorAvailable.copy(alpha = 0.25f),
                                    startAngle = -90f,
                                    sweepAngle = availableAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                            // Draw Reserved
                            if (reservedAngle > 0f) {
                                drawArc(
                                    color = colorReserved,
                                    startAngle = -90f + availableAngle,
                                    sweepAngle = reservedAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                            // Draw Paid
                            if (paidAngle > 0f) {
                                drawArc(
                                    color = colorPaid,
                                    startAngle = -90f + availableAngle + reservedAngle,
                                    sweepAngle = paidAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }
                    }

                    // Centered Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val totalSold = paid + reserved
                        Text(
                            text = "$totalSold",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vendidos",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    LegendItemRow(
                        color = colorPaid,
                        label = "Pagos",
                        count = paid,
                        percentage = "${(paid * 100f / total).toInt()}%"
                    )
                    LegendItemRow(
                        color = colorReserved,
                        label = "Reservados",
                        count = reserved,
                        percentage = "${(reserved * 100f / total).toInt()}%"
                    )
                    LegendItemRow(
                        color = colorAvailable.copy(alpha = 0.4f),
                        label = "Livres",
                        count = available,
                        percentage = "${(available * 100f / total).toInt()}%"
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItemRow(color: Color, label: String, count: Int, percentage: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = percentage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ParticipantPaymentDashboard(
    tickets: List<Ticket>,
    ticketPrice: Double,
    viewModel: RaffleViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(0) } // 0: Todos, 1: Pago, 2: Pendente
    val context = LocalContext.current

    val buyers = tickets
        .filter { it.status != "DISPONIVEL" && !it.buyerName.isNullOrEmpty() }
        .groupBy { Pair(it.buyerName!!, it.buyerPhone!!) }
        .map { (buyerKey, numbersList) ->
            val paidCount = numbersList.count { it.status == "PAGO" }
            val reservedCount = numbersList.count { it.status == "RESERVADO" }
            val totalCount = numbersList.size
            val paidValue = paidCount * ticketPrice
            val pendingValue = reservedCount * ticketPrice
            val isFullyPaid = reservedCount == 0
            
            val overallStatus = if (isFullyPaid) "PAGO" else if (paidCount == 0) "PENDENTE" else "MISTO"
            
            Triple(buyerKey, numbersList, mapOf(
                "paidCount" to paidCount,
                "reservedCount" to reservedCount,
                "totalCount" to totalCount,
                "paidValue" to paidValue,
                "pendingValue" to pendingValue,
                "overallStatus" to overallStatus
            ))
        }
        .filter { (buyerKey, _, stats) ->
            val name = buyerKey.first
            val phone = buyerKey.second
            val statusMatches = when (statusFilter) {
                0 -> true
                1 -> stats["overallStatus"] == "PAGO"
                2 -> stats["overallStatus"] == "PENDENTE" || stats["overallStatus"] == "MISTO"
                else -> true
            }
            val queryMatches = name.contains(searchQuery, ignoreCase = true) || phone.contains(searchQuery)
            statusMatches && queryMatches
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "STATUS DE PAGAMENTO DOS PARTICIPANTES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar participante...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filters = listOf("Todos", "Pagos", "Pendentes")
                filters.forEachIndexed { idx, label ->
                    val isSelected = statusFilter == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { statusFilter = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (buyers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum participante encontrado.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    buyers.take(15).forEach { (buyerKey, numbersList, stats) ->
                        val name = buyerKey.first
                        val phone = buyerKey.second
                        val paidCount = stats["paidCount"] as Int
                        val reservedCount = stats["reservedCount"] as Int
                        val totalCount = stats["totalCount"] as Int
                        val paidValue = stats["paidValue"] as Double
                        val pendingValue = stats["pendingValue"] as Double
                        val overallStatus = stats["overallStatus"] as String

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = phone,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                when (overallStatus) {
                                                    "PAGO" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                                    "PENDENTE" -> Color(0xFF3B82F6).copy(alpha = 0.12f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (overallStatus) {
                                                "PAGO" -> "Pago"
                                                "PENDENTE" -> "Pendente"
                                                else -> "Parcial"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (overallStatus) {
                                                "PAGO" -> if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)
                                                "PENDENTE" -> if (isSystemInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                                else -> if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    if (paidCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(paidCount.toFloat())
                                                .background(Color(0xFF10B981))
                                        )
                                    }
                                    if (reservedCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(reservedCount.toFloat())
                                                .background(Color(0xFF3B82F6))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${numbersList.map { it.number }.sorted().joinToString(", ")} ($totalCount nº)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (paidValue > 0.0) {
                                            Text(
                                                text = "Pago: R$ ${String.format("%.2f", paidValue)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                        if (pendingValue > 0.0) {
                                            Text(
                                                text = "Pend: R$ ${String.format("%.2f", pendingValue)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3B82F6)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (buyers.size > 15) {
                        Text(
                            text = "E mais ${buyers.size - 15} participantes...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// EXTRA CO-BRANDED SHARE & PAYMENT GATEWAY ENGINE
// ---------------------------------------------------------------------------------

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun ShareRaffleCard(raffle: Raffle) {
    val context = LocalContext.current
    var referrerInput by remember { mutableStateOf("") }
    var isReferralExpanded by remember { mutableStateOf(false) }
    var isMarketingExpanded by remember { mutableStateOf(false) }

    val shareText = "Venha participar da nossa Rifa: *${raffle.title}*! 🎉\n\n" +
            "🏆 Prêmios:\n" +
            "🥇 1º: ${raffle.prize1}\n" +
            "🥈 2º: ${raffle.prize2}\n" +
            "🥉 3º: ${raffle.prize3}\n\n" +
            "🎟️ Valor: R$ ${String.format(Locale.US, "%.2f", raffle.ticketPrice)} cada número!\n\n" +
            "Participe pelo link: https://ais-pre-dwdyhlg5k4d7rlkrazobcg-102752084731.us-east5.run.app/?id=${raffle.id}"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "COMPARTILHAR LINK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Envie para amigos e compradores",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Raffle Link", "https://ais-pre-dwdyhlg5k4d7rlkrazobcg-102752084731.us-east5.run.app/?id=${raffle.id}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Link",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar Rifa"))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enviar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Expandable referral discount generator section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isReferralExpanded = !isReferralExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "👥 Indique Amigos e dê 10% de Desconto!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Icon(
                        imageVector = if (isReferralExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isReferralExpanded) "Minimizar" else "Expandir",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isReferralExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gere seu link de indicação personalizado para compartilhar. Seus indicados ganham 10% de desconto na compra dos números, e as reservas ficam marcadas com o seu nome para o administrador saber quem os indicou!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = referrerInput,
                                onValueChange = { referrerInput = it },
                                placeholder = { Text("Seu Nome ou WhatsApp") },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF16A34A),
                                    unfocusedBorderColor = Color(0xFF16A34A).copy(alpha = 0.4f)
                                )
                            )

                            Button(
                                onClick = {
                                    if (referrerInput.trim().isEmpty()) {
                                        Toast.makeText(context, "Insira seu Nome ou WhatsApp para gerar seu link!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val referralUrl = "https://ais-pre-dwdyhlg5k4d7rlkrazobcg-102752084731.us-east5.run.app/?id=${raffle.id}&ref=${referrerInput.trim()}"
                                    val referralShareText = "Participe da Rifa: *${raffle.title}*! 🎉\n" +
                                            "Comprando pelo meu link de indicação, você ganha *10% de desconto* de presente! 🎟️🎁\n\n" +
                                            "🏆 Prêmios:\n" +
                                            "🥇 1º: ${raffle.prize1}\n" +
                                            "🥈 2º: ${raffle.prize2}\n" +
                                            "🥉 3º: ${raffle.prize3}\n\n" +
                                            "Garanta seus números com desconto por aqui: $referralUrl"

                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Raffle Referral Link", referralUrl)
                                    clipboard.setPrimaryClip(clip)

                                    // Open Share Sheet
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, referralShareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartilhar com Desconto de Indicação"))
                                    Toast.makeText(context, "Link de indicação copiado e pronto para compartilhar!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gerar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Expandable digital marketing tips section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMarketingExpanded = !isMarketingExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "💡 Dicas de Mercado Digital (Venda Mais!)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                    Icon(
                        imageVector = if (isMarketingExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isMarketingExpanded) "Minimizar" else "Expandir",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isMarketingExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acelere a venda de suas dezenas utilizando estratégias profissionais de marketing digital:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )

                        // Tip 1
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "1. 📢 Parcerias e Afiliados (Recurso Ativo!)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use o recurso de indicação acima. Peça para influenciadores locais e amigos compartilharem seu link de indicação personalizado. Seus compradores ganham 10% de desconto e você vende muito mais rápido!",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }

                        // Tip 2
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "2. ⏳ Gatilho Mental da Escassez",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Poste em redes sociais mostrando o progresso de vendas: 'X% das dezenas já foram reservadas! Garanta seu número antes que acabe.'",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }

                        // Tip 3
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "3. 🎁 Combos Promocionais (Recurso Ativo!)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "O sistema aplica descontos progressivos automáticos: 5% para 3+ números, 10% para 5+ números e 15% para 10+ números. Divulgue isso! 'Compre em quantidade e ganhe descontos automáticos de até 15%!'",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedQRCode(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val qrColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White

    Canvas(modifier = modifier) {
        val sizePx = size.width
        val cellSize = sizePx / 15f

        drawRect(color = bgColor)

        val randomMatrix = listOf(
            listOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1),
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0),
            listOf(1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1),
            listOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1)
        )

        for (row in 0 until 15) {
            val rowPattern = randomMatrix.getOrElse(row) { List(15) { 0 } }
            for (col in 0 until 15) {
                val cellVal = rowPattern.getOrElse(col) { 0 }
                if (cellVal == 1) {
                    drawRect(
                        color = qrColor,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
fun UnavailableTicketDetailsDialog(
    ticket: Ticket,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (ticket.status == "PAGO") Color(0xFF10B981) else Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Número ${ticket.number}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Este número já foi adquirido por outro participante. Confira os detalhes abaixo:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = if (ticket.status == "PAGO") "PAGO" else "RESERVADO",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (ticket.status == "PAGO") Color(0xFF10B981) else Color(0xFF3B82F6)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Comprador:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = ticket.buyerName ?: "N/A",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Contato:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = ticket.buyerPhone ?: "N/A",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data/Hora:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = formatDateTime(ticket.updatedAt),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun PixPaymentDialog(
    raffle: Raffle,
    selectedNumbers: Set<String>,
    buyerName: String,
    buyerPhone: String,
    originalTotal: Double,
    discountAmount: Double,
    finalTotal: Double,
    onDismiss: () -> Unit,
    onGoToGateway: () -> Unit
) {
    val context = LocalContext.current
    val totalVal = finalTotal
    val pixKey = "00020126360014BR.GOV.BCB.PIX0114+55119999988885204000053039865405" + String.format(Locale.US, "%.2f", totalVal) + "5802BR5915RIFA_DIGITAL_6009SAO_PAULO62070503***6304"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Pagar via Pix",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Escaneie o QR Code ou copie a chave Pix abaixo para prosseguir com o pagamento:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(150.dp)
                        .padding(4.dp)
                ) {
                    SimulatedQRCode(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                if (discountAmount > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Valor Original: R$ ${String.format(Locale.US, "%.2f", originalTotal)}",
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "VALOR COM DESCONTO: R$ ${String.format(Locale.US, "%.2f", finalTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "(10% de desconto por indicação)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                } else {
                    Text(
                        text = "VALOR TOTAL: R$ ${String.format(Locale.US, "%.2f", totalVal)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Números: ${selectedNumbers.sorted().joinToString(", ")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chave Pix Copia e Cola",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Pix Key", pixKey)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Chave copiada!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Pix",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Para liberação imediata automática de seus números, clique no botão de gateway abaixo:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToGateway,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ir para Gateway do Banco", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun BankGatewayDialog(
    raffle: Raffle,
    selectedNumbers: Set<String>,
    buyerName: String,
    buyerPhone: String,
    originalTotal: Double,
    discountAmount: Double,
    finalTotal: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var stepMessage by remember { mutableStateOf("Aguardando confirmação...") }
    var success by remember { mutableStateOf(false) }

    val totalVal = finalTotal

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            stepMessage = "1. Conectando-se ao Gateway de Pagamentos do Banco..."
            kotlinx.coroutines.delay(1000)
            stepMessage = "2. Validando saldo e autenticando via Pix..."
            kotlinx.coroutines.delay(1000)
            stepMessage = "3. Confirmando recebimento e atualizando números..."
            kotlinx.coroutines.delay(1000)
            isProcessing = false
            success = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Gateway do Banco Nacional",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF0F766E)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!success) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFCCFBF1), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ambiente de Pagamento 100% Seguro",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DETALHES DA TRANSAÇÃO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Favorecido:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Rifa Digital Administrador", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Comprador:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(buyerName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Números:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(selectedNumbers.sorted().joinToString(", "), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            if (discountAmount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Valor Original:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "R$ ${String.format(Locale.US, "%.2f", originalTotal)}",
                                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Desconto (10%):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A34A))
                                    Text(
                                        "- R$ ${String.format(Locale.US, "%.2f", discountAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Valor Final:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(
                                    "R$ ${String.format(Locale.US, "%.2f", finalTotal)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (discountAmount > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (isProcessing) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF0F766E),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = stepMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Clique abaixo para autorizar e simular a liberação do seu Pix. O banco processará a transação em tempo real.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFD1FAE5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Sucesso",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "PAGAMENTO CONFIRMADO! 🎉",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )

                        Text(
                            text = "Parabéns, ${buyerName}! Seu pagamento de R$ ${String.format("%.2f", totalVal)} foi aprovado pelo gateway do banco.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Seus números [${selectedNumbers.sorted().joinToString(", ")}] foram marcados de VERDE (PAGO) e estão totalmente garantidos em seu nome no sorteio.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (success) {
                Button(
                    onClick = {
                        onConfirmPayment()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Concluir e Ver Rifa", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { isProcessing = true },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F766E)
                    )
                ) {
                    Text("Autorizar e Pagar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!success && !isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Voltar")
                }
            }
        }
    )
}


