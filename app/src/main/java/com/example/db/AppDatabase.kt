package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "battery_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chargeLevel: Int,
    val status: String,
    val temperature: Double,
    val voltage: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "test_records")
data class TestRecord(
    @PrimaryKey val testId: String,
    val testName: String,
    val isPassed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT 50")
    fun getBatteryLogs(): Flow<List<BatteryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatteryLog(log: BatteryLog)

    @Query("SELECT * FROM test_records ORDER BY timestamp DESC")
    fun getTestRecords(): Flow<List<TestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestRecord(record: TestRecord)

    @Query("DELETE FROM test_records")
    suspend fun clearTestRecords()
}

@Database(entities = [BatteryLog::class, TestRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
