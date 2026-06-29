package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE serialNumber = :serialNumber")
    suspend fun getDeviceBySerialNumber(serialNumber: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("UPDATE devices SET isConnected = :isConnected WHERE serialNumber = :serialNumber")
    suspend fun updateDeviceConnectionStatus(serialNumber: String, isConnected: Boolean)

    @Query("UPDATE devices SET status = :status, rssi = :rssi WHERE serialNumber = :serialNumber")
    suspend fun updateDeviceStatus(serialNumber: String, status: String, rssi: Int)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE deviceSerialNumber = :serialNumber ORDER BY timestamp DESC")
    fun getMeasurementsForDevice(serialNumber: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE deviceSerialNumber = :serialNumber ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestMeasurements(serialNumber: String, limit: Int): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE deviceSerialNumber = :serialNumber AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getMeasurementsInTimeRange(serialNumber: String, startTime: Long, endTime: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE deviceSerialNumber = :serialNumber ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMeasurement(serialNumber: String): Flow<MeasurementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE deviceSerialNumber = :serialNumber")
    suspend fun deleteMeasurementsForDevice(serialNumber: String)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY timestamp DESC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE deviceSerialNumber = :serialNumber ORDER BY timestamp DESC")
    fun getAlarmsForDevice(serialNumber: String): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeAlarm(id: Long)

    @Query("UPDATE alarms SET status = 'Cleared' WHERE id = :id")
    suspend fun clearAlarm(id: Long)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE id = 'app_settings'")
    fun getSettings(): Flow<SettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingEntity)
}
