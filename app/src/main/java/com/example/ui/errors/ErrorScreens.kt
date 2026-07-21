package com.example.ui.errors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton

@Composable
fun ErrorScreen(
    errorType: ErrorType,
    customMessage: String? = null,
    onRetry: () -> Unit
) {
    val (title, defaultMessage) = when (errorType) {
        ErrorType.NO_INTERNET -> Pair("No Internet Connection", "Please check your Wi-Fi or cellular network connection and try again.")
        ErrorType.INVALID_URL -> Pair("Invalid Pinterest URL", "Please make sure you copied a valid Pinterest video pin URL.")
        ErrorType.DOWNLOAD_FAILED -> Pair("Download Failed", "We couldn't retrieve the video file. Pinterest might be temporarily throttling request speed.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ErrorIllustration(type = errorType)

            Spacer(modifier = Modifier.height(28.dp))

            ErrorMessageBlock(
                title = title,
                message = customMessage ?: defaultMessage
            )

            Spacer(modifier = Modifier.height(36.dp))

            PrimaryButton(
                text = "Try Again",
                onClick = onRetry,
                icon = Icons.Rounded.Refresh,
                modifier = Modifier.widthIn(max = 240.dp)
            )
        }
    }
}
