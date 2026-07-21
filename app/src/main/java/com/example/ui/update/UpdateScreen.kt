package com.example.ui.update

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppToolbar
import com.example.ui.components.PrimaryButton

@Composable
fun UpdateScreen(
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    val uiState = UpdateUiState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "App Update")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Version ${uiState.latestVersion} Available",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A new version of PinDown is ready with ultra-fast video extraction.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            WhatsNewList(features = uiState.whatsNewList)

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Update Now",
                onClick = onUpdate
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onLater) {
                Text(
                    text = "Maybe Later",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
