package com.example

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class AppScreen {
    Splash,
    Main,
    Settings
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel.initPrefs(applicationContext)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Splash) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_scaffold"),
                    containerColor = BackgroundDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(durationMillis = 500),
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.Splash -> SplashScreen(
                                    onTimeout = { currentScreen = AppScreen.Main }
                                )
                                AppScreen.Main -> MainScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreen = AppScreen.Settings }
                                )
                                AppScreen.Settings -> SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = AppScreen.Main }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // App Icon and Title in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_walhero_icon),
                contentDescription = "Walhero Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                    .shadow(16.dp, RoundedCornerShape(32.dp), clip = false)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Walhero",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "LIVE WALLPAPER",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Branding Banner at the absolute bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.85f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_wbj_banner),
                contentDescription = "WBJ Brand Logo Banner",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val videoState by viewModel.videoState.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    // File picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleSelectedVideo(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // App Header Row (Image 3 style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_walhero_icon),
                contentDescription = "Walhero App Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Walhero",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Live Wallpaper",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = "Premium Video Wallpaper Experience",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Gear icon for settings
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .testTag("settings_button")
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card
        StatusCard(videoState = videoState)

        Spacer(modifier = Modifier.height(20.dp))

        // Button: Choose Video
        GlowingActionButton(
            text = "Choose Video",
            icon = Icons.Default.Add,
            glowColor = NeonBlue,
            onClick = {
                pickerLauncher.launch(arrayOf("video/*"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("choose_video_button")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Switch Card
        SoundSwitchCard(
            soundEnabled = soundEnabled,
            onToggle = { enabled -> viewModel.setSoundEnabled(context, enabled) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button: Apply Live Wallpaper
        GlowingActionButton(
            text = "Apply Live Wallpaper",
            icon = Icons.Default.PlayArrow,
            glowColor = GlowPurple,
            onClick = {
                if (videoState is VideoState.Ready) {
                    launchWallpaperChooser(context)
                } else {
                    Toast.makeText(context, "Please choose a video first", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_wallpaper_button")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button: Clear Video
        OutlinedActionCard(
            text = "Clear Selected Video",
            icon = Icons.Default.Delete,
            borderColor = StatusRed.copy(alpha = 0.5f),
            onClick = {
                viewModel.clearVideo(context)
                Toast.makeText(context, "Video cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_video_button")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Info Card explaining wallpaper behaviors (Image 3 style)
        InfoBehaviorsCard()
    }
}

@Composable
fun StatusCard(videoState: VideoState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left icon (checkmark circle or idle info circle)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (videoState is VideoState.Ready) StatusGreen.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (videoState is VideoState.Ready) StatusGreen else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = when (videoState) {
                        is VideoState.Ready -> Icons.Default.Check
                        is VideoState.Copying -> Icons.Default.Refresh
                        is VideoState.Error -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (videoState) {
                        is VideoState.Ready -> StatusGreen
                        is VideoState.Copying -> NeonCyan
                        is VideoState.Error -> StatusRed
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (videoState) {
                        is VideoState.Ready -> "Video ready"
                        is VideoState.Copying -> "Processing video..."
                        is VideoState.Error -> "Selection error"
                        else -> "No video selected"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = when (videoState) {
                        is VideoState.Ready -> "Active: ${videoState.fileName} (${videoState.sizeStr})"
                        is VideoState.Copying -> "Copying video files to secure cache..."
                        is VideoState.Error -> videoState.message
                        else -> "Tap 'Choose Video' to import an MP4"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SoundSwitchCard(
    soundEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable { onToggle(!soundEnabled) }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (soundEnabled) NeonCyan else TextSecondary,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Sound On / Off",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = soundEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonBlue,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BackgroundDark,
                    uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.testTag("sound_switch")
            )
        }
    }
}

@Composable
fun GlowingActionButton(
    text: String,
    icon: ImageVector,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(glowColor, glowColor.copy(alpha = 0.7f))
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .background(gradientBrush, shape = RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun OutlinedActionCard(
    text: String,
    icon: ImageVector,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground.copy(alpha = 0.6f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StatusRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InfoBehaviorsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground.copy(alpha = 0.5f))
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "On some Android versions, the system may apply the wallpaper to home and lock screens together.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // The overlapping dual-phone visual mockup from Image 3
            Row(
                horizontalArrangement = Arrangement.spacedBy((-14).dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Home phone silhouette
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF070F25))
                        .border(1.dp, NeonBlue.copy(alpha = 0.6f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Lock phone silhouette (overlayed slightly)
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0C0720))
                        .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Settings Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .testTag("back_button")
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sound Toggle Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wallpaper Audio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Play sound when setting live wallpaper",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { enabled -> viewModel.setSoundEnabled(context, enabled) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonBlue,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = BackgroundDark,
                        uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear Video Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .clickable {
                    viewModel.clearVideo(context)
                    Toast.makeText(context, "Active wallpaper video cleared", Toast.LENGTH_SHORT).show()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Clear Wallpaper Data",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Delete stored video from cache storage",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "About Walhero",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Walhero Live Wallpaper lets you choose any local MP4 video file and seamlessly apply it as your Android background. Featuring custom center-cropping algorithms, lightweight looping engines, and responsive sound toggles.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "App Version", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "1.0 (Premium)", fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Branding banner at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_wbj_banner),
                contentDescription = "WBJ Copyright Logo Banner",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

private fun launchWallpaperChooser(context: Context) {
    try {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, VideoWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Failed to open Live Wallpaper picker.", Toast.LENGTH_LONG).show()
        }
    }
}
