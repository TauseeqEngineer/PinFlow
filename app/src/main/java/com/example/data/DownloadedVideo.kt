package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pinterestUrl: String,
    val videoUrl: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val downloadProgress: Int = 0,
    val status: String = "Downloading", // "Downloading", "Completed", "Failed", "Paused"
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
