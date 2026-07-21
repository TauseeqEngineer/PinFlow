package com.example.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.VideoLibrary
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
fun DownloadsScreen(viewModel: PinDownViewModel) {
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(
            title = "Downloads (${allVideos.size})",
            actions = {
                if (allVideos.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearCache() }) {
                        Icon(
                            imageVector = Icons.Rounded.FolderDelete,
                            contentDescription = "Clear all downloads",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        )

        if (allVideos.isEmpty()) {
            EmptyState(
                title = "No Downloads Yet",
                description = "Extract videos from Pinterest on the Home screen to save them here for offline viewing.",
                icon = Icons.Rounded.VideoLibrary,
                actionText = "Go to Home",
                onAction = { viewModel.setTab("home") },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allVideos, key = { it.id }) { video ->
                    DownloadedVideoCard(
                        video = video,
                        onPlay = { viewModel.selectVideoToPlay(video) },
                        onToggleFavorite = { viewModel.toggleFavorite(video) },
                        onDelete = { viewModel.deleteVideo(video) }
                    )
                }
            }
        }
    }
}
