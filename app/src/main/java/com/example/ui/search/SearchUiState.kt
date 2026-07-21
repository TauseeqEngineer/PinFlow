package com.example.ui.search

import com.example.data.ParsedVideoInfo

data class SearchUiState(
    val query: String = "",
    val searchResults: List<ParsedVideoInfo> = emptyList(),
    val recentSearches: List<String> = listOf("Aesthetic Crafts", "Outfit Inspo", "Recipe Videos", "Home Decor", "Wallpapers")
)
