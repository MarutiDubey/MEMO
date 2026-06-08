package com.example.memo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insertEntry(entry: EntryEntity)

    @Query("SELECT DISTINCT appName, packageName, COUNT(id) as entryCount FROM entries GROUP BY appName, packageName ORDER BY MAX(timestamp) DESC")
    fun getDistinctApps(): Flow<List<AppFolder>>

    @Query("SELECT * FROM entries WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getEntriesForApp(packageName: String): Flow<List<EntryEntity>>

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Int)

    @Query("SELECT * FROM entries WHERE typedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchEntries(query: String): Flow<List<EntryEntity>>

    @Query("DELETE FROM entries WHERE timestamp < :cutoffTime")
    suspend fun deleteEntriesOlderThan(cutoffTime: Long)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()
}

data class AppFolder(
    val appName: String,
    val packageName: String,
    val entryCount: Int
)
