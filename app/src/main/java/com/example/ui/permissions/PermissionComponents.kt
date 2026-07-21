package com.example.ui.permissions

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.ui.components.PermissionCard
import com.example.ui.theme.PinterestRed

@Composable
fun PermissionsAnimatedHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldScale"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .scale(scale)
            .background(PinterestRed.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(PinterestRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = PinterestRed,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun PermissionsList(
    isNotificationGranted: Boolean,
    onRequestNotification: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PermissionCard(
            title = "Download Notifications",
            description = "Notify you in real-time when your video download progress reaches 100% in the background.",
            icon = Icons.Rounded.NotificationsActive,
            isGranted = isNotificationGranted,
            onRequest = onRequestNotification
        )

        PermissionCard(
            title = "Gallery Storage Access",
            description = "Save Pinterest videos safely in your device's Movies directory so you can watch them offline.",
            icon = Icons.Rounded.Storage,
            isGranted = true,
            onRequest = {}
        )
    }
}
