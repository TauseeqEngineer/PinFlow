package com.example.ui.onboarding

import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badge: String
)

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3
)
