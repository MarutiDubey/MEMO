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
import androidx.compose.ui.Modifier
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
            val excludedApps by viewModel.excludedApps.collectAsState()
            val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()

            SettingsScreen(
                isCapturing = isCapturing,
                excludedApps = excludedApps,
                autoDeleteDays = autoDeleteDays,
                onBackClick = { navController.popBackStack() },
                onCapturingToggle = { viewModel.setCapturingEnabled(it) },
                onAddExcludedApp = { viewModel.addExcludedApp(it) },
                onRemoveExcludedApp = { viewModel.removeExcludedApp(it) },
                onAutoDeleteChanged = { viewModel.setAutoDeleteDays(it) },
                onClearAllData = { viewModel.clearAllData() }
            )
        }
    }
}
