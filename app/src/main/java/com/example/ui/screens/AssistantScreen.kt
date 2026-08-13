package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.CanvasDonutChart
import com.example.ui.components.CategoryExpenseSlice
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.util.formatCurrency
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AssistantScreen(
    viewModel: MainViewModel
) {
    val overviewState by viewModel.overviewState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val isEn = overviewState.language == com.example.ui.viewmodel.AppLanguage.EN

    var userQuery by remember { mutableStateOf("") }
    var chatHistory by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = if (isEn)
                        "Hello! I am your local Financial Assistant. You can enter transactions directly in chat (e.g. 'expense, food, 55.5, Carrefour' or 'income, salary, 2500, Work'). If a category doesn't exist, I will automatically create it for you!"
                    else
                        "Bonjour ! Je suis votre Assistant Financier local. Vous pouvez saisir vos transactions directement par message (ex: 'depense, alimentation, 55.5, Carrefour' ou 'revenu, salaire, 2500, Virement'). Si la catégorie n'existe pas, je la créerai automatiquement !",
                    isUser = false
                )
            )
        )
    }

    // Offline Intelligent Calculations
    val incomeTotal = overviewState.theoreticalIncome
    val expenseTotal = overviewState.theoreticalExpense
    val savings = (incomeTotal - expenseTotal).coerceAtLeast(0.0)
    val savingsRate = if (incomeTotal > 0) ((savings / incomeTotal) * 100).toInt() else 0

    val totalBudgetLimit = budgets.sumOf { it.monthlyLimit }
    val remainingBudget = (totalBudgetLimit - expenseTotal).coerceAtLeast(0.0)

    // Expense breakdown slices
    val palette = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF97316)
    )

    val categoryExpenses = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    val donutSlices = remember(categoryExpenses) {
        categoryExpenses.mapIndexed { idx, entry ->
            CategoryExpenseSlice(
                category = entry.key,
                amount = entry.value,
                color = palette[idx % palette.size]
            )
        }
    }

    val suggestions = if (isEn) listOf(
        "expense, food, 55.5, Carrefour",
        "income, salary, 2500, Monthly pay",
        "How much did I spend this month?",
        "Which category costs the most?",
        "What is my remaining budget?"
    ) else listOf(
        "depense, alimentation, 55.5, Carrefour",
        "revenu, salaire, 2500, Virement",
        "Combien ai-je dépensé ce mois ?",
        "Quelle catégorie coûte le plus ?",
        "Quel est mon budget restant ?"
    )

    data class ParsedTransaction(
        val title: String,
        val amount: Double,
        val type: TransactionType,
        val category: String
    )

    fun guessCategory(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("carrefour") || lower.contains("auchan") || lower.contains("lidl") || lower.contains("monoprix") || lower.contains("courses") || lower.contains("leclerc") -> "Alimentation"
            lower.contains("property") || lower.contains("loyer") || lower.contains("quadral") || lower.contains("edf") || lower.contains("eau") || lower.contains("logement") -> "Logement"
            lower.contains("avanssur") || lower.contains("santé") || lower.contains("pharmacie") || lower.contains("docteur") || lower.contains("mutuelle") -> "Santé"
            lower.contains("uber") || lower.contains("navigo") || lower.contains("essence") || lower.contains("sncf") || lower.contains("transport") -> "Transport"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema") || lower.contains("loisir") -> "Loisirs"
            lower.contains("free") || lower.contains("orange") || lower.contains("sfr") || lower.contains("abonnement") -> "Abonnements"
            lower.contains("salaire") || lower.contains("virement") || lower.contains("paye") || lower.contains("income") -> "Salaire"
            else -> "Autres"
        }
    }

    fun parseTransactionsFromText(input: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotBlank() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lowerLine = line.lowercase()

            // Format 1: Comma / Semicolon / Pipe separated (e.g., "depense, alimentation, 55.5, Carrefour" or "revenu, salaire, 2500, Virement")
            if (line.contains(",") || line.contains(";") || line.contains("|")) {
                val delimiter = when {
                    line.contains(";") -> ";"
                    line.contains("|") -> "|"
                    else -> ","
                }
                val parts = line.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }

                if (parts.size >= 2) {
                    var detectedType: TransactionType? = null
                    var detectedAmount: Double? = null
                    val stringParts = mutableListOf<String>()

                    for (p in parts) {
                        val pLower = p.lowercase()
                        if (detectedType == null && (pLower == "depense" || pLower == "dépense" || pLower == "expense" || pLower == "debit" || pLower == "dépenses" || pLower == "depenses")) {
                            detectedType = TransactionType.EXPENSE
                        } else if (detectedType == null && (pLower == "revenu" || pLower == "income" || pLower == "credit" || pLower == "gain" || pLower == "salaire" || pLower == "revenus")) {
                            detectedType = TransactionType.INCOME
                        } else {
                            val cleanNumStr = p.replace("€", "").replace("$", "").replace("EUR", "").replace("eur", "").replace(" ", "").replace(",", ".")
                            val num = cleanNumStr.toDoubleOrNull()
                            if (detectedAmount == null && num != null && num > 0) {
                                detectedAmount = num
                            } else {
                                stringParts.add(p)
                            }
                        }
                    }

                    val finalType = detectedType ?: if (lowerLine.contains("revenu") || lowerLine.contains("income")) TransactionType.INCOME else TransactionType.EXPENSE

                    if (detectedAmount != null && detectedAmount > 0) {
                        var cat = if (finalType == TransactionType.INCOME) "Salaire" else "Autres"
                        var title = if (finalType == TransactionType.INCOME) "Revenu" else "Dépense"

                        if (stringParts.size >= 2) {
                            cat = stringParts[0]
                            title = stringParts.subList(1, stringParts.size).joinToString(" ")
                        } else if (stringParts.size == 1) {
                            title = stringParts[0]
                            cat = guessCategory(title)
                        }

                        results.add(ParsedTransaction(title = title, amount = detectedAmount, type = finalType, category = cat))
                        i++
                        continue
                    }
                }
            }

            // Format 2: Keywords / Space-separated (e.g. "depense alimentation 55.5 Carrefour" or "revenu salaire 2500 Virement")
            val hasExpenseKeyword = lowerLine.contains("depense") || lowerLine.contains("dépense") || lowerLine.contains("expense")
            val hasIncomeKeyword = lowerLine.contains("revenu") || lowerLine.contains("income") || lowerLine.contains("salaire") || lowerLine.contains("gain")

            if (hasExpenseKeyword || hasIncomeKeyword) {
                val finalType = if (hasIncomeKeyword) TransactionType.INCOME else TransactionType.EXPENSE
                val numRegex = Regex("""(\d+(?:[\s.,]\d+)?)\s*(?:€|\$|EUR|euros|euro)?""", RegexOption.IGNORE_CASE)
                val matchNum = numRegex.find(line)

                if (matchNum != null) {
                    val amtStr = matchNum.groupValues[1].replace(" ", "").replace(",", ".")
                    val amt = amtStr.toDoubleOrNull()

                    if (amt != null && amt > 0) {
                        val cleaned = line
                            .replace(Regex("""(?i)\b(depense|dépense|expense|debit|dépenses|depenses|revenu|income|credit|gain|salaire|revenus)\b"""), "")
                            .replace(matchNum.value, "")
                            .replace(Regex("""[-−:,]"""), " ")
                            .trim()

                        val words = cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }

                        var cat = if (finalType == TransactionType.INCOME) "Salaire" else "Autres"
                        var title = if (finalType == TransactionType.INCOME) "Revenu" else "Dépense"

                        if (words.size >= 2) {
                            cat = words[0]
                            title = words.subList(1, words.size).joinToString(" ")
                        } else if (words.size == 1) {
                            title = words[0]
                            cat = guessCategory(title)
                        }

                        results.add(ParsedTransaction(title = title, amount = amt, type = finalType, category = cat))
                        i++
                        continue
                    }
                }
            }

            // Format 3: Title and Amount single line (e.g., "Carrefour - 50€" or "Carrefour 50€")
            val singleLineRegex = Regex("""^(.+?)\s*[-−:]?\s*[-−]?\s*(\d+(?:[\s.,]\d+)?)\s*(?:€|euro|euros)?$""", RegexOption.IGNORE_CASE)
            val matchSingle = singleLineRegex.find(line)

            if (matchSingle != null && !line.matches(Regex("""^[-−]?\s*\d+(?:[\s.,]\d+)?\s*(?:€|euro|euros)?$""", RegexOption.IGNORE_CASE))) {
                val rawTitle = matchSingle.groupValues[1].trim()
                val rawAmtStr = matchSingle.groupValues[2].replace(" ", "").replace(",", ".")
                val amt = rawAmtStr.toDoubleOrNull()
                if (amt != null && amt > 0 && rawTitle.length > 1) {
                    val type = if (rawTitle.lowercase().contains("salaire") || rawTitle.lowercase().contains("revenu") || rawTitle.lowercase().contains("virement")) TransactionType.INCOME else TransactionType.EXPENSE
                    val cat = if (type == TransactionType.INCOME) "Salaire" else guessCategory(rawTitle)
                    results.add(ParsedTransaction(title = rawTitle, amount = amt, type = type, category = cat))
                    i++
                    continue
                }
            }

            // Format 4: Two-line Title then Amount line
            val amountLineRegex = Regex("""^[-−]?\s*(\d[\d\s.,]*\d|\d+)\s*(?:€|euro|euros)?$""", RegexOption.IGNORE_CASE)
            val matchAmt = amountLineRegex.find(line)

            if (matchAmt != null && i > 0) {
                val prevTitle = lines[i - 1].trim()
                val cleanAmtStr = matchAmt.groupValues[1].replace(" ", "").replace(",", ".")
                val amt = cleanAmtStr.toDoubleOrNull()
                if (amt != null && amt > 0 && prevTitle.isNotBlank()) {
                    val type = if (prevTitle.lowercase().contains("salaire") || prevTitle.lowercase().contains("revenu")) TransactionType.INCOME else TransactionType.EXPENSE
                    val cat = if (type == TransactionType.INCOME) "Salaire" else guessCategory(prevTitle)
                    results.add(ParsedTransaction(title = prevTitle, amount = amt, type = type, category = cat))
                }
            }
            i++
        }
        return results
    }

    fun answerQuery(query: String) {
        val parsedTxs = parseTransactionsFromText(query)

        val reply = if (parsedTxs.isNotEmpty()) {
            val existingCatNames = (categoriesState.map { it.name } + listOf(
                "Alimentation", "Logement", "Transport", "Loisirs", "Santé", "Abonnements", "Autres", "Salaire", "Freelance", "Investissement"
            )).map { it.lowercase().trim() }

            val sb = StringBuilder(
                if (isEn) "✅ **${parsedTxs.size} transaction(s) recorded successfully:**\n\n"
                else "✅ **${parsedTxs.size} opération(s) enregistrée(s) avec succès :**\n\n"
            )

            parsedTxs.forEach { tx ->
                var catName = tx.category.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                if (catName.isBlank()) {
                    catName = if (tx.type == TransactionType.INCOME) "Salaire" else "Autres"
                }

                val isNewCat = !existingCatNames.contains(catName.lowercase())
                if (isNewCat) {
                    viewModel.addCategory(catName, "#3B82F6")
                }

                viewModel.addTransaction(
                    title = tx.title,
                    amount = tx.amount,
                    type = tx.type,
                    category = catName,
                    incomeType = if (tx.type == TransactionType.INCOME) catName else null
                )

                val typeLabel = if (tx.type == TransactionType.INCOME) (if (isEn) "Income" else "Revenu") else (if (isEn) "Expense" else "Dépense")
                val newCatBadge = if (isNewCat) (if (isEn) " 🌟 [New Category Created]" else " 🌟 [Nouvelle catégorie créée]") else ""

                sb.append("• **${tx.title}** : ${tx.amount.formatCurrency(overviewState.currencySymbol)} ($typeLabel | ${catName})$newCatBadge\n")
            }
            sb.toString()
        } else {
            val qLower = query.lowercase().trim()
            when {
                qLower.contains("dépensé") || qLower.contains("combien") -> {
                    val count = transactions.count { it.type == TransactionType.EXPENSE }
                    "Ce mois-ci, vous avez dépensé un total de ${expenseTotal.formatCurrency(overviewState.currencySymbol)} sur $count opérations enregistrées."
                }
                qLower.contains("catégorie") || qLower.contains("plus") || qLower.contains("poste") -> {
                    if (categoryExpenses.isNotEmpty()) {
                        val top = categoryExpenses.first()
                        val pct = if (expenseTotal > 0) ((top.value / expenseTotal) * 100).toInt() else 0
                        "La catégorie la plus coûteuse est '${top.key}' avec ${top.value.formatCurrency(overviewState.currencySymbol)} ($pct% de vos dépenses)."
                    } else {
                        "Aucune dépense enregistrée pour le moment."
                    }
                }
                qLower.contains("budget") || qLower.contains("restant") -> {
                    if (totalBudgetLimit > 0) {
                        "Votre budget prévisionnel global est de ${totalBudgetLimit.formatCurrency(overviewState.currencySymbol)}. Vous avez dépensé ${expenseTotal.formatCurrency(overviewState.currencySymbol)}. Il vous reste ${remainingBudget.formatCurrency(overviewState.currencySymbol)}."
                    } else {
                        "Vous n'avez pas encore défini de limite de budget. Rendez-vous dans la section 'Budget' pour la configurer."
                    }
                }
                qLower.contains("écart") || qLower.contains("différence") || qLower.contains("cohérent") -> {
                    if (overviewState.isCoherent) {
                        "Vos finances sont parfaitement cohérentes ! Solde théorique = ${overviewState.theoreticalBalance.formatCurrency(overviewState.currencySymbol)}, Comptes réels = ${overviewState.realBalance.formatCurrency(overviewState.currencySymbol)}."
                    } else {
                        val diff = kotlin.math.abs(overviewState.theoreticalBalance - overviewState.realBalance)
                        "Écart de ${diff.formatCurrency(overviewState.currencySymbol)} détecté entre le solde théorique (${overviewState.theoreticalBalance.formatCurrency(overviewState.currencySymbol)}) et vos comptes réels (${overviewState.realBalance.formatCurrency(overviewState.currencySymbol)}). Vous pouvez ajuster vos comptes dans l'onglet Outils."
                    }
                }
                qLower.contains("évoluent") || qLower.contains("résumé") || qLower.contains("analyse") || qLower.contains("santé") -> {
                    "Résumé analytique :\n- Revenus : ${incomeTotal.formatCurrency(overviewState.currencySymbol)}\n- Dépenses : ${expenseTotal.formatCurrency(overviewState.currencySymbol)}\n- Taux d'épargne : $savingsRate%\n- Disponibilité théorique : ${overviewState.theoreticalBalance.formatCurrency(overviewState.currencySymbol)}"
                }
                else -> {
                    if (isEn)
                        "Enter your transactions directly like: 'expense, food, 55.5, Carrefour' or 'income, salary, 2500, Monthly pay'."
                    else
                        "Saisissez directement vos transactions ex: 'depense, alimentation, 55.5, Carrefour' ou 'revenu, salaire, 2500, Virement'."
                }
            }
        }

        chatHistory = chatHistory + ChatMessage(text = query, isUser = true) + ChatMessage(text = reply, isUser = false)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("assistant_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isEn) "Financial Assistant" else "Assistant Financier",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn) "Analysis based strictly on your local saved data" else "Analyse basée uniquement sur vos données enregistrées",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (chatHistory.size > 1) {
                    IconButton(onClick = {
                        chatHistory = listOf(
                            ChatMessage(
                                text = if (isEn) "History reset. Ask me a question about your finances." else "Historique réinitialisé. Posez-moi une question sur vos finances.",
                                isUser = false
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isEn) "Clear history" else "Effacer l'historique",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Savings Rate Gauge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEn) "Theoretical Savings Rate" else "Taux d'Épargne Théorique",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$savingsRate%",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (savingsRate >= 15) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = if (isEn) "Savings capacity: ${savings.formatCurrency(overviewState.currencySymbol)}" else "Capacité d'épargne: ${savings.formatCurrency(overviewState.currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (savingsRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = if (savingsRate >= 15) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Donut Chart
        item {
            CanvasDonutChart(
                slices = donutSlices,
                currencySymbol = overviewState.currencySymbol
            )
        }

        // Suggestions Rapides
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isEn) "Quick suggestions" else "Suggestions rapides",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = { answerQuery(suggestion) },
                            label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }

        // Conversation Chat Area
        item {
            Text(
                text = if (isEn) "Chat & Insights" else "Discussion & Analyses",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(chatHistory.size) { idx ->
            val msg = chatHistory[idx]
            val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
            val containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = alignment
            ) {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (msg.isUser) 16.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!msg.isUser) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input Field
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    placeholder = { Text(if (isEn) "Ask a question..." else "Posez une question...", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                IconButton(
                    onClick = {
                        if (userQuery.isNotBlank()) {
                            answerQuery(userQuery)
                            userQuery = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEn) "Send" else "Envoyer",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

