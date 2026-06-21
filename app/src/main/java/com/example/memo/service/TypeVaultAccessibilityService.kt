package com.example.memo.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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

    // ── Keyboard capture state ──
    private var debounceJob: Job? = null
    private var lastPackageName: String? = null
    private var lastAppName: String? = null
    private var currentText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = AppDatabase.getDatabase(applicationContext)
        settingsManager = SettingsManager(applicationContext)

        try {
            val clipIntent = Intent(applicationContext, ClipboardMonitorService::class.java)
            applicationContext.startService(clipIntent)
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!settingsManager.isCapturingEnabled) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val fullText = event.text.joinToString()

            if (fullText.isNotBlank()) {
                val appName = getAppLabel(packageName)
                if (lastPackageName != null && lastPackageName != packageName) commitCurrentText()
                lastPackageName = packageName
                lastAppName = appName
                currentText = fullText
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
            database.entryDao().insertEntry(
                EntryEntity(
                    appName = appName,
                    packageName = packageName,
                    typedText = text,
                    timestamp = System.currentTimeMillis(),
                    source = "KEYBOARD"
                )
            )
        }
    }
}
