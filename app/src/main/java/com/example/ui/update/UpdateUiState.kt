package com.example.ui.update

data class UpdateUiState(
    val currentVersion: String = "2.3.0",
    val latestVersion: String = "2.4.0",
    val whatsNewList: List<String> = listOf(
        "⚡ 3x faster video extraction engine",
        "🎨 Play Store Featured Quality Material 3 UI",
        "📂 Redesigned downloads & favorites management",
        "🛠️ Improved network timeout handling and retry logic"
    )
)
