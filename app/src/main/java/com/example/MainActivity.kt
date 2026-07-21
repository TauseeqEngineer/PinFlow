package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import com.example.data.PinterestParser
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.DownloadedVideo
import com.example.data.ParsedVideoInfo
import com.example.ui.PinDownViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PinDownViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PinDownMainScreen(viewModel = viewModel, activity = this@MainActivity)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinDownMainScreen(viewModel: PinDownViewModel, activity: MainActivity) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activePlayingVideo by viewModel.activePlayingVideo.collectAsStateWithLifecycle()
    val errorNotification by viewModel.errorNotification.collectAsStateWithLifecycle()
    val successNotification by viewModel.successNotification.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time clipboard trigger on resumption
    DisposableEffect(Unit) {
        viewModel.checkClipboard()
        onDispose {}
    }

    // Main layout wrapper
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) with fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "splash" -> SplashScreen()
                "onboarding" -> OnboardingScreen(onFinish = {
                    viewModel.setScreen("permissions")
                })
                "permissions" -> PermissionsScreen(onComplete = {
                    viewModel.setScreen("main")
                })
                "main" -> DashboardScreen(viewModel = viewModel, activity = activity)
            }
        }

        // Full Screen Video Player Overlay
        if (activePlayingVideo != null) {
            FullScreenPlayerView(
                video = activePlayingVideo!!,
                onDismiss = { viewModel.selectVideoToPlay(null) }
            )
        }

        // Notifications/Toasts (Polished floaters)
        AnimatedVisibility(
            visible = errorNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (errorNotification != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorNotification!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.0f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                LaunchedEffect(errorNotification) {
                    delay(5000)
                    viewModel.clearError()
                }
            }
        }

        AnimatedVisibility(
            visible = successNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (successNotification != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = successNotification!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.0f)
                        )
                        IconButton(onClick = { viewModel.clearSuccess() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
                LaunchedEffect(successNotification) {
                    delay(4000)
                    viewModel.clearSuccess()
                }
            }
        }
    }
}

// ==========================================
// SCREEN: Splash Screen
// ==========================================
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE60023), Color(0xFF5A0009))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant glowing launcher logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = this.size.width / 1.5f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = this.size.width * 1.1f
                        )
                    }
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = "Logo",
                    tint = Color(0xFFE60023),
                    modifier = Modifier.size(54.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "PinDown",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Premium Pinterest Downloader",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Initializing workspace...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ==========================================
// SCREEN: Onboarding Screen
// ==========================================
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "Copy Pinterest URL",
            description = "Browse Pinterest and copy the link of any video Pin you wish to save.",
            icon = Icons.Rounded.Link,
            image = "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=600&auto=format&fit=crop"
        ),
        OnboardingPage(
            title = "Fast Auto-Detection",
            description = "Our smart downloader instantly monitors your clipboard and pastes the Pinterest URL for you.",
            icon = Icons.Rounded.ContentPaste,
            image = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600&auto=format&fit=crop"
        ),
        OnboardingPage(
            title = "Built-in HD Video Player",
            description = "Play downloaded videos in beautiful high-definition offline. Manage history and favorites seamlessly.",
            icon = Icons.Rounded.PlayArrow,
            image = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val page = pages[currentPage]
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Visually Stunning Header Image Card
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(page.image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Onboarding Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    // Floater round icon
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color(0xFFE60023),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Description block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = page.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Indicator and Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Bullets
                Row {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(width = if (index == currentPage) 20.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) Color(0xFFE60023)
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // CTA Button
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("onboarding_next")
                ) {
                    Text(
                        text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val image: String
)

// ==========================================
// SCREEN: Permissions Screen
// ==========================================
@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFFE60023),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Permissions Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PinDown requires standard access permissions to run correctly. On modern Android versions, storage access is restricted securely so you can save videos directly to your gallery. We also request notification permissions to notify you of background download progress and completion.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("grant_permission_button")
                ) {
                    Text("Grant Notification Access", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("skip_permission_button")
                ) {
                    Text("Skip for Now", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ==========================================
// MAIN SCREEN: Dashboard with Bottom Navigation Tabs
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: PinDownViewModel, activity: MainActivity) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeDownload by viewModel.activeDownload.collectAsStateWithLifecycle()
    val showExitDialog by viewModel.showExitDialog.collectAsStateWithLifecycle()
    val showRateDialog by viewModel.showRateDialog.collectAsStateWithLifecycle()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsStateWithLifecycle()
    val showFaqDialog by viewModel.showFaqDialog.collectAsStateWithLifecycle()
    val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()
    val showUpdateAvailable by viewModel.showUpdateAvailable.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    selected = currentTab == "home",
                    onClick = { viewModel.setTab("home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE60023),
                        selectedTextColor = Color(0xFFE60023),
                        indicatorColor = Color(0xFFE60023).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("home_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    label = { Text("Explore", fontSize = 11.sp) },
                    selected = currentTab == "explore",
                    onClick = { viewModel.setTab("explore") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE60023),
                        selectedTextColor = Color(0xFFE60023),
                        indicatorColor = Color(0xFFE60023).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("explore_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Download, contentDescription = "Downloads") },
                    label = { Text("Downloads", fontSize = 11.sp) },
                    selected = currentTab == "downloads",
                    onClick = { viewModel.setTab("downloads") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE60023),
                        selectedTextColor = Color(0xFFE60023),
                        indicatorColor = Color(0xFFE60023).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("downloads_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites", fontSize = 11.sp) },
                    selected = currentTab == "favorites",
                    onClick = { viewModel.setTab("favorites") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE60023),
                        selectedTextColor = Color(0xFFE60023),
                        indicatorColor = Color(0xFFE60023).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("favorites_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    selected = currentTab == "settings",
                    onClick = { viewModel.setTab("settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE60023),
                        selectedTextColor = Color(0xFFE60023),
                        indicatorColor = Color(0xFFE60023).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switcher
            Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    "home" -> HomeTabScreen(viewModel = viewModel)
                    "explore" -> ExploreTabScreen(viewModel = viewModel)
                    "downloads" -> DownloadsTabScreen(viewModel = viewModel)
                    "favorites" -> FavoritesTabScreen(viewModel = viewModel)
                    "settings" -> SettingsTabScreen(viewModel = viewModel)
                }
            }

            // Global active download progress floater
            if (activeDownload != null) {
                ActiveDownloadOverlayCard(
                    activeDownload = activeDownload!!,
                    onCancel = { /* Simple simulation skip */ }
                )
            }

            // Dialog overlay managers
            if (showExitDialog) {
                ExitConfirmationDialog(
                    onConfirm = { activity.finish() },
                    onDismiss = { viewModel.showExitDialog(false) }
                )
            }

            if (showRateDialog) {
                RateAppDialog(
                    onDismiss = { viewModel.showRateDialog(false) }
                )
            }

            if (showFeedbackDialog) {
                FeedbackDialog(
                    onSubmit = { email, feedback, rating ->
                        viewModel.submitFeedback(email, feedback, rating)
                    },
                    onDismiss = { viewModel.showFeedbackDialog(false) }
                )
            }

            if (showFaqDialog) {
                FaqDialog(onDismiss = { viewModel.showFaqDialog(false) })
            }

            if (showAboutDialog) {
                AboutDialog(onDismiss = { viewModel.showAboutDialog(false) })
            }
        }
    }
}

// ==========================================
// TAB: HOME SCREEN (Paste URL, fetch info, download triggers)
// ==========================================
@Composable
fun HomeTabScreen(viewModel: PinDownViewModel) {
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

                    // Small quick info trigger
                    IconButton(
                        onClick = { viewModel.showAboutDialog(true) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "About", tint = Color(0xFFE60023))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mega Bold Typography Title
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

        // Paste URL primary Card in Bold Typography theme
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

                    // Input Field with integrated Paste Button
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

                        // Integrated Paste Button
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

                    // Huge Bold Fetch Button
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

        // Beautiful curated explore header
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aesthetic Live Previews",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Interactive", color = Color(0xFFE60023), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4 curations to instantly try/test
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (pin in PinterestParser.PRE_SEEDED_PINS) {
                    CuratedPinItem(pin = pin, onClick = {
                        inputUrl = pin.pinterestUrl
                        viewModel.fetchPinterestVideo(pin.pinterestUrl)
                    })
                }
            }
        }
    }

    // Dynamic Download Options Bottom Sheet Dialog
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

// ==========================================
// SUBVIEW: Fetching Video Ring
// ==========================================
@Composable
fun FetchingAnimationBlock() {
    val infiniteTransition = rememberInfiniteTransition(label = "fetching")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .drawBehind {
                        drawArc(
                            color = Color(0xFFE60023),
                            startAngle = angle,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 8f)
                        )
                        drawArc(
                            color = Color(0xFFE60023).copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8f)
                        )
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Scraping Pinterest Content...",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Following redirections to retrieve media files safely.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// SUBVIEW: Video Details display
// ==========================================
@Composable
fun VideoDetailsBlock(
    video: ParsedVideoInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }

                // Title overlay
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = video.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Download Quality", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SUBVIEW: Curated/Preset Interactive Pin Item
// ==========================================
@Composable
fun CuratedPinItem(pin: ParsedVideoInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(64.dp)
            ) {
                AsyncImage(
                    model = pin.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = pin.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pin.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Fetch",
                tint = Color(0xFFE60023),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// COMPONENT: Download options bottom picker dialog
// ==========================================
@Composable
fun DownloadOptionsPicker(
    video: ParsedVideoInfo,
    onSelectOption: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Choose Quality",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                val options = listOf(
                    QualityOption("Full HD MP4 (1080p)", "Fast - Recommended", "24.5 MB"),
                    QualityOption("Standard MP4 (720p)", "Normal - Data Saver", "12.2 MB"),
                    QualityOption("Compact MP4 (480p)", "Compact - Low Res", "6.8 MB")
                )

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectOption(option.title) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = option.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = option.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = option.size,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE60023)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    }
}

data class QualityOption(val title: String, val subtitle: String, val size: String)

// ==========================================
// COMPONENT: Active download Overlay Floater
// ==========================================
@Composable
fun ActiveDownloadOverlayCard(
    activeDownload: DownloadedVideo,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            border = BorderStroke(1.dp, Color(0xFFE60023).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive dynamic progress circular ring
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFE60023).copy(alpha = 0.12f),
                                radius = this.size.width / 2f
                            )
                            drawArc(
                                color = Color(0xFFE60023),
                                startAngle = -90f,
                                sweepAngle = (activeDownload.downloadProgress * 3.6f),
                                useCenter = false,
                                style = Stroke(width = 6f)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${activeDownload.downloadProgress}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE60023)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = activeDownload.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val formattedSize = if (activeDownload.fileSize > 0) {
                        "${String.format("%.1f", activeDownload.fileSize / 1024f / 1024f)} MB"
                    } else {
                        "Streaming..."
                    }
                    Text(
                        text = "Downloading in background • $formattedSize",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB: EXPLORE SCREEN (Masonry visual grid)
// ==========================================
@Composable
fun ExploreTabScreen(viewModel: PinDownViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredPins by viewModel.filteredExplorePins.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Explore Ideas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Discover popular themes on Pinterest",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search wallpaper, cafe, nature...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Creative Masonry Grid
        if (filteredPins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No pre-seeds matched. Clear search to reset.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPins) { pin ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.fetchPinterestVideo(pin.pinterestUrl)
                                viewModel.setTab("home")
                            }
                    ) {
                        Box {
                            AsyncImage(
                                model = pin.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            // Play icon overlaid
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = pin.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB: DOWNLOADS SCREEN
// ==========================================
@Composable
fun DownloadsTabScreen(viewModel: PinDownViewModel) {
    val downloads by viewModel.allVideos.collectAsStateWithLifecycle()
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Completed, 1 = Active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Library",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom elegant segment tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            val isCompletedSelected = activeSubTab == 0
            val isActiveSelected = activeSubTab == 1
            
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCompletedSelected) Color(0xFFE60023) else Color.Transparent)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Completed (${downloads.filter { it.status == "Completed" }.size})",
                    color = if (isCompletedSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActiveSelected) Color(0xFFE60023) else Color.Transparent)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Active (${downloads.filter { it.status == "Downloading" }.size})",
                    color = if (isActiveSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentList = if (activeSubTab == 0) {
            downloads.filter { it.status == "Completed" }
        } else {
            downloads.filter { it.status == "Downloading" }
        }

        if (currentList.isEmpty()) {
            // Empty downloads state with CTA to home
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (activeSubTab == 0) "No Videos Saved Yet" else "No Active Downloads",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (activeSubTab == 0) "Your completed downloads will appear here ready to watch offline." else "When you trigger a download, check here for speed and progress.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    
                    if (activeSubTab == 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.setTab("home") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Download a Video Now", color = Color.White)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentList, key = { it.id }) { item ->
                    DownloadGridItem(
                        video = item,
                        onPlay = { viewModel.selectVideoToPlay(item) },
                        onFavoriteToggle = { viewModel.toggleFavorite(item) },
                        onDelete = { viewModel.deleteVideo(item) }
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: Item row in Downloads library
// ==========================================
@Composable
fun DownloadGridItem(
    video: DownloadedVideo,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail Box with Overlay Play Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlay)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val formattedSize = if (video.fileSize > 0) {
                    "${String.format("%.1f", video.fileSize / 1024f / 1024f)} MB"
                } else {
                    "Completed"
                }
                Text(
                    text = "MP4 • $formattedSize",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action triggers
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) Color(0xFFE60023) else Color.Gray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

// ==========================================
// TAB: FAVORITES SCREEN
// ==========================================
@Composable
fun FavoritesTabScreen(viewModel: PinDownViewModel) {
    val favorites by viewModel.favoriteVideos.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Curated Favorites",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Quick access to your preferred saves",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Favorites Saved", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Bookmark items using the heart icons inside library.", color = Color.Gray.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectVideoToPlay(item) }
                    ) {
                        Box {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                            IconButton(
                                onClick = { viewModel.toggleFavorite(item) },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Rounded.Favorite, contentDescription = "Unfavorite", tint = Color(0xFFE60023), modifier = Modifier.size(18.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB: SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsTabScreen(viewModel: PinDownViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val autoPasteEnabled by viewModel.autoPasteEnabled.collectAsStateWithLifecycle()
    val showUpdateAvailable by viewModel.showUpdateAvailable.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Preferences",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize look and file mechanics",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Custom update available banner
        if (showUpdateAvailable) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE60023).copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color(0xFFE60023).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFE60023))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("New Update Available! (v2.0)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "We've added multi-threaded connection acceleration to make Pinterest video extraction up to 4x faster.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.dismissUpdateBanner() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Update Now", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.dismissUpdateBanner() }) {
                                Text("Dismiss", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Display Theme
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Comfort Dark Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Switch colors to gentle dark cocoa tone", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE60023), checkedTrackColor = Color(0xFFE60023).copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Section: Clipboard Listener
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Auto-Paste Clipboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Detect active Pinterest links automatically", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = autoPasteEnabled,
                        onCheckedChange = { viewModel.toggleAutoPaste(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE60023), checkedTrackColor = Color(0xFFE60023).copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Section: Cleaner/Cache Action
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearCache() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Clear Downloader Cache", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Clean storage logs and temporary parsed links", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Section: Dynamic modal togglers (FAQ, Feedback, About, Rate)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsInteractiveRow(
                        title = "Help & FAQ",
                        icon = Icons.Rounded.Help,
                        onClick = { viewModel.showFaqDialog(true) }
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    SettingsInteractiveRow(
                        title = "Submit Feedback",
                        icon = Icons.Rounded.Feedback,
                        onClick = { viewModel.showFeedbackDialog(true) },
                        tag = "feedback_button"
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    SettingsInteractiveRow(
                        title = "Rate PinDown",
                        icon = Icons.Rounded.Star,
                        onClick = { viewModel.showRateDialog(true) },
                        tag = "rate_button"
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    SettingsInteractiveRow(
                        title = "About PinDown",
                        icon = Icons.Rounded.Info,
                        onClick = { viewModel.showAboutDialog(true) }
                    )
                }
            }
        }

        // Section: Exit Dialog Trigger
        item {
            Button(
                onClick = { viewModel.showExitDialog(true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Exit Application", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsInteractiveRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// ==========================================
// SCREEN/COMP: Fullscreen Native Video Player
// ==========================================
@Composable
fun FullScreenPlayerView(video: DownloadedVideo, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Real local native Android VideoView stream integration
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            if (video.filePath != null && File(video.filePath).exists()) {
                                setVideoPath(video.filePath)
                            } else {
                                setVideoURI(Uri.parse(video.videoUrl))
                            }
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Top scrim & Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.0f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Dynamic playback indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Offline Playback • Seamless Loop", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// MODAL DIALOGS
// ==========================================

@Composable
fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Exit PinDown?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Are you sure you want to close the Pinterest Video Downloader application?", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Stay") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023))
                    ) {
                        Text("Exit", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RateAppDialog(onDismiss: () -> Unit) {
    var rating by remember { mutableStateOf(5) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Enjoying PinDown?", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please support us by giving a 5-star rating on the Google Play Store!", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(20.dp))

                // Interactive 5-star row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = if (i <= rating) Color(0xFFFFB300) else Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Submit Rating", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onSubmit: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf("") }
    var userRating by remember { mutableStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Submit App Feedback", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Your Email") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = feedbackMsg,
                    onValueChange = { feedbackMsg = it },
                    placeholder = { Text("How can we improve PinDown? Let us know what you think.") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Your Experience Rating:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    for (i in 1..5) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (i <= userRating) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { userRating = i }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(email, feedbackMsg, userRating) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                        enabled = feedbackMsg.isNotBlank()
                    ) {
                        Text("Send", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Help & FAQ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFE60023))
                Spacer(modifier = Modifier.height(16.dp))

                val faqs = listOf(
                    FaqItem(
                        "How do I download Pinterest videos?",
                        "Just browse Pinterest, copy the Pin's link, return to PinDown, and tap 'Fetch Video'. Choose your quality and the video will save to your library instantly!"
                    ),
                    FaqItem(
                        "Are private pins supported?",
                        "No, Pinterest keeps private boards strictly secured. PinDown only parses public Pinterest links and pins."
                    ),
                    FaqItem(
                        "Where are files saved on my device?",
                        "Files are downloaded to your standard Android Movies folder in application storage. You can access and delete them at any time in the Library tab."
                    )
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(260.dp)) {
                    items(faqs) { faq ->
                        var expanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { expanded = !expanded }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(faq.q, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f))
                                    Icon(
                                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(faq.a, fontSize = 11.sp, lineHeight = 15.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Got It", color = Color.White)
                }
            }
        }
    }
}

data class FaqItem(val q: String, val a: String)

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE60023).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFFE60023), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("PinDown", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("v1.0.0 Pro Edition", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "PinDown is a modern premium client-side utility built for high-performance extraction of media clips directly from Pinterest public CDN storage. Features secure sandboxed storage, instant clipboard detection, curations, and loopable native playback. No advertising tracking or account sign-ups required.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Designed & Built in Antigravity • © 2026", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = Color.White)
                }
            }
        }
    }
}
