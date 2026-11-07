package com.peltoche.fyde

import androidx.compose.runtime.Composable

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

    private val invoice = document(
        "d1", "Facture EDF mars 2026", FydeCategory.HOUSING, FydeSubcategory.ENERGY, FydeDocumentType.INVOICE, T0,
        mapOf("issuer" to FydeAttribute.Text("EDF"), "total_amount" to FydeAttribute.Amount(8_423, "EUR")),
    )
    private val water = document("d2", "Facture eau février", FydeCategory.HOUSING, FydeSubcategory.WATER, FydeDocumentType.INVOICE, T0 - 20 * DAY)
    private val payslip = document("d3", "Bulletin de paie février 2026", FydeCategory.WORK, FydeSubcategory.PAYSLIPS, FydeDocumentType.PAYSLIP, T0 - 12 * DAY)
    private val refund = document("d4", "Décompte de remboursement", FydeCategory.HEALTH, FydeSubcategory.HEALTH_INSURANCE, FydeDocumentType.REIMBURSEMENT_STATEMENT, T0 - 3 * DAY)
    private val tax = document("d5", "Avis d'impôt 2025", FydeCategory.TAXES, FydeSubcategory.INCOME_TAX, FydeDocumentType.TAX_NOTICE, T0 - 200 * DAY)
    private val internet = document("d6", "Facture Freebox janvier", FydeCategory.HOUSING, FydeSubcategory.INTERNET, FydeDocumentType.INVOICE, T0 - 60 * DAY)
    private val housing = listOf(invoice, water, internet)

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
        categories = listOf(
            SidebarCategory(FydeCategory.HOUSING, 18),
            SidebarCategory(FydeCategory.HEALTH, 9),
            SidebarCategory(FydeCategory.WORK, 12),
            SidebarCategory(FydeCategory.TAXES, 3),
        ),
        sourceCount = 2,
        deviceCount = 2,
    )
    private val navigation = TopLevelNavigation(
        summary = summary,
        onOpenVault = {},
        onOpenCategory = {},
        onOpenScripts = {},
        onOpenSettings = {},
    )
    private val home = VaultHome(
        categories = listOf(
            VaultCategorySummary(FydeCategory.HOUSING, 18, invoice),
            VaultCategorySummary(FydeCategory.HEALTH, 9, refund),
            VaultCategorySummary(FydeCategory.WORK, 12, payslip),
            VaultCategorySummary(FydeCategory.TAXES, 3, tax),
        ),
        latest = listOf(invoice, refund, payslip, water),
    )
    private val housingPage = VaultPage.Category(FydeCategory.HOUSING)
    private val housingContents = VaultCategoryContents(
        page = housingPage,
        allCount = 3,
        options = VaultFilterOptions(
            subcategories = listOf(
                FydeFacetCount(FydeSubcategory.ENERGY, 1),
                FydeFacetCount(FydeSubcategory.WATER, 1),
                FydeFacetCount(FydeSubcategory.INTERNET, 1),
            ),
            years = listOf(YearFilter.In(2026)),
        ),
        documents = housing,
    )
    private val searchQuery = VaultQuery(text = "facture")
    private val searchContents = VaultSearchContents(
        query = searchQuery,
        options = VaultFilterOptions(
            categories = listOf(FydeFacetCount(FydeCategory.HOUSING, 3)),
            types = listOf(FydeFacetCount(FydeDocumentType.INVOICE, 3)),
        ),
        documents = housing,
    )
    private val vaultLoaded = VaultUiState.Loaded(home = home, category = housingContents, search = searchContents, deviceCount = 2)
    private val recoveryWords = List(24) { "mot${it + 1}" }
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
        "welcome" to { WelcomeScreen(serverName = "doc.halium.fr", onCreateVault = {}, onOpenVault = {}) },
        "login" to { LoginScreen(onLogIn = { _, _ -> }, onNavigateToRegister = {}, onBack = {}) },
        "login-error" to {
            LoginScreen(
                onLogIn = { _, _ -> },
                onNavigateToRegister = {},
                onBack = {},
                noticeMessage = "Votre session a expiré.",
                errorMessage = "Identifiant ou mot de passe incorrect.",
            )
        },
        "register" to { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}) },
        "register-error" to { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}, errorMessage = "Ce nom d'utilisateur est déjà pris.") },
        "recovery-generating" to { RecoveryKitScreen(state = RecoveryKitUiState.Generating, username = "pierre", onDone = {}, onLater = {}) },
        "recovery-ready" to { RecoveryKitScreen(state = RecoveryKitUiState.Ready(recoveryWords), username = "pierre", onDone = {}, onLater = {}) },
        "onboarding-sources" to {
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
        "onboarding-sources-loading" to {
            SourcesScreen(state = SourcesUiState.Loading, selected = emptySet(), onToggle = {}, query = "", onQueryChange = {}, onConnect = {}, onSkip = {})
        },
        "connect-source" to {
            ConnectSourceScreen(script = ameli, position = 2, count = 3, sources = creationSources, onConnect = {}, onSkip = {}, onBack = {})
        },
        "source-fetching" to {
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
        "source-fetching-failed" to {
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
        "session-limit" to { SessionLimitScreen(maxSessions = 2, sessions = sessions.map { it.copy(isCurrent = false) }, onReplace = {}, onCancel = {}) },
        "insecure-storage" to { InsecureStorageSheet(isRegistration = true, onContinue = {}, onRetry = {}, onDismiss = {}) },
        "already-running" to { AlreadyRunningScreen(message = "Fyde est déjà ouvert sur cet appareil.", onRetry = {}) },
        "vault-loading" to { vault(VaultUiState.Loaded(deviceCount = 2), VaultFocus()) },
        "vault-home" to { vault(vaultLoaded, VaultFocus()) },
        "vault-category" to { vault(vaultLoaded, VaultFocus(page = housingPage)) },
        "vault-category-loading" to { vault(VaultUiState.Loaded(home = home), VaultFocus(page = housingPage)) },
        "vault-search" to { vault(vaultLoaded, VaultFocus(page = VaultPage.Search(searchQuery, VaultPage.Home))) },
        "vault-document" to { vault(vaultLoaded, VaultFocus(page = housingPage, document = invoice)) },
        "vault-failed" to { vault(VaultUiState.ConnectFailed("Le serveur est injoignable.", onRetry = {}), VaultFocus()) },
        "sources-tab" to { sourcesTab(sourcesTab, null) },
        "sources-tab-loading" to { sourcesTab(SourcesTabUiState.Loaded(scripts = null), null) },
        "sources-tab-detail" to { sourcesTab(sourcesTab, SourceFocus.Detail(edf.id)) },
        "sources-tab-fetching" to { sourcesTab(sourcesTab, SourceFocus.Fetching(ameli.id)) },
        "sources-tab-connect" to { sourcesTab(sourcesTab, SourceFocus.Connect(free.id)) },
        "sources-tab-catalogue" to { sourcesTab(sourcesTab, null, catalogueOpen = true) },
        "settings-menu" to { settings(null) },
        "settings-loading" to {
            settings(SettingsSection.Devices, planUsage = PlanUsageUiState.Loading, sessions = SessionsUiState.Loading)
        },
        "settings-plan-loading" to { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Loading) },
        "settings-plan-failed" to { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Failed("Impossible de charger l'offre.", onRetry = {})) },
    ) + SettingsSection.entries.map { section -> "settings-${section.name.lowercase()}" to @Composable { settings(section) } } +
        listOf("design-system" to @Composable { DesignSystemScreen(onBack = {}) })

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
    ) {
        SettingsScreen(
            openSection = section,
            onOpenSectionChange = {},
            currentLanguage = "fr",
            onLanguageChange = {},
            navigation = navigation,
            planUsage = planUsage,
            sessions = sessions,
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
        category: FydeCategory,
        subcategory: FydeSubcategory,
        type: FydeDocumentType,
        createdAt: Long,
        attributes: Map<String, FydeAttribute> = emptyMap(),
    ) = FydeDocument(
        id = id,
        name = name,
        contentType = "application/pdf",
        createdAt = createdAt,
        size = 182_000,
        documentType = type,
        category = category,
        subcategory = subcategory,
        attributes = attributes,
    )
}
