package com.example.ui.settings

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val autoPasteEnabled: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val downloadQuality: String = "1080p Full HD",
    val appVersion: String = "v2.4.0 Pro"
)
