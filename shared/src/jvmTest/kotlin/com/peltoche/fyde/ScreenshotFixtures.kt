package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import java.io.ByteArrayOutputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

/**
 * The screens [ScreenshotsJvmTest] renders, each in a fixed state from fake data,
 * written in English like the UI they're rendered in. Also the source of
 * `design/current/screens/`: a new screen or state belongs here.
 */
internal object ScreenshotFixtures {

    // 2026-03-14T10:00:00Z and earlier: fixed, so dates render the same on every run.
    private const val T0 = 1_773_482_400L
    private const val DAY = 86_400L

    private val edf = script("edf", "EDF", "Electricity bills", listOf("edf.fr"))
    private val ameli = script("ameli", "Ameli", "Health refunds", listOf("ameli.fr"))
    private val impots = script("impots", "Impôts", "Tax notices", listOf("impots.gouv.fr"))
    private val free = script("free", "Free", "Internet and mobile bills", listOf("free.fr"))
    private val scripts = listOf(ameli, edf, free, impots)

    private val housingFolder = folder("housing", "Housing", FydeFolderAppearance(FydeFolderColor.SAND, FydeFolderIcon.HOME))
    private val folders = VaultFolders(
        listOf(
            housingFolder,
            folder("energy", "Energy", parentId = "housing"),
            folder("water", "Water", parentId = "housing"),
            folder("rent", "Rent", parentId = "housing"),
            folder("health", "Health", FydeFolderAppearance(FydeFolderColor.ROSE, FydeFolderIcon.HEART)),
            folder("health_insurance", "Health insurance", parentId = "health"),
            folder("work", "Work", FydeFolderAppearance(FydeFolderColor.TEAL, FydeFolderIcon.BRIEFCASE)),
            folder("payslips", "Payslips", parentId = "work"),
            folder("taxes", "Taxes", FydeFolderAppearance(FydeFolderColor.BRICK, FydeFolderIcon.LANDMARK)),
            folder("pets", "Pets", FydeFolderAppearance(FydeFolderColor.RUST, FydeFolderIcon.PAW)),
            folder("income_tax", "Income tax", parentId = "taxes"),
        ),
    )

    private val invoice = document(
        "d1", "EDF bill March 2026", "energy", FydeDocumentType.INVOICE, T0,
        mapOf("issuer" to FydeAttribute.Text("EDF"), "total_amount" to FydeAttribute.Amount(8_423, "EUR")),
    )
    private val water = document("d2", "Water bill February", "water", FydeDocumentType.INVOICE, T0 - 20 * DAY)
    private val payslip = document("d3", "Payslip February 2026", "payslips", FydeDocumentType.PAYSLIP, T0 - 12 * DAY)
    private val refund = document("d4", "Refund statement", "health_insurance", FydeDocumentType.REIMBURSEMENT_STATEMENT, T0 - 3 * DAY)
    private val tax = document("d5", "Tax notice 2025", "income_tax", FydeDocumentType.TAX_NOTICE, T0 - 200 * DAY)
    private val rent = document("d6", "Rent receipt January", "rent", FydeDocumentType.RECEIPT, T0 - 60 * DAY)
    private val unfiled = document("d7", "Scan 2026-03-02", null, null, T0 - DAY)
    private val housing = listOf(invoice, water, rent)

    // The invoice's similar documents (V6c, V16, D11), most similar first.
    private val invoiceCopy = document("d8", "EDF bill March 2026 (2)", null, FydeDocumentType.INVOICE, T0 - DAY)
    private val invoiceFebruary = document(
        "d9", "EDF bill February 2026", "energy", FydeDocumentType.INVOICE, T0 - 28 * DAY,
        mapOf("issuer" to FydeAttribute.Text("EDF"), "total_amount" to FydeAttribute.Amount(7_963, "EUR")),
    )
    private val invoiceJanuary = document("d10", "EDF bill January 2026", "energy", FydeDocumentType.INVOICE, T0 - 59 * DAY)
    private val schedule = document("d11", "EDF payment schedule 2026-2027", "energy", FydeDocumentType.STATEMENT, T0 - 70 * DAY)
    private val similarToInvoice = listOf(
        FydeSimilarDocument(invoiceCopy, 0.995f),
        FydeSimilarDocument(invoiceFebruary, 0.96f),
        FydeSimilarDocument(invoiceJanuary, 0.93f),
        FydeSimilarDocument(schedule, 0.62f),
        FydeSimilarDocument(water, 0.41f),
    )
    private val invoiceSimilar = VaultSimilarContents("d1", similarToInvoice)
    private val similarPage = VaultPage.Similar(invoice, origin = VaultPage.Folder("housing"))
    private val similarTicked = similarPage.copy(ticked = setOf("d1", "d8", "d9"))
    private val ticked = listOf(invoice, invoiceCopy, invoiceFebruary)

    private val sessions = listOf(
        FydeOpenSession("s1", "Pierre's MacBook Pro", T0 - 40 * DAY, isCurrent = true),
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
        onNewFolder = {},
    )
    private val home = VaultHome(
        folders = listOf(
            VaultFolderSummary(null, 1, unfiled),
            VaultFolderSummary(folders["pets"], 0),
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
    private val searchQuery = VaultQuery(text = "bill")
    private val searchContents = VaultSearchContents(
        query = searchQuery,
        options = VaultFilterOptions(
            folders = listOf(FydeFacetCount(FydeFolderFilter.Within("housing"), 3)),
            types = listOf(FydeFacetCount(FydeDocumentType.INVOICE, 3)),
        ),
        documents = housing,
    )
    private val vaultLoaded = VaultUiState.Loaded(
        folders = folders,
        home = home,
        folder = housingContents,
        search = searchContents,
        documentSimilar = invoiceSimilar,
        similar = invoiceSimilar,
        deviceCount = 2,
    )
    private val creationSources = listOf(
        VaultSource("EDF", VaultSourceState.Connected),
        VaultSource("Ameli", VaultSourceState.Current),
        VaultSource("Impôts", VaultSourceState.Next),
    )
    // Ameli with two accounts (design-system.md § Accounts of a source).
    private val sourcesTab = SourcesTabUiState.Loaded(
        scripts = scripts,
        installations = listOf(
            FydeInstallation("edf-1", edf.id, "pierre@example.com", mapOf("username" to FydeScriptParameterValue.Text("pierre@example.com")), folderId = "energy"),
            FydeInstallation("ameli-pierre", ameli.id, "Pierre", emptyMap(), folderId = "health"),
            FydeInstallation("ameli-marie", ameli.id, "Marie", emptyMap()),
        ),
        quota = SourceQuota("free", 2, 3),
        folders = folders,
    )
    private val statuses = mapOf(
        "edf-1" to SourceFetchStatus.Succeeded(found = 12),
        "ameli-pierre" to SourceFetchStatus.Running(FydeScraperStep.DOWNLOADING_DOCUMENTS, FydeScraperProgress.Update(3, 10), 3),
        "ameli-marie" to SourceFetchStatus.Succeeded(found = 17),
    )

    val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        shot("welcome") { WelcomeScreen(serverName = "doc.halium.fr", onCreateVault = {}, onOpenVault = {}) },
        shot("login") { LoginScreen(onLogIn = { _, _ -> }, onNavigateToRegister = {}, onBack = {}) },
        shot("login-error") {
            LoginScreen(
                onLogIn = { _, _ -> },
                onNavigateToRegister = {},
                onBack = {},
                noticeMessage = "Your session has expired.",
                errorMessage = "Incorrect username or password.",
            )
        },
        shot("register") { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}) },
        shot("register-error") { RegisterScreen(onRegister = { _, _ -> }, onNavigateToLogin = {}, onBack = {}, errorMessage = "This username is already taken.") },
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
        shot("connect-source") { connectSource(folders) },
        // S3c / DO7b: a new vault has no folder yet; DO7i over it.
        shot("connect-source-empty-vault") { connectSource(VaultFolders()) },
        shot("connect-source-folder-new") {
            connectSource(VaultFolders())
            CompositionLocalProvider(LocalVaultFolders provides VaultFolders()) {
                SourceFolderSheets(
                    sheet = SourceFolderSheet.Create(parentId = null, fromPicker = false),
                    onSheetChange = {},
                    accountName = ameli.name,
                    sourceName = ameli.name,
                    current = null,
                    onChoose = {},
                    createFolder = { _, _ -> FolderCreation.Refused("") },
                )
            }
        },
        shot("source-fetching") {
            SourceFetchingScreen(
                script = ameli,
                position = 2,
                count = 3,
                sources = creationSources,
                status = statuses.getValue("ameli-pierre"),
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
                status = SourceFetchStatus.Failed("The site isn't responding.", FydeScraperStep.LOGGING_IN, null),
                isLast = true,
                isWaiting = false,
                onWait = {},
                onRetry = {},
                onNext = {},
            )
        },
        shot("session-limit") { SessionLimitScreen(maxSessions = 2, sessions = sessions.map { it.copy(isCurrent = false) }, onReplace = {}, onCancel = {}) },
        shot("opening") { OpeningScreen(progress = FydeSyncProgress(consumed = 142, total = 214, caughtUp = false), onBrowse = {}) },
        shot("opening-uncounted") { OpeningScreen(progress = FydeSyncProgress(consumed = 12, total = null, caughtUp = false), onBrowse = {}) },
        shot("opening-done") { OpeningScreen(progress = FydeSyncProgress(consumed = 214, total = 214, caughtUp = true), onBrowse = {}) },
        shot("insecure-storage") { InsecureStorageSheet(isRegistration = true, onContinue = {}, onRetry = {}, onDismiss = {}) },
        shot("already-running") { AlreadyRunningScreen(message = "Fyde is already open on this device.", onRetry = {}) },
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
                vaultLoaded.copy(deletion = DocumentDeletion(invoice, error = "Deleting documents isn't available yet.")),
                VaultFocus(page = housingPage, document = invoice),
            )
        },
        shot("vault-similar") { vault(vaultLoaded, VaultFocus(page = similarPage)) },
        shot("vault-similar-loading") { vault(vaultLoaded.copy(similar = null), VaultFocus(page = similarPage)) },
        shot("vault-similar-selected") { vault(vaultLoaded, VaultFocus(page = similarTicked)) },
        shot("vault-similar-beside") { vault(vaultLoaded, VaultFocus(page = similarTicked, document = invoice)) },
        shot("vault-documents-move") { vault(vaultLoaded.copy(documentMove = DocumentMove(ticked)), VaultFocus(page = similarTicked)) },
        shot("vault-documents-delete") {
            vault(vaultLoaded.copy(deletion = DocumentDeletion(ticked + invoiceJanuary + schedule)), VaultFocus(page = similarTicked))
        },
        shot("vault-documents-delete-failed") {
            vault(
                vaultLoaded.copy(deletion = DocumentDeletion(listOf(invoiceCopy), error = "1 of 3 couldn't be deleted: the server can't be reached.")),
                VaultFocus(page = similarTicked),
            )
        },
        shot("vault-failed") { vault(VaultUiState.ConnectFailed("The server can't be reached.", onRetry = {}), VaultFocus()) },
        shot("sources-tab") { sourcesTab(sourcesTab, null) },
        shot("sources-tab-loading") { sourcesTab(SourcesTabUiState.Loaded(scripts = null), null) },
        shot("sources-tab-detail") { sourcesTab(sourcesTab, SourceFocus.Detail("edf-1")) },
        shot("sources-tab-account-detail") { sourcesTab(sourcesTab, SourceFocus.Detail("ameli-marie")) },
        shot("sources-tab-fetching") { sourcesTab(sourcesTab, SourceFocus.Fetching("ameli-pierre")) },
        shot("sources-tab-connect") { sourcesTab(sourcesTab, SourceFocus.Connect(free.id)) },
        shot("sources-tab-connect-another") { sourcesTab(sourcesTab, SourceFocus.Connect(ameli.id)) },
        shot("sources-tab-catalogue") { sourcesTab(sourcesTab, null, catalogueOpen = true) },
        // A source's folder (S3c, S9 / D12, S9b): the sheets drawn over the account they're for.
        shot("sources-tab-connect-empty-vault") { sourcesTab(sourcesTab.copy(folders = VaultFolders()), SourceFocus.Connect(free.id)) },
        shot("sources-tab-folder-pick") { sourceFolderSheet(SourceFolderSheet.Pick, current = "health") },
        shot("sources-tab-folder-pick-automatic") { sourceFolderSheet(SourceFolderSheet.Pick, current = null) },
        shot("sources-tab-folder-new") { sourceFolderSheet(SourceFolderSheet.Create(parentId = null, fromPicker = true), current = null) },
        shot("settings-menu") { settings(null) },
        shot("settings-loading") {
            settings(SettingsSection.Devices, planUsage = PlanUsageUiState.Loading, sessions = SessionsUiState.Loading)
        },
        shot("settings-plan-loading") { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Loading) },
        shot("settings-security-no-kit") { settings(SettingsSection.Security, recoveryKeyStatus = RecoveryKeyStatus.Missing) },
        shot("settings-recoverykit-missing") { settings(SettingsSection.RecoveryKit, recoveryKeyStatus = RecoveryKeyStatus.Missing) },
        shot("settings-recoverykit-ready") {
            settings(SettingsSection.RecoveryKit, recoveryKit = RecoveryKitUiState.Ready(List(24) { "word${it + 1}" }))
        },
        shot("settings-plan-failed") { settings(SettingsSection.Plan, planUsage = PlanUsageUiState.Failed("Couldn't load the plan.", onRetry = {})) },
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

    /** A one-page EDF bill, so open documents preview a real page. */
    private val invoicePdf: ByteArray by lazy {
        PDDocument().use { pdf ->
            val page = PDPage(PDRectangle.A4).also(pdf::addPage)
            val regular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            val bold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
            PDPageContentStream(pdf, page).use { out ->
                fun text(x: Float, y: Float, size: Float, value: String, font: PDType1Font = regular) {
                    out.beginText()
                    out.setFont(font, size)
                    out.newLineAtOffset(x, y)
                    out.showText(value)
                    out.endText()
                }
                out.setNonStrokingColor(0.0f, 0.36f, 0.66f)
                out.addRect(0f, 782f, 595f, 60f)
                out.fill()
                out.setNonStrokingColor(1f, 1f, 1f)
                text(48f, 805f, 24f, "EDF", bold)
                text(400f, 808f, 11f, "Electricity bill")
                out.setNonStrokingColor(0.13f, 0.12f, 0.1f)
                text(48f, 730f, 11f, "Bill no. 1024 5568 31  -  14 March 2026")
                text(48f, 712f, 11f, "Pierre Martin, 12 rue des Lilas, 69003 Lyon")
                text(48f, 650f, 16f, "Amount due", bold)
                text(48f, 612f, 34f, "EUR 84.23", bold)
                text(48f, 590f, 11f, "Direct debit on 27 March 2026")
                val rows = listOf(
                    "Subscription (9 kVA)" to "EUR 16.41",
                    "Consumption, 412 kWh" to "EUR 55.62",
                    "Taxes and contributions" to "EUR 12.20",
                )
                rows.forEachIndexed { i, (label, amount) ->
                    val y = 520f - i * 28f
                    text(48f, y, 12f, label)
                    text(460f, y, 12f, amount)
                    out.setStrokingColor(0.85f, 0.84f, 0.82f)
                    out.moveTo(48f, y - 10f)
                    out.lineTo(547f, y - 10f)
                    out.stroke()
                }
                text(48f, 420f, 12f, "Total incl. tax", bold)
                text(460f, 420f, 12f, "EUR 84.23", bold)
                text(48f, 120f, 9f, "Sample document rendered for Fyde's design screenshots.")
            }
            ByteArrayOutputStream().also(pdf::save).toByteArray()
        }
    }

    private fun shot(name: String, content: @Composable () -> Unit): Pair<String, @Composable () -> Unit> = name to content

    @Composable
    private fun vault(state: VaultUiState, focus: VaultFocus) {
        VaultScreen(
            state = state,
            focus = focus,
            onFocusChange = {},
            onUpload = {},
            onSave = {},
            loadContent = { Result.success(invoicePdf) },
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
            onConnect = { _, _, _, _ -> },
            onUpdate = { _, _, _, _ -> },
            onChangeFolder = { _, _ -> },
            createFolder = { _, _ -> FolderCreation.Refused("") },
            onRun = {},
            onStop = {},
            onRemove = {},
            onOpenPlan = {},
            navigation = navigation,
        )
    }

    @Composable
    private fun connectSource(folders: VaultFolders) {
        ConnectSourceScreen(
            script = ameli,
            position = 2,
            count = 3,
            sources = creationSources,
            folders = folders,
            onConnect = { _, _ -> },
            createFolder = { _, _ -> FolderCreation.Refused("") },
            onSkip = {},
            onBack = {},
        )
    }

    /** [sheet] over Ameli · Pierre's page, its folder [current]. */
    @Composable
    private fun sourceFolderSheet(sheet: SourceFolderSheet, current: String?) {
        sourcesTab(sourcesTab, SourceFocus.Detail("ameli-pierre"))
        CompositionLocalProvider(LocalVaultFolders provides folders) {
            SourceFolderSheets(
                sheet = sheet,
                onSheetChange = {},
                accountName = "Ameli · Pierre",
                sourceName = ameli.name,
                current = current,
                onChoose = {},
                createFolder = { _, _ -> FolderCreation.Refused("") },
            )
        }
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
            currentLanguage = "en",
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
            "username" to FydeScriptParameter("Username", "you@example.com", FydeScriptParameterType.STRING, required = true, secret = false),
            "password" to FydeScriptParameter("Password", "", FydeScriptParameterType.STRING, required = true, secret = true),
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
    ) = FydeFolder(id = id, parentId = parentId, name = name, appearance = appearance)
}
