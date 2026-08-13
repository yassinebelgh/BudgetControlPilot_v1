package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.util.formatCurrency
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.FastAddTransactionDialog
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ExpensesDisplayMode {
    BY_EXPENSE, BY_CATEGORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: MainViewModel,
    onOpenFastAdd: () -> Unit
) {
    var displayMode by remember { mutableStateOf(ExpensesDisplayMode.BY_EXPENSE) }

    val filteredTx by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val currentPlan by viewModel.currentPlan.collectAsState()
    val overviewState by viewModel.overviewState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCatFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf("") }
    var currentLimitInput by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var categoryForColorPicker by remember { mutableStateOf<CategoryEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH) }

    val defaultCategories = listOf("Alimentation", "Logement", "Transport", "Loisirs", "Santé", "Abonnements", "Autres")
    val catNames = if (categories.isNotEmpty()) (listOf("Tous") + categories.map { it.name }).distinct() else (listOf("Tous") + defaultCategories).distinct()

    // Calculate spent amount per category for budget view
    val spentMap = remember(allTransactions) {
        allTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold(
        floatingActionButton = {
            if (displayMode == ExpensesDisplayMode.BY_EXPENSE) {
                FloatingActionButton(
                    onClick = onOpenFastAdd,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter une transaction")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("expenses_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val lang = overviewState.language
            val isEn = lang == com.example.ui.viewmodel.AppLanguage.EN

            // Screen Header Title
            Text(
                text = if (isEn) "Expenses & Budget" else "Dépenses & Budget",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Primary View Mode Selector (Par Dépense vs Par Catégorie)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = displayMode == ExpensesDisplayMode.BY_EXPENSE,
                    onClick = { displayMode = ExpensesDisplayMode.BY_EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text(if (isEn) "By Expense" else "Par Dépense", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = displayMode == ExpensesDisplayMode.BY_CATEGORY,
                    onClick = { displayMode = ExpensesDisplayMode.BY_CATEGORY },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text(if (isEn) "By Category" else "Par Catégorie", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (displayMode == ExpensesDisplayMode.BY_EXPENSE) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = if (isEn) "Search..." else "Rechercher...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = if (isEn) "Clear" else "Effacer", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 56.dp)
                        .testTag("search_tx_input")
                )

                // Type Filter Segmented Buttons (Tous, Dépenses, Revenus)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTypeFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text(if (isEn) "All" else "Tous")
                    }
                    SegmentedButton(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text(if (isEn) "Expenses" else "Dépenses")
                    }
                    SegmentedButton(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { viewModel.setTypeFilter(TransactionType.INCOME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text(if (isEn) "Income" else "Revenus")
                    }
                }

                // Category Chips Row with Category Colors Menu Button
                var showCategoryColorsMenu by remember { mutableStateOf(false) }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { showCategoryColorsMenu = true },
                            label = { Text(if (isEn) "Category Colors" else "Couleurs Catégories") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Palette",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    items(catNames) { cat ->
                        val isSelected = (cat == "Tous" && selectedCatFilter == null) || (cat == selectedCatFilter)
                        val catEntity = categories.find { it.name.equals(cat, ignoreCase = true) }
                        val catColor = catEntity?.let { parseCategoryColor(it.colorHex) }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (cat == "Tous") viewModel.setCategoryFilter(null)
                                else viewModel.setCategoryFilter(cat)
                            },
                            leadingIcon = if (catColor != null && cat != "Tous") {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                }
                            } else null,
                            label = { Text(if (cat == "Tous" && isEn) "All" else cat) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (showCategoryColorsMenu) {
                    AlertDialog(
                        onDismissRequest = { showCategoryColorsMenu = false },
                        title = { Text(if (isEn) "Category Color Customization" else "Personnaliser la couleur des catégories") },
                        text = {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val allCats = (categories.map { it.name } + defaultCategories).distinct()
                                items(allCats) { catName ->
                                    val catEntity = categories.find { it.name.equals(catName, ignoreCase = true) }
                                    val catColor = parseCategoryColor(catEntity?.colorHex)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            categoryForColorPicker = catEntity ?: CategoryEntity(name = catName, colorHex = "#3B82F6")
                                            showCategoryColorsMenu = false
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(text = catName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "Color",
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCategoryColorsMenu = false }) {
                                Text(if (isEn) "Close" else "Fermer")
                            }
                        }
                    )
                }

                // Transactions List
                if (filteredTx.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isEn) "No transactions match your criteria." else "Aucune opération ne correspond à vos critères.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredTx, key = { "expense_tx_${it.id}" }) { tx ->
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
            } else {
                // BY_CATEGORY (Budget & Spending breakdown)
                val budgetCategories = if (categories.isNotEmpty()) categories.map { it.name }.distinct() else defaultCategories.distinct()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(budgetCategories, key = { "cat_budget_$it" }) { category ->
                        val planAlloc = currentPlan.allocations.find { it.categoryName.equals(category, ignoreCase = true) }
                        val budgetEntity = budgets.find { it.category.equals(category, ignoreCase = true) }
                        val limit = if (planAlloc != null && planAlloc.linkToCategoryLimit) {
                            planAlloc.allocatedAmount
                        } else {
                            budgetEntity?.monthlyLimit ?: 0.0
                        }
                        val spent = spentMap[category] ?: 0.0
                        val ratio = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else if (spent > 0) 1f else 0f
                        val isExceeded = limit > 0 && spent > limit
                        val diff = limit - spent

                        val catEntity = categories.find { it.name.equals(category, ignoreCase = true) }
                        val catColor = parseCategoryColor(catEntity?.colorHex)

                        val progressColor = when {
                            isExceeded -> Color(0xFFDC2626)
                            spent >= 0.85 * limit -> Color(0xFFD97706)
                            else -> Color(0xFF16A34A)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isExceeded) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEE2E2)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = if (isEn) "Exceeded" else "Dépassé",
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isEn) "Exceeded" else "Dépassé",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = if (isEn) "On Budget" else "En budget",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                categoryForColorPicker = catEntity ?: CategoryEntity(name = category, colorHex = "#3B82F6")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = if (isEn) "Change color" else "Changer la couleur",
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(
                                            onClick = {
                                                selectedCategoryForEdit = category
                                                currentLimitInput = limit.toString()
                                                showEditBudgetDialog = true
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = if (isEn) "Adjust budget" else "Ajuster le budget", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isEn) "Adjust" else "Ajuster", style = MaterialTheme.typography.labelMedium)
                                        }
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

                                // Numerical Details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isEn) "Spent: ${spent.formatCurrency(overviewState.currencySymbol)}" else "Dépensé : ${spent.formatCurrency(overviewState.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isEn) "Limit: ${limit.formatCurrency(overviewState.currencySymbol)}" else "Plafond : ${limit.formatCurrency(overviewState.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = if (isExceeded) {
                                            "-${(spent - limit).formatCurrency(overviewState.currencySymbol)}"
                                        } else {
                                            "+${diff.formatCurrency(overviewState.currencySymbol)} ${if (isEn) "free" else "libre"}"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isExceeded) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Edit Category Budget Limit
    if (showEditBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = { Text("Budget Mensuel - $selectedCategoryForEdit") },
            text = {
                OutlinedTextField(
                    value = currentLimitInput,
                    onValueChange = { currentLimitInput = it },
                    label = { Text("Plafond de dépense (${overviewState.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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

    editingTransaction?.let { tx ->
        FastAddTransactionDialog(
            onDismiss = { editingTransaction = null },
            initialTransaction = tx,
            categories = categories,
            currencySymbol = overviewState.currencySymbol,
            appLanguage = overviewState.language,
            onDelete = { viewModel.deleteTransaction(tx) },
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
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

    categoryForColorPicker?.let { cat ->
        CategoryColorPickerDialog(
            categoryName = cat.name,
            currentColorHex = cat.colorHex,
            onDismiss = { categoryForColorPicker = null },
            onColorSelected = { selectedHex ->
                viewModel.saveCategoryColor(cat.name, selectedHex)
            }
        )
    }
}

@Composable
fun CategoryColorPickerDialog(
    categoryName: String,
    currentColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val colorPalette = listOf(
        "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6",
        "#EC4899", "#EF4444", "#06B6D4", "#84CC16",
        "#F97316", "#6366F1", "#14B8A6", "#64748B"
    )
    var selectedHex by remember { mutableStateOf(currentColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Couleur - $categoryName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Choisissez une couleur d'identification :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rows = colorPalette.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowHexes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowHexes.forEach { hex ->
                                val color = parseCategoryColor(hex)
                                val isChosen = hex.equals(selectedHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
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
                    onColorSelected(selectedHex)
                    onDismiss()
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

