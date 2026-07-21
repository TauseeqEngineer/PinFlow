package com.example.ui.feedback

data class FeedbackUiState(
    val rating: Int = 5,
    val selectedCategory: String = "General",
    val email: String = "",
    val feedbackText: String = "",
    val categories: List<String> = listOf("General", "Bug Report", "Feature Idea", "Design"),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
)
