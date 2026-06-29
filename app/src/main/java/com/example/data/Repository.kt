package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class Repository(
    private val deviceDao: DeviceDao,
    private val measurementDao: MeasurementDao,
    private val alarmDao: AlarmDao,
    private val settingDao: SettingDao
) {
    // Devices
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    suspend fun getDeviceBySerialNumber(sn: String): DeviceEntity? {
        return deviceDao.getDeviceBySerialNumber(sn)
    }

    suspend fun insertDevice(device: DeviceEntity) {
        deviceDao.insertDevice(device)
    }

    suspend fun updateDevice(device: DeviceEntity) {
        deviceDao.updateDevice(device)
    }

    suspend fun deleteDevice(device: DeviceEntity) {
        deviceDao.deleteDevice(device)
    }

    suspend fun updateDeviceConnectionStatus(serialNumber: String, isConnected: Boolean) {
        deviceDao.updateDeviceConnectionStatus(serialNumber, isConnected)
    }

    suspend fun updateDeviceStatus(serialNumber: String, status: String, rssi: Int) {
        deviceDao.updateDeviceStatus(serialNumber, status, rssi)
    }

    // Measurements
    fun getMeasurementsForDevice(serialNumber: String): Flow<List<MeasurementEntity>> {
        return measurementDao.getMeasurementsForDevice(serialNumber)
    }

    fun getLatestMeasurements(serialNumber: String, limit: Int): Flow<List<MeasurementEntity>> {
        return measurementDao.getLatestMeasurements(serialNumber, limit)
    }

    fun getLatestMeasurement(serialNumber: String): Flow<MeasurementEntity?> {
        return measurementDao.getLatestMeasurement(serialNumber)
    }

    suspend fun getMeasurementsInTimeRange(serialNumber: String, startTime: Long, endTime: Long): List<MeasurementEntity> {
        return measurementDao.getMeasurementsInTimeRange(serialNumber, startTime, endTime)
    }

    suspend fun insertMeasurement(measurement: MeasurementEntity) {
        measurementDao.insertMeasurement(measurement)
    }

    suspend fun clearMeasurements(serialNumber: String) {
        measurementDao.deleteMeasurementsForDevice(serialNumber)
    }

    // Alarms
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    fun getAlarmsForDevice(serialNumber: String): Flow<List<AlarmEntity>> {
        return alarmDao.getAlarmsForDevice(serialNumber)
    }

    suspend fun insertAlarm(alarm: AlarmEntity) {
        alarmDao.insertAlarm(alarm)
    }

    suspend fun acknowledgeAlarm(id: Long) {
        alarmDao.acknowledgeAlarm(id)
    }

    suspend fun clearAlarm(id: Long) {
        alarmDao.clearAlarm(id)
    }

    // Settings
    val settings: Flow<SettingEntity?> = settingDao.getSettings()

    suspend fun saveSettings(settingsEntity: SettingEntity) {
        settingDao.insertSettings(settingsEntity)
    }
}
