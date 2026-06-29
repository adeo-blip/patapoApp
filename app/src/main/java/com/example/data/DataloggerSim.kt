package com.example.data

import java.util.Calendar
import java.util.Date
import kotlin.math.sin
import kotlin.random.Random

object DataloggerSim {

    // Mock discovered devices
    val mockDiscoveredDevices = listOf(
        DeviceEntity(
            serialNumber = "SN12345678",
            name = "Main Solar Inverter",
            ipAddress = "192.168.1.100",
            macAddress = "E4:F4:C6:18:A2:9B",
            rssi = -52,
            firmwareVersion = "v3.0.2-Anenji",
            status = "Online"
        ),
        DeviceEntity(
            serialNumber = "SN98724105",
            name = "Backup Pool Inverter",
            ipAddress = "192.168.1.115",
            macAddress = "E4:F4:C6:19:D4:5C",
            rssi = -75,
            firmwareVersion = "v2.4.11-Anenji",
            status = "Online"
        ),
        DeviceEntity(
            serialNumber = "SN55124106",
            name = "Garage Battery Bank",
            ipAddress = "192.168.1.154",
            macAddress = "D8:A0:1D:C4:F2:1E",
            rssi = -42,
            firmwareVersion = "v3.1.0-Anenji",
            status = "Online"
        )
    )

    // State holders for smoothly evolving values
    private val socStates = mutableMapOf<String, Int>()
    private val totalSolarStates = mutableMapOf<String, Float>()
    private val dailySolarStates = mutableMapOf<String, Float>()
    private val monthlySolarStates = mutableMapOf<String, Float>()
    private val hourlyCounter = mutableMapOf<String, Int>()

    fun getInitialSOC(serialNumber: String): Int {
        return socStates.getOrPut(serialNumber) { Random.nextInt(45, 85) }
    }

    fun generateNextReading(
        serialNumber: String,
        timestamp: Long = System.currentTimeMillis()
    ): MeasurementEntity {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        // Time of day solar factors: peak at 13:00 (1.0), zero between 19:00 and 05:00
        val timeFactor = if (hour in 6..18) {
            val angle = Math.PI * (hour - 6 + (minute / 60.0)) / 12.0
            sin(angle).toFloat().coerceAtLeast(0f)
        } else {
            0f
        }

        // 1. Solar generation
        val basePv1Power = 1500f * timeFactor * (0.9f + Random.nextFloat() * 0.2f)
        val pv1Voltage = if (timeFactor > 0) 180f + 40f * timeFactor + Random.nextFloat() * 5f else 0f
        val pv1Current = if (pv1Voltage > 0) basePv1Power / pv1Voltage else 0f
        val pv1Power = pv1Current * pv1Voltage

        val basePv2Power = 1300f * timeFactor * (0.85f + Random.nextFloat() * 0.3f)
        val pv2Voltage = if (timeFactor > 0) 170f + 35f * timeFactor + Random.nextFloat() * 5f else 0f
        val pv2Current = if (pv2Voltage > 0) basePv2Power / pv2Voltage else 0f
        val pv2Power = pv2Current * pv2Voltage

        val totalSolarPower = pv1Power + pv2Power

        // Energy increment
        val stepHours = 60.0 / 3600.0 // Assuming updated regularly
        val currentDailySolar = dailySolarStates.getOrDefault(serialNumber, 4.2f) + (totalSolarPower / 1000f) * stepHours.toFloat()
        val currentMonthlySolar = monthlySolarStates.getOrDefault(serialNumber, 150.5f) + (totalSolarPower / 1000f) * stepHours.toFloat()
        val currentTotalSolar = totalSolarStates.getOrDefault(serialNumber, 4250.0f) + (totalSolarPower / 1000f) * stepHours.toFloat()

        dailySolarStates[serialNumber] = currentDailySolar
        monthlySolarStates[serialNumber] = currentMonthlySolar
        totalSolarStates[serialNumber] = currentTotalSolar

        // 2. Load consumption
        // Base load 250W. Peaks in morning (7-9) and evening (17-21).
        val hourPeakFactor = when (hour) {
            in 7..9 -> 2.5f
            in 17..21 -> 3.2f
            else -> 1.0f
        }
        val loadW = (300f * hourPeakFactor + Random.nextInt(0, 150))
        val loadPercent = ((loadW / 3000f) * 100).toInt().coerceIn(5, 95)
        val loadVA = loadW * 1.1f // Apparent power is slightly higher

        // 3. Battery System
        var currentSOC = socStates.getOrDefault(serialNumber, getInitialSOC(serialNumber))
        
        // Solar charging power minus load power
        val netPower = totalSolarPower - loadW
        val batteryCurrent = (netPower / 48f) * 0.9f // with efficiency loss
        val batteryPower = batteryCurrent * 48f

        // Evolve SOC based on battery current
        val socIncrementChance = Random.nextFloat()
        if (batteryCurrent > 10f) {
            if (socIncrementChance < 0.15 && currentSOC < 100) {
                currentSOC += 1
            }
        } else if (batteryCurrent < -10f) {
            if (socIncrementChance < 0.15 && currentSOC > 0) {
                currentSOC -= 1
            }
        } else {
            // Idle drift
            if (socIncrementChance < 0.02) {
                if (Random.nextBoolean() && currentSOC < 100) currentSOC += 1
                else if (currentSOC > 0) currentSOC -= 1
            }
        }
        socStates[serialNumber] = currentSOC

        val batteryVoltage = 44.0f + (currentSOC / 100f) * 10.0f + (batteryCurrent * 0.02f)
        val batteryTemperature = 28f + (currentSOC / 100f) * 5f + (Math.abs(batteryCurrent) / 10f) + Random.nextFloat()
        val batteryStatus = when {
            batteryCurrent > 1f -> "Charging"
            batteryCurrent < -1f -> "Discharging"
            else -> "Idle"
        }
        val batteryMode = if (currentSOC > 20) "Normal" else "Eco-Save"
        val batteryHealth = 98

        // 4. AC Output
        val outputVoltage = 230f + Random.nextFloat() * 1.5f - 0.75f
        val outputCurrent = loadW / outputVoltage
        val outputFrequency = 50f + Random.nextFloat() * 0.05f - 0.025f
        val outputPower = loadW
        val outputMode = if (totalSolarPower > 100f || currentSOC > 25) "Inverter Mode" else "Line Mode"

        // 5. Grid connection
        val isGridAvailable = true
        val gridVoltage = if (isGridAvailable) 230f + Random.nextFloat() * 2f - 1f else 0f
        val gridFrequency = if (isGridAvailable) 50f + Random.nextFloat() * 0.06f - 0.03f else 0f
        val gridStatus = if (isGridAvailable) "Available" else "Unavailable"
        
        // If solar + battery cannot supply load, import from grid
        val gridImport = if (netPower < 0 && currentSOC < 20) Math.abs(netPower) else 0f
        // If battery is fully charged (SOC > 95) and we have surplus solar, export to grid
        val gridExport = if (netPower > 0 && currentSOC > 95) netPower * 0.8f else 0f
        val gridCurrent = (gridImport + gridExport) / 230f

        // 6. Inverter Mode and Temp
        val inverterMode = when {
            netPower > 100f && currentSOC < 95 -> "Solar"
            netPower < 0f && currentSOC >= 20 -> "Battery"
            gridImport > 0f -> "Grid"
            else -> "Line"
        }
        val temperature = 32f + (loadW / 3000f) * 20f + (totalSolarPower / 3000f) * 10f + Random.nextFloat() * 2f
        val efficiency = 94.2f + (loadPercent / 100f) * 2f - (loadPercent * loadPercent / 10000f) * 3f
        val fanStatus = when {
            temperature > 55f -> "High"
            temperature > 42f -> "Medium"
            temperature > 32f -> "Low"
            else -> "Off"
        }

        // Codes
        val alarmCode = if (currentSOC < 15) "04" else if (temperature > 72f) "02" else "00"
        val warningCode = if (currentSOC in 15..25) "12" else if (temperature in 62f..72f) "08" else "00"
        val faultCode = "00"

        val runningHours = hourlyCounter.getOrDefault(serialNumber, 1280)
        if (second == 0 && minute == 0 && Random.nextFloat() < 0.2f) {
            hourlyCounter[serialNumber] = runningHours + 1
        }

        return MeasurementEntity(
            deviceSerialNumber = serialNumber,
            timestamp = timestamp,
            batteryVoltage = batteryVoltage,
            batteryCurrent = batteryCurrent,
            batteryPower = batteryPower,
            soc = currentSOC,
            batteryTemperature = batteryTemperature,
            batteryStatus = batteryStatus,
            batteryMode = batteryMode,
            batteryHealth = batteryHealth,
            pv1Voltage = pv1Voltage,
            pv1Current = pv1Current,
            pv1Power = pv1Power,
            pv2Voltage = pv2Voltage,
            pv2Current = pv2Current,
            pv2Power = pv2Power,
            totalSolarPower = totalSolarPower,
            dailySolarEnergy = currentDailySolar,
            monthlySolarEnergy = currentMonthlySolar,
            totalSolarEnergy = currentTotalSolar,
            outputVoltage = outputVoltage,
            outputCurrent = outputCurrent,
            outputFrequency = outputFrequency,
            outputPower = outputPower,
            loadPercent = loadPercent,
            loadVA = loadVA,
            loadW = loadW,
            outputMode = outputMode,
            gridVoltage = gridVoltage,
            gridFrequency = gridFrequency,
            gridStatus = gridStatus,
            gridCurrent = gridCurrent,
            gridImport = gridImport,
            gridExport = gridExport,
            inverterMode = inverterMode,
            temperature = temperature,
            efficiency = efficiency,
            fanStatus = fanStatus,
            alarmCode = alarmCode,
            warningCode = warningCode,
            faultCode = faultCode,
            runningTimeHours = runningHours
        )
    }

    // Helper to generate some historical values for a device for the charts/history screens!
    fun generateHistoricalData(serialNumber: String, days: Int = 7): List<MeasurementEntity> {
        val list = mutableListOf<MeasurementEntity>()
        val currentMills = System.currentTimeMillis()
        // Generate values every 1 hour (for charts) or 5 minutes.
        // Let's generate data points spaced by 1 hour to have a realistic trend.
        val intervalMs = 3600_000L
        val points = days * 24
        
        var initialSOC = getInitialSOC(serialNumber)
        socStates[serialNumber] = initialSOC
        
        for (i in points downTo 1) {
            val t = currentMills - (i * intervalMs)
            list.add(generateNextReading(serialNumber, t))
        }
        return list
    }
}
