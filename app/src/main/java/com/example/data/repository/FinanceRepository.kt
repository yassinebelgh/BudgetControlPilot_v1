package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(private val db: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgets()
    val allRealAccounts: Flow<List<RealAccountEntity>> = db.realAccountDao().getAllRealAccounts()
    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()

    // Default categories specified by requirement
    val defaultCategories = listOf(
        "Logement",
        "Transport",
        "Alimentation",
        "Loisirs",
        "Santé",
        "Abonnements",
        "Autres"
    )

    val defaultIncomeTypes = listOf(
        "Salaire",
        "Remboursement",
        "Autre"
    )

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return db.transactionDao().insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        db.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        db.transactionDao().deleteTransactionById(id)
    }

    suspend fun saveBudget(budget: BudgetEntity): Long {
        return db.budgetDao().insertOrUpdateBudget(budget)
    }

    suspend fun saveRealAccount(account: RealAccountEntity): Long {
        return db.realAccountDao().insertRealAccount(account)
    }

    suspend fun updateRealAccountBalance(id: Long, newBalance: Double) {
        db.realAccountDao().updateAccountBalance(id, newBalance)
    }

    suspend fun deleteRealAccount(id: Long) {
        db.realAccountDao().deleteRealAccountById(id)
    }

    suspend fun saveProject(project: ProjectEntity): Long {
        return db.projectDao().insertProject(project)
    }

    suspend fun deleteProject(id: Long) {
        db.projectDao().deleteProjectById(id)
    }

    suspend fun saveCategory(category: CategoryEntity): Long {
        return db.categoryDao().insertCategory(category)
    }

    suspend fun deleteCategory(id: Long) {
        db.categoryDao().deleteCategoryById(id)
    }

    suspend fun seedDefaultDataIfEmpty() {
        val existingAccounts = db.realAccountDao().getAllRealAccounts().first()

        if (existingCategoriesEmpty()) {
            defaultCategories.forEachIndexed { idx, catName ->
                db.categoryDao().insertCategory(
                    CategoryEntity(
                        name = catName,
                        isExpense = true,
                        colorHex = when (idx % 5) {
                            0 -> "#3B82F6"
                            1 -> "#10B981"
                            2 -> "#F59E0B"
                            3 -> "#8B5CF6"
                            else -> "#EC4899"
                        }
                    )
                )
            }
        }

        if (existingAccounts.isEmpty()) {
            db.realAccountDao().insertRealAccount(
                RealAccountEntity(
                    name = "Compte Courant Principal",
                    balance = 0.00,
                    accountType = "BANK"
                )
            )
        }
    }

    private suspend fun existingCategoriesEmpty(): Boolean {
        return db.categoryDao().getAllCategories().first().isEmpty()
    }

    suspend fun clearAllData() {
        db.transactionDao().clearAll()
        db.budgetDao().clearAll()
        db.realAccountDao().clearAll()
        db.projectDao().clearAll()
        db.categoryDao().clearAll()
    }
}
