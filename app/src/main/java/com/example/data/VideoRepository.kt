package com.example.data

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {
    val allVideos: Flow<List<DownloadedVideo>> = videoDao.getAllVideos()
    val favoriteVideos: Flow<List<DownloadedVideo>> = videoDao.getFavoriteVideos()

    fun getVideoById(id: Int): Flow<DownloadedVideo?> = videoDao.getVideoById(id)

    suspend fun getVideoByPinterestUrl(url: String): DownloadedVideo? = videoDao.getVideoByPinterestUrl(url)

    suspend fun insertVideo(video: DownloadedVideo): Long = videoDao.insertVideo(video)

    suspend fun updateVideo(video: DownloadedVideo) = videoDao.updateVideo(video)

    suspend fun deleteVideo(video: DownloadedVideo) = videoDao.deleteVideo(video)

    suspend fun deleteVideoById(id: Int) = videoDao.deleteVideoById(id)
}
