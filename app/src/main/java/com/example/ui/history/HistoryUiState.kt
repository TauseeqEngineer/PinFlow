package com.example.ui.history

import com.example.data.DownloadedVideo

data class HistoryUiState(
    val historyItems: List<DownloadedVideo> = emptyList()
)
