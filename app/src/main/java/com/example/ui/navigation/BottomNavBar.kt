package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.ui.util.AppText
import com.example.ui.viewmodel.AppLanguage

sealed class NavTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Cockpit : NavTab("cockpit", Icons.Filled.Speed, Icons.Outlined.Speed)
    object Expenses : NavTab("expenses", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Plan : NavTab("plan", Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object Budget : NavTab("budget", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    object Assistant : NavTab("assistant", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object Coach : NavTab("coach", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    object Outils : NavTab("outils", Icons.Filled.Build, Icons.Outlined.Build)

    fun title(lang: AppLanguage): String = when (this) {
        Cockpit -> AppText.tabCockpit(lang)
        Expenses -> AppText.tabExpenses(lang)
        Plan -> AppText.tabPlan(lang)
        Budget -> if (lang == AppLanguage.EN) "Budget" else "Budget"
        Assistant -> AppText.tabAssistant(lang)
        Coach -> if (lang == AppLanguage.EN) "Coach" else "Coach"
        Outils -> AppText.tabOutils(lang)
    }
}

val navTabs = listOf(
    NavTab.Cockpit,
    NavTab.Expenses,
    NavTab.Plan,
    NavTab.Coach
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    appLanguage: AppLanguage = AppLanguage.FR,
    onTabSelected: (NavTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        navTabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            val localizedTitle = tab.title(appLanguage)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = localizedTitle
                    )
                },
                label = {
                    Text(
                        text = localizedTitle,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${tab.route}")
            )
        }
    }
}
