package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.components.content.CategoryBand
import com.peltoche.fyde.components.content.CategoryCard
import com.peltoche.fyde.components.content.CategoryCardPlaceholder
import com.peltoche.fyde.components.content.DocumentCard
import com.peltoche.fyde.components.content.DocumentPass
import com.peltoche.fyde.components.content.DocumentPassCell
import com.peltoche.fyde.components.content.DocumentPassHeadline
import com.peltoche.fyde.components.content.DocumentRow
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.AppPlanQuota
import com.peltoche.fyde.components.feedback.AppProgressBar
import com.peltoche.fyde.components.feedback.RunStep
import com.peltoche.fyde.components.feedback.RunStepState
import com.peltoche.fyde.components.feedback.RunStepper
import com.peltoche.fyde.components.feedback.SourceStatus
import com.peltoche.fyde.components.feedback.SourceStatusKind
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.feedback.AppSnackbarHost
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.form.AppDropdown
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.form.AppSwitch
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.navigation.AppNavigationBar
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.components.navigation.AppNavigationRail
import com.peltoche.fyde.components.navigation.AppNavigationTopBar
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppTwoPaneLayout
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import kotlinx.coroutines.launch

/**
 * A visual catalogue of every component under `components/` and its
 * important states — not an application screen. Nothing here reads or
 * writes real app data; every value is local demo state.
 */
@Composable
fun DesignSystemScreen(onBack: (() -> Unit)? = null) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppTopBar(title = "Design system", onBack = onBack, backContentDescription = "Back") },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        containerColor = AppColors.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space9),
        ) {
            ActionsSection(onShowSnackbar = { scope.launch { snackbarHostState.showSnackbar("This is a snackbar message.") } })
            FormsSection()
            ContentSection()
            FeedbackSection()
            NavigationSection()
            LayoutSection()
            OverlaySection()
        }
    }
}

@Composable
private fun CatalogueSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space5)) {
        Text(text = title, style = AppTypography.title, color = AppColors.text)
        content()
    }
}

@Composable
private fun CatalogueRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        Text(text = label, style = AppTypography.caption, color = AppColors.textMuted)
        content()
    }
}

/** A list row's canonical leading content — see `AppIconTile`. */
@Composable
private fun CatalogueTile() {
    AppIconTile(tint = AppColors.primary) { DocumentGlyph(tint = AppColors.primary, modifier = Modifier.size(AppIconSize.md)) }
}

@Composable
private fun DotIcon() {
    Text("●", color = AppColors.text)
}

// ---------------------------------------------------------------- Actions

@Composable
private fun ActionsSection(onShowSnackbar: () -> Unit) {
    CatalogueSection("Actions") {
        CatalogueRow("AppButton — Filled / Tonal / Outlined") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppButton(text = "Filled", onClick = {}, variant = AppButtonVariant.Filled)
                AppButton(text = "Tonal", onClick = {}, variant = AppButtonVariant.Tonal)
                AppButton(text = "Outlined", onClick = {}, variant = AppButtonVariant.Outlined)
            }
        }
        CatalogueRow("AppButton — disabled / loading") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppButton(text = "Disabled", onClick = {}, enabled = false)
                AppButton(text = "Loading", onClick = {}, loading = true)
            }
        }
        CatalogueRow("AppIconButton — enabled / disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppIconButton(onClick = onShowSnackbar, contentDescription = "Show snackbar") { DotIcon() }
                AppIconButton(onClick = {}, contentDescription = "Disabled action", enabled = false) { DotIcon() }
            }
        }
        CatalogueRow("AppTextButton — enabled / disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppTextButton(text = "Text button", onClick = {})
                AppTextButton(text = "Disabled", onClick = {}, enabled = false)
            }
        }
        CatalogueRow("AppBackButton — enabled / disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppBackButton(onClick = {})
                AppBackButton(onClick = {}, enabled = false)
            }
        }
        CatalogueRow("AppCircleButton — enabled / disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppCircleButton(onClick = {}, contentDescription = "Add") {
                    PlusGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
                }
                AppCircleButton(onClick = {}, contentDescription = "Add", enabled = false) {
                    PlusGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.textMuted)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Forms

@Composable
private fun FormsSection() {
    var textValue by remember { mutableStateOf("") }
    var searchValue by remember { mutableStateOf("Invoice") }
    var checked1 by remember { mutableStateOf(true) }
    var checked2 by remember { mutableStateOf(false) }
    var switchOn by remember { mutableStateOf(true) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    CatalogueSection("Forms") {
        CatalogueRow("AppTextField — normal / password with help / error / disabled") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppTextField(value = textValue, onValueChange = { textValue = it }, label = "Document name")
                AppTextField(value = "hunter2", onValueChange = {}, label = "Password", isPassword = true, supportingText = "It never leaves this device.")
                AppTextField(value = "", onValueChange = {}, label = "Document name", errorText = "This field is required.")
                AppTextField(value = "Locked", onValueChange = {}, label = "Document name", enabled = false)
            }
        }
        CatalogueRow("AppSearchField — with clear") {
            AppSearchField(
                value = searchValue,
                onValueChange = { searchValue = it },
                placeholder = "Search documents",
                onClear = { searchValue = "" },
                clearContentDescription = "Clear search",
            )
        }
        CatalogueRow("AppCheckbox — checked / unchecked / disabled") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space2)) {
                AppCheckbox(checked = checked1, onCheckedChange = { checked1 = it }, label = "Notify me on upload")
                AppCheckbox(checked = checked2, onCheckedChange = { checked2 = it }, label = "Auto-delete after 30 days")
                AppCheckbox(checked = true, onCheckedChange = {}, label = "Disabled, checked", enabled = false)
            }
        }
        CatalogueRow("AppSwitch — on / off") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space2)) {
                AppSwitch(checked = switchOn, onCheckedChange = { switchOn = it }, label = "Sync over Wi-Fi only")
                AppSwitch(checked = false, onCheckedChange = {}, label = "Disabled", enabled = false)
            }
        }
        CatalogueRow("AppDropdown") {
            AppDropdown(
                label = "Sort by",
                options = listOf("Name", "Date", "Size"),
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it },
                optionLabel = { it },
                placeholder = "Choose an option",
            )
        }
    }
}

// ---------------------------------------------------------------- Content

@Composable
private fun ContentSection() {
    CatalogueSection("Content") {
        CatalogueRow("AppCard — plain / outlined / clickable") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppCard(modifier = Modifier.size(120.dp)) { Text("Plain", style = AppTypography.body, color = AppColors.text) }
                AppCard(modifier = Modifier.size(120.dp), outlined = true) {
                    Text("Outlined", style = AppTypography.body, color = AppColors.text)
                }
                AppCard(modifier = Modifier.size(120.dp), onClick = {}) {
                    Text("Clickable", style = AppTypography.body, color = AppColors.text)
                }
            }
        }
        CatalogueRow("AppListItem — with/without supporting text and trailing content") {
            Column {
                AppListItem(title = "invoice-march.pdf", supportingText = "2.1 MB · 2026-08-01", leadingContent = { CatalogueTile() })
                AppDivider()
                AppListItem(
                    title = "contract-final.pdf",
                    supportingText = "540 KB · 2026-07-14",
                    leadingContent = { CatalogueTile() },
                    trailingContent = { AppBadge(text = "New", variant = AppBadgeVariant.Primary) },
                    onClick = {},
                )
                AppDivider()
                AppListItem(title = "notes.pdf", leadingContent = { CatalogueTile() })
            }
        }
        CatalogueRow("AppIconTile — tinted (app-colored glyph) / neutral (brings its own colors)") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppIconTile(tint = AppColors.primary) { DocumentGlyph(tint = AppColors.primary, modifier = Modifier.size(AppIconSize.md)) }
                AppIconTile(tint = AppColors.secondary) { DocumentGlyph(tint = AppColors.secondary, modifier = Modifier.size(AppIconSize.md)) }
                AppIconTile { PuzzleGlyph(tint = AppColors.textMuted, modifier = Modifier.size(AppIconSize.md)) }
                AppSkeleton(modifier = Modifier.size(AppDimensions.iconTile), shape = AppShapes.iconTile)
                AppIconTile(size = AppDimensions.iconTileSmall, tint = AppColors.primary) { BoltGlyph(tint = AppColors.primary, modifier = Modifier.size(AppIconSize.md)) }
                AppIconTile(size = AppDimensions.iconTileSmall, tint = AppColors.primary) { SyncGlyph(tint = AppColors.primary, modifier = Modifier.size(AppIconSize.md)) }
            }
        }
        CatalogueRow("CategoryCard — every category, in display order, then the placeholder") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                (CategoryDisplayOrder + listOf<FydeCategory?>(null)).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                        row.forEach { category ->
                            CategoryCard(
                                name = category?.label() ?: "À classer",
                                count = "38",
                                caption = "Facture · 12 oct.",
                                color = category.colors(),
                                contentDescription = category?.label() ?: "À classer",
                                onClick = {},
                                modifier = Modifier.width(180.dp),
                                icon = { tint -> CategoryGlyph(category, tint) },
                            )
                        }
                    }
                }
                CategoryCardPlaceholder(modifier = Modifier.width(180.dp))
            }
        }
        CatalogueRow("CategoryBand — a category page's head") {
            CategoryBand(
                name = FydeCategory.HOUSING.label(),
                subtitle = "38 documents",
                color = FydeCategory.HOUSING.colors(),
                icon = { tint -> CategoryGlyph(FydeCategory.HOUSING, tint) },
            )
        }
        CatalogueRow("DocumentRow — plain / highlighted / selected") {
            Column {
                DocumentRow(
                    title = "Facture d'électricité",
                    subtitle = "Logement · Facture",
                    trailing = "12 oct.",
                    color = FydeCategory.HOUSING.colors(),
                    onClick = {},
                    icon = { tint -> CategoryGlyph(FydeCategory.HOUSING, tint) },
                )
                DocumentRow(
                    title = "Attestation d'assurance habitation",
                    subtitle = "Logement · Attestation",
                    trailing = "1 oct.",
                    color = FydeCategory.HOUSING.colors(),
                    onClick = {},
                    highlightTerms = listOf("attestation"),
                    icon = { tint -> CategoryGlyph(FydeCategory.HOUSING, tint) },
                )
                DocumentRow(
                    title = "Bulletin de paie — septembre",
                    subtitle = "Travail · Bulletin de paie",
                    trailing = "10 oct.",
                    color = FydeCategory.WORK.colors(),
                    onClick = {},
                    selected = true,
                    icon = { tint -> CategoryGlyph(FydeCategory.WORK, tint) },
                )
            }
        }
        CatalogueRow("DocumentCard — a category's grid at Expanded") {
            DocumentCard(
                title = "Facture EDF — octobre",
                caption = "12 oct.",
                color = FydeCategory.HOUSING.colors(),
                onClick = {},
                modifier = Modifier.width(240.dp),
                icon = { tint -> CategoryGlyph(FydeCategory.HOUSING, tint) },
            )
        }
        CatalogueRow("DocumentPass — value headline / name headline") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                DocumentPass(
                    issuer = "EDF",
                    type = "Facture",
                    headline = "84,20 €",
                    line = "À régler avant le 27 octobre",
                    cells = listOf(DocumentPassCell("Période", "sept.–oct."), DocumentPassCell("Référence", "3001 4471"), DocumentPassCell("Reçue", "12 oct.")),
                    color = FydeCategory.HOUSING.colors(),
                    modifier = Modifier.width(340.dp),
                    icon = { tint -> CategoryGlyph(FydeCategory.HOUSING, tint) },
                )
                DocumentPass(
                    issuer = FydeCategory.MONEY.label(),
                    type = "Relevé",
                    headline = "Relevé de compte — octobre",
                    line = null,
                    cells = listOf(DocumentPassCell("Reçu", "1 oct."), DocumentPassCell("Taille", "182 Ko")),
                    color = FydeCategory.MONEY.colors(),
                    modifier = Modifier.width(340.dp),
                    headlineKind = DocumentPassHeadline.Name,
                    icon = { tint -> CategoryGlyph(FydeCategory.MONEY, tint) },
                )
            }
        }
        CatalogueRow("Vault glyphs — grid / sort") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                GridGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
                SortGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
            }
        }
        CatalogueRow("SealHero — the seal (Welcome, Sécurité)") {
            SealHero()
        }
        CatalogueRow("AppBadge — Neutral / Primary / Success / Warning / Error") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppBadge(text = "Draft", variant = AppBadgeVariant.Neutral)
                AppBadge(text = "New", variant = AppBadgeVariant.Primary)
                AppBadge(text = "Synced", variant = AppBadgeVariant.Success)
                AppBadge(text = "Expiring", variant = AppBadgeVariant.Warning)
                AppBadge(text = "Failed", variant = AppBadgeVariant.Error)
            }
        }
    }
}

// --------------------------------------------------------------- Feedback

@Composable
private fun FeedbackSection() {
    CatalogueSection("Feedback") {
        CatalogueRow("AppNote — Info / Ok / Warning / Error") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppNote(message = "Search runs on this device.", kind = AppNoteKind.Info, icon = { tint -> SearchGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) })
                AppNote(message = "Sealed and up to date.", kind = AppNoteKind.Ok, icon = { tint -> LockGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) })
                AppNote(message = "Your session has expired.", kind = AppNoteKind.Warning, icon = { tint -> ClockGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) })
                AppNote(message = "Wrong password.", kind = AppNoteKind.Error, icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) })
            }
        }
        CatalogueRow("AppPlanQuota — room left / at the limit") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppPlanQuota(label = AnnotatedString("Free plan"), used = 3, max = 5, description = "3 sources chosen of 5")
                AppPlanQuota(label = AnnotatedString("Limit reached"), used = 5, max = 5, description = "5 sources chosen of 5")
            }
        }
        CatalogueRow("AppLoading") {
            AppLoading(modifier = Modifier.height(120.dp), message = "Connecting…")
        }
        CatalogueRow("AppSkeleton") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                AppSkeleton(modifier = Modifier.size(width = 160.dp, height = 14.dp))
                AppSkeleton(modifier = Modifier.size(width = 60.dp, height = 14.dp))
            }
        }
        CatalogueRow("AppProgressBar — determinate / warning / error / indeterminate") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
                AppProgressBar(progress = 0.42f, modifier = Modifier.fillMaxWidth())
                AppProgressBar(progress = 0.9f, color = AppColors.warning, modifier = Modifier.fillMaxWidth())
                AppProgressBar(progress = 1f, color = AppColors.error, modifier = Modifier.fillMaxWidth())
                AppProgressBar(progress = null, modifier = Modifier.fillMaxWidth())
            }
        }
        CatalogueRow("RunStepper — downloading / failed while signing in") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    RunStepper(
                        steps = listOf(
                            RunStep("Signing in", RunStepState.Done),
                            RunStep("Looking for documents", RunStepState.Done, detail = "12 found"),
                            RunStep("Downloading", RunStepState.Current, detail = "5 / 12", progress = 5f / 12),
                            RunStep("Storing in the vault", RunStepState.ToDo),
                        ),
                    )
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    RunStepper(
                        steps = listOf(
                            RunStep("Signing in", RunStepState.Failed),
                            RunStep("Looking for documents", RunStepState.ToDo),
                            RunStep("Downloading", RunStepState.ToDo),
                            RunStep("Storing in the vault", RunStepState.ToDo),
                        ),
                    )
                }
            }
        }
        CatalogueRow("SourceStatus — running / waiting / failed / up to date / queued") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                SourceStatus(SourceStatusKind.Running, "Fetching… 5 / 12")
                SourceStatus(SourceStatusKind.Waiting, "Waiting for your code")
                SourceStatus(SourceStatusKind.Failed, "Wrong password")
                SourceStatus(SourceStatusKind.UpToDate, "Up to date")
                SourceStatus(SourceStatusKind.Queued, "Queued")
            }
        }
        CatalogueRow("AppEmptyState") {
            AppEmptyState(
                title = "No documents yet",
                description = "Documents you import will show up here.",
                modifier = Modifier.height(260.dp),
                icon = { DocumentGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                actionLabel = "Import a document",
                onAction = {},
            )
        }
        CatalogueRow("AppErrorState") {
            AppErrorState(
                title = "Can't reach the server",
                description = "Check your connection and try again.",
                modifier = Modifier.height(260.dp),
                icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
                actionLabel = "Retry",
                onAction = {},
            )
        }
        CatalogueRow("AppSnackbar — trigger from AppIconButton above") {
            Text(
                "Use the first icon button in Actions to show a live snackbar via this screen's own AppSnackbarHost.",
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
        }
    }
}

// ------------------------------------------------------------- Navigation

@Composable
private fun NavigationSection() {
    var selectedTab by remember { mutableStateOf(0) }
    val navItems = listOf("Documents", "Shared", "Settings")

    CatalogueSection("Navigation") {
        CatalogueRow("AppTopBar — this screen's own top bar, above, demonstrates both title sizes (with/without back)") {
            AppTopBar(title = "Top-level title", onBack = null)
        }
        CatalogueRow("AppNavigationBar") {
            AppNavigationBar(
                items = navItems.mapIndexed { index, label ->
                    AppNavigationItem(label = label, selected = selectedTab == index, onClick = { selectedTab = index }, icon = { DotIcon() })
                },
            )
        }
        CatalogueRow("AppNavigationRail") {
            AppNavigationRail(
                modifier = Modifier.height(240.dp),
                items = navItems.mapIndexed { index, label ->
                    AppNavigationItem(label = label, selected = selectedTab == index, onClick = { selectedTab = index }, icon = { DotIcon() })
                },
            )
        }
        CatalogueRow("AppNavigationTopBar") {
            AppNavigationTopBar(
                items = navItems.mapIndexed { index, label ->
                    AppNavigationItem(label = label, selected = selectedTab == index, onClick = { selectedTab = index }, icon = { DotIcon() })
                },
            )
        }
    }
}

// ------------------------------------------------------------------ Layout

/**
 * Each frame forces its own [AppWindowSizeClass] on every primitive inside
 * it, rather than relying on this window's real size — that's what makes it
 * possible to show all three breakpoints at once here. A real screen would
 * only ever pass one, taken from [com.peltoche.fyde.layout.LocalAppWindowSizeClass].
 */
@Composable
private fun LayoutSection() {
    CatalogueSection("Layout") {
        CatalogueRow(
            "AppScaffold + AppNavigationLayout + AppContentContainer + AppTwoPaneLayout, together — " +
                "bottom nav becomes a rail then a top bar, single pane becomes two, margins grow",
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space5),
            ) {
                SizeClassMockFrame("Compact", AppWindowSizeClass.Compact, width = 300.dp, height = 480.dp)
                SizeClassMockFrame("Medium", AppWindowSizeClass.Medium, width = 520.dp, height = 420.dp)
                SizeClassMockFrame("Expanded", AppWindowSizeClass.Expanded, width = 720.dp, height = 420.dp)
            }
        }
    }
}

@Composable
private fun SizeClassMockFrame(caption: String, sizeClass: AppWindowSizeClass, width: Dp, height: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space2)) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(AppRadius.lg)
                .border(width = 1.dp, color = AppColors.border, shape = AppRadius.lg),
        ) {
            AppScaffold(
                windowSizeClass = sizeClass,
                navigationItems = mockNavigationItems(),
                topBar = { AppTopBar(title = "Documents", windowSizeClass = sizeClass) },
            ) { contentPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    // The 1280dp list max-width never actually engages at
                    // these deliberately small preview sizes — real desktop
                    // windows are wider — but the breakpoint-driven margin
                    // (space5 → space7 → space8) still shows here.
                    AppContentContainer(width = AppContentWidth.List, windowSizeClass = sizeClass) {
                        AppTwoPaneLayout(
                            windowSizeClass = sizeClass,
                            primary = { MockDocumentList() },
                            secondary = { MockDocumentDetail() },
                        )
                    }
                }
            }
        }
        Text(caption, style = AppTypography.caption, color = AppColors.textMuted, modifier = Modifier.width(width))
    }
}

private fun mockNavigationItems(): List<AppNavigationItem> = listOf("Documents", "Shared", "Settings").mapIndexed { index, label ->
    AppNavigationItem(label = label, selected = index == 0, onClick = {}, icon = { DotIcon() })
}

@Composable
private fun MockDocumentList() {
    Column {
        AppListItem(title = "invoice-march.pdf", supportingText = "2.1 MB", leadingContent = { CatalogueTile() })
        AppDivider()
        AppListItem(title = "contract-final.pdf", supportingText = "540 KB", leadingContent = { CatalogueTile() })
        AppDivider()
        AppListItem(title = "notes.pdf", supportingText = "12 KB", leadingContent = { CatalogueTile() })
    }
}

@Composable
private fun MockDocumentDetail() {
    AppEmptyState(
        title = "Select a document",
        description = "Choose a document from the list to preview it here.",
        icon = { DotIcon() },
    )
}

// ---------------------------------------------------------------- Overlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlaySection() {
    var showDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    CatalogueSection("Overlay") {
        CatalogueRow("AppDialog — actions stacked at Compact, right-aligned wider (M8)") {
            AppButton(text = "Open dialog", onClick = { showDialog = true }, variant = AppButtonVariant.Outlined)
        }
        CatalogueRow("AppBottomSheet — a bottom sheet at Compact, a centred panel wider (O4)") {
            AppButton(text = "Open bottom sheet", onClick = { showSheet = true }, variant = AppButtonVariant.Outlined)
        }
    }

    if (showDialog) {
        AppDialog(
            onDismissRequest = { showDialog = false },
            title = "Delete document?",
            text = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { showDialog = false },
            dismissLabel = "Cancel",
            onDismiss = { showDialog = false },
            destructive = true,
        )
    }

    if (showSheet) {
        AppBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.space5).fillMaxWidth()) {
                AppIconTile(size = AppDimensions.iconTileLarge, tint = AppColors.warning) {
                    AlertTriangleGlyph(modifier = Modifier.size(24.dp), tint = AppColors.warning)
                }
                Spacer(Modifier.height(AppSpacing.space4))
                Text("Bottom sheet title", style = AppTypography.title, color = AppColors.text)
                Spacer(Modifier.height(AppSpacing.space4))
                AppButton(text = "Close", onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
