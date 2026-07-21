package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PinDownViewModel

@Composable
fun HomeScreen(viewModel: PinDownViewModel) {
    var inputUrl by remember { mutableStateOf("") }
    val isFetching by viewModel.isFetching.collectAsStateWithLifecycle()
    val fetchedVideo by viewModel.fetchedVideo.collectAsStateWithLifecycle()
    val clipboardText by viewModel.clipboardText.collectAsStateWithLifecycle()
    val showDownloadOptions by viewModel.showDownloadOptions.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header banner in Bold Typography style
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE60023), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                        Text(
                            text = "PINFLOW PRO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showAboutDialog(true) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "About", tint = Color(0xFFE60023))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        append("GRAB\n")
                        withStyle(style = SpanStyle(color = Color(0xFFE60023))) {
                            append("EVERY")
                        }
                        append("\nPIXEL.")
                    },
                    fontSize = 44.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Download Pinterest videos in stunning 4K quality instantly.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.widthIn(max = 240.dp)
                )
            }
        }

        // Paste URL primary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ENTER VIDEO URL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFE60023)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("Paste Pinterest URL here...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paste_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Rounded.Link, contentDescription = "Link", tint = Color(0xFFE60023))
                            },
                            trailingIcon = {
                                if (inputUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inputUrl = "" },
                                        modifier = Modifier.padding(end = 64.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                if (inputUrl.isNotBlank()) {
                                    viewModel.fetchPinterestVideo(inputUrl)
                                }
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color(0xFFE60023),
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.12f)
                            )
                        )

                        Button(
                            onClick = {
                                val clipboard = viewModel.clipboardText.value
                                if (clipboard.isNotBlank()) {
                                    inputUrl = clipboard
                                }
                            },
                            enabled = clipboardText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                disabledContainerColor = Color.White.copy(alpha = 0.03f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                                .testTag("paste_button")
                        ) {
                            Text("PASTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (inputUrl.isNotBlank()) {
                                viewModel.fetchPinterestVideo(inputUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .testTag("download_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FETCH VIDEO",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Clipboard auto-detect banner
        if (clipboardText.isNotBlank() && inputUrl != clipboardText) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE60023).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFE60023).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1.0f)) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = Color(0xFFE60023))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detected Pin URL in clipboard: $clipboardText",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Row {
                            TextButton(
                                onClick = { inputUrl = clipboardText },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Use", color = Color(0xFFE60023), fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.clearClipboardSuggestion() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Fetching shimmer state
        if (isFetching) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                FetchingAnimationBlock()
            }
        }

        // Fetched Video Details Card
        if (fetchedVideo != null && !isFetching) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                VideoDetailsBlock(
                    video = fetchedVideo!!,
                    onDownload = { viewModel.triggerDownloadOptions(fetchedVideo!!) },
                    onDismiss = { viewModel.resetFetchedVideo() }
                )
            }
        }

        // Empty State when no video is fetched
        if (fetchedVideo == null && !isFetching) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color(0xFFE60023),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "READY FOR EXTRACTION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Paste a Pinterest video URL in the input field above and click FETCH VIDEO to extract the media file in high definition.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showDownloadOptions != null) {
        DownloadOptionsPicker(
            video = showDownloadOptions!!,
            onSelectOption = { quality ->
                viewModel.startDownload(showDownloadOptions!!, quality)
            },
            onDismiss = { viewModel.dismissDownloadOptions() }
        )
    }
}
