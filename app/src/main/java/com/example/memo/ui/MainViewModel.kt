package com.example.memo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memo.data.AppDatabase
import com.example.memo.data.AppFolder
import com.example.memo.data.EntryEntity
import com.example.memo.data.EntryRepository
import com.example.memo.data.SettingsManager
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

    private val _autoDeleteDays = MutableStateFlow(0)
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EntryRepository(database.entryDao())
        settings = SettingsManager(application)

        // Load initial settings
        _isCapturing.value = settings.isCapturingEnabled
        _autoDeleteDays.value = settings.autoDeleteDays

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
}
