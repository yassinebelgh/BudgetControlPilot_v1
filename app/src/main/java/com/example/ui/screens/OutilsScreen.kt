package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectEntity
import com.example.data.model.RealAccountEntity
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutilsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val projects by viewModel.projects.collectAsState()
    val overviewState by viewModel.overviewState.collectAsState()
    val backupLogs by viewModel.backupLogs.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val isEn = appLanguage == AppLanguage.EN

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectTitleInput by remember { mutableStateOf("") }
    var projectTargetInput by remember { mutableStateOf("") }
    var projectCurrentInput by remember { mutableStateOf("") }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var jsonExportText by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }

    var showBilanDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEn) "Cockpit Tools & Settings" else "Outils & Paramètres du Cockpit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEn) "Projects, archives, backups and settings" else "Projets, archives, sauvegardes et paramètres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("outils_screen"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // 1. Projets & Objectifs
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

            items(projects, key = { "project_${it.id}" }) { project ->
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
                                text = if (isEn) "Reached: ${project.currentAmount.formatCurrency(overviewState.currencySymbol)}" else "Atteint: ${project.currentAmount.formatCurrency(overviewState.currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = if (isEn) "Goal: ${project.targetAmount.formatCurrency(overviewState.currencySymbol)}" else "Objectif: ${project.targetAmount.formatCurrency(overviewState.currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Bilans & Archives
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

            // 3. Paramètres du Cockpit (Thème -> Langue -> Devise)
            item {
                Text(
                    text = if (isEn) "Cockpit Settings" else "Paramètres du Cockpit",
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
                        // 1. Thème d'affichage
                        Text(text = if (isEn) "Theme" else "Thème d'affichage", style = MaterialTheme.typography.labelLarge)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = appTheme == AppThemeMode.LIGHT || appTheme == AppThemeMode.SYSTEM,
                                onClick = { viewModel.setAppTheme(AppThemeMode.LIGHT) },
                                label = { Text(if (isEn) "Light" else "Clair", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(imageVector = Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = appTheme == AppThemeMode.DARK,
                                onClick = { viewModel.setAppTheme(AppThemeMode.DARK) },
                                label = { Text(if (isEn) "Dark" else "Sombre", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 2. Langue / Language
                        Text(text = if (isEn) "Language / Langue" else "Langue / Language", style = MaterialTheme.typography.labelLarge)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = appLanguage == AppLanguage.FR,
                                onClick = { viewModel.setAppLanguage(AppLanguage.FR) },
                                label = { Text("Français 🇫🇷", fontWeight = FontWeight.Bold) }
                            )
                            FilterChip(
                                selected = appLanguage == AppLanguage.EN,
                                onClick = { viewModel.setAppLanguage(AppLanguage.EN) },
                                label = { Text("English 🇬🇧", fontWeight = FontWeight.Bold) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 3. Currency Symbol / Devise
                        Text(text = if (isEn) "Currency Symbol" else "Symbole Monétaire", style = MaterialTheme.typography.labelLarge)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("€", "$", "£", "CHF", "DZD", "MAD").forEach { symbol ->
                                FilterChip(
                                    selected = overviewState.currencySymbol == symbol,
                                    onClick = { viewModel.setCurrency(symbol) },
                                    label = { Text(symbol, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = if (isEn) "Sample Data Reset" else "Données d'Exemple", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (isEn) "Reset and reload default sample data" else "Réinitialiser et recharger le cockpit avec les données par défaut",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showResetConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // 4. Sauvegarde & Restauration (JSON)
            item {
                Text(
                    text = if (isEn) "Backup & Restore (JSON)" else "Sauvegarde & Restauration (JSON)",
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
                        Text(
                            text = if (isEn) "100% local and private data. Saved in local backups." else "Données 100% locales et privées. Fichiers sauvegardés dans :\n/data/user/0/com.example/files/backups/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    jsonExportText = viewModel.exportDataAsJson()
                                    showExportDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Backup" else "Créer Sauvegarde")
                            }

                            OutlinedButton(
                                onClick = {
                                    jsonImportText = ""
                                    showImportDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Restore" else "Restaurer JSON")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Text(
                            text = if (isEn) "Available Backup Versions" else "Versions de Sauvegarde Disponibles",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        backupLogs.take(8).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.fileName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${log.dateStr} • ${log.sizeBytes / 1024 + 1} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            viewModel.restoreBackupFile(log.file)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(if (isEn) "Restore" else "Restaurer", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
                        label = { Text(if (isEn) "Goal Amount (${overviewState.currencySymbol})" else "Objectif (${overviewState.currencySymbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = projectCurrentInput,
                        onValueChange = { projectCurrentInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (isEn) "Saved so far (${overviewState.currencySymbol})" else "Déjà épargné (${overviewState.currencySymbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

    // Confirm Reset Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Réinitialiser l'application ?") },
            text = { Text("Toutes vos données personnalisées seront réinitialisées avec les données d'exemple.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Export JSON Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportation JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voici votre sauvegarde au format JSON (100% locale) :", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = jsonExportText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restauration JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Collez les données JSON de votre sauvegarde ci-dessous :", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = { jsonImportText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\n  \"transactions\": [...]\n}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonImportText.isNotBlank()) {
                            val success = viewModel.importDataFromJson(jsonImportText)
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Restaurer les données")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Annuler") }
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
                            Text("Revenus: +${overviewState.theoreticalIncome.formatCurrency(overviewState.currencySymbol)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Dépenses: -${overviewState.theoreticalExpense.formatCurrency(overviewState.currencySymbol)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Épargne théorique: ${overviewState.theoreticalBalance.formatCurrency(overviewState.currencySymbol)}", style = MaterialTheme.typography.bodyMedium)
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
