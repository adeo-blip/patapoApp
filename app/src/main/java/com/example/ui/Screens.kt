package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.InteractiveChart
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Universal SCADA Top bar with live server clock and active multi-device selector
@Composable
fun ScadaHeader(
    viewModel: MainViewModel,
    onNavigateToDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()
    val connStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val registeredList by viewModel.registeredDevices.collectAsStateWithLifecycle()
    var showDeviceSelector by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek Branding & Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1A237E), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analytics Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ADI",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeDevice?.let { "SN: ${it.serialNumber}" } ?: "Not Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Active Connection Badge & Quick Scan Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDevice != null) {
                        val statusBg = if (connStatus == "Connected") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        val statusText = if (connStatus == "Connected") Color(0xFF15803D) else Color(0xFFEF4444)
                        val dotColor = if (connStatus == "Connected") Color(0xFF22C55E) else Color(0xFFEF4444)

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = statusBg,
                            border = BorderStroke(1.dp, statusText.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .clickable { showDeviceSelector = true }
                                .testTag("active_device_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Text(
                                    text = if (connStatus == "Connected") "ONLINE" else "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusText,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Device",
                                    tint = statusText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onNavigateToDiscovery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("quick_connect_header_button")
                        ) {
                            Text("Connect Device", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.startScan() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Expanded dropdown list for multi-device swift switching
            if (showDeviceSelector && registeredList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Registered Solar Dataloggers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        registeredList.forEach { dev ->
                            val isCurrent = dev.serialNumber == activeDevice?.serialNumber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0xFF1A237E).copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable {
                                        showDeviceSelector = false
                                        viewModel.connectToDevice(dev.serialNumber, dev.username, dev.password, dev.rememberCredentials)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        dev.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "S/N: ${dev.serialNumber} | IP: ${dev.ipAddress}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = Color(0xFF1A237E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global SCADA footer with admin email reporting
@Composable
fun ScadaFooter(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var feedbackSubject by remember { mutableStateOf("") }
    var feedbackBody by remember { mutableStateOf("") }
    var showContactDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ADI Solar Monitoring System | Security Protected",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Need assistance? Contact admin at",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "adeohere@gmail.com",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScadaBlueAccent,
                    modifier = Modifier
                        .clickable { showContactDialog = true }
                        .testTag("contact_email_link")
                )
            }
        }
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact Solar Administrator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Send diagnostic feedback directly to the database administrator at adeohere@gmail.com.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = feedbackSubject,
                        onValueChange = { feedbackSubject = it },
                        label = { Text("Diagnostic Subject") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_subject_input")
                    )
                    OutlinedTextField(
                        value = feedbackBody,
                        onValueChange = { feedbackBody = it },
                        label = { Text("Description / Log context") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_body_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackSubject.isNotBlank() && feedbackBody.isNotBlank()) {
                            viewModel.sendContactEmail(feedbackSubject, feedbackBody, context)
                            showContactDialog = false
                        } else {
                            Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("send_email_button")
                ) {
                    Text("Compose Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 1. LOGIN SCREEN
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var user by remember { mutableStateOf("adeohere@gmail.com") }
    var pass by remember { mutableStateOf("admin1234") }
    var rememberMe by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Header Illustration
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF1A237E), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Solar Inverter Icon",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ADI",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1A237E)
                )
                Text(
                    text = "Anenji Datalogger Interface",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }

            // Login input card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "SCADA CONTROL CENTER LOGIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username or Admin Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Text("Remember control station", fontSize = 13.sp, color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = { viewModel.login(user, pass, rememberMe) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        Text("Login to Terminal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }

            ScadaFooter(viewModel = viewModel)
        }
    }
}

// 2. DEVICE DISCOVERY & SCANNING
@Composable
fun DiscoveryScreen(
    viewModel: MainViewModel,
    onNavigateToConnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val list by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showManualIPDialog by remember { mutableStateOf(false) }
    var mName by remember { mutableStateOf("") }
    var mSN by remember { mutableStateOf("") }
    var mIP by remember { mutableStateOf("192.168.1.100") }
    var mMAC by remember { mutableStateOf("AA:BB:CC:DD:EE:FF") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Datalogger Net Discovery",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1A237E)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan and discover nearby Anenji Wi-Fi Datalogger units connected on the same local network subnet.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startScan() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("scan_button"),
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Subnet...", color = Color.White)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Scan", color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = { showManualIPDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("manual_ip_button")
                    ) {
                        Text("Manual IP", color = Color(0xFF1A237E))
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search by serial, IP, or name") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = Color(0xFF64748B)) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_search_input")
        )

        // Loading animation indicator
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ScadaBlueAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "ADI subnet network scan running...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val filtered = list.filter {
                it.name.contains(search, ignoreCase = true) ||
                        it.serialNumber.contains(search, ignoreCase = true) ||
                        it.ipAddress.contains(search, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No dataloggers discovered yet.\nTry triggering a scan above.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Text(
                    "Discovered Units (${filtered.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )

                filtered.forEach { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = device.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "S/N: ${device.serialNumber}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Signal RSSI",
                                        tint = if (device.rssi > -60) ScadaGreenOk else ScadaOrangeWarn,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${device.rssi} dBm",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "IP: ${device.ipAddress}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        "MAC: ${device.macAddress}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        "Firmware: ${device.firmwareVersion}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Button(
                                    onClick = { onNavigateToConnect(device.serialNumber) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("connect_btn_${device.serialNumber}")
                                ) {
                                    Text("Connect", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        ScadaFooter(viewModel = viewModel)
    }

    if (showManualIPDialog) {
        AlertDialog(
            onDismissRequest = { showManualIPDialog = false },
            title = { Text("Add Manual Inverter IP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = mName,
                        onValueChange = { mName = it },
                        label = { Text("Device Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_name_input")
                    )
                    OutlinedTextField(
                        value = mSN,
                        onValueChange = { mSN = it },
                        label = { Text("Serial Number (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_sn_input")
                    )
                    OutlinedTextField(
                        value = mIP,
                        onValueChange = { mIP = it },
                        label = { Text("Inverter Gateway IP Address") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_ip_input")
                    )
                    OutlinedTextField(
                        value = mMAC,
                        onValueChange = { mMAC = it },
                        label = { Text("MAC Address") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_mac_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addManualIPDevice(mName, mSN, mIP, mMAC)
                        showManualIPDialog = false
                    },
                    modifier = Modifier.testTag("manual_ip_dialog_confirm")
                ) {
                    Text("Add Unit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualIPDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 3. DEVICE CONNECTION OPTIONS
@Composable
fun ConnectionScreen(
    serialNumber: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf(serialNumber) } // Default = Serial Number
    var password by remember { mutableStateOf("anenji2026") }
    var rememberCreds by remember { mutableStateOf(true) }
    var autoReconn by remember { mutableStateOf(true) }

    val status by viewModel.connectionStatus.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        if (status == "Connected") {
            onConnected()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1A237E).copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsInputHdmi,
                    contentDescription = null,
                    tint = Color(0xFF1A237E),
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Datalogger Authentication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A237E)
                )
                Text(
                    text = "Enter terminal password credentials for S/N: $serialNumber",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "DEVICE ACCESS SECURITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Device Username (Default = Serial No.)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_username_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Access Password") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberCreds,
                            onCheckedChange = { rememberCreds = it },
                            modifier = Modifier.testTag("remember_credentials_checkbox")
                        )
                        Text("Remember login credentials", fontSize = 13.sp, color = Color(0xFF64748B))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoReconn,
                            onCheckedChange = { autoReconn = it },
                            modifier = Modifier.testTag("auto_reconnect_checkbox")
                        )
                        Text("Automatically reconnect to station", fontSize = 13.sp, color = Color(0xFF64748B))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cancel_conn_button")
                        ) {
                            Text("Go Back", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.connectToDevice(serialNumber, username, password, rememberCreds)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("authenticate_device_button")
                        ) {
                            if (status == "Connecting") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Authenticate")
                            }
                        }
                    }
                }
            }

            ScadaFooter(viewModel = viewModel)
        }
    }
}

// 4. MAIN CONTAINER WITH LIVE DASHBOARD VIEWS
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()
    val device by viewModel.activeDevice.collectAsStateWithLifecycle()
    val connStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

    if (device == null || telemetry == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ScadaBlueSecondary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Active Solar Connection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Please navigate to Net Discovery to connect to an Anenji WiFi datalogger.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    val m = telemetry!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // System diagnostics card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ADI Live Terminal Node",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        device!!.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "S/N: ${device!!.serialNumber} | Firmware: ${device!!.firmwareVersion}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (connStatus == "Connected") ScadaGreenOk.copy(alpha = 0.15f) else ScadaRedAlarm.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = connStatus.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = if (connStatus == "Connected") ScadaGreenOk else ScadaRedAlarm,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Last sync: " + SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(m.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Row 1: Interactive Radial Battery SOC Gauge and Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circle Battery Gauge
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(200.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "BATTERY STATE (SOC)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        // Drawing Circle Radial ARC
                        val socColor = when {
                            m.soc > 50 -> ScadaGreenOk
                            m.soc > 20 -> ScadaOrangeWarn
                            else -> ScadaRedAlarm
                        }

                        Canvas(modifier = Modifier.size(100.dp)) {
                            // Back ring
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                radius = size.minDimension / 2 - 8.dp.toPx(),
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Progress sweep Arc
                            drawArc(
                                color = socColor,
                                startAngle = -90f,
                                sweepAngle = (m.soc / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${m.soc}%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = socColor
                            )
                            Text(
                                "Capacity",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        m.batteryStatus.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (m.batteryStatus == "Charging") ScadaGreenOk else if (m.batteryStatus == "Discharging") ScadaOrangeWarn else ScadaGreyIdle,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Battery details list
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "DIAGNOSTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailItem("Voltage", "${String.format(Locale.US, "%.1f", m.batteryVoltage)} V")
                        DetailItem("Current", "${String.format(Locale.US, "%.1f", m.batteryCurrent)} A")
                        DetailItem("Power", "${String.format(Locale.US, "%.0f", m.batteryPower)} W")
                        DetailItem("Temp", "${String.format(Locale.US, "%.1f", m.batteryTemperature)}°C")
                        DetailItem("Health", "${m.batteryHealth}% SOC")
                    }

                    Text(
                        text = "Mode: ${m.batteryMode}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScadaBlueAccent
                    )
                }
            }
        }

        // Section: Solar PV Panels Details
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.Yellow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Solar Panels (PV Array)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        "Total: ${String.format(Locale.US, "%.0f", m.totalSolarPower)} W",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ScadaBlueAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PV1 Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("PV1 Array", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScadaBlueSecondary)
                            DetailItem("Voltage", "${String.format(Locale.US, "%.1f", m.pv1Voltage)} V")
                            DetailItem("Current", "${String.format(Locale.US, "%.1f", m.pv1Current)} A")
                            DetailItem("Power", "${String.format(Locale.US, "%.0f", m.pv1Power)} W")
                        }
                    }

                    // PV2 Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("PV2 Array", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScadaBlueSecondary)
                            DetailItem("Voltage", "${String.format(Locale.US, "%.1f", m.pv2Voltage)} V")
                            DetailItem("Current", "${String.format(Locale.US, "%.1f", m.pv2Current)} A")
                            DetailItem("Power", "${String.format(Locale.US, "%.0f", m.pv2Power)} W")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Accumulators row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccumulatorItem("Daily", "${String.format(Locale.US, "%.2f", m.dailySolarEnergy)} kWh", Modifier.weight(1f))
                    AccumulatorItem("Monthly", "${String.format(Locale.US, "%.1f", m.monthlySolarEnergy)} kWh", Modifier.weight(1f))
                    AccumulatorItem("Total", "${String.format(Locale.US, "%.0f", m.totalSolarEnergy)} kWh", Modifier.weight(1.2f))
                }
            }
        }

        // Section: AC Output Load details
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AC Inverter Output & Load", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem("AC Output Voltage", "${String.format(Locale.US, "%.1f", m.outputVoltage)} V")
                        DetailItem("Output Frequency", "${String.format(Locale.US, "%.1f", m.outputFrequency)} Hz")
                        DetailItem("Active Load Power", "${String.format(Locale.US, "%.0f", m.outputPower)} W")
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Inverter Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(m.outputMode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScadaBlueAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Apparent Load", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("${String.format(Locale.US, "%.0f", m.loadVA)} VA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Load Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Load capacity utilization", fontSize = 11.sp)
                        Text("${m.loadPercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    LinearProgressIndicator(
                        progress = m.loadPercent / 100f,
                        color = if (m.loadPercent > 80) ScadaRedAlarm else if (m.loadPercent > 50) ScadaOrangeWarn else ScadaBlueAccent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Section: Grid Details
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Utility Utility Power Grid", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (m.gridStatus == "Available") ScadaGreenOk.copy(alpha = 0.15f) else ScadaRedAlarm.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = m.gridStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (m.gridStatus == "Available") ScadaGreenOk else ScadaRedAlarm,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem("Grid Voltage", "${String.format(Locale.US, "%.1f", m.gridVoltage)} V")
                        DetailItem("Grid Frequency", "${String.format(Locale.US, "%.1f", m.gridFrequency)} Hz")
                        DetailItem("Current Draw", "${String.format(Locale.US, "%.1f", m.gridCurrent)} A")
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem("Grid Import", "${String.format(Locale.US, "%.0f", m.gridImport)} W")
                        DetailItem("Grid Export", "${String.format(Locale.US, "%.0f", m.gridExport)} W")
                        DetailItem("Net Flow", "${String.format(Locale.US, "%.0f", m.gridExport - m.gridImport)} W")
                    }
                }
            }
        }

        // Section: Inverter stats
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Inverter Physical Health Diagnostics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem("Inverter Mode", m.inverterMode)
                        DetailItem("Internal Temp", "${String.format(Locale.US, "%.1f", m.temperature)} °C")
                        DetailItem("Cooling Fan Status", m.fanStatus)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem("Conversion Efficiency", "${String.format(Locale.US, "%.1f", m.efficiency)} %")
                        DetailItem("Alarms Triggered", if (m.alarmCode == "00") "None (00)" else "Active (${m.alarmCode})")
                        DetailItem("System Running Time", "${m.runningTimeHours} Hours")
                    }
                }
            }
        }

        ScadaFooter(viewModel = viewModel)
    }
}

// 5. CHARTS VIEWS (TABBED INTERACTIVE PLOTS)
@Composable
fun ChartsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyData.collectAsStateWithLifecycle()
    val device by viewModel.activeDevice.collectAsStateWithLifecycle()
    val filterOpt by viewModel.historyFilter.collectAsStateWithLifecycle()

    var activeChartTab by remember { mutableStateOf(0) }

    if (device == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Connect to a datalogger first to display trends.")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Solar Trend Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1A237E)
        )

        // Filters selector row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Today", "Yesterday", "7 Days", "Month").forEach { opt ->
                    val selected = opt == filterOpt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ScadaBlueAccent else Color.Transparent)
                            .clickable { viewModel.setHistoryFilter(opt) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Charts tabs row
        ScrollableTabRow(
            selectedTabIndex = activeChartTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabs = listOf("Battery SOC", "Solar Generation", "AC Load Draw", "Grid Feed", "Core Temp")
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = activeChartTab == idx,
                    onClick = { activeChartTab = idx },
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Interactive Chart display card
        when (activeChartTab) {
            0 -> {
                InteractiveChart(
                    data = history,
                    metricSelector = { it.soc.toFloat() },
                    metricName = "Battery State of Charge",
                    metricUnit = "%",
                    lineColor = ScadaGreenOk
                )
            }
            1 -> {
                InteractiveChart(
                    data = history,
                    metricSelector = { it.totalSolarPower },
                    metricName = "Total PV Output Power",
                    metricUnit = "W",
                    lineColor = Color.Yellow
                )
            }
            2 -> {
                InteractiveChart(
                    data = history,
                    metricSelector = { it.loadW },
                    metricName = "Inverter AC Load Consumption",
                    metricUnit = "W",
                    lineColor = ScadaOrangeWarn
                )
            }
            3 -> {
                InteractiveChart(
                    data = history,
                    metricSelector = { it.gridImport - it.gridExport },
                    metricName = "Utility Net Energy Flow",
                    metricUnit = "W",
                    lineColor = ScadaBlueAccent
                )
            }
            4 -> {
                InteractiveChart(
                    data = history,
                    metricSelector = { it.temperature },
                    metricName = "Inverter Internal Temperature",
                    metricUnit = "°C",
                    lineColor = ScadaRedAlarm
                )
            }
        }

        ScadaFooter(viewModel = viewModel)
    }
}

// 6. HISTORICAL DATA VIEWER & CSV EXPORTER
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyData.collectAsStateWithLifecycle()
    val filterOpt by viewModel.historyFilter.collectAsStateWithLifecycle()
    val device by viewModel.activeDevice.collectAsStateWithLifecycle()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (device == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Activate a solar datalogger connection to monitor history logs.")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CSV Logging Registry",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A237E)
            )

            // PDF button
            Button(
                onClick = {
                    val reportPath = viewModel.savePDFSummary()
                    if (reportPath.isNotEmpty()) {
                        Toast.makeText(context, "System summary PDF compiled in locally", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                modifier = Modifier.testTag("pdf_summary_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate PDF", fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CSV Export Trigger
            Button(
                onClick = {
                    val path = viewModel.exportCSVFile(filterOpt)
                    if (path.isNotEmpty()) {
                        Toast.makeText(context, "CSV exported successfully:\n$path", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("csv_export_trigger")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Local CSV")
            }

            OutlinedButton(
                onClick = { viewModel.refreshHistory() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("refresh_history_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync records")
            }
        }

        // Search records summary
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Filter Interval", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(filterOpt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Records Found", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${history.size} points", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScadaBlueAccent)
                }
            }
        }

        // Table
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Timestamp", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                        Text("SOC", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                        Text("Solar W", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text("Load W", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history measurements logged in range", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(history) { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(record.timestamp)),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = "${record.soc}%",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.Center,
                                color = if (record.soc < 20) ScadaRedAlarm else ScadaGreenOk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f", record.totalSolarPower)}W",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f", record.loadW)}W",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        ScadaFooter(viewModel = viewModel)
    }
}

// 7. ALARM CENTER
@Composable
fun AlarmsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarmHistory.collectAsStateWithLifecycle()
    val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SCADA Diagnostic Alarm Center",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1A237E)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System wide warning triggers, overheat alarms, and physical safety fault registries are logged dynamically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (alarms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All clear. No alarms logged in database.", color = ScadaGreenOk, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    items(alarms) { alarm ->
                        val cardColor = when (alarm.severity) {
                            "ALARM" -> ScadaOrangeWarn.copy(alpha = 0.1f)
                            "FAULT" -> ScadaRedAlarm.copy(alpha = 0.1f)
                            else -> ScadaBlueAccent.copy(alpha = 0.1f)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        alarm.severity,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (alarm.severity == "FAULT") ScadaRedAlarm else ScadaOrangeWarn,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Text(
                                        SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(alarm.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    alarm.alarmMessage,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Status: ${alarm.status}",
                                        fontSize = 12.sp,
                                        color = if (alarm.status == "Cleared") ScadaGreenOk else ScadaRedAlarm,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!alarm.acknowledged) {
                                            TextButton(
                                                onClick = { viewModel.acknowledgeAlarm(alarm.id) },
                                                modifier = Modifier.testTag("ack_alarm_${alarm.id}")
                                            ) {
                                                Text("Acknowledge", fontSize = 11.sp)
                                            }
                                        }

                                        if (alarm.status == "Active") {
                                            TextButton(
                                                onClick = { viewModel.clearAlarm(alarm.id) },
                                                modifier = Modifier.testTag("clear_alarm_${alarm.id}")
                                            ) {
                                                Text("Clear", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ScadaFooter(viewModel = viewModel)
    }
}

// 8. SETTINGS CONFIGURATOR SCREEN
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Station Settings Panel",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1A237E)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "ADI SYSTEM OPERATIONAL PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Polling Interval slider
                Column {
                    Text(
                        "Data polling interval: ${settings.pollingIntervalSeconds} seconds",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(5, 10, 30, 60, 120, 300).forEach { sec ->
                            val active = settings.pollingIntervalSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) Color(0xFF1A237E) else MaterialTheme.colorScheme.background)
                                    .clickable { viewModel.updatePollingInterval(sec) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${sec}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Divider()

                // CSV separator config
                Column {
                    Text("CSV Columns Separator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.csvSeparator == ",",
                                onClick = { viewModel.updateCSVSeparator(",") },
                                modifier = Modifier.testTag("csv_comma_radio")
                            )
                            Text("Comma (,)", fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.csvSeparator == ";",
                                onClick = { viewModel.updateCSVSeparator(";") },
                                modifier = Modifier.testTag("csv_semicolon_radio")
                            )
                            Text("Semicolon (;)", fontSize = 13.sp)
                        }
                    }
                }

                Divider()

                // Decimal separator config
                Column {
                    Text("Decimal Format Rule", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.decimalSeparator == ".",
                                onClick = { viewModel.updateDecimalSeparator(".") },
                                modifier = Modifier.testTag("dec_dot_radio")
                            )
                            Text("Dot (.)", fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.decimalSeparator == ",",
                                onClick = { viewModel.updateDecimalSeparator(",") },
                                modifier = Modifier.testTag("dec_comma_radio")
                            )
                            Text("Comma (,)", fontSize = 13.sp)
                        }
                    }
                }

                Divider()

                // Timezone info
                DetailItem("System Timezone", settings.timezone)
                DetailItem("Export Date Format", settings.dateFormat)
                DetailItem("Language Translation", "English (en) / Standard")
            }
        }

        ScadaFooter(viewModel = viewModel)
    }
}

// Visual reusable row helpers
@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AccumulatorItem(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScadaBlueSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
