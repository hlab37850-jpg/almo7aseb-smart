package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppointmentsScreen
import com.example.ui.screens.CashBoxScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.PurchasesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SmartAccountantApp()
                }
            }
        }
    }
}

@Composable
fun SmartAccountantApp(
    viewModel: AccountingViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Back handling: if not on Home screen, back goes to Home
    BackHandler(enabled = currentScreen !is ScreenDestination.Home) {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentScreen is ScreenDestination.Home,
                    onClick = { viewModel.navigateTo(ScreenDestination.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldDark,
                        indicatorColor = EmeraldLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is ScreenDestination.Sales,
                    onClick = { viewModel.navigateTo(ScreenDestination.Sales) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "المبيعات") },
                    label = { Text("المبيعات") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldDark,
                        indicatorColor = EmeraldLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is ScreenDestination.CashBox,
                    onClick = { viewModel.navigateTo(ScreenDestination.CashBox) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "الصندوق") },
                    label = { Text("الصندوق") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldDark,
                        indicatorColor = EmeraldLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is ScreenDestination.Customers || currentScreen is ScreenDestination.Suppliers,
                    onClick = { viewModel.navigateTo(ScreenDestination.Customers) },
                    icon = { Icon(Icons.Default.People, contentDescription = "الحسابات") },
                    label = { Text("العملاء") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldDark,
                        indicatorColor = EmeraldLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is ScreenDestination.Inventory,
                    onClick = { viewModel.navigateTo(ScreenDestination.Inventory) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "المخزون") },
                    label = { Text("المخزون") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldDark,
                        indicatorColor = EmeraldLight.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            is ScreenDestination.Home -> HomeScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Sales -> SalesScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Purchases -> PurchasesScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.CashBox -> CashBoxScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Customers, is ScreenDestination.Suppliers -> PartiesScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Inventory -> InventoryScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.AccountsJournal -> JournalScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Expenses -> CashBoxScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Appointments -> AppointmentsScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Reports -> ReportsScreen(viewModel, modifier = Modifier.padding(innerPadding))
            is ScreenDestination.Settings -> SettingsScreen(viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}
