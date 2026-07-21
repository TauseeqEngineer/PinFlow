package com.example.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PinDownViewModel
import com.example.ui.components.AppToolbar
import com.example.ui.components.EmptyState

@Composable
fun FavoritesScreen(
    viewModel: PinDownViewModel,
    onBack: (() -> Unit)? = null
) {
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(
            title = "Favorites (${favoriteVideos.size})",
            onBackClick = onBack
        )

        if (favoriteVideos.isEmpty()) {
            EmptyState(
                title = "No Favorites Saved",
                description = "Tap the heart icon on any downloaded video to add it to your personal favorite collection.",
                icon = Icons.Rounded.FavoriteBorder,
                actionText = "Explore Downloads",
                onAction = { viewModel.setTab("downloads") },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoriteVideos, key = { it.id }) { video ->
                    FavoriteVideoCard(
                        video = video,
                        onPlay = { viewModel.selectVideoToPlay(video) },
                        onRemoveFavorite = { viewModel.toggleFavorite(video) }
                    )
                }
            }
        }
    }
}
