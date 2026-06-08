package com.example.memo.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("memo_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CAPTURING_ENABLED = "capturing_enabled"
        const val KEY_EXCLUDED_APPS = "excluded_apps"
        const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    }

    var isCapturingEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAPTURING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CAPTURING_ENABLED, value).apply()

    var autoDeleteDays: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_DAYS, 0) // 0 = disabled
        set(value) = prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, value).apply()

    fun getExcludedApps(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet()) ?: emptySet()
    }

    fun addExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, current).apply()
    }

    fun removeExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, current).apply()
    }

    fun isExcluded(packageName: String): Boolean {
        return getExcludedApps().contains(packageName)
    }
}
