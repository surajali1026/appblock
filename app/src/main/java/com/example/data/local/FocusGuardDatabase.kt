package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val dateKey: String,
    val category: String, // SHORT_VIDEO, USAGE_LIMIT, ANTI_UNINSTALL
    val packageName: String,
    val appName: String,
    val detail: String
)

@Entity(tableName = "focus_tracks")
data class FocusTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    val modelUsed: String,
    val filePath: String,
    val mimeType: String,
    val durationLabel: String,
    val lyricsOrNotes: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface FocusGuardDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun observeAllLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    suspend fun getEnabledLimitsOnce(): List<AppLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)

    @Query("SELECT * FROM block_events ORDER BY timestampMs DESC LIMIT 50")
    fun observeRecentBlockEvents(): Flow<List<BlockEventEntity>>

    @Query("SELECT COUNT(*) FROM block_events WHERE dateKey = :dateKey")
    fun observeTodayBlockCount(dateKey: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE dateKey = :dateKey AND category = :category")
    fun observeTodayCategoryCount(dateKey: String, category: String): Flow<Int>

    @Insert
    suspend fun insertBlockEvent(event: BlockEventEntity)

    @Query("SELECT * FROM focus_tracks ORDER BY createdAtMs DESC")
    fun observeAllFocusTracks(): Flow<List<FocusTrackEntity>>

    @Insert
    suspend fun insertFocusTrack(track: FocusTrackEntity): Long

    @Query("DELETE FROM focus_tracks WHERE id = :trackId")
    suspend fun deleteFocusTrack(trackId: Long)
}

@Database(
    entities = [AppLimitEntity::class, BlockEventEntity::class, FocusTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FocusGuardDatabase : RoomDatabase() {
    abstract fun focusGuardDao(): FocusGuardDao

    companion object {
        @Volatile
        private var INSTANCE: FocusGuardDatabase? = null

        fun getInstance(context: Context): FocusGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusGuardDatabase::class.java,
                    "focusguard_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
