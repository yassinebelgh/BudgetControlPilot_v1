package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CanvasBarChart
import com.example.ui.components.FastAddTransactionDialog
import com.example.ui.components.HeroCockpitCard
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(
    viewModel: MainViewModel,
    onOpenFastAdd: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToOutilsAccounts: () -> Unit,
    onOpenOutils: () -> Unit = {}
) {
    val overviewState by viewModel.overviewState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.FRENCH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cockpit_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // App Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "B",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "BudgetControlPilot",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "Offline Financial Dashboard" else "Cockpit Financier Hors-Ligne",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onOpenOutils,
                    modifier = Modifier.testTag("cockpit_key_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "Tools & Settings" else "Outils & Paramètres",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero Card (Solde Théorique vs Solde Réel)
        item {
            HeroCockpitCard(overview = overviewState)
        }

        // Quick Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenFastAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "+ Expense" else "+ Dépense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToOutilsAccounts,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "Real Accounts" else "Comptes Réels", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Bar Chart (Revenus vs Dépenses)
        item {
            CanvasBarChart(
                incomeAmount = overviewState.theoreticalIncome,
                expenseAmount = overviewState.theoreticalExpense,
                currencySymbol = overviewState.currencySymbol,
                appLanguage = overviewState.language
            )
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "Recent Transactions" else "Dernières Opérations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToExpenses) {
                    Text(if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "See All" else "Tout voir", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        val recentTx = transactions.take(5)
        if (recentTx.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (overviewState.language == com.example.ui.viewmodel.AppLanguage.EN) "No transactions recorded yet. Click + Expense to add one." else "Aucune opération. Cliquez sur + Dépense pour ajouter une transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTx, key = { "cockpit_tx_${it.id}" }) { tx ->
                val cat = categories.find { it.name.equals(tx.category, ignoreCase = true) }
                val catColor = cat?.let { parseCategoryColor(it.colorHex) }
                TransactionCardItem(
                    transaction = tx,
                    currencySymbol = overviewState.currencySymbol,
                    dateFormat = dateFormat,
                    categoryColor = catColor,
                    onEdit = { editingTransaction = tx },
                    onDuplicate = {
                        viewModel.addTransaction(
                            title = tx.title,
                            amount = tx.amount,
                            type = tx.type,
                            category = tx.category,
                            incomeType = tx.incomeType
                        )
                    },
                    onDelete = { viewModel.deleteTransaction(tx) }
                )
            }
        }
    }

    editingTransaction?.let { tx ->
        FastAddTransactionDialog(
            onDismiss = { editingTransaction = null },
            initialTransaction = tx,
            categories = categories,
            currencySymbol = overviewState.currencySymbol,
            appLanguage = overviewState.language,
            onDelete = { viewModel.deleteTransaction(tx) },
            onSave = { title, amount, type, category, incomeType, note ->
                viewModel.updateTransaction(
                    id = tx.id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    incomeType = incomeType,
                    note = note,
                    date = tx.date
                )
            }
        )
    }
}

fun parseCategoryColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF3B82F6)
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = android.graphics.Color.parseColor("#$cleanHex")
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }
}

@Composable
fun TransactionCardItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    dateFormat: SimpleDateFormat,
    categoryColor: Color? = null,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isDebt = transaction.incomeType?.contains("Dette", ignoreCase = true) == true ||
            transaction.incomeType?.contains("Crédit", ignoreCase = true) == true ||
            transaction.category.contains("Dette", ignoreCase = true) ||
            transaction.category.contains("Crédit", ignoreCase = true)

    val amountColor = when {
        isExpense -> Color(0xFFEF4444)
        isDebt -> Color(0xFFE11D48) // Red-rose distinct for debt/credit income vs real earned revenue
        else -> Color(0xFF10B981) // Green for real revenue
    }
    val amountSign = if (isExpense) "-" else "+"
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background((categoryColor ?: amountColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = categoryColor ?: amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (categoryColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${transaction.category} • ${dateFormat.format(Date(transaction.date))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountSign${transaction.amount.formatCurrency(currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = amountColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Éditer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDuplicate != null) {
                        IconButton(
                            onClick = onDuplicate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
