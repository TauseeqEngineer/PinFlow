package com.example.ui

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class PinDownViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = VideoRepository(db.videoDao())

    // App Streams from DB
    val allVideos: StateFlow<List<DownloadedVideo>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteVideos: StateFlow<List<DownloadedVideo>> = repository.favoriteVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bottom Navigation Sub-Screens inside Main: "home", "downloads", "explore", "favorites", "settings"
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Core Screen Navigation: "splash", "onboarding", "permissions", "main"
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Clipboard Paste Autodetect
    private val _clipboardText = MutableStateFlow("")
    val clipboardText: StateFlow<String> = _clipboardText.asStateFlow()

    // Active Search/Explore items
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredExplorePins: StateFlow<List<ParsedVideoInfo>> = _searchQuery
        .map { query ->
            if (query.isBlank()) {
                PinterestParser.PRE_SEEDED_PINS
            } else {
                PinterestParser.PRE_SEEDED_PINS.filter {
                    it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PinterestParser.PRE_SEEDED_PINS)

    // Parsing state
    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _fetchedVideo = MutableStateFlow<ParsedVideoInfo?>(null)
    val fetchedVideo: StateFlow<ParsedVideoInfo?> = _fetchedVideo.asStateFlow()

    // Overlays / Dialogs States
    private val _activePlayingVideo = MutableStateFlow<DownloadedVideo?>(null)
    val activePlayingVideo: StateFlow<DownloadedVideo?> = _activePlayingVideo.asStateFlow()

    private val _showDownloadOptions = MutableStateFlow<ParsedVideoInfo?>(null)
    val showDownloadOptions: StateFlow<ParsedVideoInfo?> = _showDownloadOptions.asStateFlow()

    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

    private val _showRateDialog = MutableStateFlow(false)
    val showRateDialog: StateFlow<Boolean> = _showRateDialog.asStateFlow()

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    private val _showFaqDialog = MutableStateFlow(false)
    val showFaqDialog: StateFlow<Boolean> = _showFaqDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    private val _showUpdateAvailable = MutableStateFlow(true) // Show update banner by default to display state!
    val showUpdateAvailable: StateFlow<Boolean> = _showUpdateAvailable.asStateFlow()

    // Toast/Alert Notifications
    private val _errorNotification = MutableStateFlow<String?>(null)
    val errorNotification: StateFlow<String?> = _errorNotification.asStateFlow()

    private val _successNotification = MutableStateFlow<String?>(null)
    val successNotification: StateFlow<String?> = _successNotification.asStateFlow()

    // Settings
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _autoPasteEnabled = MutableStateFlow(true)
    val autoPasteEnabled: StateFlow<Boolean> = _autoPasteEnabled.asStateFlow()

    // Active downloading track (for progress overlay / indicator)
    private val _activeDownload = MutableStateFlow<DownloadedVideo?>(null)
    val activeDownload: StateFlow<DownloadedVideo?> = _activeDownload.asStateFlow()

    init {
        // Run Splash timeout to onboarding
        viewModelScope.launch {
            delay(2200)
            // Check if onboarding was completed (simulated)
            _currentScreen.value = "onboarding"
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleAutoPaste(enabled: Boolean) {
        _autoPasteEnabled.value = enabled
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorNotification.value = null
    }

    fun clearSuccess() {
        _successNotification.value = null
    }

    fun dismissUpdateBanner() {
        _showUpdateAvailable.value = false
    }

    fun showExitDialog(show: Boolean) {
        _showExitDialog.value = show
    }

    fun showRateDialog(show: Boolean) {
        _showRateDialog.value = show
    }

    fun showFeedbackDialog(show: Boolean) {
        _showFeedbackDialog.value = show
    }

    fun showFaqDialog(show: Boolean) {
        _showFaqDialog.value = show
    }

    fun showAboutDialog(show: Boolean) {
        _showAboutDialog.value = show
    }

    fun selectVideoToPlay(video: DownloadedVideo?) {
        _activePlayingVideo.value = video
    }

    // Check internet availability
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // Detect clipboard and suggest paste
    fun checkClipboard() {
        if (!_autoPasteEnabled.value) return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    if ((text.contains("pinterest.com") || text.contains("pin.it")) && text != _clipboardText.value) {
                        _clipboardText.value = text
                    }
                }
            } catch (e: Exception) {
                Log.e("PinDownVM", "Clipboard error", e)
            }
        }
    }

    fun clearClipboardSuggestion() {
        _clipboardText.value = ""
    }

    // Scrape Pinterest Link
    fun fetchPinterestVideo(url: String) {
        if (!isNetworkAvailable()) {
            _errorNotification.value = "No Internet Connection. Please check your network and try again."
            return
        }

        _isFetching.value = true
        _fetchedVideo.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mimic dynamic loading delay for visual micro-interactions
                delay(1500)
                val result = PinterestParser.parseUrl(url)
                _fetchedVideo.value = result
            } catch (e: IllegalArgumentException) {
                _errorNotification.value = "Invalid URL: ${e.message ?: "Please input a valid Pinterest link."}"
            } catch (e: Exception) {
                _errorNotification.value = "Failed to fetch video. Pinterest might be blocking scrapers. Try one of our demo pins below!"
            } finally {
                _isFetching.value = false
            }
        }
    }

    fun resetFetchedVideo() {
        _fetchedVideo.value = null
    }

    fun triggerDownloadOptions(video: ParsedVideoInfo) {
        _showDownloadOptions.value = video
    }

    fun dismissDownloadOptions() {
        _showDownloadOptions.value = null
    }

    // Download Video Coroutine
    fun startDownload(video: ParsedVideoInfo, quality: String) {
        _showDownloadOptions.value = null
        
        // Check if already downloaded
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getVideoByPinterestUrl(video.pinterestUrl)
            if (existing != null && existing.status == "Completed" && existing.filePath != null) {
                val file = File(existing.filePath)
                if (file.exists()) {
                    _successNotification.value = "This video is already downloaded!"
                    return@launch
                }
            }

            // Create initial database record for the download
            val downloadedVideo = DownloadedVideo(
                pinterestUrl = video.pinterestUrl,
                videoUrl = video.videoUrl,
                title = video.title,
                description = video.description,
                thumbnailUrl = video.thumbnailUrl,
                status = "Downloading",
                downloadProgress = 0,
                fileSize = 0,
                isFavorite = false
            )
            
            val dbId = repository.insertVideo(downloadedVideo).toInt()
            val activeVideoWithId = downloadedVideo.copy(id = dbId)
            
            _activeDownload.value = activeVideoWithId

            // Launch actual HTTP Stream download
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(video.videoUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Server returned code ${response.code}")

                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()
                    val inputStream: InputStream = body.byteStream()

                    // Store inside external movies directory or internal cache if unavailable
                    val moviesDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: getApplication<Application>().cacheDir
                    
                    val fileName = "pindown_${System.currentTimeMillis()}_$quality.mp4"
                    val file = File(moviesDir, fileName)
                    val outputStream = FileOutputStream(file)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var downloadedBytes: Long = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            0
                        }

                        // Update db and state at controlled intervals to minimize recompositions
                        if (progress % 5 == 0 || progress == 100) {
                            val updated = activeVideoWithId.copy(
                                downloadProgress = progress,
                                fileSize = downloadedBytes
                            )
                            _activeDownload.value = updated
                            repository.updateVideo(updated)
                        }
                    }

                    outputStream.close()
                    inputStream.close()

                    // Complete
                    val finalVideo = activeVideoWithId.copy(
                        status = "Completed",
                        downloadProgress = 100,
                        filePath = file.absolutePath,
                        fileSize = totalBytes
                    )
                    repository.updateVideo(finalVideo)
                    _activeDownload.value = null
                    _successNotification.value = "Download completed successfully!"
                }
            } catch (e: Exception) {
                Log.e("PinDownVM", "Download failed", e)
                val failedVideo = activeVideoWithId.copy(
                    status = "Failed",
                    downloadProgress = 0
                )
                repository.updateVideo(failedVideo)
                _activeDownload.value = null
                _errorNotification.value = "Download Failed: Network issue. Try again."
            }
        }
    }

    // Delete a downloaded item
    fun deleteVideo(video: DownloadedVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete file
            video.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            repository.deleteVideo(video)
            _successNotification.value = "Item removed from downloads."
        }
    }

    // Toggle favorite state
    fun toggleFavorite(video: DownloadedVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = video.copy(isFavorite = !video.isFavorite)
            repository.updateVideo(updated)
            if (updated.isFavorite) {
                _successNotification.value = "Added to Favorites!"
            } else {
                _successNotification.value = "Removed from Favorites"
            }
        }
    }

    // Submit user feedback
    fun submitFeedback(email: String, message: String, rating: Int) {
        viewModelScope.launch {
            delay(1000) // Beautiful fake network delay
            _showFeedbackDialog.value = false
            _successNotification.value = "Thank you for your feedback! We will get back to you shortly."
        }
    }

    // Clear history/cache
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val videos = db.videoDao().getAllVideos().first()
            for (v in videos) {
                v.filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                repository.deleteVideo(v)
            }
            _successNotification.value = "Storage and download history cleared."
        }
    }
}
