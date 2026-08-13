package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.data.model.RealAccountEntity
import com.example.ui.components.CanvasDonutChart
import com.example.ui.components.CategoryExpenseSlice
import com.example.ui.viewmodel.IncomeLine
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MonthlyPlan
import com.example.ui.viewmodel.PlanAllocation
import java.util.Locale

import com.example.ui.util.formatAmount
import com.example.ui.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentPlan by viewModel.currentPlan.collectAsState()
    val realAccounts by viewModel.realAccounts.collectAsState()
    val currencySymbol by viewModel.currency.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val overviewState by viewModel.overviewState.collectAsState()
    val isEn = overviewState.language == com.example.ui.viewmodel.AppLanguage.EN

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditIncomeDialog by remember { mutableStateOf(false) }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf<RealAccountEntity?>(null) }
    var accountNameInput by remember { mutableStateOf("") }
    var accountBalanceInput by remember { mutableStateOf("") }
    var accountTypeInput by remember { mutableStateOf("BANK") }
    var editAccountNameInput by remember { mutableStateOf("") }
    var editBalanceInput by remember { mutableStateOf("") }
    var editAccountTypeInput by remember { mutableStateOf("BANK") }

    val totalAllocated = currentPlan.allocations.sumOf { it.allocatedAmount }
    val remaining = currentPlan.expectedIncome - totalAllocated

    val availableColors = listOf(
        "#3F51B5", "#4CAF50", "#FF9800", "#E91E63", "#00BCD4", "#9C27B0", "#FF5722", "#607D8B"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEn) "Monthly Budget Plan" else "Plan Prévisionnel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEn) "Monthly projection slate" else "Ardoise de projection mensuelle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("plan_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Section: Situation Réelle (Comptes Physiques)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEn) "Real Situation (Physical Accounts)" else "Situation Réelle (Comptes Physiques)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn) "Real bank, cash and debt balances" else "Soldes réels en banque, espèces et dettes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.testTag("plan_add_real_account_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Add" else "Ajouter", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (realAccounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) "No real accounts added yet." else "Aucun compte réel configuré.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(realAccounts, key = { "plan_account_${it.id}" }) { account ->
                    val isDebtAccount = account.accountType.equals("DEBT", true) ||
                            account.accountType.equals("CREDIT", true) ||
                            account.balance < 0 ||
                            account.name.contains("Dette", ignoreCase = true) ||
                            account.name.contains("Crédit", ignoreCase = true)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (isDebtAccount) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDebtAccount) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isDebtAccount) {
                                                Icons.Default.CreditCard
                                            } else when (account.accountType) {
                                                "SAVINGS" -> Icons.Default.Savings
                                                "CASH" -> Icons.Default.Payments
                                                else -> Icons.Default.AccountBalance
                                            },
                                            contentDescription = null,
                                            tint = if (isDebtAccount) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isDebtAccount) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF2F2)
                                            ) {
                                                Text(
                                                    text = if (isEn) "Debt/Credit" else "Dette/Crédit",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isDebtAccount) {
                                            "-${kotlin.math.abs(account.balance).formatCurrency(currencySymbol)}"
                                        } else {
                                            "+${account.balance.formatCurrency(currencySymbol)}"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDebtAccount) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        showEditAccountDialog = account
                                        editAccountNameInput = account.name
                                        editBalanceInput = account.balance.toString()
                                        editAccountTypeInput = account.accountType
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = if (isEn) "Edit account" else "Modifier compte", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteRealAccount(account.id) }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = if (isEn) "Delete" else "Supprimer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp)) }

            // Information Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isEn) "The Plan is an estimation tool. It does not alter your Cockpit or real account balance." else "Le Plan est un outil d'estimation pure. Il ne modifie pas votre Cockpit, ni vos rapports ou votre solde réel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Expected Income Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentPlan.monthYear,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            TextButton(
                                onClick = { showEditIncomeDialog = true },
                                modifier = Modifier.testTag("edit_income_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Edit" else "Modifier")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isEn) "EXPECTED INCOME" else "REVENU PRÉVU",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentPlan.expectedIncome.formatCurrency(currencySymbol),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isEn) "TOTAL ALLOCATED" else "TOTAL RÉPARTI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = totalAllocated.formatCurrency(currencySymbol),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Remaining Status Banner
                        val bannerColor = when {
                            remaining > 0 -> MaterialTheme.colorScheme.primaryContainer
                            remaining < 0 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                        val bannerTextColor = when {
                            remaining > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            remaining < 0 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                        val statusMsg = when {
                            remaining > 0 -> if (isEn) "Remaining to allocate: ${remaining.formatCurrency(currencySymbol)}" else "Reste à répartir : ${remaining.formatCurrency(currencySymbol)}"
                            remaining < 0 -> if (isEn) "Allocation exceeds income by ${kotlin.math.abs(remaining).formatCurrency(currencySymbol)}" else "Répartition supérieure au revenu de ${kotlin.math.abs(remaining).formatCurrency(currencySymbol)}"
                            else -> if (isEn) "100% of expected income allocated!" else "100% du revenu prévisionnel est réparti !"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bannerColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (remaining < 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = bannerTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusMsg,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = bannerTextColor
                                )
                            }
                        }
                    }
                }
            }

            // Visual Chart Section
            if (currentPlan.expectedIncome > 0 && currentPlan.allocations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isEn) "Category Allocation" else "Répartition par Catégorie",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            // Chart
                            val chartSlices = currentPlan.allocations.map {
                                CategoryExpenseSlice(
                                    category = it.categoryName,
                                    amount = it.allocatedAmount,
                                    color = parseHexColor(it.colorHex)
                                )
                            } + if (remaining > 0) listOf(
                                CategoryExpenseSlice(
                                    category = if (isEn) "Free Remaining" else "Reste Libre",
                                    amount = remaining,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            ) else emptyList()

                            CanvasDonutChart(
                                slices = chartSlices,
                                currencySymbol = currencySymbol,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Percentage Chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentPlan.allocations.forEach { alloc ->
                                    val pct = if (currentPlan.expectedIncome > 0) (alloc.allocatedAmount / currentPlan.expectedIncome) * 100 else 0.0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = parseHexColor(alloc.colorHex).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, parseHexColor(alloc.colorHex).copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(alloc.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${alloc.categoryName} (${String.format(Locale.getDefault(), "%.1f", pct)}%)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (remaining > 0) {
                                    val remainingPct = (remaining / currentPlan.expectedIncome) * 100
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.outline)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${if (isEn) "Free" else "Libre"} (${String.format(Locale.getDefault(), "%.1f", remainingPct)}%)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Allocations List Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "Estimated Allocation" else "Répartition Estimée",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.testTag("add_plan_category_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Add" else "Ajouter")
                    }
                }
            }

            // Allocations List Items with Up/Down Arrows for Reordering
            itemsIndexed(currentPlan.allocations, key = { _, alloc -> "plan_alloc_${alloc.categoryName}" }) { index, alloc ->
                val pct = if (currentPlan.expectedIncome > 0) (alloc.allocatedAmount / currentPlan.expectedIncome) * 100 else 0.0
                var showEditCatAllocDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Reorder Arrows
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.movePlanAllocationUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isEn) "Move Up" else "Monter",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.movePlanAllocationDown(index) },
                                enabled = index < currentPlan.allocations.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isEn) "Move Down" else "Descendre",
                                    tint = if (index < currentPlan.allocations.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(alloc.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = alloc.categoryName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), if (isEn) "%.1f%% of income" else "%.1f%% du revenu", pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alloc.allocatedAmount.formatCurrency(currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { showEditCatAllocDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = if (isEn) "Edit allocation" else "Modifier l'estimation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.removePlanCategory(alloc.categoryName) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEn) "Delete" else "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (showEditCatAllocDialog) {
                    var editAmountText by remember { mutableStateOf(alloc.allocatedAmount.toString()) }
                    AlertDialog(
                        onDismissRequest = { showEditCatAllocDialog = false },
                        title = { Text("${if (isEn) "Edit" else "Modifier"} - ${alloc.categoryName}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = editAmountText,
                                    onValueChange = { editAmountText = it },
                                    label = { Text(if (isEn) "New allocation goal ($currencySymbol)" else "Nouvel objectif d'allocation ($currencySymbol)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val newAmt = editAmountText.toDoubleOrNull() ?: alloc.allocatedAmount
                                viewModel.updatePlanAllocation(alloc.categoryName, newAmt, false)
                                showEditCatAllocDialog = false
                            }) {
                                Text(if (isEn) "Confirm" else "Valider")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditCatAllocDialog = false }) {
                                Text(if (isEn) "Cancel" else "Annuler")
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Dialog: Edit Multi-Line Expected Income
    if (showEditIncomeDialog) {
        var tempMonthYear by remember { mutableStateOf(currentPlan.monthYear) }
        val incomeListState = remember { mutableStateListOf<IncomeLine>().apply { addAll(currentPlan.incomeLines) } }

        val grandTotal = incomeListState.sumOf { it.amount }

        AlertDialog(
            onDismissRequest = { showEditIncomeDialog = false },
            title = { Text(if (isEn) "Manage Monthly Income" else "Gestion des Revenus du Mois") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempMonthYear,
                        onValueChange = { tempMonthYear = it },
                        label = { Text(if (isEn) "Month / Year" else "Mois / Année") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isEn) "Expected Income Sources:" else "Sources de Revenus Prévisionnels :",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    incomeListState.forEachIndexed { index, line ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = line.title,
                                onValueChange = { newTitle ->
                                    incomeListState[index] = line.copy(title = newTitle)
                                },
                                label = { Text(if (isEn) "Label" else "Libellé") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = if (line.amount > 0) line.amount.toString() else "",
                                onValueChange = { newAmtStr ->
                                    val newAmt = newAmtStr.toDoubleOrNull() ?: 0.0
                                    incomeListState[index] = line.copy(amount = newAmt)
                                },
                                label = { Text(if (isEn) "Amount ($currencySymbol)" else "Montant ($currencySymbol)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (incomeListState.size > 1) {
                                        incomeListState.removeAt(index)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEn) "Delete line" else "Supprimer la ligne",
                                    tint = if (incomeListState.size > 1) MaterialTheme.colorScheme.error else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            incomeListState.add(IncomeLine(title = if (isEn) "Income ${incomeListState.size + 1}" else "Revenu ${incomeListState.size + 1}", amount = 0.0))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Add income line (+)" else "Ajouter une ligne de revenu (+)")
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isEn) "Total Calculated:" else "Total Pris en Charge :", fontWeight = FontWeight.Bold)
                            Text(
                                text = grandTotal.formatCurrency(currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePlanMonth(tempMonthYear.ifBlank { if (isEn) "August 2026" else "Août 2026" })
                        // Save income lines
                        currentPlan.incomeLines.forEach { viewModel.removeIncomeLine(it.id) }
                        incomeListState.forEach { line ->
                            viewModel.addIncomeLine(line.title.ifBlank { if (isEn) "Income" else "Revenu" }, line.amount)
                        }
                        showEditIncomeDialog = false
                    }
                ) {
                    Text(if (isEn) "Confirm" else "Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditIncomeDialog = false }) {
                    Text(if (isEn) "Cancel" else "Annuler")
                }
            }
        )
    }

    // Dialog: Add Category Allocation
    if (showAddCategoryDialog) {
        var selectedCatName by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Housing") }
        var amountText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(if (isEn) "Add Category to Plan" else "Ajouter une catégorie au Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isEn) "Select or type a category:" else "Choisir ou saisir une catégorie :", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = { selectedCatName = it },
                        label = { Text(if (isEn) "Category Name" else "Nom catégorie") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (isEn) "Estimated Amount ($currencySymbol)" else "Montant estimé ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (selectedCatName.isNotBlank() && amt > 0) {
                            val color = availableColors[kotlin.math.abs(selectedCatName.hashCode()) % availableColors.size]
                            viewModel.addPlanCategory(selectedCatName.trim(), amt, color, false)
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(if (isEn) "Add" else "Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(if (isEn) "Cancel" else "Annuler")
                }
            }
        )
    }

    // Add Real Account Dialog
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text(if (isEn) "New Real Account" else "Nouveau Compte Réel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text(if (isEn) "Account Name (e.g., BNPP, Cash...)" else "Nom du compte (ex. BNPP, Cash...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountBalanceInput,
                        onValueChange = { accountBalanceInput = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text(if (isEn) "Balance / Amount ($currencySymbol)" else "Solde / Montant ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(if (isEn) "Account Type:" else "Type de compte :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = accountTypeInput == "BANK",
                            onClick = { accountTypeInput = "BANK" },
                            label = { Text(if (isEn) "Bank" else "Banque") }
                        )
                        FilterChip(
                            selected = accountTypeInput == "CASH",
                            onClick = { accountTypeInput = "CASH" },
                            label = { Text(if (isEn) "Cash" else "Cash") }
                        )
                        FilterChip(
                            selected = accountTypeInput == "DEBT",
                            onClick = { accountTypeInput = "DEBT" },
                            label = { Text(if (isEn) "Debt/Credit (-)" else "Dette/Crédit (-)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = accountBalanceInput.toDoubleOrNull() ?: 0.0
                        if (accountNameInput.isNotBlank()) {
                            viewModel.saveRealAccount(name = accountNameInput, balance = bal, accountType = accountTypeInput)
                            accountNameInput = ""
                            accountBalanceInput = ""
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text(if (isEn) "Add" else "Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) { Text(if (isEn) "Cancel" else "Annuler") }
            }
        )
    }

    // Edit Real Account Dialog
    showEditAccountDialog?.let { account ->
        AlertDialog(
            onDismissRequest = { showEditAccountDialog = null },
            title = { Text(if (isEn) "Edit Account: ${account.name}" else "Modifier le compte : ${account.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editAccountNameInput,
                        onValueChange = { editAccountNameInput = it },
                        label = { Text(if (isEn) "Account Name" else "Nom du compte") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBalanceInput,
                        onValueChange = { editBalanceInput = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text(if (isEn) "New Real Balance ($currencySymbol)" else "Nouveau solde réel ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(if (isEn) "Account Type:" else "Type de compte :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = editAccountTypeInput == "BANK",
                            onClick = { editAccountTypeInput = "BANK" },
                            label = { Text(if (isEn) "Bank" else "Banque") }
                        )
                        FilterChip(
                            selected = editAccountTypeInput == "CASH",
                            onClick = { editAccountTypeInput = "CASH" },
                            label = { Text(if (isEn) "Cash" else "Cash") }
                        )
                        FilterChip(
                            selected = editAccountTypeInput == "DEBT",
                            onClick = { editAccountTypeInput = "DEBT" },
                            label = { Text(if (isEn) "Debt/Credit (-)" else "Dette/Crédit (-)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBal = editBalanceInput.toDoubleOrNull() ?: account.balance
                        val nameToSave = editAccountNameInput.ifBlank { account.name }
                        viewModel.saveRealAccount(
                            id = account.id,
                            name = nameToSave,
                            balance = newBal,
                            accountType = editAccountTypeInput
                        )
                        showEditAccountDialog = null
                    }
                ) {
                    Text(if (isEn) "Save Changes" else "Mettre à jour")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAccountDialog = null }) { Text(if (isEn) "Cancel" else "Annuler") }
            }
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF3F51B5)
    }
}
