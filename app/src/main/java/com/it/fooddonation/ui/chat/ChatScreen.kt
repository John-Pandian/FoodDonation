package com.it.fooddonation.ui.chat

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.it.fooddonation.data.model.Message
import com.it.fooddonation.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat screen for messaging between donors and receivers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    otherUserName: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

    // Load messages when screen opens
    LaunchedEffect(otherUserId) {
        viewModel.loadMessages(otherUserId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    // Show error messages
    LaunchedEffect(chatState.errorMessage) {
        chatState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Messages",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
        ) {
            // Messages list
            if (chatState.isLoading || !chatState.hasLoadedOnce) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (chatState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet\nStart the conversation!",
                        fontSize = 16.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatState.messages) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == chatState.currentUserId,
                            onRetryClick = { failedMessage ->
                                viewModel.retryMessage(failedMessage)
                            }
                        )
                    }
                }
            }

            // Message input
            MessageInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    viewModel.sendMessage(
                        receiverId = otherUserId,
                        messageText = messageText,
                        onSuccess = {
                            messageText = ""
                        }
                    )
                },
                isSending = chatState.isSending
            )
        }
    }
}

/**
 * Message bubble component
 */
@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onRetryClick: ((Message) -> Unit)? = null
) {
    // Update timestamp every minute
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Update every minute
            currentTime = System.currentTimeMillis()
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.75f),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Sender name (only for other user's messages)
            if (!isCurrentUser) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        color = if (isCurrentUser) {
                            when (message.status) {
                                com.it.fooddonation.data.model.MessageStatus.FAILED -> Color(0xFFEF4444)
                                else -> Primary
                            }
                        } else {
                            Color.White
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .clickable(enabled = message.status == com.it.fooddonation.data.model.MessageStatus.FAILED) {
                        onRetryClick?.invoke(message)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.message,
                    fontSize = 15.sp,
                    color = if (isCurrentUser) Color.White else Color(0xFF1A1A1A),
                    lineHeight = 20.sp
                )
            }

            // Timestamp and status
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.timestamp, currentTime),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )

                // Show status for current user's messages
                if (isCurrentUser) {
                    when (message.status) {
                        com.it.fooddonation.data.model.MessageStatus.SENDING -> {
                            // Clock icon while sending
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Sending",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                        com.it.fooddonation.data.model.MessageStatus.FAILED -> {
                            Text(
                                text = "• Tap to retry",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        com.it.fooddonation.data.model.MessageStatus.SENT -> {
                            // Single tick - gray if not read, blue if read
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (message.isRead) "Read" else "Sent",
                                modifier = Modifier.size(14.dp),
                                tint = if (message.isRead) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Message input component
 */
@Composable
private fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            maxLines = 4
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (messageText.isNotBlank() && !isSending) Primary else Color(0xFFE5E7EB),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onSendClick,
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp for display with real-time updates
 */
private fun formatTimestamp(timestamp: Long, currentTime: Long = System.currentTimeMillis()): String {
    val date = Date(timestamp)
    val diff = currentTime - timestamp

    return when {
        diff < 60000 -> "Just now" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date) // Less than 1 day
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date) // Older
    }
}
