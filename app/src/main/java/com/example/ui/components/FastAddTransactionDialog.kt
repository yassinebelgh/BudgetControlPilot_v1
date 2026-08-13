package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Locale

import com.example.ui.util.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastAddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: TransactionType, category: String, incomeType: String?, note: String?) -> Unit,
    categories: List<CategoryEntity>,
    currencySymbol: String = "€",
    initialTransaction: TransactionEntity? = null,
    onDelete: (() -> Unit)? = null,
    onAddCategory: ((name: String, colorHex: String) -> Unit)? = null,
    appLanguage: com.example.ui.viewmodel.AppLanguage = com.example.ui.viewmodel.AppLanguage.FR
) {
    val isEn = appLanguage == com.example.ui.viewmodel.AppLanguage.EN

    var amountText by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.amount?.formatAmount() ?: "")
    }
    var titleText by remember(initialTransaction) { mutableStateOf(initialTransaction?.title ?: "") }
    var selectedType by remember(initialTransaction) { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }

    val defaultExpenseCategories = if (isEn) {
        listOf("Groceries", "Housing", "Transport", "Leisure", "Health", "Subscriptions", "Other")
    } else {
        listOf("Alimentation", "Logement", "Transport", "Loisirs", "Santé", "Abonnements", "Autres")
    }
    val defaultIncomeTypes = if (isEn) {
        listOf("Salary", "Refund", "Debt / Credit", "Other")
    } else {
        listOf("Salaire", "Remboursement", "Dette / Crédit", "Autre")
    }

    val categoryList = remember(categories) {
        if (categories.isNotEmpty()) categories.map { it.name }.distinct() else defaultExpenseCategories
    }
    var selectedCategory by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.category ?: categoryList.firstOrNull() ?: "Alimentation")
    }
    var selectedIncomeType by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.incomeType ?: "Salaire")
    }

    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) {
                        if (isEn) "Edit Transaction" else "Modifier l'opération"
                    } else if (selectedType == TransactionType.EXPENSE) {
                        if (isEn) "New Expense" else "Nouvelle Dépense"
                    } else {
                        if (isEn) "New Income" else "Nouveau Revenu"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = {
                            onDelete()
                            onDismiss()
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = if (isEn) "Delete" else "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = if (isEn) "Close" else "Fermer")
                    }
                }
            }

            // Type Selector Tab
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        if (!categoryList.contains(selectedCategory)) {
                            selectedCategory = categoryList.firstOrNull() ?: defaultExpenseCategories.first()
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFFEF4444),
                        activeContentColor = Color.White
                    ),
                    modifier = Modifier.testTag("type_expense_button")
                ) {
                    Text(if (isEn) "Expense" else "Dépense", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF10B981),
                        activeContentColor = Color.White
                    ),
                    modifier = Modifier.testTag("type_income_button")
                ) {
                    Text(if (isEn) "Income" else "Revenu", fontWeight = FontWeight.SemiBold)
                }
            }

            // Amount Field with Calculator Button
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' || char == ',' || char == '+' || char == '-' || char == '*' || char == '/' } },
                label = { Text(if (isEn) "Amount ($currencySymbol)" else "Montant ($currencySymbol)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = {
                    IconButton(onClick = { showCalculatorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = if (isEn) "Built-in calculator" else "Calculatrice intégrée",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Title Field
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(if (isEn) "Title / Description" else "Titre / Description") },
                placeholder = { Text(if (selectedType == TransactionType.EXPENSE) (if (isEn) "e.g. Groceries, Rent..." else "ex. Courses, Loyer...") else (if (isEn) "e.g. Salary, Transfer..." else "ex. Salaire, Virement...")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input")
            )

            // Category or Income Type Selector
            if (selectedType == TransactionType.EXPENSE) {
                Text(
                    text = if (isEn) "Category" else "Catégorie",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryList) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showNewCategoryDialog = true },
                            leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (isEn) "New" else "Nouvelle", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = if (isEn) "Income Type" else "Type de revenu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultIncomeTypes.forEach { incType ->
                        FilterChip(
                            selected = selectedIncomeType == incType,
                            onClick = { selectedIncomeType = incType },
                            label = { Text(incType) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val evaluatedAmount = evaluateExpression(amountText)
                    if (evaluatedAmount > 0.0) {
                        onSave(
                            titleText,
                            evaluatedAmount,
                            selectedType,
                            if (selectedType == TransactionType.EXPENSE) selectedCategory else (if (isEn) "Income" else "Revenu"),
                            if (selectedType == TransactionType.INCOME) selectedIncomeType else null,
                            null
                        )
                        onDismiss()
                    }
                },
                enabled = evaluateExpression(amountText) > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            ) {
                Icon(
                    imageVector = if (initialTransaction != null) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction != null) (if (isEn) "Save Changes" else "Enregistrer les modifications") else (if (isEn) "Save Immediately" else "Enregistrer immédiatement"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Calculator Dialog
    if (showCalculatorDialog) {
        CalculatorModalDialog(
            initialExpr = amountText,
            onDismiss = { showCalculatorDialog = false },
            onApply = { result ->
                amountText = result
                showCalculatorDialog = false
            }
        )
    }

    // New Category Dialog with Color Picker
    if (showNewCategoryDialog) {
        CreateCategoryModalDialog(
            onDismiss = { showNewCategoryDialog = false },
            onSave = { name, colorHex ->
                onAddCategory?.invoke(name, colorHex)
                selectedCategory = name
                showNewCategoryDialog = false
            }
        )
    }
}

fun evaluateExpression(expr: String): Double {
    val clean = expr.replace(',', '.').replace(" ", "")
    if (clean.isBlank()) return 0.0
    return try {
        val parts = clean.split("+")
        if (parts.size > 1) {
            return parts.sumOf { evaluateExpression(it) }
        }
        val subParts = clean.split("-")
        if (subParts.size > 1) {
            val first = evaluateExpression(subParts[0])
            val rest = subParts.drop(1).sumOf { evaluateExpression(it) }
            return first - rest
        }
        clean.toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        0.0
    }
}

@Composable
fun CalculatorModalDialog(
    initialExpr: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var expr by remember { mutableStateOf(initialExpr) }
    var calcResult by remember { mutableStateOf("") }

    fun updateResult() {
        val res = evaluateExpression(expr)
        calcResult = if (res > 0.0) res.formatAmount() else ""
    }

    LaunchedEffect(expr) { updateResult() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculatrice de dépenses") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expr,
                    onValueChange = { expr = it },
                    label = { Text("Expression (ex. 12.50 + 45 + 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (calcResult.isNotBlank()) {
                    Text(
                        text = "Résultat total = $calcResult €",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                }
                val buttons = listOf(
                    listOf("7", "8", "9", "+"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "*"),
                    listOf("0", ".", "C", "=")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    buttons.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { btn ->
                                Button(
                                    onClick = {
                                        when (btn) {
                                            "C" -> expr = ""
                                            "=" -> updateResult()
                                            else -> expr += btn
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (btn in listOf("+", "-", "*", "=")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (btn in listOf("+", "-", "*", "=")) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(btn, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalVal = if (calcResult.isNotBlank()) calcResult else expr
                onApply(finalVal)
            }) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun CreateCategoryModalDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    val colorPalette = listOf(
        "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6",
        "#EC4899", "#EF4444", "#06B6D4", "#84CC16",
        "#F97316", "#6366F1", "#14B8A6", "#64748B"
    )
    var selectedColorHex by remember { mutableStateOf(colorPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une nouvelle catégorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nom de la catégorie") },
                    placeholder = { Text("ex. Abonnements, Animaux...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Couleur d'identification :",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rows = colorPalette.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowHexes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowHexes.forEach { hex ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) { Color(0xFF3B82F6) }
                                val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onSave(categoryName.trim(), selectedColorHex)
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
