package com.example.ui.help

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String = "General"
)

data class HelpUiState(
    val searchQuery: String = "",
    val faqs: List<FaqItem> = listOf(
        FaqItem(
            question = "How do I download a video from Pinterest?",
            answer = "1. Open the Pinterest app or website.\n2. Tap the share button on any video pin and copy its link.\n3. Open PinDown and tap 'PASTE' or click 'FETCH VIDEO'."
        ),
        FaqItem(
            question = "Where are my downloaded videos saved?",
            answer = "All downloaded videos are saved in your phone's internal storage under the 'Movies/PinDown' folder, making them instantly visible in your system Gallery app."
        ),
        FaqItem(
            question = "Does PinDown add watermarks to videos?",
            answer = "No! PinDown extracts the original raw video source directly from Pinterest, keeping 100% of the original quality without any added logos or watermarks."
        ),
        FaqItem(
            question = "Why did my download fail?",
            answer = "Ensure you have an active internet connection. Private or deleted pins cannot be downloaded. If the problem persists, try clearing app cache in Settings."
        ),
        FaqItem(
            question = "Is PinDown free to use?",
            answer = "Yes, PinDown Pro is 100% free with unlimited high-definition video downloads."
        )
    )
)
