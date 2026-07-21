package com.example.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.SettingItem

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AboutHeaderLogo()

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PinDown is a modern, high-speed media downloader for Pinterest. Built with Jetpack Compose, Kotlin Coroutines, and Material 3 design.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingItem(
                            title = "Privacy Policy",
                            subtitle = "Read our strict data privacy terms",
                            icon = Icons.Rounded.PrivacyTip,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        SettingItem(
                            title = "Terms of Service",
                            subtitle = "Usage rights and guidelines",
                            icon = Icons.Rounded.Description,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        SettingItem(
                            title = "Open Source Licenses",
                            subtitle = "Kotlin, OkHttp, Coil, Room",
                            icon = Icons.Rounded.Code,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        SettingItem(
                            title = "Contact Support",
                            subtitle = "support@pindown.app",
                            icon = Icons.Rounded.Email,
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
