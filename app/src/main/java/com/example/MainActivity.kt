package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.FastAddTransactionDialog
import com.example.ui.navigation.BottomNavBar
import com.example.ui.navigation.NavTab
import com.example.ui.screens.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val useDarkTheme = when (appTheme) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                var currentTab by remember { mutableStateOf<NavTab>(NavTab.Cockpit) }
                var showFastAddDialog by remember { mutableStateOf(false) }

                val overviewState by viewModel.overviewState.collectAsState()
                val categories by viewModel.categories.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentTab.route,
                            appLanguage = overviewState.language,
                            onTabSelected = { tab -> currentTab = tab }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavTab.Cockpit -> CockpitScreen(
                                viewModel = viewModel,
                                onOpenFastAdd = { showFastAddDialog = true },
                                onNavigateToExpenses = { currentTab = NavTab.Expenses },
                                onNavigateToOutilsAccounts = { currentTab = NavTab.Plan },
                                onOpenOutils = { currentTab = NavTab.Outils }
                            )
                            NavTab.Expenses, NavTab.Budget -> ExpensesScreen(
                                viewModel = viewModel,
                                onOpenFastAdd = { showFastAddDialog = true }
                            )
                            NavTab.Plan -> PlanScreen(
                                viewModel = viewModel
                            )
                            NavTab.Coach -> CoachScreen(
                                viewModel = viewModel
                            )
                            NavTab.Outils -> OutilsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentTab = NavTab.Cockpit }
                            )
                            else -> CoachScreen(
                                viewModel = viewModel
                            )
                        }
                    }

                    // Global Fast Add Sheet (<10s UX priority)
                    if (showFastAddDialog) {
                        FastAddTransactionDialog(
                            onDismiss = { showFastAddDialog = false },
                            onSave = { title, amount, type, category, incomeType, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    incomeType = incomeType,
                                    note = note
                                )
                            },
                            categories = categories,
                            currencySymbol = overviewState.currencySymbol,
                            appLanguage = overviewState.language,
                            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) }
                        )
                    }
                }
            }
        }
    }
}
