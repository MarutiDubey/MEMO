package com.example.memo.data

import kotlinx.coroutines.flow.Flow

class EntryRepository(private val entryDao: EntryDao) {

    val distinctApps: Flow<List<AppFolder>> = entryDao.getDistinctApps()

    fun getEntriesForApp(packageName: String): Flow<List<EntryEntity>> {
        return entryDao.getEntriesForApp(packageName)
    }

    fun searchEntries(query: String): Flow<List<EntryEntity>> {
        return entryDao.searchEntries(query)
    }

    suspend fun insertEntry(entry: EntryEntity) {
        entryDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entryId: Int) {
        entryDao.deleteEntry(entryId)
    }

    suspend fun deleteAllEntries() {
        entryDao.deleteAllEntries()
    }

    suspend fun deleteEntriesOlderThan(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        entryDao.deleteEntriesOlderThan(cutoffTime)
    }
}

