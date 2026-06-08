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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isCapturing: Boolean,
    excludedApps: Set<String>,
    autoDeleteDays: Int,
    onBackClick: () -> Unit,
    onCapturingToggle: (Boolean) -> Unit,
    onAddExcludedApp: (String) -> Unit,
    onRemoveExcludedApp: (String) -> Unit,
    onAutoDeleteChanged: (Int) -> Unit,
    onClearAllData: () -> Unit
) {
    var showAddExcludeDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var newPackageName by remember { mutableStateOf("") }

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

            // --- Excluded Apps ---
            SettingSection(title = "Excluded Apps") {
                Column {
                    Text(
                        "Apps in this list will NOT be logged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (excludedApps.isEmpty()) {
                        Text("No apps excluded.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        excludedApps.forEach { pkg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pkg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemoveExcludedApp(pkg) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddExcludeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Excluded App")
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

    // --- Add Excluded App Dialog ---
    if (showAddExcludeDialog) {
        AlertDialog(
            onDismissRequest = { showAddExcludeDialog = false },
            title = { Text("Add Excluded App") },
            text = {
                Column {
                    Text("Enter the package name of the app to exclude (e.g., com.example.banking).")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPackageName,
                        onValueChange = { newPackageName = it },
                        label = { Text("Package name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newPackageName.isNotBlank()) {
                                onAddExcludedApp(newPackageName.trim())
                                newPackageName = ""
                                showAddExcludeDialog = false
                            }
                        })
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPackageName.isNotBlank()) {
                        onAddExcludedApp(newPackageName.trim())
                        newPackageName = ""
                        showAddExcludeDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExcludeDialog = false }) { Text("Cancel") }
            }
        )
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
