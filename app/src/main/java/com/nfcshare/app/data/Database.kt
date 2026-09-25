package com.nfcshare.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanHistory(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val kind: String, val summary: String, val json: String, val date: Long, val favorite: Boolean = false)
@Entity(tableName = "saved_tags")
data class SavedTag(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val kind: String, val summary: String, val json: String, val date: Long, val favorite: Boolean = false)
@Entity(tableName = "share_profiles")
data class ShareProfile(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val kind: String, val summary: String, val json: String, val date: Long, val favorite: Boolean = false)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM scan_history ORDER BY date DESC") fun history(): Flow<List<ScanHistory>>
    @Query("SELECT * FROM saved_tags ORDER BY date DESC") fun saved(): Flow<List<SavedTag>>
    @Query("SELECT * FROM share_profiles ORDER BY date DESC") fun profiles(): Flow<List<ShareProfile>>
    @Insert suspend fun insert(item: ScanHistory): Long
    @Insert suspend fun insert(item: SavedTag): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: ShareProfile): Long
    @Query("UPDATE scan_history SET name=:name, favorite=:favorite WHERE id=:id") suspend fun updateHistory(id: Long, name: String, favorite: Boolean)
    @Query("UPDATE saved_tags SET name=:name, favorite=:favorite WHERE id=:id") suspend fun updateSaved(id: Long, name: String, favorite: Boolean)
    @Query("UPDATE share_profiles SET name=:name, favorite=:favorite WHERE id=:id") suspend fun updateProfile(id: Long, name: String, favorite: Boolean)
    @Query("DELETE FROM scan_history WHERE id=:id") suspend fun deleteHistory(id: Long)
    @Query("DELETE FROM saved_tags WHERE id=:id") suspend fun deleteSaved(id: Long)
    @Query("DELETE FROM share_profiles WHERE id=:id") suspend fun deleteProfile(id: Long)
    @Query("DELETE FROM scan_history") suspend fun clearHistory()
}
@Database(entities = [ScanHistory::class, SavedTag::class, ShareProfile::class], version = 1, exportSchema = true)
abstract class NfcDatabase : RoomDatabase() { abstract fun library(): LibraryDao }
