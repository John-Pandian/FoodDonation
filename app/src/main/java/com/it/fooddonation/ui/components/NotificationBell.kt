package com.it.fooddonation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it.fooddonation.ui.theme.Primary

/**
 * Notification bell icon with badge showing unread count
 *
 * @param unreadCount Number of unread messages/notifications
 * @param onClick Callback when the bell is clicked
 */
@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = Color(0xFFEF4444), // Red badge
                        contentColor = Color.White,
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (unreadCount > 0) Primary else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Simple notification dot indicator (alternative minimal design)
 */
@Composable
fun NotificationDot(
    hasUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (hasUnread) Primary else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
        }

        // Red dot indicator
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = 6.dp, y = 6.dp)
                    .background(
                        color = Color(0xFFEF4444),
                        shape = CircleShape
                    )
                    .align(Alignment.TopEnd)
            )
        }
    }
}
