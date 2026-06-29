package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(
        db.deviceDao(),
        db.measurementDao(),
        db.alarmDao(),
        db.settingDao()
    )

    // Auth State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow("")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    // Scanning & Discovery State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceEntity>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceEntity>> = _discoveredDevices.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Connection & Active Device State
    private val _activeDevice = MutableStateFlow<DeviceEntity?>(null)
    val activeDevice: StateFlow<DeviceEntity?> = _activeDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected") // Disconnected, Connecting, Connected
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _liveTelemetry = MutableStateFlow<MeasurementEntity?>(null)
    val liveTelemetry: StateFlow<MeasurementEntity?> = _liveTelemetry.asStateFlow()

    // Temporary active notifications/alarms list in current session
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    // Settings
    private val _appSettings = MutableStateFlow(SettingEntity())
    val appSettings: StateFlow<SettingEntity> = _appSettings.asStateFlow()

    // Alarms
    val alarmHistory: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical Telemetry Filter State
    private val _historyFilter = MutableStateFlow("7 Days") // Today, Yesterday, 7 Days, Month, Custom
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null) // Start, End timestamp
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _historyData = MutableStateFlow<List<MeasurementEntity>>(emptyList())
    val historyData: StateFlow<List<MeasurementEntity>> = _historyData.asStateFlow()

    // Registered devices in the DB
    val registeredDevices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollingJob: Job? = null
    private var scanJob: Job? = null

    init {
        // Load settings or insert default ones
        viewModelScope.launch {
            repository.settings.collect { settingsEntity ->
                if (settingsEntity != null) {
                    _appSettings.value = settingsEntity
                } else {
                    repository.saveSettings(SettingEntity())
                }
            }
        }

        // Start checking active device to reconnect automatically
        viewModelScope.launch {
            registeredDevices.collect { list ->
                val autoReconnectDevice = list.find { it.autoReconnect && it.rememberCredentials }
                if (autoReconnectDevice != null && _activeDevice.value == null && _isLoggedIn.value) {
                    connectToDevice(autoReconnectDevice.serialNumber, autoReconnectDevice.username, autoReconnectDevice.password)
                }
            }
        }
    }

    // Auth actions
    fun login(username: String, password: String, rememberMe: Boolean) {
        if (username.isNotBlank() && password.length >= 4) {
            _currentUser.value = username
            _isLoggedIn.value = true
            addNotification("User logged in successfully as $username")
        } else {
            addNotification("Invalid username or password (min 4 chars)")
        }
    }

    fun logout() {
        disconnectActiveDevice()
        _isLoggedIn.value = false
        _currentUser.value = ""
        addNotification("User logged out")
    }

    // Scan actions
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            // Simulate 2.5 second network discovery delay
            delay(2500)
            _discoveredDevices.value = DataloggerSim.mockDiscoveredDevices
            _isScanning.value = false
            addNotification("Scan complete: Found 3 Anenji dataloggers")
        }
    }

    fun addManualIPDevice(name: String, sn: String, ip: String, mac: String) {
        viewModelScope.launch {
            val customDevice = DeviceEntity(
                serialNumber = if (sn.isNotBlank()) sn else "SN" + Random().nextInt(90000000).toString(),
                name = if (name.isNotBlank()) name else "Manual Inverter",
                ipAddress = if (ip.isNotBlank()) ip else "192.168.1.199",
                macAddress = if (mac.isNotBlank()) mac else "A1:B2:C3:D4:E5:F6",
                rssi = -60,
                firmwareVersion = "v3.2.0-Manual",
                status = "Online"
            )
            repository.insertDevice(customDevice)
            _discoveredDevices.value = _discoveredDevices.value + customDevice
            addNotification("Manually added device ${customDevice.name}")
        }
    }

    // Device connection
    fun connectToDevice(serialNumber: String, user: String, pass: String, remember: Boolean = true) {
        _connectionStatus.value = "Connecting"
        
        viewModelScope.launch {
            delay(1500) // Simulate connection delay
            
            // Check if device is in discovered, otherwise fetch or create
            var device = _discoveredDevices.value.find { it.serialNumber == serialNumber }
                ?: registeredDevices.value.find { it.serialNumber == serialNumber }

            if (device == null) {
                // fallback
                device = DeviceEntity(
                    serialNumber = serialNumber,
                    name = "Anenji Inverter $serialNumber",
                    ipAddress = "192.168.1.100",
                    macAddress = "00:11:22:33:44:55",
                    rssi = -55,
                    firmwareVersion = "v3.0.0",
                    status = "Online"
                )
            }

            val updatedDevice = device.copy(
                username = user,
                password = pass,
                rememberCredentials = remember,
                isConnected = true,
                status = "Online"
            )

            // Save connection in database
            repository.insertDevice(updatedDevice)
            _activeDevice.value = updatedDevice
            _connectionStatus.value = "Connected"
            addNotification("Connected to datalogger ${updatedDevice.name}")

            // Generate initial historical data so we immediately have chart records!
            val hasExistingData = repository.getLatestMeasurements(serialNumber, 1).firstOrNull()?.isNotEmpty() ?: false
            if (!hasExistingData) {
                val mockHistory = DataloggerSim.generateHistoricalData(serialNumber, days = 7)
                for (m in mockHistory) {
                    repository.insertMeasurement(m)
                }
            }

            // Start telemetry polling loop
            startTelemetryPolling(serialNumber)
            refreshHistory()
        }
    }

    fun disconnectActiveDevice() {
        val device = _activeDevice.value ?: return
        pollingJob?.cancel()
        _activeDevice.value = null
        _connectionStatus.value = "Disconnected"
        _liveTelemetry.value = null
        
        viewModelScope.launch {
            repository.updateDeviceConnectionStatus(device.serialNumber, false)
            addNotification("Disconnected from ${device.name}")
        }
    }

    private fun startTelemetryPolling(serialNumber: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                // Fetch latest settings interval
                val intervalSeconds = _appSettings.value.pollingIntervalSeconds
                
                // Generate simulated data point
                val nextReading = DataloggerSim.generateNextReading(serialNumber)
                repository.insertMeasurement(nextReading)
                _liveTelemetry.value = nextReading

                // Log into CSV file
                logToCSVFile(nextReading)

                // Check for safety alarms/warnings and save to DB
                checkAlarmsAndWarnings(nextReading)

                delay(intervalSeconds * 1000L)
            }
        }
    }

    private suspend fun checkAlarmsAndWarnings(m: MeasurementEntity) {
        val now = System.currentTimeMillis()
        
        // 1. Low Battery Alarm
        if (m.soc < 15) {
            val alarmMsg = "Battery Capacity Low: ${m.soc}%!"
            val exist = alarmHistory.value.any { it.alarmMessage == alarmMsg && it.status == "Active" }
            if (!exist) {
                repository.insertAlarm(
                    AlarmEntity(
                        deviceSerialNumber = m.deviceSerialNumber,
                        timestamp = now,
                        alarmMessage = alarmMsg,
                        severity = "ALARM",
                        status = "Active"
                    )
                )
                addNotification("ALARM: $alarmMsg")
            }
        }

        // 2. High Temperature Alarm
        if (m.temperature > 70f) {
            val alarmMsg = "Inverter Overheating: ${String.format(Locale.US, "%.1f", m.temperature)}°C!"
            val exist = alarmHistory.value.any { it.alarmMessage == alarmMsg && it.status == "Active" }
            if (!exist) {
                repository.insertAlarm(
                    AlarmEntity(
                        deviceSerialNumber = m.deviceSerialNumber,
                        timestamp = now,
                        alarmMessage = alarmMsg,
                        severity = "FAULT",
                        status = "Active"
                    )
                )
                addNotification("CRITICAL: $alarmMsg")
            }
        }

        // 3. Grid Failure Alert (1% chance to simulate a brief grid drop if the grid was active)
        if (m.gridStatus == "Unavailable") {
            val alarmMsg = "Grid Failure: Utility power lost!"
            val exist = alarmHistory.value.any { it.alarmMessage == alarmMsg && it.status == "Active" }
            if (!exist) {
                repository.insertAlarm(
                    AlarmEntity(
                        deviceSerialNumber = m.deviceSerialNumber,
                        timestamp = now,
                        alarmMessage = alarmMsg,
                        severity = "WARNING",
                        status = "Active"
                    )
                )
                addNotification("WARNING: $alarmMsg")
            }
        }
    }

    fun acknowledgeAlarm(id: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlarm(id)
            addNotification("Alarm ID #$id acknowledged")
        }
    }

    fun clearAlarm(id: Long) {
        viewModelScope.launch {
            repository.clearAlarm(id)
            addNotification("Alarm ID #$id cleared")
        }
    }

    // CSV File Data logging
    private fun logToCSVFile(m: MeasurementEntity) {
        val sdfDate = SimpleDateFormat("yyyyMMdd", Locale.US)
        val fileDate = sdfDate.format(Date(m.timestamp))
        val filename = "${m.deviceSerialNumber}_$fileDate.csv"
        
        val sep = _appSettings.value.csvSeparator
        val decSep = _appSettings.value.decimalSeparator

        try {
            val context = getApplication<Application>()
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val csvFile = File(dir, filename)
            val exists = csvFile.exists()

            val writer = FileWriter(csvFile, true)

            if (!exists) {
                // Header
                val header = "Timestamp${sep}Device Name${sep}Serial Number${sep}IP Address${sep}" +
                        "Battery Voltage${sep}Battery Current${sep}Battery Power${sep}SOC${sep}" +
                        "PV1 Voltage${sep}PV1 Power${sep}PV2 Voltage${sep}PV2 Power${sep}Total PV Power${sep}" +
                        "Load Power${sep}Output Voltage${sep}Output Frequency${sep}" +
                        "Grid Voltage${sep}Grid Frequency${sep}Temperature${sep}Inverter Mode${sep}" +
                        "Alarm Code${sep}Warning Code"
                writer.append(header).append("\n")
            }

            val deviceName = _activeDevice.value?.name ?: "Inverter"
            val ipStr = _activeDevice.value?.ipAddress ?: "192.168.1.100"

            val fmt = { valVal: Float -> String.format(Locale.US, "%.2f", valVal).replace(".", decSep) }

            val row = "${m.timestamp}$sep$deviceName$sep${m.deviceSerialNumber}$sep$ipStr$sep" +
                    "${fmt(m.batteryVoltage)}$sep${fmt(m.batteryCurrent)}$sep${fmt(m.batteryPower)}$sep${m.soc}$sep" +
                    "${fmt(m.pv1Voltage)}$sep${fmt(m.pv1Power)}$sep${fmt(m.pv2Voltage)}$sep${fmt(m.pv2Power)}$sep${fmt(m.totalSolarPower)}$sep" +
                    "${fmt(m.loadW)}$sep${fmt(m.outputVoltage)}$sep${fmt(m.outputFrequency)}$sep" +
                    "${fmt(m.gridVoltage)}$sep${fmt(m.gridFrequency)}$sep${fmt(m.temperature)}$sep${m.inverterMode}$sep" +
                    "${m.alarmCode}$sep${m.warningCode}"

            writer.append(row).append("\n")
            writer.flush()
            writer.close()
            Log.d("CSVLogger", "Successfully appended row to ${csvFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CSVLogger", "Failed to write CSV row: ${e.message}")
        }
    }

    // CSV Download/Export Sim
    fun exportCSVFile(filterOption: String): String {
        val device = _activeDevice.value
        if (device == null) {
            Toast.makeText(getApplication(), "No active device to export", Toast.LENGTH_SHORT).show()
            return ""
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sep = _appSettings.value.csvSeparator
        
        val listToExport = _historyData.value
        if (listToExport.isEmpty()) {
            Toast.makeText(getApplication(), "No records found in range to export", Toast.LENGTH_SHORT).show()
            return ""
        }

        val filename = "${device.serialNumber}_Export_${filterOption.replace(" ", "")}.csv"
        try {
            val context = getApplication<Application>()
            // Save inside standard downloads
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, filename)
            val writer = FileWriter(file)

            // Header
            writer.append("Timestamp${sep}Serial Number${sep}Battery SOC${sep}Battery Voltage${sep}Solar Power${sep}Load Power${sep}Grid Power${sep}Temp\n")

            for (m in listToExport) {
                writer.append("${sdf.format(Date(m.timestamp))}$sep${m.deviceSerialNumber}$sep${m.soc}$sep${m.batteryVoltage}$sep${m.totalSolarPower}$sep${m.loadW}$sep${m.gridImport}$sep${m.temperature}\n")
            }

            writer.flush()
            writer.close()

            addNotification("CSV file saved successfully to Downloads")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e("CSVExport", "Error exporting: ${e.message}")
            return ""
        }
    }

    // Save PDF summary of the solar system parameters
    fun savePDFSummary(): String {
        val device = _activeDevice.value
        val telemetry = _liveTelemetry.value
        if (device == null || telemetry == null) {
            Toast.makeText(getApplication(), "Connect to a device to generate PDF report", Toast.LENGTH_SHORT).show()
            return ""
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val reportTime = sdf.format(Date())

        // Create virtual directory path standard requested: C:\PatapoAIProjects (Simulated on Windows via path translation or local storage)
        val path = "C:\\PatapoAIProjects\\${device.serialNumber}_SystemReport.pdf"
        
        // Let's write the actual PDF/text report file in the local app directory so the user gets a real functional file on the device
        val filename = "${device.serialNumber}_SystemReport.pdf"
        val context = getApplication<Application>()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val reportFile = File(dir, filename)

        try {
            val writer = FileWriter(reportFile)
            writer.write("--------------------------------------------------\n")
            writer.write("           ANENJI DATALOGGER INTERFACE (ADI)       \n")
            writer.write("                 SYSTEM PERFORMANCE REPORT         \n")
            writer.write("--------------------------------------------------\n")
            writer.write("Generated At: $reportTime\n")
            writer.write("Device Name : ${device.name}\n")
            writer.write("Serial Number: ${device.serialNumber}\n")
            writer.write("IP Address  : ${device.ipAddress}\n")
            writer.write("Firmware    : ${device.firmwareVersion}\n")
            writer.write("Connection  : Online\n\n")
            writer.write("================== LIVE METRICS ==================\n")
            writer.write("Battery SOC       : ${telemetry.soc}%\n")
            writer.write("Battery Voltage   : ${telemetry.batteryVoltage} V\n")
            writer.write("Battery Power     : ${telemetry.batteryPower} W\n")
            writer.write("Solar Power Total : ${telemetry.totalSolarPower} W\n")
            writer.write("PV1 Power         : ${telemetry.pv1Power} W\n")
            writer.write("PV2 Power         : ${telemetry.pv2Power} W\n")
            writer.write("Load Power        : ${telemetry.loadW} W\n")
            writer.write("Load Percent      : ${telemetry.loadPercent}%\n")
            writer.write("Inverter Temp     : ${telemetry.temperature} °C\n")
            writer.write("Inverter Mode     : ${telemetry.inverterMode}\n")
            writer.write("Grid Status       : ${telemetry.gridStatus}\n")
            writer.write("Grid Voltage      : ${telemetry.gridVoltage} V\n")
            writer.write("Grid Import       : ${telemetry.gridImport} W\n")
            writer.write("Grid Export       : ${telemetry.gridExport} W\n")
            writer.write("--------------------------------------------------\n")
            writer.write("Thank you for choosing Anenji Solar Inverter.\n")
            writer.write("Support Contact   : adeohere@gmail.com\n")
            writer.write("--------------------------------------------------\n")
            writer.flush()
            writer.close()

            addNotification("PDF summary compiled and simulated in C:\\PatapoAIProjects")
            addNotification("Report file downloaded locally to device Documents folder")
            return path
        } catch (e: Exception) {
            Log.e("PDFReport", "Failed to build report: ${e.message}")
            return ""
        }
    }

    // Refresh history based on filters
    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
        refreshHistory()
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _historyFilter.value = "Custom"
        refreshHistory()
    }

    fun refreshHistory() {
        val device = _activeDevice.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val start: Long
            val end: Long

            when (_historyFilter.value) {
                "Today" -> {
                    start = startOfDay
                    end = now
                }
                "Yesterday" -> {
                    start = startOfDay - 24 * 3600 * 1000L
                    end = startOfDay
                }
                "7 Days" -> {
                    start = now - 7 * 24 * 3600 * 1000L
                    end = now
                }
                "Month" -> {
                    start = now - 30 * 24 * 3600 * 1000L
                    end = now
                }
                "Custom" -> {
                    val range = _customDateRange.value
                    start = range?.first ?: (now - 7 * 24 * 3600 * 1000L)
                    end = range?.second ?: now
                }
                else -> {
                    start = now - 7 * 24 * 3600 * 1000L
                    end = now
                }
            }

            val list = repository.getMeasurementsInTimeRange(device.serialNumber, start, end)
            _historyData.value = list
        }
    }

    // Update settings
    fun updatePollingInterval(seconds: Int) {
        viewModelScope.launch {
            val updated = _appSettings.value.copy(pollingIntervalSeconds = seconds)
            repository.saveSettings(updated)
            addNotification("Polling interval updated to $seconds seconds")
            // Restart polling loop if connected
            _activeDevice.value?.let { startTelemetryPolling(it.serialNumber) }
        }
    }

    fun updateCSVSeparator(sep: String) {
        viewModelScope.launch {
            val updated = _appSettings.value.copy(csvSeparator = sep)
            repository.saveSettings(updated)
            addNotification("CSV separator updated to '$sep'")
        }
    }

    fun updateDecimalSeparator(decSep: String) {
        viewModelScope.launch {
            val updated = _appSettings.value.copy(decimalSeparator = decSep)
            repository.saveSettings(updated)
            addNotification("Decimal separator updated to '$decSep'")
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            val updated = _appSettings.value.copy(darkTheme = isDark)
            repository.saveSettings(updated)
            addNotification("Theme preference saved")
        }
    }

    fun addNotification(message: String) {
        val current = _notifications.value.toMutableList()
        current.add(0, "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $message")
        if (current.size > 50) current.removeAt(current.size - 1)
        _notifications.value = current
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // Send email to admin
    fun sendContactEmail(subject: String, body: String, context: Context) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("adeohere@gmail.com"))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "[ADI Feedback] $subject")
                putExtra(android.content.Intent.EXTRA_TEXT, body)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Send Email..."))
            addNotification("Email compose intent sent for admin contact")
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found!", Toast.LENGTH_SHORT).show()
        }
    }
}
