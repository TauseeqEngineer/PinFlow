package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PinDownViewModel
import com.example.ui.about.AboutDialog
import com.example.ui.components.ExitBottomSheet
import com.example.ui.components.FullScreenPlayerView
import com.example.ui.components.RateAppBottomSheet
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.feedback.FeedbackDialog
import com.example.ui.help.FaqDialog
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PinterestRed
import kotlinx.coroutines.delay

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

    DisposableEffect(Unit) {
        viewModel.checkClipboard()
        onDispose {}
    }

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

        // Floating Error Notification Toast
        AnimatedVisibility(
            visible = errorNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (errorNotification != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
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
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                LaunchedEffect(errorNotification) {
                    delay(5000)
                    viewModel.clearError()
                }
            }
        }

        // Floating Success Notification Toast
        AnimatedVisibility(
            visible = successNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (successNotification != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: PinDownViewModel, activity: MainActivity) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showExitDialog by viewModel.showExitDialog.collectAsStateWithLifecycle()
    val showRateDialog by viewModel.showRateDialog.collectAsStateWithLifecycle()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsStateWithLifecycle()
    val showFaqDialog by viewModel.showFaqDialog.collectAsStateWithLifecycle()
    val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = currentTab == "home",
                    onClick = { viewModel.setTab("home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PinterestRed,
                        selectedTextColor = PinterestRed,
                        indicatorColor = PinterestRed.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("home_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Download, contentDescription = "Downloads") },
                    label = { Text("Downloads", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = currentTab == "downloads",
                    onClick = { viewModel.setTab("downloads") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PinterestRed,
                        selectedTextColor = PinterestRed,
                        indicatorColor = PinterestRed.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("downloads_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = currentTab == "settings",
                    onClick = { viewModel.setTab("settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PinterestRed,
                        selectedTextColor = PinterestRed,
                        indicatorColor = PinterestRed.copy(alpha = 0.12f)
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
            Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    "home" -> HomeScreen(viewModel = viewModel)
                    "downloads" -> DownloadsScreen(viewModel = viewModel)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Dialog overlay managers
            if (showExitDialog) {
                ExitBottomSheet(
                    onConfirmExit = { activity.finish() },
                    onDismiss = { viewModel.showExitDialog(false) }
                )
            }

            if (showRateDialog) {
                RateAppBottomSheet(
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
