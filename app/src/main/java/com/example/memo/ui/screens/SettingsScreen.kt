package com.example.memo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.memo.ui.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isCapturing: Boolean,
    includedApps: Set<String>,
    installedApps: List<InstalledAppInfo>,
    autoDeleteDays: Int,
    onBackClick: () -> Unit,
    onCapturingToggle: (Boolean) -> Unit,
    onIncludeApp: (String) -> Unit,
    onExcludeApp: (String) -> Unit,
    onAutoDeleteChanged: (Int) -> Unit,
    onClearAllData: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val autoDeleteOptions = listOf(0 to "Off", 30 to "30 days", 60 to "60 days", 90 to "90 days")
    var autoDeleteExpanded by remember { mutableStateOf(false) }
    val selectedLabel = autoDeleteOptions.find { it.first == autoDeleteDays }?.second ?: "Off"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Capturing Toggle ---
            SettingSection(title = "Capture") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Enable Capturing", style = MaterialTheme.typography.bodyLarge)
                        Text("Log keystrokes from all apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isCapturing, onCheckedChange = onCapturingToggle)
                }
            }

            // --- Auto-Delete ---
            SettingSection(title = "Auto-Delete") {
                Column {
                    Text("Delete entries older than", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = autoDeleteExpanded,
                        onExpandedChange = { autoDeleteExpanded = !autoDeleteExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoDeleteExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = autoDeleteExpanded,
                            onDismissRequest = { autoDeleteExpanded = false }
                        ) {
                            autoDeleteOptions.forEach { (days, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onAutoDeleteChanged(days)
                                        autoDeleteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Included Apps ---
            SettingSection(title = "Included Apps") {
                Column {
                    Text(
                        "Only the apps selected below will be logged. By default, no apps are logged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (installedApps.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        // We will limit height of this list so it's scrollable within the column, or since
                        // the whole screen is vertically scrollable, we can just render them all. 
                        // Rendering all apps could be slow, but it's simple. Let's render them all.
                        installedApps.forEach { appInfo ->
                            val isChecked = includedApps.contains(appInfo.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) onIncludeApp(appInfo.packageName)
                                        else onExcludeApp(appInfo.packageName)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(appInfo.appName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        appInfo.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Battery Optimization ---
            SettingSection(title = "Battery") {
                val context = LocalContext.current
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)

                Column {
                    Text("Prevent the app from being killed in the background.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isIgnoring) {
                        Text("✅ Battery optimization is disabled. App will run in background.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disable Battery Optimization")
                        }
                    }
                }
            }

            // --- Danger Zone ---
            SettingSection(title = "Danger Zone") {
                Button(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Captured Data", color = Color.White)
                }
            }
        }
    }

    // Add Excluded App Dialog removed

    // --- Confirm Clear All Dialog ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete ALL captured entries. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAllData()
                    showClearConfirmDialog = false
                }) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
