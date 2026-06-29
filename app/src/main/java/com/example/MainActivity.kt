package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }
}

enum class ScadaTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  DASHBOARD("Dashboard", Icons.Default.Dashboard),
  CHARTS("Charts", Icons.Default.ShowChart),
  HISTORY("History", Icons.Default.History),
  ALARMS("Alarms", Icons.Default.NotificationImportant),
  SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppContent() {
  val viewModel: MainViewModel = viewModel()
  val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
  val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()

  // App routing state machine
  var currentRoute by remember { mutableStateOf("main_flow") } // login, discovery, connection, main_flow
  var connectionTargetSN by remember { mutableStateOf("") }
  var selectedTab by remember { mutableStateOf(ScadaTab.DASHBOARD) }

  // Route routing checks
  LaunchedEffect(isLoggedIn, activeDevice) {
    if (!isLoggedIn) {
      currentRoute = "login"
    } else if (activeDevice == null) {
      currentRoute = "discovery"
    } else {
      currentRoute = "main_flow"
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      if (isLoggedIn && activeDevice != null && currentRoute == "main_flow") {
        NavigationBar(
          modifier = Modifier.testTag("scada_bottom_nav"),
          windowInsets = WindowInsets.navigationBars
        ) {
          ScadaTab.values().forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
              selected = selected,
              onClick = { selectedTab = tab },
              label = { Text(tab.title) },
              icon = { Icon(tab.icon, contentDescription = tab.title) },
              modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          bottom = if (isLoggedIn && activeDevice != null && currentRoute == "main_flow") innerPadding.calculateBottomPadding() else 0.dp
        )
    ) {
      when (currentRoute) {
        "login" -> {
          LoginScreen(viewModel = viewModel)
        }

        "discovery" -> {
          DiscoveryScreen(
            viewModel = viewModel,
            onNavigateToConnect = { sn ->
              connectionTargetSN = sn
              currentRoute = "connection"
            }
          )
        }

        "connection" -> {
          ConnectionScreen(
            serialNumber = connectionTargetSN,
            viewModel = viewModel,
            onNavigateBack = { currentRoute = "discovery" },
            onConnected = {
              currentRoute = "main_flow"
              selectedTab = ScadaTab.DASHBOARD
            }
          )
        }

        "main_flow" -> {
          Column(modifier = Modifier.fillMaxSize()) {
            ScadaHeader(
              viewModel = viewModel,
              onNavigateToDiscovery = { currentRoute = "discovery" }
            )

            Box(modifier = Modifier.weight(1f)) {
              when (selectedTab) {
                ScadaTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                ScadaTab.CHARTS -> ChartsScreen(viewModel = viewModel)
                ScadaTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                ScadaTab.ALARMS -> AlarmsScreen(viewModel = viewModel)
                ScadaTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }
  }
}

