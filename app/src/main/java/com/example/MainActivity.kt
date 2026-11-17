package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.PinVerificationDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UsageLimitsScreen
import com.example.ui.theme.DeepEmeraldContainer
import com.example.ui.theme.FocusGuardTheme
import com.example.ui.theme.LuminousEmerald
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.FocusGuardViewModel
import com.example.ui.viewmodel.PinActionPurpose
import com.example.usage.BatteryOptimizationHelper
import com.example.usage.UsageStatsHelper

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_LIMITS = "limits"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val NAV_ITEMS = listOf(
    NavDestinationItem(
        route = ROUTE_DASHBOARD,
        labelResId = R.string.nav_shield,
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        tag = "nav_tab_shield"
    ),
    NavDestinationItem(
        route = ROUTE_LIMITS,
        labelResId = R.string.nav_limits,
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        tag = "nav_tab_limits"
    ),
    NavDestinationItem(
        route = ROUTE_SETTINGS,
        labelResId = R.string.nav_vault,
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock,
        tag = "nav_tab_vault"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusGuardTheme {
                FocusGuardApp()
            }
        }
    }
}

@Composable
private fun FocusGuardApp(
    viewModel: FocusGuardViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val health by viewModel.permissionHealth.collectAsStateWithLifecycle()
    val settings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val appLimits by viewModel.appLimits.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val dailyUsageMap by viewModel.dailyUsageMap.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentBlockEvents.collectAsStateWithLifecycle()
    val todayBlockCount by viewModel.todayBlockCount.collectAsStateWithLifecycle()
    val pinDialogState by viewModel.pinDialogState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSystemHealthAndUsage()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemHealthAndUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MidnightNavy,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            val label = stringResource(item.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LuminousEmerald,
                                    selectedTextColor = LuminousEmerald,
                                    indicatorColor = DeepEmeraldContainer,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MidnightNavy) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            val label = stringResource(item.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = LuminousEmerald,
                                    selectedTextColor = LuminousEmerald,
                                    indicatorColor = DeepEmeraldContainer,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                health = health,
                                settings = settings,
                                todayBlockCount = todayBlockCount,
                                recentEvents = recentEvents,
                                onOpenAccessibilitySettings = {
                                    UsageStatsHelper.openAccessibilitySettings(context)
                                },
                                onOpenUsageAccessSettings = {
                                    UsageStatsHelper.openUsageAccessSettings(context)
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestBatteryOptimization = {
                                    if (!BatteryOptimizationHelper.requestBatteryOptimizationExemption(context)) {
                                        context.startActivity(
                                            BatteryOptimizationHelper.createBatteryOptimizationSettingsIntent()
                                        )
                                    }
                                },
                                onToggleEnforcerService = { viewModel.toggleEnforcerService() },
                                onToggleReelsBlocker = { viewModel.setReelsBlocker(it) },
                                onToggleShortsBlocker = { viewModel.setShortsBlocker(it) },
                                onToggleTiktokBlocker = { viewModel.setTiktokBlocker(it) },
                                onToggleAntiUninstall = {
                                    viewModel.requestPinAction(PinActionPurpose.TOGGLE_ANTI_UNINSTALL)
                                }
                            )
                        }
                        composable(ROUTE_LIMITS) {
                            UsageLimitsScreen(
                                usageStatsGranted = health.usageStatsGranted,
                                appLimits = appLimits,
                                installedApps = installedApps,
                                dailyUsageMap = dailyUsageMap,
                                onOpenUsageSettings = {
                                    UsageStatsHelper.openUsageAccessSettings(context)
                                },
                                onRefreshUsage = { viewModel.refreshSystemHealthAndUsage() },
                                onSaveLimit = { pkg, name, mins ->
                                    viewModel.saveAppLimit(pkg, name, mins)
                                },
                                onToggleLimitEnabled = { viewModel.toggleAppLimitEnabled(it) },
                                onDeleteLimit = { viewModel.deleteAppLimit(it) }
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                onRequestPinAction = { purpose ->
                                    viewModel.requestPinAction(purpose)
                                },
                                onToggleStrictRedirectHome = { viewModel.setStrictRedirectHome(it) },
                                onToggleUsageEnforcerFlag = { viewModel.setUsageEnforcerEnabled(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    PinVerificationDialog(
        state = pinDialogState,
        onDismiss = { viewModel.dismissPinDialog() },
        onSubmitNewPin = { pin, confirm -> viewModel.submitNewPin(pin, confirm) },
        onVerifyPin = { pin, newPin -> viewModel.verifyPinAndExecute(pin, newPin) }
    )
}
