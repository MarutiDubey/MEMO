package com.example.memo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.memo.data.AppDatabase
import com.example.memo.data.EntryEntity
import com.example.memo.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TypeVaultAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var settingsManager: SettingsManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = AppDatabase.getDatabase(applicationContext)
        settingsManager = SettingsManager(applicationContext)
    }

    private var debounceJob: Job? = null
    private var lastPackageName: String? = null
    private var lastAppName: String? = null
    private var currentText: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Respect global capturing toggle
        if (!settingsManager.isCapturingEnabled) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val text = event.text.joinToString()
            val packageName = event.packageName?.toString() ?: "Unknown"

            // Skip our own app
            if (packageName == this.packageName) return
            // ONLY process included apps
            if (!settingsManager.isIncluded(packageName)) return

            if (text.isNotBlank()) {
                val appName = getAppLabel(packageName)
                
                // If user switched apps, save immediately and start new
                if (lastPackageName != null && lastPackageName != packageName) {
                    commitCurrentText()
                }

                lastPackageName = packageName
                lastAppName = appName
                currentText = text

                debounceJob?.cancel()
                debounceJob = serviceScope.launch {
                    delay(2000)
                    commitCurrentText()
                }
            }
        }
    }

    private fun commitCurrentText() {
        if (currentText.isNotBlank() && lastPackageName != null && lastAppName != null) {
            saveEntry(currentText, lastPackageName!!, lastAppName!!)
            currentText = ""
            lastPackageName = null
            lastAppName = null
        }
    }

    override fun onInterrupt() {}

    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun saveEntry(text: String, packageName: String, appName: String) {
        serviceScope.launch {
            val entry = EntryEntity(
                appName = appName,
                packageName = packageName,
                typedText = text,
                timestamp = System.currentTimeMillis()
            )
            database.entryDao().insertEntry(entry)
        }
    }
}

