package com.it.fooddonation.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.it.fooddonation.data.model.Notification
import com.it.fooddonation.data.repository.MessagingRepository
import com.it.fooddonation.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing all messages/conversations for a user
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    onBackClick: () -> Unit,
    onConversationClick: (otherUserId: String, otherUserName: String) -> Unit,
    viewModel: MessagesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    // Auto-mark all notifications as read in database when screen is viewed
    // UI keeps the red borders visible so user can see which are new
    // When they navigate back and return later, reload will show them as read
    LaunchedEffect(state.notifications) {
        if (state.notifications.isNotEmpty() && state.notifications.any { !it.isRead }) {
            viewModel.markAllNotificationsAsRead()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                state.conversations.isEmpty() && state.notifications.none { !it.isRead } -> {
                    EmptyMessagesState()
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Notifications section (only show if there are unread notifications)
                        val unreadNotifications = state.notifications.filter { !it.isRead }
                        if (unreadNotifications.isNotEmpty()) {
                            NotificationsList(
                                notifications = unreadNotifications,
                                viewModel = viewModel
                            )
                        }

                        // Conversations section
                        if (state.conversations.isNotEmpty()) {
                            ConversationsList(
                                conversations = state.conversations,
                                onConversationClick = onConversationClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no messages
 */
@Composable
private fun EmptyMessagesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFD1D5DB)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start a conversation by accepting a donation",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * List of conversations
 */
@Composable
private fun ConversationsList(
    conversations: List<MessagingRepository.ConversationSummary>,
    onConversationClick: (otherUserId: String, otherUserName: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(conversations) { conversation ->
            ConversationCard(
                conversation = conversation,
                onClick = {
                    onConversationClick(
                        conversation.otherUserId,
                        conversation.otherUserName
                    )
                }
            )
        }
    }
}

/**
 * Individual conversation card
 */
@Composable
private fun ConversationCard(
    conversation: MessagingRepository.ConversationSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(conversation.otherUserName),
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Conversation details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherUserName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTimestamp(conversation.lastMessageTimestamp),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 14.sp,
                        color = if (conversation.unreadCount > 0) {
                            Color(0xFF374151)
                        } else {
                            Color(0xFF9CA3AF)
                        },
                        fontWeight = if (conversation.unreadCount > 0) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread badge
                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
 * List of notifications
 */
@Composable
private fun NotificationsList(
    notifications: List<Notification>,
    viewModel: MessagesViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = "Notifications",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Notifications
        notifications.forEach { notification ->
            NotificationCard(
                notification = notification,
                onClick = {
                    // Notifications are auto-marked as read when screen is viewed
                    // Click action could navigate to related donation if needed
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFF5F5F5))
        )

        // Conversations header
        Text(
            text = "Messages",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * Individual notification card
 */
@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .then(
                if (!notification.isRead) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFEF4444), // Red-500 for unread notifications
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Primary.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatTimestamp(notification.createdAt),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Unread indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = Primary,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to relative time
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
