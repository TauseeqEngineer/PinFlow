package com.example.ui.favorites

import com.example.data.DownloadedVideo

data class FavoritesUiState(
    val favoriteVideos: List<DownloadedVideo> = emptyList()
)
