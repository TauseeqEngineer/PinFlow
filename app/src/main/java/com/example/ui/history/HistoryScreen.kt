package com.example.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
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
fun HistoryScreen(
    viewModel: PinDownViewModel,
    onBack: () -> Unit
) {
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(
            title = "Download History",
            onBackClick = onBack,
            actions = {
                if (allVideos.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearCache() }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear history",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        )

        if (allVideos.isEmpty()) {
            EmptyState(
                title = "No History Records",
                description = "Your past downloads will be recorded here for easy access.",
                icon = Icons.Rounded.History,
                actionText = "Extract Videos",
                onAction = {
                    onBack()
                    viewModel.setTab("home")
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allVideos, key = { it.id }) { video ->
                    HistoryCard(
                        video = video,
                        onPlay = { viewModel.selectVideoToPlay(video) },
                        onDelete = { viewModel.deleteVideo(video) }
                    )
                }
            }
        }
    }
}
