package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.ui.util.formatCurrency
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class FinancialOverviewState(
    val theoreticalIncome: Double = 0.0,
    val theoreticalExpense: Double = 0.0,
    val theoreticalBalance: Double = 0.0,
    val realBalance: Double = 0.0,
    val difference: Double = 0.0,
    val isCoherent: Boolean = true,
    val statusText: String = "COHÉRENT",
    val currencySymbol: String = "€",
    val language: AppLanguage = AppLanguage.FR
)

data class BackupHistoryLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val dateStr: String,
    val sizeBytes: Long,
    val status: String,
    val isSafetyBackup: Boolean = false,
    val fullPath: String = ""
) {
    val file: File get() = File(fullPath)
}

data class IncomeLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double
)

data class PlanAllocation(
    val categoryName: String,
    val allocatedAmount: Double,
    val colorHex: String = "#3F51B5",
    val linkToCategoryLimit: Boolean = true
)

data class MonthlyPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val monthYear: String,
    val expectedIncome: Double = 3000.0,
    val incomeLines: List<IncomeLine> = listOf(
        IncomeLine(title = "Salaire principal", amount = 3000.0)
    ),
    val allocations: List<PlanAllocation>
) {
    val totalExpectedIncome: Double
        get() = if (incomeLines.isNotEmpty()) incomeLines.sumOf { it.amount } else expectedIncome
}

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage {
    FR, EN
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("budget_control_prefs", Context.MODE_PRIVATE)
    private val repository: FinanceRepository

    val transactions: StateFlow<List<TransactionEntity>>
    val budgets: StateFlow<List<BudgetEntity>>
    val realAccounts: StateFlow<List<RealAccountEntity>>
    val projects: StateFlow<List<ProjectEntity>>
    val categories: StateFlow<List<CategoryEntity>>

    private val _currency = MutableStateFlow("€")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _appTheme = MutableStateFlow(
        try {
            val savedTheme = prefs.getString("app_theme_mode", "LIGHT")
            if (savedTheme == "DARK") AppThemeMode.DARK else AppThemeMode.LIGHT
        } catch (e: Exception) { AppThemeMode.LIGHT }
    )
    val appTheme: StateFlow<AppThemeMode> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            val savedLang = prefs.getString("app_language", "FR")
            if (savedLang == "EN") AppLanguage.EN else AppLanguage.FR
        } catch (e: Exception) { AppLanguage.FR }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setAppTheme(mode: AppThemeMode) {
        val finalMode = if (mode == AppThemeMode.SYSTEM) AppThemeMode.LIGHT else mode
        _appTheme.value = finalMode
        prefs.edit().putString("app_theme_mode", finalMode.name).apply()
    }

    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _backupLogs = MutableStateFlow<List<BackupHistoryLog>>(
        listOf(
            BackupHistoryLog(
                fileName = "BudgetControlPilot_backup_initial.json",
                dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                sizeBytes = 2048,
                status = "SUCCÈS",
                isSafetyBackup = false
            )
        )
    )
    val backupLogs: StateFlow<List<BackupHistoryLog>> = _backupLogs.asStateFlow()

    private fun savePlanToPrefs(plan: MonthlyPlan) {
        try {
            val json = JSONObject().apply {
                put("id", plan.id)
                put("monthYear", plan.monthYear)
                put("expectedIncome", plan.expectedIncome)
                val linesArr = JSONArray()
                plan.incomeLines.forEach { line ->
                    linesArr.put(JSONObject().apply {
                        put("id", line.id)
                        put("title", line.title)
                        put("amount", line.amount)
                    })
                }
                put("incomeLines", linesArr)
                val allocsArr = JSONArray()
                plan.allocations.forEach { alloc ->
                    allocsArr.put(JSONObject().apply {
                        put("categoryName", alloc.categoryName)
                        put("allocatedAmount", alloc.allocatedAmount)
                        put("colorHex", alloc.colorHex)
                        put("linkToCategoryLimit", alloc.linkToCategoryLimit)
                    })
                }
                put("allocations", allocsArr)
            }
            prefs.edit().putString("saved_current_plan", json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPlanFromPrefs(): MonthlyPlan? {
        try {
            val jsonStr = prefs.getString("saved_current_plan", null) ?: return null
            val json = JSONObject(jsonStr)
            val id = json.optString("id", java.util.UUID.randomUUID().toString())
            val monthYear = json.optString("monthYear", "Août 2026")
            val expectedIncome = json.optDouble("expectedIncome", 3000.0)

            val linesList = mutableListOf<IncomeLine>()
            val linesArr = json.optJSONArray("incomeLines")
            if (linesArr != null) {
                for (i in 0 until linesArr.length()) {
                    val item = linesArr.getJSONObject(i)
                    linesList.add(
                        IncomeLine(
                            id = item.optString("id", java.util.UUID.randomUUID().toString()),
                            title = item.optString("title", "Revenu"),
                            amount = item.optDouble("amount", 0.0)
                        )
                    )
                }
            }

            val allocsList = mutableListOf<PlanAllocation>()
            val allocsArr = json.optJSONArray("allocations")
            if (allocsArr != null) {
                for (i in 0 until allocsArr.length()) {
                    val item = allocsArr.getJSONObject(i)
                    allocsList.add(
                        PlanAllocation(
                            categoryName = item.optString("categoryName", ""),
                            allocatedAmount = item.optDouble("allocatedAmount", 0.0),
                            colorHex = item.optString("colorHex", "#3F51B5"),
                            linkToCategoryLimit = item.optBoolean("linkToCategoryLimit", true)
                        )
                    )
                }
            }
            return MonthlyPlan(
                id = id,
                monthYear = monthYear,
                expectedIncome = expectedIncome,
                incomeLines = if (linesList.isNotEmpty()) linesList else listOf(IncomeLine(title = "Salaire principal", amount = expectedIncome)),
                allocations = allocsList
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun syncPlanToDatabaseBudgets(plan: MonthlyPlan) {
        viewModelScope.launch {
            plan.allocations.filter { it.linkToCategoryLimit }.forEach { alloc ->
                repository.saveBudget(
                    BudgetEntity(
                        category = alloc.categoryName,
                        monthlyLimit = alloc.allocatedAmount,
                        monthYear = "CURRENT"
                    )
                )
            }
        }
    }

    private val _currentPlan = MutableStateFlow(
        MonthlyPlan(
            monthYear = "Août 2026",
            expectedIncome = 3000.0,
            allocations = listOf(
                PlanAllocation("Logement", 1000.0, "#3F51B5"),
                PlanAllocation("Alimentation", 400.0, "#4CAF50"),
                PlanAllocation("Transport", 200.0, "#FF9800"),
                PlanAllocation("Loisirs", 200.0, "#E91E63"),
                PlanAllocation("Épargne", 500.0, "#00BCD4"),
                PlanAllocation("Autres", 300.0, "#9C27B0")
            )
        )
    )
    val currentPlan: StateFlow<MonthlyPlan> = _currentPlan.asStateFlow()

    private val _planHistory = MutableStateFlow<List<MonthlyPlan>>(
        listOf(
            MonthlyPlan(
                monthYear = "Juillet 2026",
                expectedIncome = 2800.0,
                allocations = listOf(
                    PlanAllocation("Logement", 1000.0, "#3F51B5"),
                    PlanAllocation("Alimentation", 400.0, "#4CAF50"),
                    PlanAllocation("Transport", 150.0, "#FF9800"),
                    PlanAllocation("Épargne", 600.0, "#00BCD4")
                )
            )
        )
    )
    val planHistory: StateFlow<List<MonthlyPlan>> = _planHistory.asStateFlow()

    fun addIncomeLine(title: String, amount: Double) {
        val current = _currentPlan.value
        val newLines = current.incomeLines + IncomeLine(title = title, amount = amount.coerceAtLeast(0.0))
        val newTotal = newLines.sumOf { it.amount }
        val newPlan = current.copy(incomeLines = newLines, expectedIncome = newTotal)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
    }

    fun removeIncomeLine(id: String) {
        val current = _currentPlan.value
        val newLines = current.incomeLines.filterNot { it.id == id }
        val newTotal = newLines.sumOf { it.amount }
        val newPlan = current.copy(incomeLines = newLines, expectedIncome = newTotal)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
    }

    fun updateIncomeLine(id: String, title: String, amount: Double) {
        val current = _currentPlan.value
        val newLines = current.incomeLines.map {
            if (it.id == id) it.copy(title = title, amount = amount.coerceAtLeast(0.0)) else it
        }
        val newTotal = newLines.sumOf { it.amount }
        val newPlan = current.copy(incomeLines = newLines, expectedIncome = newTotal)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
    }

    fun updatePlanIncome(income: Double) {
        val newPlan = _currentPlan.value.copy(
            expectedIncome = income.coerceAtLeast(0.0),
            incomeLines = listOf(IncomeLine(title = "Revenu global", amount = income.coerceAtLeast(0.0)))
        )
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
    }

    fun updatePlanMonth(monthYear: String) {
        val newPlan = _currentPlan.value.copy(monthYear = monthYear)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
    }

    fun updatePlanAllocation(categoryName: String, amount: Double, linkToCategoryLimit: Boolean? = null) {
        val current = _currentPlan.value
        val updatedAllocations = current.allocations.map {
            if (it.categoryName.equals(categoryName, ignoreCase = true)) {
                it.copy(
                    allocatedAmount = amount.coerceAtLeast(0.0),
                    linkToCategoryLimit = linkToCategoryLimit ?: it.linkToCategoryLimit
                )
            } else it
        }
        val newPlan = current.copy(allocations = updatedAllocations)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
        syncPlanToDatabaseBudgets(newPlan)
    }

    fun togglePlanAllocationLink(categoryName: String, linkToCategoryLimit: Boolean) {
        val current = _currentPlan.value
        val updatedAllocations = current.allocations.map {
            if (it.categoryName.equals(categoryName, ignoreCase = true)) {
                it.copy(linkToCategoryLimit = linkToCategoryLimit)
            } else it
        }
        val newPlan = current.copy(allocations = updatedAllocations)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
        if (linkToCategoryLimit) {
            syncPlanToDatabaseBudgets(newPlan)
        }
    }

    fun addPlanCategory(categoryName: String, amount: Double, colorHex: String = "#3F51B5", linkToCategoryLimit: Boolean = true) {
        val current = _currentPlan.value
        val newAllocations = if (current.allocations.any { it.categoryName.equals(categoryName, ignoreCase = true) }) {
            current.allocations.map {
                if (it.categoryName.equals(categoryName, ignoreCase = true)) {
                    it.copy(allocatedAmount = amount.coerceAtLeast(0.0), linkToCategoryLimit = linkToCategoryLimit)
                } else it
            }
        } else {
            current.allocations + PlanAllocation(categoryName, amount.coerceAtLeast(0.0), colorHex, linkToCategoryLimit = linkToCategoryLimit)
        }
        val newPlan = current.copy(allocations = newAllocations)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
        addCategory(categoryName, colorHex)
        syncPlanToDatabaseBudgets(newPlan)
    }

    fun removePlanCategory(categoryName: String) {
        val current = _currentPlan.value
        val filtered = current.allocations.filterNot { it.categoryName.equals(categoryName, ignoreCase = true) }
        val newPlan = current.copy(allocations = filtered)
        _currentPlan.value = newPlan
        savePlanToPrefs(newPlan)
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    category = categoryName,
                    monthlyLimit = 0.0,
                    monthYear = "CURRENT"
                )
            )
        }
    }

    fun movePlanAllocationUp(index: Int) {
        val current = _currentPlan.value
        val list = current.allocations.toMutableList()
        if (index > 0 && index < list.size) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            val newPlan = current.copy(allocations = list)
            _currentPlan.value = newPlan
            savePlanToPrefs(newPlan)
        }
    }

    fun movePlanAllocationDown(index: Int) {
        val current = _currentPlan.value
        val list = current.allocations.toMutableList()
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            val newPlan = current.copy(allocations = list)
            _currentPlan.value = newPlan
            savePlanToPrefs(newPlan)
        }
    }

    fun saveCurrentPlanToHistory() {
        val current = _currentPlan.value
        val existingIndex = _planHistory.value.indexOfFirst { it.monthYear == current.monthYear }
        if (existingIndex >= 0) {
            val list = _planHistory.value.toMutableList()
            list[existingIndex] = current
            _planHistory.value = list
        } else {
            _planHistory.value = listOf(current) + _planHistory.value
        }
    }

    fun loadPlanFromHistory(plan: MonthlyPlan) {
        _currentPlan.value = plan
        savePlanToPrefs(plan)
        syncPlanToDatabaseBudgets(plan)
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)

        val savedPlan = loadPlanFromPrefs()
        if (savedPlan != null) {
            _currentPlan.value = savedPlan
        }

        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
            syncPlanToDatabaseBudgets(_currentPlan.value)
            refreshBackupLogs()
        }

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        budgets = repository.allBudgets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        realAccounts = repository.allRealAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        projects = repository.allProjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        categories = repository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val overviewState: StateFlow<FinancialOverviewState> = combine(
        transactions,
        realAccounts,
        _currency,
        _appLanguage
    ) { txList, accounts, currSymbol, lang ->
        val totalIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val theoreticalBal = totalIncome - totalExpense

        val realBal = accounts.sumOf { acc ->
            if (acc.accountType.equals("DEBT", ignoreCase = true) || acc.accountType.equals("CREDIT", ignoreCase = true) || acc.accountType.equals("DETTE", ignoreCase = true)) {
                -abs(acc.balance)
            } else {
                acc.balance
            }
        }
        val diff = realBal - theoreticalBal
        val coherent = abs(diff) < 1.0

        val status = if (coherent) {
            if (lang == AppLanguage.EN) "RECONCILED / COHERENT" else "COHÉRENT"
        } else {
            val formattedDiff = abs(diff).formatCurrency(currSymbol)
            val sign = if (diff > 0) "+" else "-"
            if (lang == AppLanguage.EN) "Difference: $sign$formattedDiff" else "Différence : $sign$formattedDiff"
        }

        FinancialOverviewState(
            theoreticalIncome = totalIncome,
            theoreticalExpense = totalExpense,
            theoreticalBalance = theoreticalBal,
            realBalance = realBal,
            difference = diff,
            isCoherent = coherent,
            statusText = status,
            currencySymbol = currSymbol,
            language = lang
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialOverviewState())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedTypeFilter
    ) { list, query, cat, type ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() || tx.title.contains(query, ignoreCase = true) || tx.category.contains(query, ignoreCase = true)
            val matchesCat = cat == null || tx.category == cat
            val matchesType = type == null || tx.type == type
            matchesQuery && matchesCat && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setCurrency(symbol: String) {
        _currency.value = symbol
    }

    // Actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        incomeType: String? = null,
        note: String? = null,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title.ifBlank { if (type == TransactionType.INCOME) "Revenu" else "Dépense" },
                    amount = amount,
                    type = type,
                    category = category.ifBlank { if (type == TransactionType.INCOME) "Autre" else "Autres" },
                    incomeType = if (type == TransactionType.INCOME) (incomeType ?: "Autre") else null,
                    note = note,
                    date = date
                )
            )
        }
    }

    fun updateTransaction(
        id: Long,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        incomeType: String? = null,
        note: String? = null,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.updateTransaction(
                TransactionEntity(
                    id = id,
                    title = title.ifBlank { if (type == TransactionType.INCOME) "Revenu" else "Dépense" },
                    amount = amount,
                    type = type,
                    category = category.ifBlank { if (type == TransactionType.INCOME) "Autre" else "Autres" },
                    incomeType = if (type == TransactionType.INCOME) (incomeType ?: "Autre") else null,
                    note = note,
                    date = date
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit,
                    monthYear = "CURRENT"
                )
            )
        }
        val current = _currentPlan.value
        val alloc = current.allocations.find { it.categoryName.equals(category, ignoreCase = true) }
        if (alloc == null || alloc.linkToCategoryLimit) {
            val updatedAllocations = if (alloc != null) {
                current.allocations.map {
                    if (it.categoryName.equals(category, ignoreCase = true)) {
                        it.copy(allocatedAmount = monthlyLimit.coerceAtLeast(0.0))
                    } else it
                }
            } else {
                current.allocations + PlanAllocation(category, monthlyLimit.coerceAtLeast(0.0), "#3F51B5", linkToCategoryLimit = true)
            }
            val newPlan = current.copy(allocations = updatedAllocations)
            _currentPlan.value = newPlan
            savePlanToPrefs(newPlan)
        }
    }

    fun saveRealAccount(id: Long = 0, name: String, balance: Double, accountType: String = "BANK") {
        viewModelScope.launch {
            repository.saveRealAccount(
                RealAccountEntity(
                    id = id,
                    name = name,
                    balance = balance,
                    accountType = accountType
                )
            )
        }
    }

    fun updateAccountBalance(id: Long, newBalance: Double) {
        viewModelScope.launch {
            repository.updateRealAccountBalance(id, newBalance)
        }
    }

    fun deleteRealAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteRealAccount(id)
        }
    }

    fun saveProject(id: Long = 0, title: String, targetAmount: Double, currentAmount: Double, note: String? = null) {
        viewModelScope.launch {
            repository.saveProject(
                ProjectEntity(
                    id = id,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    note = note
                )
            )
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun addCategory(name: String, colorHex: String = "#3B82F6") {
        viewModelScope.launch {
            repository.saveCategory(
                CategoryEntity(
                    name = name,
                    isExpense = true,
                    colorHex = colorHex
                )
            )
        }
    }

    fun saveCategoryColor(name: String, colorHex: String) {
        viewModelScope.launch {
            val existing = categories.value.find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                repository.saveCategory(existing.copy(colorHex = colorHex))
            } else {
                repository.saveCategory(
                    CategoryEntity(
                        name = name,
                        isExpense = true,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    private fun getBackupFolder(): File {
        val folder = File(getApplication<Application>().filesDir, "backups")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun refreshBackupLogs() {
        val folder = getBackupFolder()
        val files = folder.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        val logs = files.map { file ->
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            BackupHistoryLog(
                id = file.name,
                fileName = file.name,
                dateStr = dateStr,
                sizeBytes = file.length(),
                status = if (file.name.contains("Safety", ignoreCase = true)) "SÉCURITÉ" else "DISPONIBLE",
                isSafetyBackup = file.name.contains("Safety", ignoreCase = true),
                fullPath = file.absolutePath
            )
        }
        _backupLogs.value = logs
    }

    fun restoreBackupFile(fileName: String): Boolean {
        val folder = getBackupFolder()
        val file = File(folder, fileName)
        if (file.exists()) {
            val jsonStr = file.readText()
            return importDataFromJson(jsonStr)
        }
        return false
    }

    fun exportDataAsJson(): String {
        val txList = transactions.value
        val budgetList = budgets.value
        val accountList = realAccounts.value
        val projectList = projects.value
        val catList = categories.value

        val json = org.json.JSONObject()
        json.put("database_version", "BCP_BACKUP_V1")
        json.put("app_name", "BudgetControlPilot")
        json.put("exported_at", System.currentTimeMillis())

        val settingsObj = org.json.JSONObject()
        settingsObj.put("currency", _currency.value)
        json.put("settings", settingsObj)

        val txArray = org.json.JSONArray()
        txList.forEach { tx ->
            val obj = org.json.JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type.name)
            obj.put("category", tx.category)
            obj.put("incomeType", tx.incomeType ?: "")
            obj.put("note", tx.note ?: "")
            obj.put("date", tx.date)
            txArray.put(obj)
        }
        json.put("transactions", txArray)

        val budgetArray = org.json.JSONArray()
        budgetList.forEach { b ->
            val obj = org.json.JSONObject()
            obj.put("id", b.id)
            obj.put("category", b.category)
            obj.put("monthlyLimit", b.monthlyLimit)
            obj.put("monthYear", b.monthYear)
            budgetArray.put(obj)
        }
        json.put("budgets", budgetArray)

        val accountsArray = org.json.JSONArray()
        accountList.forEach { acc ->
            val obj = org.json.JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("balance", acc.balance)
            obj.put("accountType", acc.accountType)
            accountsArray.put(obj)
        }
        json.put("realAccounts", accountsArray)

        val projectsArray = org.json.JSONArray()
        projectList.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("targetAmount", p.targetAmount)
            obj.put("currentAmount", p.currentAmount)
            obj.put("note", p.note ?: "")
            projectsArray.put(obj)
        }
        json.put("projects", projectsArray)

        val categoriesArray = org.json.JSONArray()
        catList.forEach { c ->
            val obj = org.json.JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("isExpense", c.isExpense)
            obj.put("colorHex", c.colorHex)
            categoriesArray.put(obj)
        }
        json.put("categories", categoriesArray)

        val planObj = org.json.JSONObject()
        planObj.put("monthYear", _currentPlan.value.monthYear)
        planObj.put("expectedIncome", _currentPlan.value.expectedIncome)
        val allocArr = org.json.JSONArray()
        _currentPlan.value.allocations.forEach { a ->
            val aObj = org.json.JSONObject()
            aObj.put("categoryName", a.categoryName)
            aObj.put("allocatedAmount", a.allocatedAmount)
            aObj.put("colorHex", a.colorHex)
            allocArr.put(aObj)
        }
        planObj.put("allocations", allocArr)
        json.put("currentPlan", planObj)

        val resultStr = json.toString(2)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "BudgetControl_Backup_$timeStamp.json"
        
        try {
            val file = File(getBackupFolder(), fileName)
            file.writeText(resultStr)
            refreshBackupLogs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultStr
    }

    fun createSafetyBackup() {
        val backupJson = exportDataAsJson()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val fileName = "Safety_AutoBackup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
        val safetyLog = BackupHistoryLog(
            fileName = fileName,
            dateStr = dateStr,
            sizeBytes = backupJson.toByteArray().size.toLong(),
            status = "SÉCURITÉ",
            isSafetyBackup = true
        )
        _backupLogs.value = listOf(safetyLog) + _backupLogs.value
    }

    fun restoreBackupFile(file: File): Boolean {
        return if (file.exists()) {
            importDataFromJson(file.readText())
        } else false
    }

    fun importDataFromJson(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            
            // Create automatic safety backup before restoring
            createSafetyBackup()

            viewModelScope.launch {
                if (json.has("settings")) {
                    val settingsObj = json.getJSONObject("settings")
                    if (settingsObj.has("currency")) {
                        _currency.value = settingsObj.getString("currency")
                    }
                }

                if (json.has("transactions")) {
                    val arr = json.getJSONArray("transactions")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val typeStr = obj.optString("type", "EXPENSE")
                        val type = if (typeStr == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                        val incType = if (obj.has("incomeType")) obj.optString("incomeType").ifBlank { null } else null
                        val noteStr = if (obj.has("note")) obj.optString("note").ifBlank { null } else null
                        repository.insertTransaction(
                            TransactionEntity(
                                title = obj.optString("title", "Opération"),
                                amount = obj.optDouble("amount", 0.0),
                                type = type,
                                category = obj.optString("category", "Autres"),
                                incomeType = incType,
                                note = noteStr,
                                date = obj.optLong("date", System.currentTimeMillis())
                            )
                        )
                    }
                }

                if (json.has("budgets")) {
                    val arr = json.getJSONArray("budgets")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.saveBudget(
                            BudgetEntity(
                                category = obj.optString("category", "Autres"),
                                monthlyLimit = obj.optDouble("monthlyLimit", 100.0),
                                monthYear = obj.optString("monthYear", "CURRENT")
                            )
                        )
                    }
                }

                if (json.has("realAccounts")) {
                    val arr = json.getJSONArray("realAccounts")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.saveRealAccount(
                            RealAccountEntity(
                                name = obj.optString("name", "Compte"),
                                balance = obj.optDouble("balance", 0.0),
                                accountType = obj.optString("accountType", "BANK")
                            )
                        )
                    }
                }

                if (json.has("projects")) {
                    val arr = json.getJSONArray("projects")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.saveProject(
                            ProjectEntity(
                                title = obj.optString("title", "Projet"),
                                targetAmount = obj.optDouble("targetAmount", 1000.0),
                                currentAmount = obj.optDouble("currentAmount", 0.0),
                                note = if (obj.has("note")) obj.optString("note").ifBlank { null } else null
                            )
                        )
                    }
                }

                if (json.has("currentPlan")) {
                    val planObj = json.getJSONObject("currentPlan")
                    val mYear = planObj.optString("monthYear", "Août 2026")
                    val expIncome = planObj.optDouble("expectedIncome", 3000.0)
                    val allocs = mutableListOf<PlanAllocation>()
                    if (planObj.has("allocations")) {
                        val aArr = planObj.getJSONArray("allocations")
                        for (k in 0 until aArr.length()) {
                            val aObj = aArr.getJSONObject(k)
                            allocs.add(
                                PlanAllocation(
                                    categoryName = aObj.optString("categoryName", "Catégorie"),
                                    allocatedAmount = aObj.optDouble("allocatedAmount", 0.0),
                                    colorHex = aObj.optString("colorHex", "#3F51B5")
                                )
                            )
                        }
                    }
                    _currentPlan.value = MonthlyPlan(monthYear = mYear, expectedIncome = expIncome, allocations = allocs)
                }
            }

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val importLog = BackupHistoryLog(
                fileName = "Restauration_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json",
                dateStr = dateStr,
                sizeBytes = jsonString.toByteArray().size.toLong(),
                status = "RESTAURÉ",
                isSafetyBackup = false
            )
            _backupLogs.value = listOf(importLog) + _backupLogs.value

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedDefaultDataIfEmpty()
        }
    }
}
