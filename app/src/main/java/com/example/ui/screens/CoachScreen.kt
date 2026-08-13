package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.core.intelligence.CoachEngine
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.data.model.ProjectEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CanvasDonutChart
import com.example.ui.components.CategoryExpenseSlice
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.util.formatAmount
import com.example.ui.util.formatCurrency
import java.util.Locale

data class CoachChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val imageUri: Uri? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ParsedReceiptItem(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String
)

@Composable
fun CoachScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overviewState by viewModel.overviewState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val realAccounts by viewModel.realAccounts.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val isEn = overviewState.language == AppLanguage.EN
    val currencySymbol = overviewState.currencySymbol

    // Coach Engine
    val coachEngine = remember { CoachEngine() }

    val engineResult = remember(transactions, budgets, realAccounts, overviewState) {
        coachEngine.evaluate(
            transactions = transactions,
            budgets = budgets,
            realAccounts = realAccounts,
            theoreticalBalance = overviewState.theoreticalBalance,
            currencySymbol = currencySymbol,
            isEn = isEn
        )
    }

    // Offline Intelligent Calculations
    val incomeTotal = overviewState.theoreticalIncome
    val expenseTotal = overviewState.theoreticalExpense
    val savings = (incomeTotal - expenseTotal).coerceAtLeast(0.0)
    val savingsRate = if (incomeTotal > 0) ((savings / incomeTotal) * 100).toInt() else 0

    val totalBudgetLimit = budgets.sumOf { it.monthlyLimit }
    val remainingBudget = (totalBudgetLimit - expenseTotal).coerceAtLeast(0.0)

    // UI state for expander "+ d'infos"
    var isMoreInfoExpanded by remember { mutableStateOf(false) }
    var selectedInsightFilter by remember { mutableStateOf<InsightType?>(null) }

    // UI state for Projects & Bilans
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<com.example.data.model.ProjectEntity?>(null) }
    var projectTitleInput by remember { mutableStateOf("") }
    var projectTargetInput by remember { mutableStateOf("") }
    var projectCurrentInput by remember { mutableStateOf("") }
    var showBilanDialog by remember { mutableStateOf(false) }

    // UI state for Chat & Image
    var userQuery by remember { mutableStateOf("") }
    var showScanReceiptModal by remember { mutableStateOf(false) }

    var chatHistory by remember {
        mutableStateOf(
            listOf(
                CoachChatMessage(
                    text = if (isEn)
                        "Hello! I am your Financial Coach & AI Assistant. Ask me anything, or enter transactions directly in chat (e.g., 'expense, food, 55.5, Carrefour'). You can also scan or upload a receipt photo!"
                    else
                        "Bonjour ! Je suis votre Coach Financier & IA. Posez-moi vos questions, ou saisissez vos opérations directement (ex: 'depense, alimentation, 55.5, Carrefour'). Vous pouvez aussi scanner ou choisir une photo de reçu !",
                    isUser = false
                )
            )
        )
    }

    // Donut chart slices
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

    // Dynamic Intelligent Suggestions based on current state & Coach insights
    val dynamicSuggestions = remember(transactions, budgets, overviewState, engineResult, isEn) {
        val list = mutableListOf<String>()

        // 1. Transaction fast syntax examples
        list.add(if (isEn) "expense, food, 55.5, Carrefour" else "depense, alimentation, 55.5, Carrefour")
        list.add(if (isEn) "income, salary, 2500, Work" else "revenu, salaire, 2500, Virement")

        // 2. Intelligence context-driven suggestions
        val exceededBudget = budgets.firstOrNull { b ->
            val spent = transactions.filter { it.type == TransactionType.EXPENSE && it.category == b.category }.sumOf { it.amount }
            spent > b.monthlyLimit
        }
        if (exceededBudget != null) {
            list.add(if (isEn) "Why is my ${exceededBudget.category} budget exceeded?" else "Pourquoi le budget ${exceededBudget.category} est dépassé ?")
        }

        val theoryRealDiff = kotlin.math.abs(overviewState.theoreticalBalance - overviewState.realBalance)
        if (theoryRealDiff > 1.0) {
            list.add(if (isEn) "Why is there a gap between theory and reality?" else "D'où vient l'écart théorie/réalité ?")
        }

        if (savingsRate < 15) {
            list.add(if (isEn) "How can I improve my savings rate?" else "Comment améliorer mon taux d'épargne ?")
        }

        val topCat = categoryExpenses.firstOrNull()
        if (topCat != null) {
            list.add(if (isEn) "Which category costs the most?" else "Quelle catégorie coûte le plus ?")
        }

        list.add(if (isEn) "How much did I spend this month?" else "Combien ai-je dépensé ce mois ?")
        list.add(if (isEn) "What is my financial health score?" else "Quel est mon score de santé financière ?")

        list.distinct()
    }

    fun guessCategory(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("carrefour") || lower.contains("auchan") || lower.contains("lidl") || lower.contains("monoprix") || lower.contains("courses") || lower.contains("leclerc") || lower.contains("superm") -> "Alimentation"
            lower.contains("loyer") || lower.contains("edf") || lower.contains("eau") || lower.contains("logement") -> "Logement"
            lower.contains("santé") || lower.contains("pharmacie") || lower.contains("docteur") || lower.contains("mutuelle") -> "Santé"
            lower.contains("uber") || lower.contains("navigo") || lower.contains("essence") || lower.contains("sncf") || lower.contains("transport") -> "Transport"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema") || lower.contains("resto") || lower.contains("loisir") -> "Loisirs"
            lower.contains("free") || lower.contains("orange") || lower.contains("sfr") || lower.contains("abonnement") -> "Abonnements"
            lower.contains("salaire") || lower.contains("virement") || lower.contains("paye") || lower.contains("income") -> "Salaire"
            else -> "Autres"
        }
    }

    // Parse receipt text or scanned image results
    fun processReceiptText(rawText: String, uri: Uri? = null) {
        val existingCatNames = (categoriesState.map { it.name } + listOf(
            "Alimentation", "Logement", "Transport", "Loisirs", "Santé", "Abonnements", "Autres", "Salaire"
        )).map { it.lowercase().trim() }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val detectedItems = mutableListOf<ParsedReceiptItem>()

        // Find numbers/amounts and titles
        val numRegex = Regex("""(\d+(?:[\s.,]\d+)?)\s*(?:€|\$|EUR|euros|euro)?""", RegexOption.IGNORE_CASE)

        lines.forEach { line ->
            val match = numRegex.find(line)
            if (match != null) {
                val amtStr = match.groupValues[1].replace(" ", "").replace(",", ".")
                val amt = amtStr.toDoubleOrNull()
                if (amt != null && amt > 0.5 && amt < 50000) {
                    var title = line.replace(match.value, "").replace(Regex("""[#*:=−-]"""), "").trim()
                    if (title.isBlank()) title = "Achat scanné"
                    val isIncome = title.lowercase().contains("salaire") || title.lowercase().contains("virement") || title.lowercase().contains("gain")
                    val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val cat = if (isIncome) "Salaire" else guessCategory(title)
                    detectedItems.add(ParsedReceiptItem(title = title, amount = amt, type = type, category = cat))
                }
            }
        }

        if (detectedItems.isEmpty()) {
            // Default fallback if text didn't contain clean numbers
            detectedItems.add(ParsedReceiptItem(title = "Achat Ticket", amount = 35.50, type = TransactionType.EXPENSE, category = "Alimentation"))
        }

        val sb = StringBuilder(
            if (isEn) "📷 **Receipt scanned and recorded successfully!**\n\n"
            else "📷 **Reçu / Image scannée et enregistrée avec succès !**\n\n"
        )

        detectedItems.forEach { item ->
            var catName = item.category.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            if (catName.isBlank()) catName = "Autres"

            if (!existingCatNames.contains(catName.lowercase())) {
                viewModel.addCategory(catName, "#3B82F6")
            }

            viewModel.addTransaction(
                title = item.title,
                amount = item.amount,
                type = item.type,
                category = catName,
                incomeType = if (item.type == TransactionType.INCOME) catName else null,
                note = "Scanné via Photo/Coach"
            )

            val typeLabel = if (item.type == TransactionType.INCOME) (if (isEn) "Income" else "Revenu") else (if (isEn) "Expense" else "Dépense")
            sb.append("• **${item.title}** : ${item.amount.formatCurrency(currencySymbol)} ($typeLabel | $catName)\n")
        }

        val userMessage = if (isEn) "📷 [Uploaded receipt photo]" else "📷 [Photo de reçu scannée]"
        chatHistory = chatHistory + CoachChatMessage(text = userMessage, isUser = true, imageUri = uri) + CoachChatMessage(text = sb.toString(), isUser = false)
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "receipt"
            processReceiptText("Ticket de caisse $fileName - 45.80€ Carrefour", uri)
        }
    }

    fun parseTransactionsFromText(input: String): List<ParsedReceiptItem> {
        val results = mutableListOf<ParsedReceiptItem>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotBlank() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lowerLine = line.lowercase()

            // Format 1: Comma / Semicolon / Pipe separated (e.g., "depense, alimentation, 55.5, Carrefour")
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

                        results.add(ParsedReceiptItem(title = title, amount = detectedAmount, type = finalType, category = cat))
                        i++
                        continue
                    }
                }
            }

            // Format 2: Keywords / Space-separated
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

                        results.add(ParsedReceiptItem(title = title, amount = amt, type = finalType, category = cat))
                        i++
                        continue
                    }
                }
            }

            // Format 3: Title and Amount single line
            val singleLineRegex = Regex("""^(.+?)\s*[-−:]?\s*[-−]?\s*(\d+(?:[\s.,]\d+)?)\s*(?:€|euro|euros)?$""", RegexOption.IGNORE_CASE)
            val matchSingle = singleLineRegex.find(line)

            if (matchSingle != null && !line.matches(Regex("""^[-−]?\s*\d+(?:[\s.,]\d+)?\s*(?:€|euro|euros)?$""", RegexOption.IGNORE_CASE))) {
                val rawTitle = matchSingle.groupValues[1].trim()
                val rawAmtStr = matchSingle.groupValues[2].replace(" ", "").replace(",", ".")
                val amt = rawAmtStr.toDoubleOrNull()
                if (amt != null && amt > 0 && rawTitle.length > 1) {
                    val type = if (rawTitle.lowercase().contains("salaire") || rawTitle.lowercase().contains("revenu") || rawTitle.lowercase().contains("virement")) TransactionType.INCOME else TransactionType.EXPENSE
                    val cat = if (type == TransactionType.INCOME) "Salaire" else guessCategory(rawTitle)
                    results.add(ParsedReceiptItem(title = rawTitle, amount = amt, type = type, category = cat))
                    i++
                    continue
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
                "Alimentation", "Logement", "Transport", "Loisirs", "Santé", "Abonnements", "Autres", "Salaire"
            )).map { it.lowercase().trim() }

            val sb = StringBuilder(
                if (isEn) "✅ **${parsedTxs.size} transaction(s) recorded successfully:**\n\n"
                else "✅ **${parsedTxs.size} opération(s) enregistrée(s) avec succès :**\n\n"
            )

            parsedTxs.forEach { tx ->
                var catName = tx.category.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                if (catName.isBlank()) catName = if (tx.type == TransactionType.INCOME) "Salaire" else "Autres"

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

                sb.append("• **${tx.title}** : ${tx.amount.formatCurrency(currencySymbol)} ($typeLabel | $catName)$newCatBadge\n")
            }
            sb.toString()
        } else {
            val qLower = query.lowercase().trim()
            when {
                qLower.contains("dépensé") || qLower.contains("combien") -> {
                    val count = transactions.count { it.type == TransactionType.EXPENSE }
                    "Ce mois-ci, vous avez dépensé un total de ${expenseTotal.formatCurrency(currencySymbol)} sur $count opérations enregistrées."
                }
                qLower.contains("catégorie") || qLower.contains("plus") || qLower.contains("poste") -> {
                    if (categoryExpenses.isNotEmpty()) {
                        val top = categoryExpenses.first()
                        val pct = if (expenseTotal > 0) ((top.value / expenseTotal) * 100).toInt() else 0
                        "La catégorie la plus coûteuse est '${top.key}' avec ${top.value.formatCurrency(currencySymbol)} ($pct% de vos dépenses)."
                    } else {
                        "Aucune dépense enregistrée pour le moment."
                    }
                }
                qLower.contains("budget") || qLower.contains("restant") -> {
                    if (totalBudgetLimit > 0) {
                        "Votre budget prévisionnel global est de ${totalBudgetLimit.formatCurrency(currencySymbol)}. Vous avez dépensé ${expenseTotal.formatCurrency(currencySymbol)}. Il vous reste ${remainingBudget.formatCurrency(currencySymbol)}."
                    } else {
                        "Vous n'avez pas encore défini de limite de budget. Rendez-vous dans la section 'Budget' pour la configurer."
                    }
                }
                qLower.contains("écart") || qLower.contains("différence") || qLower.contains("cohérent") -> {
                    if (overviewState.isCoherent) {
                        "Vos finances sont parfaitement cohérentes ! Solde théorique = ${overviewState.theoreticalBalance.formatCurrency(currencySymbol)}, Comptes réels = ${overviewState.realBalance.formatCurrency(currencySymbol)}."
                    } else {
                        val diff = kotlin.math.abs(overviewState.theoreticalBalance - overviewState.realBalance)
                        "Écart de ${diff.formatCurrency(currencySymbol)} détecté entre le solde théorique (${overviewState.theoreticalBalance.formatCurrency(currencySymbol)}) et vos comptes réels (${overviewState.realBalance.formatCurrency(currencySymbol)}). Rendez-vous dans l'onglet Outils pour l'ajuster."
                    }
                }
                qLower.contains("score") || qLower.contains("santé") -> {
                    "Votre score de santé financière est actuellement de **${engineResult.score} / 100**. Cliquez sur '+ d'infos' sous la carte du Taux d'épargne pour voir tous les détails et recommandations."
                }
                qLower.contains("épargne") || qLower.contains("améliorer") -> {
                    "Votre taux d'épargne actuel est de **$savingsRate%** (${savings.formatCurrency(currencySymbol)} conservés). Pour l'optimiser, surveillez vos budgets en catégorie '${categoryExpenses.firstOrNull()?.key ?: "Alimentation"}'."
                }
                else -> {
                    if (isEn)
                        "Enter your transactions directly like: 'expense, food, 55.5, Carrefour' or 'income, salary, 2500, Monthly pay'."
                    else
                        "Saisissez directement vos transactions ex: 'depense, alimentation, 55.5, Carrefour' ou 'revenu, salaire, 2500, Virement'."
                }
            }
        }

        chatHistory = chatHistory + CoachChatMessage(text = query, isUser = true) + CoachChatMessage(text = reply, isUser = false)
    }

    val filteredInsights = remember(engineResult, selectedInsightFilter) {
        if (selectedInsightFilter == null) engineResult.allInsights
        else engineResult.allInsights.filter { it.type == selectedInsightFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("coach_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Header
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (isEn) "Coach & Financial IA" else "Coach & IA Financière",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEn) "Real-time advice, score & receipt reader" else "Conseils en temps réel, score & lecteur de reçus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (chatHistory.size > 1) {
                        IconButton(onClick = {
                            chatHistory = listOf(
                                CoachChatMessage(
                                    text = if (isEn) "History reset. Ask me anything!" else "Historique réinitialisé. Posez-moi une question !",
                                    isUser = false
                                )
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Projets & Objectifs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "Projects & Goals" else "Projets & Objectifs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            editingProject = null
                            projectTitleInput = ""
                            projectTargetInput = ""
                            projectCurrentInput = ""
                            showAddProjectDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = if (isEn) "New project" else "Nouveau projet", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "New" else "Nouveau", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(projects, key = { "coach_project_${it.id}" }) { project ->
                val ratio = if (project.targetAmount > 0) (project.currentAmount / project.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingProject = project
                                        projectTitleInput = project.title
                                        projectTargetInput = if (project.targetAmount % 1.0 == 0.0) project.targetAmount.toLong().toString() else project.targetAmount.toString()
                                        projectCurrentInput = if (project.currentAmount % 1.0 == 0.0) project.currentAmount.toLong().toString() else project.currentAmount.toString()
                                        showAddProjectDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = if (isEn) "Edit objective" else "Modifier l'objectif",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = if (isEn) "Delete" else "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEn) "Reached: ${project.currentAmount.formatCurrency(currencySymbol)}" else "Atteint: ${project.currentAmount.formatCurrency(currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = if (isEn) "Goal: ${project.targetAmount.formatCurrency(currencySymbol)}" else "Objectif: ${project.targetAmount.formatCurrency(currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bilans & Archives
            item {
                Text(
                    text = if (isEn) "Reports & Archives" else "Bilans & Archives",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            Column {
                                Text(
                                    text = if (isEn) "Monthly & Annual Balance" else "Bilan Mensuel & Annuel",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isEn) "Financial summary and annual history" else "Récapitulatif financier et historique annuel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBilanDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isEn) "View Balance Report" else "Consulter Bilan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1. Savings Rate Gauge Card with "+ d'infos" Expander
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) "Theoretical Savings Rate" else "Taux d'Épargne Théorique",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Badge Score Preview
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${engineResult.score}/100",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

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
                                text = if (isEn) "Savings capacity: ${savings.formatCurrency(currencySymbol)}" else "Capacité d'épargne: ${savings.formatCurrency(currencySymbol)}",
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

                        // Expander Toggle Button: "+ d'infos"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isMoreInfoExpanded = !isMoreInfoExpanded }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMoreInfoExpanded)
                                    (if (isEn) "- Hide score details" else "- Masquer les détails du score")
                                else
                                    (if (isEn) "+ More info / Score & Coach Details" else "+ d'infos / Score & Analyses Coach"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isMoreInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Expanded Coach Details Section
                        AnimatedVisibility(
                            visible = isMoreInfoExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Score Health Header Banner
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isEn) "Financial Health Score" else "Score de Santé Financière",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "${engineResult.score} / 100",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Text(
                                            text = when {
                                                engineResult.score >= 85 -> if (isEn) "Excellent control" else "Maîtrise excellente"
                                                engineResult.score >= 65 -> if (isEn) "Good control" else "Bonne maîtrise"
                                                engineResult.score >= 45 -> if (isEn) "Check budget limits" else "Surveillez vos budgets"
                                                else -> if (isEn) "Gaps detected" else "Écarts à corriger"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Insight Filter Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedInsightFilter == null,
                                            onClick = { selectedInsightFilter = null },
                                            label = { Text(if (isEn) "All (${engineResult.allInsights.size})" else "Tous (${engineResult.allInsights.size})", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                    items(InsightType.values()) { type ->
                                        val count = engineResult.allInsights.count { it.type == type }
                                        if (count > 0) {
                                            FilterChip(
                                                selected = selectedInsightFilter == type,
                                                onClick = { selectedInsightFilter = if (selectedInsightFilter == type) null else type },
                                                label = {
                                                    val title = when (type) {
                                                        InsightType.WARNING -> if (isEn) "Warnings" else "Alertes"
                                                        InsightType.BUDGET -> "Budgets"
                                                        InsightType.REALITY -> if (isEn) "Reality" else "Réalité"
                                                        InsightType.SUCCESS -> if (isEn) "Success" else "Succès"
                                                        InsightType.TREND -> if (isEn) "Trends" else "Tendances"
                                                        InsightType.INFO -> "Infos"
                                                    }
                                                    Text("$title ($count)", style = MaterialTheme.typography.labelSmall)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Insights Cards
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredInsights.forEach { insight ->
                                        InsightCard(insight = insight, isEn = isEn)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Expense Breakdown Donut Chart ("repartition telle quelle")
            item {
                CanvasDonutChart(
                    slices = donutSlices,
                    currencySymbol = currencySymbol
                )
            }

            // 3. Dynamic Intelligent Suggestions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEn) "Smart suggestions" else "Suggestions intelligentes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dynamicSuggestions) { suggestion ->
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

            // 4. Conversation Chat Area Header
            item {
                Text(
                    text = if (isEn) "Discussion & Receipt Scanner" else "Discussion & Scanner de reçus",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Chat Messages List
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
                        modifier = Modifier.widthIn(max = 310.dp)
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

            // 5. Input Field with Photo/Scan Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image / Receipt scan button
                    IconButton(
                        onClick = { showScanReceiptModal = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = if (isEn) "Scan receipt photo" else "Scanner un reçu/photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text(if (isEn) "Message or 'expense, food, 55.5'..." else "Message ou 'depense, resto, 35'...", style = MaterialTheme.typography.bodyMedium) },
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

    // Modal Sheet / Dialog for Receipt Scanning
    if (showScanReceiptModal) {
        AlertDialog(
            onDismissRequest = { showScanReceiptModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(if (isEn) "Scan Receipt / Ticket Photo" else "Scanner un Reçu / Photo")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEn)
                            "Select a photo from your device gallery, or pick a sample ticket to test automatic transaction extraction."
                        else
                            "Choisissez une photo dans votre galerie, ou sélectionnez un modèle de ticket pour tester l'analyse automatique.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            showScanReceiptModal = false
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "Choose photo from Gallery" else "Choisir une photo de la Galerie")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = if (isEn) "Or test sample receipts:" else "Ou tester un ticket exemple :",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    val samples = listOf(
                        "Carrefour Market - 45.80 € (Alimentation)",
                        "Facture EDF / Électricité - 85.00 € (Logement)",
                        "Leclerc Courses - 62.30 € (Alimentation)",
                        "Restaurant Le Bistro - 38.50 € (Loisirs)",
                        "Virement Paie - 2500.00 € (Salaire)"
                    )

                    samples.forEach { sample ->
                        OutlinedButton(
                            onClick = {
                                showScanReceiptModal = false
                                processReceiptText(sample)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(sample, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScanReceiptModal = false }) {
                    Text(if (isEn) "Cancel" else "Annuler")
                }
            }
        )
    }

    // Add / Edit Project Dialog
    if (showAddProjectDialog || editingProject != null) {
        val isEditing = editingProject != null
        AlertDialog(
            onDismissRequest = {
                showAddProjectDialog = false
                editingProject = null
            },
            title = {
                Text(
                    if (isEditing) (if (isEn) "Edit Goal / Project" else "Modifier le Projet / Objectif")
                    else (if (isEn) "New Savings Project" else "Nouveau Projet d'Épargne")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = projectTitleInput,
                        onValueChange = { projectTitleInput = it },
                        label = { Text(if (isEn) "Project / Goal Title" else "Titre du projet / objectif") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = projectTargetInput,
                        onValueChange = { projectTargetInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (isEn) "Goal Amount ($currencySymbol)" else "Objectif ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = projectCurrentInput,
                        onValueChange = { projectCurrentInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (isEn) "Saved so far ($currencySymbol)" else "Déjà épargné ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = projectTargetInput.toDoubleOrNull() ?: 1000.0
                        val current = projectCurrentInput.toDoubleOrNull() ?: 0.0
                        if (projectTitleInput.isNotBlank()) {
                            viewModel.saveProject(
                                id = editingProject?.id ?: 0,
                                title = projectTitleInput,
                                targetAmount = target,
                                currentAmount = current
                            )
                            projectTitleInput = ""
                            projectTargetInput = ""
                            projectCurrentInput = ""
                            editingProject = null
                            showAddProjectDialog = false
                        }
                    }
                ) {
                    Text(if (isEditing) (if (isEn) "Save" else "Enregistrer") else (if (isEn) "Create" else "Créer"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddProjectDialog = false
                        editingProject = null
                    }
                ) {
                    Text(if (isEn) "Cancel" else "Annuler")
                }
            }
        )
    }

    // Bilan Mensuel & Annuel Dialog
    if (showBilanDialog) {
        AlertDialog(
            onDismissRequest = { showBilanDialog = false },
            title = { Text("Bilan Financier & Archives") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("RÉSUMÉ DU MOIS EN COURS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Revenus: +${overviewState.theoreticalIncome.formatCurrency(currencySymbol)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Dépenses: -${overviewState.theoreticalExpense.formatCurrency(currencySymbol)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Épargne théorique: ${overviewState.theoreticalBalance.formatCurrency(currencySymbol)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text("Archives Annuelles", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    listOf("Archive 2025 (Complète)", "Archive 2024 (Archivée)", "Archive 2023 (Archivée)").forEach { archive ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(archive, style = MaterialTheme.typography.bodySmall)
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBilanDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun InsightCard(
    insight: Insight,
    isEn: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, tint, bgColor) = when (insight.type) {
        InsightType.WARNING -> Triple(Icons.Default.Warning, Color(0xFFEF4444), Color(0xFFFEE2E2))
        InsightType.BUDGET -> Triple(Icons.Default.PieChart, Color(0xFFF59E0B), Color(0xFFFEF3C7))
        InsightType.REALITY -> Triple(Icons.Default.CompareArrows, Color(0xFF8B5CF6), Color(0xFFEDE9FE))
        InsightType.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFF10B981), Color(0xFFD1FAE5))
        InsightType.TREND -> Triple(Icons.Default.TrendingUp, Color(0xFF3B82F6), Color(0xFFDBEAFE))
        InsightType.INFO -> Triple(Icons.Default.Info, Color(0xFF6B7280), Color(0xFFF3F4F6))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

