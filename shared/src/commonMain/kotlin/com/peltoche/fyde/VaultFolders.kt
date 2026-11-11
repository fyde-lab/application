package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.peltoche.fyde.icons.BagGlyph
import com.peltoche.fyde.icons.BriefcaseGlyph
import com.peltoche.fyde.icons.CalendarGlyph
import com.peltoche.fyde.icons.CarGlyph
import com.peltoche.fyde.icons.DocumentGlyph
import com.peltoche.fyde.icons.FolderGlyph
import com.peltoche.fyde.icons.GiftGlyph
import com.peltoche.fyde.icons.GraduationCapGlyph
import com.peltoche.fyde.icons.HeartGlyph
import com.peltoche.fyde.icons.HomeGlyph
import com.peltoche.fyde.icons.IdCardGlyph
import com.peltoche.fyde.icons.KeyGlyph
import com.peltoche.fyde.icons.LandmarkGlyph
import com.peltoche.fyde.icons.PawGlyph
import com.peltoche.fyde.icons.PhoneGlyph
import com.peltoche.fyde.icons.RepeatGlyph
import com.peltoche.fyde.icons.ScaleGlyph
import com.peltoche.fyde.icons.TicketGlyph
import com.peltoche.fyde.icons.TrainGlyph
import com.peltoche.fyde.icons.UsersGlyph
import com.peltoche.fyde.icons.WalletGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppCategoryColor
import com.peltoche.fyde.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Every folder synced to this device ([FydeSdk.listFolders]), indexed for
 * drawing: each folder by id, each level's folders by name (design-system.md
 * § Folders' ordering), and a folder's path up to its top-level folder,
 * whose appearance it shows. Pure: what the vault holds comes from the SDK.
 */
class VaultFolders(val all: List<FydeFolder> = emptyList()) {
    private val byId: Map<String, FydeFolder> = all.associateBy { it.id }

    // The SDK puts a folder whose parent is gone at the top level; a folder
    // still pointing at an unknown parent (not synced yet) is listed there too.
    private val childrenOf: Map<String?, List<FydeFolder>> = all
        .groupBy { folder -> folder.parentId?.takeIf { it in byId } }
        .mapValues { (_, folders) -> folders.sortedWith(FolderNameOrder) }

    operator fun get(id: String?): FydeFolder? = id?.let(byId::get)

    /** The folders directly in [parentId] (`null`: the top level), by name. */
    fun children(parentId: String?): List<FydeFolder> = childrenOf[parentId].orEmpty()

    val topLevel: List<FydeFolder> get() = children(null)

    /**
     * The folder [id] and the folders above it, its top-level folder first;
     * empty for the root or a folder not synced here. Stops at a cycle,
     * which the SDK never leaves but a half-synced list might.
     */
    fun path(id: String?): List<FydeFolder> {
        val path = ArrayDeque<FydeFolder>()
        val seen = mutableSetOf<String>()
        var folder = get(id)
        while (folder != null && seen.add(folder.id)) {
            path.addFirst(folder)
            folder = get(folder.parentId)
        }
        return path.toList()
    }

    /** The top-level folder [id] is in (itself if it's one), whose colour and icon it shows. */
    fun topLevelOf(id: String?): FydeFolder? = path(id).firstOrNull()

    /** The colour and icon the folder [id] and its documents show; `null` for À classer. */
    fun appearanceOf(id: String?): FydeFolderAppearance? = topLevelOf(id)?.appearance

    // Equal for the same folders, so a reload that changed none of them
    // doesn't reload what's drawn from them.
    override fun equals(other: Any?): Boolean = other is VaultFolders && other.all == all

    override fun hashCode(): Int = all.hashCode()
}

/** By name, ignoring case and accents, so "Éducation" sorts with the E's. */
internal val FolderNameOrder: Comparator<FydeFolder> = compareBy<FydeFolder> { collationKey(it.name) }.thenBy { it.name }

internal fun collationKey(text: String): String = buildString(text.length) {
    for (char in text.lowercase()) append(UNACCENTED[char] ?: char)
}

private val UNACCENTED: Map<Char, Char> = buildMap {
    fun put(letters: String, base: Char) = letters.forEach { put(it, base) }
    put("àâäáãå", 'a')
    put("ç", 'c')
    put("éèêë", 'e')
    put("îïíì", 'i')
    put("ôöóòõ", 'o')
    put("ùûüú", 'u')
    put("ÿý", 'y')
    put("ñ", 'n')
}

/**
 * The folders the open vault draws with: rows, cards and passes read a
 * document's colour, icon and path from it. Provided by [VaultScreen] and
 * the sidebar from `App.kt`'s [FydeSdk.listFolders]; empty until loaded.
 */
internal val LocalVaultFolders = staticCompositionLocalOf { VaultFolders() }

/** This appearance's fill and ink; À classer's for none. */
@Composable
internal fun FydeFolderAppearance?.colors(): AppCategoryColor {
    val palette = AppColors.category
    return when (this?.color) {
        FydeFolderColor.SAND -> palette.housing
        FydeFolderColor.GREEN -> palette.money
        FydeFolderColor.TEAL -> palette.work
        FydeFolderColor.BRICK -> palette.taxes
        FydeFolderColor.ROSE -> palette.health
        FydeFolderColor.BLUE -> palette.telecom
        FydeFolderColor.INDIGO -> palette.transport
        FydeFolderColor.AMBER -> palette.shopping
        FydeFolderColor.OCHRE -> palette.family
        FydeFolderColor.CYAN -> palette.identity
        FydeFolderColor.SLATE -> palette.legal
        FydeFolderColor.LEAF -> palette.education
        FydeFolderColor.RUST -> palette.pets
        FydeFolderColor.PLUM -> palette.leisure
        FydeFolderColor.VIOLET -> palette.subscriptions
        FydeFolderColor.MOSS -> palette.donations
        null -> palette.unclassified
    }
}

/** [icon]'s glyph (`icons.svg`'s folder set); À classer's document for none. */
@Composable
internal fun FolderIconGlyph(icon: FydeFolderIcon?, tint: Color, modifier: Modifier = Modifier) {
    when (icon) {
        FydeFolderIcon.FOLDER -> FolderGlyph(modifier, tint)
        FydeFolderIcon.HOME -> HomeGlyph(modifier, tint)
        FydeFolderIcon.WALLET -> WalletGlyph(modifier, tint)
        FydeFolderIcon.BRIEFCASE -> BriefcaseGlyph(modifier, tint)
        FydeFolderIcon.LANDMARK -> LandmarkGlyph(modifier, tint)
        FydeFolderIcon.HEART -> HeartGlyph(modifier, tint)
        FydeFolderIcon.PHONE -> PhoneGlyph(modifier, tint)
        FydeFolderIcon.TRAIN -> TrainGlyph(modifier, tint)
        FydeFolderIcon.BAG -> BagGlyph(modifier, tint)
        FydeFolderIcon.USERS -> UsersGlyph(modifier, tint)
        FydeFolderIcon.ID_CARD -> IdCardGlyph(modifier, tint)
        FydeFolderIcon.SCALE -> ScaleGlyph(modifier, tint)
        FydeFolderIcon.GRADUATION_CAP -> GraduationCapGlyph(modifier, tint)
        FydeFolderIcon.PAW -> PawGlyph(modifier, tint)
        FydeFolderIcon.TICKET -> TicketGlyph(modifier, tint)
        FydeFolderIcon.REPEAT -> RepeatGlyph(modifier, tint)
        FydeFolderIcon.GIFT -> GiftGlyph(modifier, tint)
        FydeFolderIcon.CAR -> CarGlyph(modifier, tint)
        FydeFolderIcon.KEY -> KeyGlyph(modifier, tint)
        FydeFolderIcon.CALENDAR -> CalendarGlyph(modifier, tint)
        null -> DocumentGlyph(modifier, tint)
    }
}

/** A folder's mark: a top-level folder's own icon, the folder glyph for a subfolder. */
@Composable
internal fun FolderMark(folder: FydeFolder, tint: Color, modifier: Modifier = Modifier) {
    val appearance = folder.appearance
    if (folder.parentId == null && appearance != null) {
        FolderIconGlyph(appearance.icon, tint, modifier)
    } else {
        FolderGlyph(modifier, tint)
    }
}

/** The colours [document] shows: its top-level folder's, À classer's at the root. */
@Composable
internal fun FydeDocument.colors(): AppCategoryColor = LocalVaultFolders.current.appearanceOf(folderId).colors()

/** [document]'s mark: its top-level folder's icon, the document glyph at the root. */
@Composable
internal fun DocumentFolderGlyph(document: FydeDocument, tint: Color, modifier: Modifier = Modifier) {
    val appearance = LocalVaultFolders.current.appearanceOf(document.folderId)
    if (appearance != null) FolderIconGlyph(appearance.icon, tint, modifier) else DocumentGlyph(modifier, tint)
}

/** "Logement › Énergie": the folder [id]'s path, or "À classer" for the root. */
@Composable
internal fun folderPathLabel(id: String?): String {
    val path = LocalVaultFolders.current.path(id)
    return if (path.isEmpty()) stringResource(Res.string.vault_unclassified) else path.joinToString(FOLDER_PATH_SEPARATOR) { it.name }
}

/** Where [this] document is: its folder's path, or "À classer". */
@Composable
internal fun FydeDocument.folderLabel(): String = folderPathLabel(folderId)

internal const val FOLDER_PATH_SEPARATOR = " › "
