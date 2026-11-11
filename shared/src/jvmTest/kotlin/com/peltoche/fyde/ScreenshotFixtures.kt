package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing

/** The screens [ScreenshotsJvmTest] renders, each in a fixed state from fake data. */
internal object ScreenshotFixtures {

    // 2026-03-14T10:00:00Z and earlier: fixed, so dates render the same on every run.
    private const val T0 = 1_773_482_400L
    private const val DAY = 86_400L

    private val edf = script("edf", "EDF", "Factures d'électricité", listOf("edf.fr"))
    private val ameli = script("ameli", "Ameli", "Remboursements de santé", listOf("ameli.fr"))
    private val impots = script("impots", "Impôts", "Avis d'imposition", listOf("impots.gouv.fr"))
    private val free = script("free", "Free", "Factures internet et mobile", listOf("free.fr"))
    private val scripts = listOf(ameli, edf, free, impots)

    private val housingFolder = folder("housing", "Logement", FydeFolderAppearance(FydeFolderColor.SAND, FydeFolderIcon.HOME), description = "")
    private val folders = VaultFolders(
        listOf(
            housingFolder,
            folder("energy", "Énergie", parentId = "housing", description = "Factures d'électricité et de gaz, relevés de compteur"),
            folder("water", "Eau", parentId = "housing", description = "Factures d'eau"),
            folder("rent", "Loyer", parentId = "housing", description = "Quittances de loyer et bail"),
            folder("health", "Santé", FydeFolderAppearance(FydeFolderColor.ROSE, FydeFolderIcon.HEART)),
            folder("health_insurance", "Assurance maladie", parentId = "health", description = "Décomptes de remboursement"),
            folder("work", "Travail", FydeFolderAppearance(FydeFolderColor.TEAL, FydeFolderIcon.BRIEFCASE)),
            folder("payslips", "Bulletins de paie", parentId = "work", description = "Bulletins de salaire mensuels"),
            folder("taxes", "Impôts", FydeFolderAppearance(FydeFolderColor.BRICK, FydeFolderIcon.LANDMARK)),
            folder("income_tax", "Impôt sur le revenu", parentId = "taxes", description = "Avis d'imposition, déclarations"),
        ),
    )

    private val invoice = document(
        "d1", "Facture EDF mars 2026", "energy", FydeDocumentType.INVOICE, T0,
        mapOf("issuer" to FydeAttribute.Text("EDF"), "total_amount" to FydeAttribute.Amount(8_423, "EUR")),
    )
    private val water = document("d2", "Facture eau février", "water", FydeDocumentType.INVOICE, T0 - 20 * DAY)
    private val payslip = document("d3", "Bulletin de paie février 2026", "payslips", FydeDocumentType.PAYSLIP, T0 - 12 * DAY)
    private val refund = document("d4", "Décompte de remboursement", "health_insurance", FydeDocumentType.REIMBURSEMENT_STATEMENT, T0 - 3 * DAY)
    private val tax = document("d5", "Avis d'impôt 2025", "income_tax", FydeDocumentType.TAX_NOTICE, T0 - 200 * DAY)
    private val rent = document("d6", "Quittance de loyer janvier", "rent", FydeDocumentType.RECEIPT, T0 - 60 * DAY)
    private val unfiled = document("d7", "Scan 2026-03-02", null, null, T0 - DAY)
    private val housing = listOf(invoice, water, rent)

    private val sessions = listOf(
        FydeOpenSession("s1", "MacBook Pro de Pierre", T0 - 40 * DAY, isCurrent = true),
        FydeOpenSession("s2", "Pixel 9", T0 - 10 * DAY),
    )
    private val usage = FydePlanUsage(
        planId = "free",
        maxScripts = 3,
        maxSessions = 3,
        maxSize = 1_073_741_824,
        enabledScripts = 2,
        openSessions = 2,
        totalSize = 314_572_800,
    )
    private val summary = SidebarSummary(
        total = 42,
        folders = folders,
        folderCounts = mapOf(
            "housing" to 18, "energy" to 8, "water" to 4, "rent" to 6,
            "health" to 9, "health_insurance" to 9,
            "work" to 12, "payslips" to 12,
            "taxes" to 3, "income_tax" to 3,
        ),
        unfiled = 1,
        sourceCount = 2,
        deviceCount = 2,
    )
    private val navigation = TopLevelNavigation(
        summary = summary,
        onOpenVault = {},
        onOpenFolder = {},
        onOpenScripts = {},
        onOpenSettings = {},
    )
    private val home = VaultHome(
        folders = listOf(
            VaultFolderSummary(null, 1, unfiled),
            VaultFolderSummary(folders["taxes"], 3, tax),
            VaultFolderSummary(housingFolder, 18, invoice),
            VaultFolderSummary(folders["health"], 9, refund),
            VaultFolderSummary(folders["work"], 12, payslip),
        ),
        latest = listOf(unfiled, invoice, refund, payslip, water),
        counts = mapOf("housing" to 18L, "energy" to 8L, "water" to 4L, "rent" to 6L),
    )
    private val housingPage = VaultPage.Folder("housing")
    private val housingContents = VaultFolderContents(
        page = housingPage,
        subfolders = listOf(
            VaultFolderSummary(folders["water"], 4),
            VaultFolderSummary(folders["energy"], 8),
            VaultFolderSummary(folders["rent"], 6),
        ),
        count = 18,
        options = VaultFilterOptions(years = listOf(YearFilter.In(2026), YearFilter.In(2025))),
        documents = housing,
    )
    private val searchQuery = VaultQuery(text = "facture")
    private val searchContents = VaultSearchContents(
        query = searchQuery,
        options = VaultFilterOptions(
            folders = listOf(FydeFacetCount(FydeFolderFilter.Within("housing"), 3)),
            types = listOf(FydeFacetCount(FydeDocumentType.INVOICE, 3)),
        ),
        documents = housing,
    )
    private val vaultLoaded = VaultUiState.Loaded(folders = folders, home = home, folder = housingContents, search = searchContents, deviceCount = 2)
    private val creationSources = listOf(
        VaultSource("EDF", VaultSourceState.Connected),
        VaultSource("Ameli", VaultSourceState.Current),
        VaultSource("Impôts", VaultSourceState.Next),
    )
    private val sourcesTab = SourcesTabUiState.Loaded(
        scripts = scripts,
        installedIds = setOf(edf.id, ameli.id),
        installedParameters = mapOf(edf.id to mapOf("username" to FydeScriptParameterValue.Text("pierre@example.com"))),
        quota = SourceQuota("free", 2, 3),
    )
    private val statuses = mapOf(
        edf.id to SourceFetchStatus.Succeeded(found = 12),
        ameli.id to SourceFetchStatus.Running(FydeScraperStep.DOWNLOADING_DOCUMENTS, FydeScraperProgress.Update(3, 10), 3),
    )

    val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        shot("welcome") { WelcomeScreen(serverName = "doc.halium.fr", onCreateVault = {}, onOpenVault = {}) },
        shot("login") { LoginScreen(onLogIn = { _, _ -> }, onNavigateToRegister = {}, onBack = {}) },
        shot("login-error") {
            LoginScreen(
                onLogIn = { _, _ -> },
                onNavigateToRegister = {},
                onBack = {},
                noticeMessage = "Votre session a expiré.",
                errorMessage = "Identifiant ou mot de passe incorrect.",
            )
        },
        shot("register") { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}) },
        shot("register-error") { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}, errorMessage = "Ce nom d'utilisateur est déjà pris.") },
        shot("onboarding-sources") {
            SourcesScreen(
                state = SourcesUiState.Loaded(scripts, SourceQuota("free", 0, 3)),
                selected = setOf(edf.id),
                onToggle = {},
                query = "",
                onQueryChange = {},
                onConnect = {},
                onSkip = {},
            )
        },
        shot("onboarding-sources-loading") {
            SourcesScreen(state = SourcesUiState.Loading, selected = emptySet(), onToggle = {}, query = "", onQueryChange = {}, onConnect = {}, onSkip = {})
        },
        shot("connect-source") {
            ConnectSourceScreen(script = ameli, position = 2, count = 3, sources = creationSources, onConnect = {}, onSkip = {}, onBack = {})
        },
        shot("source-fetching") {
            SourceFetchingScreen(
                script = ameli,
                position = 2,
                count = 3,
                sources = creationSources,
                status = statuses.getValue(ameli.id),
                isLast = false,
                isWaiting = false,
                onWait = {},
                onRetry = {},
                onNext = {},
            )
        },
        shot("source-fetching-failed") {
            SourceFetchingScreen(
                script = ameli,
                position = 2,
                count = 3,
                sources = creationSources,
                status = SourceFetchStatus.Failed("Le site ne répond pas.", FydeScraperStep.LOGGING_IN, null),
                isLast = true,
                isWaiting = false,
                onWait = {},
                onRetry = {},
                onNext = {},
            )
        },
        shot("session-limit") { SessionLimitScreen(maxSessions = 2, sessions = sessions.map { it.copy(isCurrent = false) }, onReplace = {}, onCancel = {}) },
        shot("insecure-storage") { InsecureStorageSheet(isRegistration = true, onContinue = {}, onRetry = {}, onDismiss = {}) },
        shot("already-running") { AlreadyRunningScreen(message = "Fyde est déjà ouvert sur cet appareil.", onRetry = {}) },
        shot("vault-loading") { vault(VaultUiState.Loaded(deviceCount = 2), VaultFocus()) },
        shot("vault-home") { vault(vaultLoaded, VaultFocus()) },
        shot("vault-folder") { vault(vaultLoaded, VaultFocus(page = housingPage)) },
        shot("vault-folder-loading") { vault(VaultUiState.Loaded(folders = folders, home = home), VaultFocus(page = housingPage)) },
        shot("vault-folder-actions") { vault(vaultLoaded.copy(folderSheet = FolderSheetState(FolderSheet.Actions("housing"))), VaultFocus(page = housingPage)) },
        shot("vault-folder-new") { vault(vaultLoaded.copy(folderSheet = FolderSheetState(FolderSheet.Create(null))), VaultFocus()) },
        shot("vault-subfolder-edit") { vault(vaultLoaded.copy(folderSheet = FolderSheetState(FolderSheet.Edit("energy"))), VaultFocus(page = housingPage)) },
        shot("vault-folder-move") { vault(vaultLoaded.copy(folderSheet = FolderSheetState(FolderSheet.Move("energy"))), VaultFocus(page = housingPage)) },
        shot("vault-folder-delete") { vault(vaultLoaded.copy(folderSheet = FolderSheetState(FolderSheet.Delete("housing"))), VaultFocus(page = housingPage)) },
        shot("vault-document-move") { vault(vaultLoaded.copy(documentMove = DocumentMove(invoice)), VaultFocus(page = housingPage, document = invoice)) },
        shot("vault-search") { vault(vaultLoaded, VaultFocus(page = VaultPage.Search(searchQuery, VaultPage.Home))) },
        shot("vault-document") { vault(vaultLoaded, VaultFocus(page = housingPage, document = invoice)) },
        shot("vault-document-delete") {
            vault(vaultLoaded.copy(deletion = DocumentDeletion(invoice)), VaultFocus(page = housingPage, document = invoice))
        },
        shot("vault-document-delete-failed") {
            vault(
                vaultLoaded.copy(deletion = DocumentDeletion(invoice, error = "La suppression des documents n’est pas encore disponible.")),
                VaultFocus(page = housingPage, document = invoice),
            )
        },
        shot("vault-failed") { vault(VaultUiState.ConnectFailed("Le serveur est injoignable.", onRetry = {}), VaultFocus()) },
        shot("folder-layout") { FolderLayoutScreen(FolderLayoutChoice.Proposed, {}, busy = false, error = null, onContinue = {}) },
        shot("folder-layout-blank") { FolderLayoutScreen(FolderLayoutChoice.Blank, {}, busy = false, error = null, onContinue = {}) },
        shot("sources-tab") { sourcesTab(sourcesTab, null) },
        shot("sources-tab-loading") { sourcesTab(SourcesTabUiState.Loaded(scripts = null), null) },
        shot("sources-tab-detail") { sourcesTab(sourcesTab, SourceFocus.Detail(edf.id)) },
        shot("sources-tab-fetching") { sourcesTab(sourcesTab, SourceFocus.Fetching(ameli.id)) },
        shot("sources-tab-connect") { sourcesTab(sourcesTab, SourceFocus.Connect(free.id)) },
        shot("sources-tab-catalogue") { sourcesTab(sourcesTab, null, catalogueOpen = true) },
        shot("settings-menu") { settings(null) },
        shot("settings-loading") {
            settings(SettingsSection.Devices, planUsage = PlanUsageUiState.Loading, sessions = SessionsUiState.Loading)
        },
        shot("settings-plan-loading") { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Loading) },
        shot("settings-security-no-kit") { settings(SettingsSection.Security, recoveryKeyStatus = RecoveryKeyStatus.Missing) },
        shot("settings-recoverykit-missing") { settings(SettingsSection.RecoveryKit, recoveryKeyStatus = RecoveryKeyStatus.Missing) },
        shot("settings-recoverykit-ready") {
            settings(SettingsSection.RecoveryKit, recoveryKit = RecoveryKitUiState.Ready(List(24) { "mot${it + 1}" }))
        },
        shot("settings-plan-failed") { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Failed("Impossible de charger l'offre.", onRetry = {})) },
    ) + SettingsSection.entries.map { section -> shot("settings-${section.name.lowercase()}") { settings(section) } } +
        listOf(
            shot("design-system") { DesignSystemScreen(onBack = {}) },
            // Shown only once a password is typed, so rendered on its own.
            shot("password-meter") {
                Column(Modifier.background(AppColors.background).padding(AppSpacing.space5)) {
                    PasswordStrength.entries.forEach { strength ->
                        PasswordStrengthMeter(strength, length = 12)
                        Spacer(Modifier.height(AppSpacing.space5))
                    }
                }
            },
        )

    private fun shot(name: String, content: @Composable () -> Unit): Pair<String, @Composable () -> Unit> = name to content

    @Composable
    private fun vault(state: VaultUiState, focus: VaultFocus) {
        VaultScreen(
            state = state,
            focus = focus,
            onFocusChange = {},
            onUpload = {},
            onSave = {},
            loadContent = { Result.success(null) },
            onRetryLoad = {},
            onDeletionChange = {},
            onDelete = {},
            navigation = navigation,
        )
    }

    @Composable
    private fun sourcesTab(state: SourcesTabUiState, focus: SourceFocus?, catalogueOpen: Boolean = false) {
        SourcesTabScreen(
            state = state,
            statuses = statuses,
            runningIds = setOf(ameli.id),
            stoppingIds = emptySet(),
            focus = focus,
            onFocusChange = {},
            catalogueOpen = catalogueOpen,
            onCatalogueOpenChange = {},
            query = "",
            onQueryChange = {},
            onConnect = { _, _ -> },
            onUpdate = { _, _, _ -> },
            onRun = {},
            onStop = {},
            onRemove = {},
            onOpenPlan = {},
            navigation = navigation,
        )
    }

    @Composable
    private fun settings(
        section: SettingsSection?,
        planUsage: PlanUsageUiState = PlanUsageUiState.Loaded(usage),
        sessions: SessionsUiState = SessionsUiState.Loaded(this.sessions),
        recoveryKeyStatus: RecoveryKeyStatus = RecoveryKeyStatus.Created(T0),
        recoveryKit: RecoveryKitUiState = RecoveryKitUiState.Idle,
    ) {
        SettingsScreen(
            openSection = section,
            onOpenSectionChange = {},
            currentLanguage = "fr",
            onLanguageChange = {},
            navigation = navigation,
            planUsage = planUsage,
            sessions = sessions,
            recoveryKeyStatus = recoveryKeyStatus,
            recoveryKit = recoveryKit,
        )
    }

    private fun script(id: String, name: String, summary: String, domains: List<String>) = FydeScript(
        id = id,
        name = name,
        description = summary,
        shortDescription = summary,
        isPublic = true,
        icon = ByteArray(0),
        allowedDomains = domains,
        version = 1,
        lastUpdated = T0,
        parameters = mapOf(
            "username" to FydeScriptParameter("Identifiant", "vous@exemple.fr", FydeScriptParameterType.STRING, required = true, secret = false),
            "password" to FydeScriptParameter("Mot de passe", "", FydeScriptParameterType.STRING, required = true, secret = true),
        ),
        type = FydeScriptType.SCRAPER,
    )

    private fun document(
        id: String,
        name: String,
        folderId: String?,
        type: FydeDocumentType?,
        createdAt: Long,
        attributes: Map<String, FydeAttribute> = emptyMap(),
    ) = FydeDocument(
        id = id,
        name = name,
        contentType = "application/pdf",
        createdAt = createdAt,
        size = 182_000,
        documentType = type,
        folderId = folderId,
        attributes = attributes,
    )

    private fun folder(
        id: String,
        name: String,
        appearance: FydeFolderAppearance? = null,
        parentId: String? = null,
        description: String = "",
    ) = FydeFolder(id = id, parentId = parentId, name = name, description = description, appearance = appearance)
}
