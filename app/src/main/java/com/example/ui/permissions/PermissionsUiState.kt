package com.example.ui.permissions

data class PermissionsUiState(
    val isNotificationGranted: Boolean = false,
    val isStorageGranted: Boolean = true
)
