# Memo— Personal Keystroke Logger (Android)

### _For Personal Use Only — Sideloaded APK_

---

## 📌 App Overview

**App Name:** MEMO
**Platform:** Android
**Installation:** Sideload APK (no Play Store needed)
**Who is it for:** Personal use on your own device only
**Core Idea:** Automatically capture and save everything you type across any app — organized by app folder with timestamps — so you never lose what you typed.

---

## ⚙️ How It Works

### Method — AccessibilityService ✅

TypeVault registers an `AccessibilityService` that listens for `TYPE_VIEW_TEXT_CHANGED` events system-wide. Whenever you type in any app, the service captures the text change and saves it locally with the source app name and timestamp.

**Why this works for personal use:**

- No Play Store approval needed — you sideload the APK yourself
- You manually enable the Accessibility Service once in your phone's Settings
- No root required
- Works on Android 10, 11, 12, 13, 14, 15
- No keyboard replacement needed — keep using Gboard, SwiftKey, whatever you like

**How to enable (one time only):**

1. Install the APK on your phone
2. Go to `Settings → Accessibility → Installed Services → TypeVault`
3. Toggle ON
4. Done — it runs silently in the background forever

---

## 🗂️ Data Organization

### Folder Structure (in local database)

```
TypeVault
├── WhatsApp/
│   ├── 15 Jan 2025, 9:32 AM — "Hey are we meeting tomorrow?"
│   ├── 15 Jan 2025, 2:10 PM — "Yes at 5pm sounds good"
│   └── ...
├── Chrome/
│   ├── 16 Jan 2025, 11:00 AM — "best restaurants in Indore"
│   └── ...
├── Instagram/
│   └── ...
├── Gmail/
│   └── ...
└── Other/
    └── ...
```

### What gets saved per entry

```
- App name         → "WhatsApp"
- App package      → "com.whatsapp"
- Typed text       → "Hey are we meeting tomorrow?"
- Date & Time      → 15 Jan 2025, 9:32 AM
- Session duration → ~34 seconds
```

---

## 📱 App Screens

### 1. Home Screen

- List of all app folders (with app icon + name + entry count)
- Search bar at top
- Tap any folder to open it

### 2. Folder Screen (e.g., WhatsApp)

- Chronological list of all typing sessions in that app
- Each row: date/time + first line of text preview
- Tap to read full text

### 3. Entry Detail Screen

- Full text of what was typed
- Date, time, app name
- Buttons: Copy / Delete

### 4. Search Screen

- Search any word across all apps and all entries
- Shows matching entries with app name + date

### 5. Settings Screen

- Enable / Disable capturing (toggle)
- Excluded apps list (add apps you don't want logged)
- Auto-delete entries older than X days (optional)
- Clear all data button

---

## 🔑 Features (Lean & Personal)

| Feature | Description |
| --- | --- |
| **Auto-capture** | Logs all typed text automatically via AccessibilityService |
| **App folders** | Text organized by source app with app icon |
| **Timestamps** | Every entry saved with exact date and time |
| **Search** | Find any word you ever typed across all apps |
| **Excluded apps** | Skip logging for apps you choose |
| **Copy entry** | One tap to copy old text to clipboard |
| **Auto-delete** | Optional: delete entries older than 30/60/90 days |
| **No internet** | App is fully offline, nothing leaves your phone |

---

## 🔒 Privacy & Security

Since this is for personal use only:

- **All data stored locally** in Room database on your device
- **No internet permission** — app cannot send data anywhere
- Password fields are **automatically excluded** by Android's AccessibilityService API (it respects `inputType = password`)
- You can manually add any app to the **excluded list** (banking apps, etc.)
- No ads, no analytics, no cloud

---

## 🧰 Tech Stack (Simple & Lean)

| Layer | Technology |
| --- | --- |
| **Language** | Kotlin |
| **UI** | Jetpack Compose |
| **Text Capture** | AccessibilityService |
| **Database** | Room (SQLite) |
| **Architecture** | Simple MVVM (ViewModel + Repository) |
| **Async** | Kotlin Coroutines + Flow |
| **Build** | Android Studio + Gradle |
| **Install** | Sideload APK via USB or file manager |

No need for: Hilt, Navigation Component, EncryptedSharedPreferences, Play Store compliance layers, onboarding flows, or permission declaration forms.

---

## 🗺️ Development Roadmap

### Phase 1 — Core (2–3 weeks)

- Create Android project in Android Studio
- Implement `AccessibilityService` to capture text + detect app name
- Save entries to Room database (text, app, timestamp)
- Home screen — list of app folders
- Folder screen — list of entries per app
- Entry detail screen — full text + copy/delete

### Phase 2 — Usability (1–2 weeks)

- Search across all entries
- Settings screen
- Excluded apps list
- Auto-delete by age option

### Phase 3 — Polish (1 week)

- App icons in folder list
- Better date formatting
- Entry count badges on folders
- Clear all data option

---

## 🛠️ Key Implementation Notes

### AccessibilityService setup (AndroidManifest.xml)

```xml
<service
    android:name=".TypeVaultAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### accessibility_service_config.xml

```xml
<accessibility-service
    android:accessibilityEventTypes="typeViewTextChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100" />
```

### Core capture logic (Kotlin)

```kotlin
class TypeVaultAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val text = event.text.joinToString()
            val packageName = event.packageName?.toString() ?: "Unknown"

            if (text.isNotBlank() && !isExcluded(packageName)) {
                saveEntry(text, packageName)
            }
        }
    }

    override fun onInterrupt() {}
}
```

### How to get app label from package name

```kotlin
fun getAppLabel(packageName: String): String {
    return try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName
    }
}
```

---

## ✅ Summary

TypeVault for personal use is a **simple, lean Android app** with one job: silently capture everything you type using Android's `AccessibilityService`, organize it by app, and let you search/browse it anytime. No Play Store, no approvals, no complex architecture — just install the APK on your phone once, enable the service in Settings, and it works forever in the background.

---

_Version: 2.0 (Personal Use Edition) | Updated: June 2026_