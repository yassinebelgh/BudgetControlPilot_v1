package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.MainViewModel

import com.example.ui.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: MainViewModel
) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val overviewState by viewModel.overviewState.collectAsState()

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf("") }
    var currentLimitInput by remember { mutableStateOf("") }

    val defaultCategories = listOf("Logement", "Alimentation", "Transport", "Loisirs", "Santé", "Abonnements", "Autres")

    // Calculate spent amount per category
    val spentMap = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("budget_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Budgets Mensuels",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contrôle des plafonds de dépenses par poste",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(defaultCategories) { category ->
                    val budgetEntity = budgets.find { it.category.equals(category, ignoreCase = true) }
                    val limit = budgetEntity?.monthlyLimit ?: 300.0
                    val spent = spentMap[category] ?: 0.0
                    val ratio = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                    val isExceeded = spent > limit
                    val diff = limit - spent
                    val isNearLimit = !isExceeded && kotlin.math.abs(diff) < 1.0

                    val progressColor = when {
                        isExceeded -> Color(0xFFDC2626)
                        isNearLimit -> Color(0xFFD97706)
                        else -> Color(0xFF16A34A)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isExceeded) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Dépassement",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        selectedCategoryForEdit = category
                                        currentLimitInput = limit.toString()
                                        showEditBudgetDialog = true
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Ajuster", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ajuster", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp),
                                color = progressColor,
                                trackColor = progressColor.copy(alpha = 0.15f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )

                            // Numerical Spent vs Limit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isExceeded) {
                                        "Dépassé de ${(spent - limit).formatCurrency(overviewState.currencySymbol)}"
                                    } else {
                                        "Consommé: ${spent.formatCurrency(overviewState.currencySymbol)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = progressColor
                                )
                                Text(
                                    text = "Plafond: ${limit.formatCurrency(overviewState.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Budget Limit Dialog
    if (showEditBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = { Text("Plafond pour $selectedCategoryForEdit") },
            text = {
                OutlinedTextField(
                    value = currentLimitInput,
                    onValueChange = { currentLimitInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Plafond mensuel (${overviewState.currencySymbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLimit = currentLimitInput.toDoubleOrNull() ?: 300.0
                        viewModel.saveBudget(selectedCategoryForEdit, newLimit)
                        showEditBudgetDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
