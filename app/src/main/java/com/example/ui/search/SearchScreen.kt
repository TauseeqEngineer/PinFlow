package com.example.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PinDownViewModel
import com.example.ui.components.AppSearchBar
import com.example.ui.components.AppToolbar
import com.example.ui.components.EmptyState
import com.example.ui.home.CuratedPinItem

@Composable
fun SearchScreen(
    viewModel: PinDownViewModel,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredPins by viewModel.filteredExplorePins.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(
            title = "Search Pins",
            onBackClick = onBack
        )

        PaddingValues(horizontal = 16.dp).let {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Search pin titles or topics..."
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        RecentSearchChips(
            recentSearches = listOf("Fashion", "Crafts", "Aesthetic", "Recipes", "Travel", "Quotes"),
            onSelectQuery = { viewModel.setSearchQuery(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredPins.isEmpty()) {
            EmptyState(
                title = "No Matching Pins Found",
                description = "Try searching with a different keyword or paste a direct Pinterest video link on the Home screen.",
                icon = Icons.Rounded.SearchOff,
                actionText = "Clear Search",
                onAction = { viewModel.setSearchQuery("") },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPins, key = { it.pinterestUrl }) { pin ->
                    CuratedPinItem(
                        pin = pin,
                        onClick = {
                            viewModel.fetchPinterestVideo(pin.pinterestUrl)
                            onBack()
                            viewModel.setTab("home")
                        }
                    )
                }
            }
        }
    }
}
