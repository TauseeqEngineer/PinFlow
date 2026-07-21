package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PinDownViewModel
import com.example.ui.components.AppToolbar
import com.example.ui.components.SettingItem
import com.example.ui.components.SettingSwitchItem

@Composable
fun SettingsScreen(viewModel: PinDownViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val autoPasteEnabled by viewModel.autoPasteEnabled.collectAsStateWithLifecycle()
    var wifiOnly by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "Settings")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General Section
            SettingSectionHeader("General")
            SettingSectionGroup {
                SettingSwitchItem(
                    title = "Auto-Detect Clipboard",
                    subtitle = "Suggest Pinterest links copied to clipboard",
                    icon = Icons.Rounded.ContentPaste,
                    checked = autoPasteEnabled,
                    onCheckedChange = { viewModel.toggleAutoPaste(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingItem(
                    title = "Clear Storage & Cache",
                    subtitle = "Free up memory and reset download history",
                    icon = Icons.Rounded.CleaningServices,
                    onClick = { viewModel.clearCache() }
                )
            }

            // Downloads Section
            SettingSectionHeader("Downloads")
            SettingSectionGroup {
                SettingItem(
                    title = "Default Quality",
                    subtitle = "1080p Full HD (Recommended)",
                    icon = Icons.Rounded.HighQuality,
                    onClick = { /* Pre-configured quality */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingSwitchItem(
                    title = "Download on Wi-Fi Only",
                    subtitle = "Save mobile cellular data",
                    icon = Icons.Rounded.Wifi,
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingItem(
                    title = "Download Location",
                    subtitle = "Internal Storage / Movies / PinDown",
                    icon = Icons.Rounded.FolderSpecial
                )
            }

            // Appearance Section
            SettingSectionHeader("Appearance")
            SettingSectionGroup {
                SettingSwitchItem(
                    title = "Dark Theme",
                    subtitle = "Toggle dark mode interface",
                    icon = Icons.Rounded.DarkMode,
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.toggleTheme() }
                )
            }

            // Support & Feedback Section
            SettingSectionHeader("Support & Feedback")
            SettingSectionGroup {
                SettingItem(
                    title = "Help & FAQ",
                    subtitle = "Frequently asked questions and guides",
                    icon = Icons.Rounded.HelpOutline,
                    onClick = { viewModel.showFaqDialog(true) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingItem(
                    title = "Send Feedback",
                    subtitle = "Report bugs or suggest improvements",
                    icon = Icons.Rounded.RateReview,
                    onClick = { viewModel.showFeedbackDialog(true) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingItem(
                    title = "Rate PinDown App",
                    subtitle = "Support us on the Google Play Store",
                    icon = Icons.Rounded.Star,
                    onClick = { viewModel.showRateDialog(true) }
                )
            }

            // About Section
            SettingSectionHeader("About & Legal")
            SettingSectionGroup {
                SettingItem(
                    title = "About PinDown",
                    subtitle = "App version, licenses, and developer info",
                    icon = Icons.Rounded.Info,
                    onClick = { viewModel.showAboutDialog(true) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
