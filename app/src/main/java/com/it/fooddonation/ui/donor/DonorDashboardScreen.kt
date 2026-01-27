package com.it.fooddonation.ui.donor

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.it.fooddonation.core.icons.logo
import com.it.fooddonation.data.model.Donation
import com.it.fooddonation.data.model.DonationStatus
import com.it.fooddonation.ui.auth.viewmodel.AuthViewModel
import com.it.fooddonation.ui.components.NotificationBell
import com.it.fooddonation.ui.donor.viewmodel.DonorViewModel
import com.it.fooddonation.ui.theme.Gray500
import com.it.fooddonation.ui.theme.Primary
import kotlinx.coroutines.launch

/**
 * Main dashboard screen for donors
 * Displays a list of donations with action buttons based on status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDashboardScreen(
    authViewModel: AuthViewModel,
    viewModel: DonorViewModel = viewModel(),
    onDonateFoodClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onChatClick: (receiverId: String, receiverName: String) -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load donations when screen is first composed
    LaunchedEffect(Unit) {
        viewModel.loadDonations()
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onDonateFoodClick,
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Donate Food",
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues)
        ) {
            // Address setup banner (if no address)
            if (authState.userProfile?.address.isNullOrBlank()) {
                AddressSetupBanner(onSetupClick = onProfileClick)
            }

            // Stats cards
            StatsSection(donations = dashboardState.donations)

            // Content
            when {
                dashboardState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                dashboardState.donations.isEmpty() -> {
                    // Empty state
                    EmptyDonationsState(onDonateFoodClick = onDonateFoodClick)
                }

                else -> {
                    // Section header
                    Text(
                        text = "Active Donations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    // Donations list
                    DonationsList(
                        donations = dashboardState.donations,
                        onCancelClick = { donationId ->
                            viewModel.cancelDonation(donationId)
                        },
                        onContactReceiverClick = { donation ->
                            donation.receiverId?.let { receiverId ->
                                donation.receiverName?.let { receiverName ->
                                    onChatClick(receiverId, receiverName)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Banner prompting user to add address
 */
@Composable
private fun AddressSetupBanner(onSetupClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onSetupClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4ED)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // TODO: Use warning/info icon
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Add Pickup Address",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Required to create donations",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Icon(
                imageVector = Icons.Default.Add, // TODO: Use arrow forward icon
                contentDescription = "Setup",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Stats section showing donation statistics
 */
@Composable
private fun StatsSection(donations: List<Donation>) {
    // Calculate stats
    val totalDonated = donations.size

    // Lives impacted: use real peopleServed data when available, fallback to estimate
    val livesImpacted = donations.sumOf { donation ->
        donation.peopleServed ?: 0 // Only count actual reported impact
    }

    val activeListings = donations.count { it.status == DonationStatus.AVAILABLE }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatCard(
                icon = Icons.Default.Favorite,
                iconBackgroundColor = Color(0xFFFED7AA),
                iconTint = Primary,
                value = totalDonated.toString().padStart(2, '0'),
                label = "Total Donated"
            )
        }
        item {
            StatCard(
                icon = Icons.Default.Group,
                iconBackgroundColor = Color(0xFFDBEAFE),
                iconTint = Color(0xFF3B82F6),
                value = livesImpacted.toString(),
                label = "Lives Impacted"
            )
        }
        item {
            StatCard(
                icon = Icons.Default.Inventory2,
                iconBackgroundColor = Color(0xFFD1FAE5),
                iconTint = Color(0xFF10B981),
                value = activeListings.toString().padStart(2, '0'),
                label = "Active Listings"
            )
        }
    }
}

/**
 * Individual stat card
 */
@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier.width(128.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Label
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                letterSpacing = 1.sp,
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * Empty state displayed when donor has no donations
 */
@Composable
private fun EmptyDonationsState(onDonateFoodClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You haven't donated food yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Gray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDonateFoodClick,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Donate Food",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * List of donations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationsList(
    donations: List<Donation>,
    onCancelClick: (String) -> Unit,
    onContactReceiverClick: (Donation) -> Unit
) {
    var selectedDonation by remember { mutableStateOf<Donation?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(donations) { donation ->
            DonationCard(
                donation = donation,
                onClick = { selectedDonation = donation }
            )
        }
    }

    // Bottom sheet for donation details
    selectedDonation?.let { donation ->
        DonationDetailsBottomSheet(
            donation = donation,
            sheetState = sheetState,
            onDismiss = { selectedDonation = null },
            onCancelClick = {
                onCancelClick(donation.id)
                scope.launch {
                    sheetState.hide()
                    selectedDonation = null
                }
            },
            onContactReceiverClick = {
                onContactReceiverClick(donation)
            }
        )
    }
}

/**
 * Compact donation card - shows summary, click to see details
 */
@Composable
private fun DonationCard(
    donation: Donation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image thumbnail
            donation.foodItems.firstOrNull()?.imageUri?.let { uriString ->
                Image(
                    painter = rememberAsyncImagePainter(uriString.toUri()),
                    contentDescription = "Food image",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Primary.copy(alpha = 0.4f)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${donation.foodItems.size} Item${if (donation.foodItems.size > 1) "s" else ""}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1A1A),
                        letterSpacing = (-0.5).sp
                    )
                    DonationStatusBadge(status = donation.status)
                }

                // Show all food items with bullet points
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    donation.foodItems.forEach { foodItem ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .padding(top = 6.dp)
                                    .background(
                                        color = Primary.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = foodItem.foodName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151),
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = "(${foodItem.quantity})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF9CA3AF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(0.4f)
                            )
                        }
                    }
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
    onCancelClick: () -> Unit,
    onContactReceiverClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
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

            // Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DonationStatusBadge(status = donation.status)
            }

            HorizontalDivider(modifier = Modifier.padding(24.dp))

            // Pickup Location Section
            authState.userProfile?.let { profile ->
                if (!profile.address.isNullOrBlank()) {
                    Text(
                        text = "Pickup Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, // TODO: Use location pin icon
                            contentDescription = "Location",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = profile.address,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!profile.city.isNullOrBlank()) {
                                Text(
                                    text = profile.city,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(24.dp))
                }
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
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
            when (donation.status) {
                DonationStatus.AVAILABLE -> {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            "Cancel Donation",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                DonationStatus.ACCEPTED, DonationStatus.PICKED -> {
                    Button(
                        onClick = onContactReceiverClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Chat with Receiver",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                DonationStatus.COMPLETED -> {
                    // No action button for completed donations
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = style.text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = style.textColor,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Extract initials from full name
 * Takes up to 2 words and gets first letter of each
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
