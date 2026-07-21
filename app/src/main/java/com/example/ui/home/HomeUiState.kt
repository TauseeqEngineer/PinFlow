package com.example.ui.home

import com.example.data.ParsedVideoInfo

data class HomeUiState(
    val inputUrl: String = "",
    val isFetching: Boolean = false,
    val fetchedVideo: ParsedVideoInfo? = null,
    val clipboardText: String = "",
    val showDownloadOptions: ParsedVideoInfo? = null
)
