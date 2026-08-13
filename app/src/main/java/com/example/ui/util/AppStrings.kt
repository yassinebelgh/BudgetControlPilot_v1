package com.example.ui.util

import com.example.ui.viewmodel.AppLanguage

fun tr(lang: AppLanguage, fr: String, en: String): String {
    return if (lang == AppLanguage.EN) en else fr
}

object AppText {
    // Navigation
    fun tabCockpit(lang: AppLanguage) = tr(lang, "Cockpit", "Dashboard")
    fun tabExpenses(lang: AppLanguage) = tr(lang, "Dépenses", "Expenses")
    fun tabPlan(lang: AppLanguage) = tr(lang, "Plan", "Planning")
    fun tabAssistant(lang: AppLanguage) = tr(lang, "Assistant", "Assistant")
    fun tabOutils(lang: AppLanguage) = tr(lang, "Outils", "Tools")

    // Cockpit
    fun theoIncome(lang: AppLanguage) = tr(lang, "Revenus Théoriques", "Theoretical Income")
    fun theoExpenses(lang: AppLanguage) = tr(lang, "Dépenses Théoriques", "Theoretical Expenses")
    fun theoBalance(lang: AppLanguage) = tr(lang, "Solde Théorique", "Theoretical Balance")
    fun realBalance(lang: AppLanguage) = tr(lang, "Solde Réel Mesuré", "Measured Real Balance")
    fun recentTx(lang: AppLanguage) = tr(lang, "Dernières opérations", "Recent Transactions")
    fun fastAdd(lang: AppLanguage) = tr(lang, "Ajout Rapide", "Quick Add")
    fun seeAll(lang: AppLanguage) = tr(lang, "Voir tout", "See All")
    fun statusCoherent(lang: AppLanguage) = tr(lang, "RÉCONCILIÉ / COHÉRENT", "RECONCILED / COHERENT")

    // Expenses
    fun listTab(lang: AppLanguage) = tr(lang, "Toutes les dépenses", "All Expenses")
    fun categoryTab(lang: AppLanguage) = tr(lang, "Par catégorie & budget", "By Category & Budget")
    fun searchPlaceholder(lang: AppLanguage) = tr(lang, "Rechercher une dépense...", "Search an expense...")
    fun filterAll(lang: AppLanguage) = tr(lang, "Tous", "All")
    fun spentLabel(lang: AppLanguage) = tr(lang, "Dépensé", "Spent")
    fun limitLabel(lang: AppLanguage) = tr(lang, "Plafond", "Limit")
    fun adjustBudget(lang: AppLanguage) = tr(lang, "Ajuster le budget", "Adjust Budget")
    fun changeColor(lang: AppLanguage) = tr(lang, "Couleur", "Color")
    fun calculatorTitle(lang: AppLanguage) = tr(lang, "Calculatrice intégrée", "Built-in Calculator")

    // Plan
    fun expectedIncomeTitle(lang: AppLanguage) = tr(lang, "Revenu prévu du mois", "Monthly Expected Income")
    fun manageIncomeSources(lang: AppLanguage) = tr(lang, "Gérer les sources de revenus", "Manage Income Sources")
    fun addIncomeLine(lang: AppLanguage) = tr(lang, "Ajouter une source", "Add Source")
    fun categoryAllocations(lang: AppLanguage) = tr(lang, "Ventilation des dépenses", "Expenses Allocation")
    fun addCategoryAllocation(lang: AppLanguage) = tr(lang, "Ajouter une catégorie", "Add Category")
    fun estimatedSavings(lang: AppLanguage) = tr(lang, "Épargne estimée", "Estimated Savings")
    fun planHistory(lang: AppLanguage) = tr(lang, "Historique des plans", "Plan History")

    // Assistant
    fun assistantTitle(lang: AppLanguage) = tr(lang, "Assistant Dépenses Texte", "Text Expense Assistant")
    fun assistantSubtitle(lang: AppLanguage) = tr(lang, "Collez votre texte de dépenses pour les ajouter en un clic", "Paste expense text to add them in one click")
    fun parseButton(lang: AppLanguage) = tr(lang, "Analyser le texte", "Analyze Text")
    fun addDetectedButton(lang: AppLanguage, count: Int) = tr(lang, "Valider et Ajouter les $count dépenses", "Confirm & Add all $count expenses")

    // Outils & Settings
    fun accountsTitle(lang: AppLanguage) = tr(lang, "Comptes Réels & Dettes", "Real Accounts & Debts")
    fun addAccount(lang: AppLanguage) = tr(lang, "Ajouter un compte / dette", "Add Account / Debt")
    fun totalAssets(lang: AppLanguage) = tr(lang, "Actifs (Liquide)", "Liquid Assets")
    fun totalDebts(lang: AppLanguage) = tr(lang, "Dettes / Crédits", "Debts / Credits")
    fun netBalance(lang: AppLanguage) = tr(lang, "Solde Net Réel", "Net Real Balance")
    fun backupsTitle(lang: AppLanguage) = tr(lang, "Sauvegardes & Restauration", "Backups & Restore")
    fun createBackupBtn(lang: AppLanguage) = tr(lang, "Créer une sauvegarde JSON", "Create JSON Backup")
    fun backupLocationInfo(lang: AppLanguage) = tr(lang, "Fichiers stockés dans le dossier privé de l'application : /data/user/0/.../files/backups/", "Files saved in private app folder: /files/backups/")
    fun restoreVersion(lang: AppLanguage) = tr(lang, "Restaurer cette version", "Restore this version")
    fun settingsTitle(lang: AppLanguage) = tr(lang, "Paramètres d'affichage", "Display Settings")
    fun themeLabel(lang: AppLanguage) = tr(lang, "Thème visuel", "Visual Theme")
    fun themeLight(lang: AppLanguage) = tr(lang, "Clair", "Light")
    fun themeDark(lang: AppLanguage) = tr(lang, "Sombre", "Dark")
    fun langLabel(lang: AppLanguage) = tr(lang, "Langue de l'application", "App Language")
}
