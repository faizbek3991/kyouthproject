package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.KitchenStatus
import com.example.data.repository.DapurPassRepository
import com.example.ui.DapurPassViewModel
import com.example.ui.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CounterScreen
import com.example.ui.screens.KitchenPassScreen
import com.example.ui.screens.MenuManageScreen
import com.example.ui.theme.DapurPassTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DapurPassTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation()
        }
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private enum class DapurDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  COUNTER("counter", R.string.nav_counter, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
  KITCHEN("kitchen", R.string.nav_kitchen, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
  ANALYTICS("analytics", R.string.nav_analytics, Icons.Filled.Analytics, Icons.Outlined.Analytics),
  MENU("menu", R.string.nav_menu, Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu)
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      DapurPassMainShell(
        currentUserId = user.uid,
        userEmail = user.email.orEmpty()
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DapurPassMainShell(
  currentUserId: String,
  userEmail: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: DapurPassViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        DapurPassViewModel(DapurPassRepository(db), currentUserId)
      }
    }
  )

  val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
  val menuItemsState by viewModel.menuItemsState.collectAsStateWithLifecycle()
  val mistakeLogsState by viewModel.mistakeLogsState.collectAsStateWithLifecycle()
  val draft by viewModel.draft.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

  val ordersList = (ordersState as? UiState.Success)?.data ?: emptyList()
  val activeKitchenCount = remember(ordersList) {
    ordersList.count { it.status != KitchenStatus.COMPLETED.wireValue }
  }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(bannerMessage) {
    bannerMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  val activeTab = DapurDestination.entries.firstOrNull { dest ->
    currentDestination?.hierarchy?.any { it.route == dest.route } == true
  } ?: DapurDestination.COUNTER

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 720.dp

    Scaffold(
      contentWindowInsets = WindowInsets.statusBars,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "DapurPass",
                style = MaterialTheme.typography.titleLarge
              )
              Text(
                text = stringResource(activeTab.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          )
        )
      },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            DapurDestination.entries.forEach { dest ->
              val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(dest.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  NavIconWithBadge(
                    destination = dest,
                    selected = selected,
                    activeKitchenCount = activeKitchenCount
                  )
                },
                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                modifier = Modifier.testTag("nav_${dest.route}")
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
          NavigationRail(
            modifier = Modifier.fillMaxHeight().padding(top = 8.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              DapurDestination.entries.forEach { dest ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(dest.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    NavIconWithBadge(
                      destination = dest,
                      selected = selected,
                      activeKitchenCount = activeKitchenCount
                    )
                  },
                  label = { Text(stringResource(dest.labelRes)) },
                  modifier = Modifier.testTag("rail_${dest.route}")
                )
              }
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = DapurDestination.COUNTER.route,
          modifier = Modifier.weight(1f).fillMaxSize()
        ) {
          composable(DapurDestination.COUNTER.route) {
            CounterScreen(
              menuItemsState = menuItemsState,
              draft = draft,
              existingOrderCount = ordersList.size,
              onSelectChannel = viewModel::setChannel,
              onTableOrPhoneChange = viewModel::setTableOrPhone,
              onCustomerNoteChange = viewModel::setCustomerNote,
              onTogglePeakHour = viewModel::togglePeakHour,
              onQuickTapItem = viewModel::quickAddMenuItem,
              onAddConfiguredItem = viewModel::addConfiguredLine,
              onUpdateLineQty = viewModel::updateDraftLineQuantity,
              onUpdateLineModifiers = viewModel::updateDraftLineModifiers,
              onClearCart = viewModel::clearDraftCart,
              onSubmitOrder = viewModel::submitOrder
            )
          }
          composable(DapurDestination.KITCHEN.route) {
            KitchenPassScreen(
              ordersState = ordersState,
              onToggleItemVerified = viewModel::toggleItemVerified,
              onAdvanceStatus = viewModel::advanceOrderStatus,
              onCancelQueued = viewModel::cancelQueuedOrder,
              onLogQualityEvent = viewModel::logQualityEvent
            )
          }
          composable(DapurDestination.ANALYTICS.route) {
            AnalyticsScreen(
              ordersState = ordersState,
              mistakeLogsState = mistakeLogsState,
              computeAnalytics = viewModel::computeAnalytics
            )
          }
          composable(DapurDestination.MENU.route) {
            MenuManageScreen(
              userEmail = userEmail,
              menuItemsState = menuItemsState,
              onSaveItem = viewModel::saveMenuItem,
              onDeleteItem = viewModel::deleteMenuItem,
              onSignOut = {
                signOutUser(credentialManager = credentialManager, scope = scope)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NavIconWithBadge(
  destination: DapurDestination,
  selected: Boolean,
  activeKitchenCount: Int
) {
  val iconVector = if (selected) destination.selectedIcon else destination.unselectedIcon
  if (destination == DapurDestination.KITCHEN && activeKitchenCount > 0) {
    BadgedBox(
      badge = {
        Badge { Text(activeKitchenCount.toString()) }
      }
    ) {
      Icon(imageVector = iconVector, contentDescription = stringResource(destination.labelRes))
    }
  } else {
    Icon(imageVector = iconVector, contentDescription = stringResource(destination.labelRes))
  }
}
