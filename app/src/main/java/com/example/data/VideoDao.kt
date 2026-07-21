package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM downloaded_videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<DownloadedVideo>>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id LIMIT 1")
    fun getVideoById(id: Int): Flow<DownloadedVideo?>

    @Query("SELECT * FROM downloaded_videos WHERE pinterestUrl = :url LIMIT 1")
    suspend fun getVideoByPinterestUrl(url: String): DownloadedVideo?

    @Query("SELECT * FROM downloaded_videos WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteVideos(): Flow<List<DownloadedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideo): Long

    @Update
    suspend fun updateVideo(video: DownloadedVideo)

    @Delete
    suspend fun deleteVideo(video: DownloadedVideo)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Int)
}
