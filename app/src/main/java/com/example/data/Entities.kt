package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val serialNumber: String,
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val rssi: Int,
    val firmwareVersion: String,
    val status: String, // "Online", "Offline"
    val username: String = "",
    val password: String = "",
    val autoReconnect: Boolean = true,
    val rememberCredentials: Boolean = true,
    val isConnected: Boolean = false
)

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceSerialNumber: String,
    val timestamp: Long,
    val batteryVoltage: Float,
    val batteryCurrent: Float,
    val batteryPower: Float,
    val soc: Int,
    val batteryTemperature: Float,
    val batteryStatus: String, // "Charging", "Discharging", "Idle"
    val batteryMode: String,
    val batteryHealth: Int, // e.g. 98%
    
    val pv1Voltage: Float,
    val pv1Current: Float,
    val pv1Power: Float,
    val pv2Voltage: Float,
    val pv2Current: Float,
    val pv2Power: Float,
    val totalSolarPower: Float,
    val dailySolarEnergy: Float,
    val monthlySolarEnergy: Float,
    val totalSolarEnergy: Float,
    
    val outputVoltage: Float,
    val outputCurrent: Float,
    val outputFrequency: Float,
    val outputPower: Float,
    val loadPercent: Int,
    val loadVA: Float,
    val loadW: Float,
    val outputMode: String,
    
    val gridVoltage: Float,
    val gridFrequency: Float,
    val gridStatus: String, // "Available", "Unavailable"
    val gridCurrent: Float,
    val gridImport: Float,
    val gridExport: Float,
    
    val inverterMode: String, // "Solar", "Grid", "Battery", "Line"
    val temperature: Float,
    val efficiency: Float,
    val fanStatus: String, // "On", "Off", "Low", "High"
    val alarmCode: String = "00",
    val warningCode: String = "00",
    val faultCode: String = "00",
    val runningTimeHours: Int = 0
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceSerialNumber: String,
    val timestamp: Long,
    val alarmMessage: String,
    val severity: String, // "INFO", "WARNING", "ALARM", "FAULT"
    val status: String, // "Active", "Cleared"
    val acknowledged: Boolean = false
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val id: String = "app_settings",
    val pollingIntervalSeconds: Int = 60,
    val csvSeparator: String = ",",
    val decimalSeparator: String = ".",
    val timezone: String = "UTC",
    val dateFormat: String = "yyyy-MM-dd",
    val darkTheme: Boolean = false,
    val language: String = "en"
)
