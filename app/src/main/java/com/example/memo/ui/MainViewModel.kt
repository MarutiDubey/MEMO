package com.example.memo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memo.data.AppDatabase
import com.example.memo.data.AppFolder
import com.example.memo.data.EntryEntity
import com.example.memo.data.EntryRepository
import com.example.memo.data.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EntryRepository
    val settings: SettingsManager

    private val _distinctApps = MutableStateFlow<List<AppFolder>>(emptyList())
    val distinctApps: StateFlow<List<AppFolder>> = _distinctApps.asStateFlow()

    private val _currentAppEntries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val currentAppEntries: StateFlow<List<EntryEntity>> = _currentAppEntries.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<EntryEntity>>(emptyList())
    val searchResults: StateFlow<List<EntryEntity>> = _searchResults.asStateFlow()

    // Settings state
    private val _isCapturing = MutableStateFlow(true)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _includedApps = MutableStateFlow<Set<String>>(emptySet())
    val includedApps: StateFlow<Set<String>> = _includedApps.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(0)
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EntryRepository(database.entryDao())
        settings = SettingsManager(application)

        // Load initial settings
        _isCapturing.value = settings.isCapturingEnabled
        _includedApps.value = settings.getIncludedApps()
        _autoDeleteDays.value = settings.autoDeleteDays

        loadInstalledApps(application)

        viewModelScope.launch {
            repository.distinctApps.collect { apps ->
                _distinctApps.value = apps
            }
        }

        // Reactively search when query changes
        viewModelScope.launch {
            _searchQuery.debounce(300).collectLatest { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                } else {
                    repository.searchEntries(query).collect { results ->
                        _searchResults.value = results
                    }
                }
            }
        }

        // Run auto-delete on startup if configured
        runAutoDelete()
    }

    fun loadEntriesForApp(packageName: String) {
        viewModelScope.launch {
            repository.getEntriesForApp(packageName).collect { entries ->
                _currentAppEntries.value = entries
            }
        }
    }

    fun deleteEntry(entryId: Int) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Settings Actions ---

    fun setCapturingEnabled(enabled: Boolean) {
        settings.isCapturingEnabled = enabled
        _isCapturing.value = enabled
    }

    fun addIncludedApp(packageName: String) {
        settings.addIncludedApp(packageName)
        _includedApps.value = settings.getIncludedApps()
    }

    fun removeIncludedApp(packageName: String) {
        settings.removeIncludedApp(packageName)
        _includedApps.value = settings.getIncludedApps()
    }

    fun setAutoDeleteDays(days: Int) {
        settings.autoDeleteDays = days
        _autoDeleteDays.value = days
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllEntries()
        }
    }

    private fun runAutoDelete() {
        val days = settings.autoDeleteDays
        if (days > 0) {
            viewModelScope.launch {
                repository.deleteEntriesOlderThan(days)
            }
        }
    }

    private fun loadInstalledApps(application: Application) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = application.packageManager
            val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val apps = packages.filter { 
                pm.getLaunchIntentForPackage(it.packageName) != null 
            }.map {
                InstalledAppInfo(
                    appName = pm.getApplicationLabel(it).toString(),
                    packageName = it.packageName
                )
            }.sortedBy { it.appName.lowercase() }
            
            _installedApps.value = apps
        }
    }
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)
