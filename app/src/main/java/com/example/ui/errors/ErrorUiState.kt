package com.example.ui.errors

enum class ErrorType {
    NO_INTERNET,
    INVALID_URL,
    DOWNLOAD_FAILED
}

data class ErrorUiState(
    val errorType: ErrorType = ErrorType.NO_INTERNET,
    val message: String = ""
)
