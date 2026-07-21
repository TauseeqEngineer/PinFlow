package com.example.ui.downloads

import com.example.data.DownloadedVideo

data class DownloadsUiState(
    val downloadedVideos: List<DownloadedVideo> = emptyList(),
    val activeDownload: DownloadedVideo? = null,
    val selectedFilter: String = "All"
)
