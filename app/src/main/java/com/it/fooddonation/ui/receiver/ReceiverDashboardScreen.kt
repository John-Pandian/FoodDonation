package com.it.fooddonation.ui.receiver

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.it.fooddonation.core.icons.logo
import com.it.fooddonation.data.model.Donation
import com.it.fooddonation.data.model.DonationStatus
import com.it.fooddonation.ui.auth.viewmodel.AuthViewModel
import com.it.fooddonation.ui.components.NotificationBell
import com.it.fooddonation.ui.receiver.viewmodel.ReceiverViewModel
import com.it.fooddonation.ui.theme.Primary
import kotlinx.coroutines.launch

/**
 * Main dashboard screen for receivers
 * Shows available donations to claim and accepted donations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverDashboardScreen(
    authViewModel: AuthViewModel,
    viewModel: ReceiverViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onChatClick: (donorId: String, donorName: String) -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Load donations when screen is first composed
    LaunchedEffect(Unit) {
        viewModel.loadAvailableDonations()
        viewModel.loadMyDonations()
        // Check email verification status
        authViewModel.checkEmailVerification()
    }

    // Show error messages in snackbar
    LaunchedEffect(dashboardState.errorMessage) {
        dashboardState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show success messages in snackbar
    LaunchedEffect(dashboardState.successMessage) {
        dashboardState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccess()
        }
    }

    // Email Verification Dialog
    if (authState.showEmailVerificationPrompt) {
        EmailVerificationDialog(
            onDismiss = { authViewModel.dismissEmailVerificationPrompt() },
            onSignOut = { authViewModel.signOut() }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = logo,
                            contentDescription = "App Logo",
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Food Donation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Notification bell
                    NotificationBell(
                        unreadCount = dashboardState.unreadMessageCount,
                        onClick = onMessagesClick,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Profile avatar with initials
                    val fullName = authState.userProfile?.fullName ?: "User"
                    val initials = getInitials(fullName)

                    Box(
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues)
        ) {
            // Tab navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Available", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("My Donations", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Content based on selected tab with pull-to-refresh
            PullToRefreshBox(
                isRefreshing = dashboardState.isRefreshing,
                onRefresh = { viewModel.refreshDonations() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTabIndex) {
                    0 -> AvailableDonationsTab(
                        donations = dashboardState.availableDonations,
                        isLoading = dashboardState.isLoading,
                        onAcceptDonation = { donationId ->
                            viewModel.acceptDonation(donationId)
                        }
                    )
                    1 -> MyDonationsTab(
                        donations = dashboardState.myDonations,
                        onMarkAsPickedUp = { donationId, peopleServed ->
                            viewModel.markAsPickedUp(donationId, peopleServed)
                        },
                        onChatClick = onChatClick
                    )
                }
            }
        }
    }
}

/**
 * Tab showing available donations for receivers to browse
 */
@Composable
private fun AvailableDonationsTab(
    donations: List<Donation>,
    isLoading: Boolean,
    onAcceptDonation: (String) -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        donations.isEmpty() -> {
            EmptyAvailableState()
        }
        else -> {
            DonationsList(
                donations = donations,
                onAcceptDonation = onAcceptDonation,
                isMyDonations = false
            )
        }
    }
}

/**
 * Tab showing receiver's accepted donations
 */
@Composable
private fun MyDonationsTab(
    donations: List<Donation>,
    onMarkAsPickedUp: (String, Int) -> Unit,
    onChatClick: (donorId: String, donorName: String) -> Unit
) {
    if (donations.isEmpty()) {
        EmptyMyDonationsState()
    } else {
        DonationsList(
            donations = donations,
            onMarkAsPickedUp = onMarkAsPickedUp,
            isMyDonations = true,
            onChatClick = onChatClick
        )
    }
}

/**
 * Empty state for available donations
 */
@Composable
private fun EmptyAvailableState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No donations available",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back later for new donations",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state for my donations
 */
@Composable
private fun EmptyMyDonationsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No accepted donations yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Browse available donations to get started",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * List of donations (reusable for both tabs)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationsList(
    donations: List<Donation>,
    onAcceptDonation: ((String) -> Unit)? = null,
    onMarkAsPickedUp: ((String, Int) -> Unit)? = null,
    isMyDonations: Boolean,
    onChatClick: ((donorId: String, donorName: String) -> Unit)? = null
) {
    var selectedDonation by remember { mutableStateOf<Donation?>(null) }
    var showPeopleCountDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(donations) { donation ->
            DonationCard(
                donation = donation,
                onClick = { selectedDonation = donation },
                isMyDonations = isMyDonations,
                onChatClick = if (isMyDonations) {
                    { onChatClick?.invoke(donation.donorId, donation.donorName) }
                } else null
            )
        }
    }

    // Bottom sheet for donation details
    selectedDonation?.let { donation ->
        DonationDetailsBottomSheet(
            donation = donation,
            sheetState = sheetState,
            onDismiss = { selectedDonation = null },
            onAcceptClick = if (!isMyDonations) {
                {
                    onAcceptDonation?.invoke(donation.id)
                    scope.launch {
                        sheetState.hide()
                        selectedDonation = null
                    }
                }
            } else null,
            onMarkAsPickedUpClick = if (isMyDonations && donation.status == DonationStatus.ACCEPTED) {
                {
                    scope.launch {
                        sheetState.hide()
                    }
                    showPeopleCountDialog = true
                }
            } else null,
            onChatClick = if (isMyDonations) {
                {
                    scope.launch {
                        sheetState.hide()
                    }
                    onChatClick?.invoke(donation.donorId, donation.donorName)
                }
            } else null
        )
    }

    // People count dialog
    if (showPeopleCountDialog && selectedDonation != null) {
        PeopleCountDialog(
            onDismiss = {
                showPeopleCountDialog = false
                selectedDonation = null
            },
            onConfirm = { peopleCount ->
                selectedDonation?.let { donation ->
                    onMarkAsPickedUp?.invoke(donation.id, peopleCount)
                }
                showPeopleCountDialog = false
                selectedDonation = null
            }
        )
    }
}

/**
 * Donation card showing summary
 */
@Composable
private fun DonationCard(
    donation: Donation,
    onClick: () -> Unit,
    isMyDonations: Boolean,
    onChatClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image thumbnail
            donation.foodItems.firstOrNull()?.imageUri?.let { uriString ->
                Image(
                    painter = rememberAsyncImagePainter(uriString.toUri()),
                    contentDescription = "Food image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Primary.copy(alpha = 0.4f)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${donation.foodItems.size} Food Item${if (donation.foodItems.size > 1) "s" else ""}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                // Show donor information
                if (donation.donorName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "By:",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = donation.donorName,
                            fontSize = 14.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isMyDonations) {
                    Spacer(modifier = Modifier.height(2.dp))
                    DonationStatusBadge(status = donation.status)
                }
            }

            // Chat icon button (only for accepted donations)
            if (onChatClick != null) {
                IconButton(
                    onClick = { onChatClick() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Chat with donor",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet showing full donation details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationDetailsBottomSheet(
    donation: Donation,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onAcceptClick: (() -> Unit)?,
    onMarkAsPickedUpClick: (() -> Unit)?,
    onChatClick: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Donation Details",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Image
            donation.foodItems.firstOrNull()?.imageUri?.let { uriString ->
                Image(
                    painter = rememberAsyncImagePainter(uriString.toUri()),
                    contentDescription = "Food image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Food Items Section
            Text(
                text = "Food Items (${donation.foodItems.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            donation.foodItems.forEach { foodItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = foodItem.foodName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.6f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = foodItem.quantity,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.4f)
                    )
                }
            }

            // Description
            if (donation.description.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(24.dp))

                Text(
                    text = "Additional Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = donation.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            onAcceptClick?.let { onClick ->
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        "Accept Donation",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            onMarkAsPickedUpClick?.let { onClick ->
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        "Mark as Picked Up",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            onChatClick?.let { onClick ->
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chat with Donor",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Status badge showing the current donation status
 */
@Composable
private fun DonationStatusBadge(status: DonationStatus) {
    data class BadgeStyle(
        val text: String,
        val backgroundColor: Color,
        val textColor: Color
    )

    val style = when (status) {
        DonationStatus.AVAILABLE -> BadgeStyle(
            text = "Available",
            backgroundColor = Color(0xFFECFDF5),
            textColor = Color(0xFF065F46)
        )
        DonationStatus.ACCEPTED -> BadgeStyle(
            text = "Accepted",
            backgroundColor = Color(0xFFEFF6FF),
            textColor = Color(0xFF1E40AF)
        )
        DonationStatus.PICKED -> BadgeStyle(
            text = "Picked Up",
            backgroundColor = Color(0xFFFEF3C7),
            textColor = Color(0xFF92400E)
        )
        DonationStatus.COMPLETED -> BadgeStyle(
            text = "Completed",
            backgroundColor = Color(0xFFF3F4F6),
            textColor = Color(0xFF4B5563)
        )
    }

    Box(
        modifier = Modifier
            .background(
                color = style.backgroundColor,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = style.text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = style.textColor,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Dialog to ask receiver how many people were served
 */
@Composable
private fun PeopleCountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var peopleCount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How many people did this donation serve?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = peopleCount,
                    onValueChange = {
                        peopleCount = it.filter { char -> char.isDigit() }
                        error = null
                    },
                    label = { Text("Number of people") },
                    placeholder = { Text("e.g., 10") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val count = peopleCount.toIntOrNull()
                            when {
                                count == null || count <= 0 -> {
                                    error = "Please enter a valid number"
                                }
                                count > 10000 -> {
                                    error = "Number seems too high"
                                }
                                else -> {
                                    onConfirm(count)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

/**
 * Extract initials from full name
 */
private fun getInitials(fullName: String): String {
    val words = fullName.trim().split("\\s+".toRegex())
    return when {
        words.isEmpty() -> "U"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/**
 * Dialog prompting user to verify their email address
 */
@Composable
private fun EmailVerificationDialog(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Email Verification Required",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Please verify your email address to access all features of the app.",
                    fontSize = 15.sp,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Check your inbox for the confirmation link we sent when you registered.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSignOut
            ) {
                Text("Sign Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
