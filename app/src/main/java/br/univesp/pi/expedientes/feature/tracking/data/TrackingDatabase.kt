package br.univesp.pi.expedientes.feature.tracking.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(
    tableName = "pending_track_points",
    indices = [Index(value = ["shiftId", "recordedAtMillis"], unique = true)],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: String,
    val recordedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val speedMs: Double?,
)

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: TrackPointEntity): Long

    @Query("SELECT * FROM pending_track_points ORDER BY recordedAtMillis, id LIMIT :limit")
    suspend fun oldest(limit: Int): List<TrackPointEntity>

    @Query("SELECT * FROM pending_track_points WHERE shiftId = :shiftId ORDER BY recordedAtMillis, id LIMIT :limit")
    suspend fun oldestForShift(
        shiftId: String,
        limit: Int,
    ): List<TrackPointEntity>

    @Query("DELETE FROM pending_track_points WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pending_track_points WHERE shiftId = :shiftId")
    suspend fun count(shiftId: String): Int

    @Query("SELECT COUNT(*) FROM pending_track_points")
    suspend fun countAll(): Int

    @Query("DELETE FROM pending_track_points")
    suspend fun clear()
}

@Database(entities = [TrackPointEntity::class], version = 1, exportSchema = false)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
}
