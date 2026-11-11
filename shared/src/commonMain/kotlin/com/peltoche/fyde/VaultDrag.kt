package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import kotlin.math.roundToInt

/** What's being dragged across the vault (design-system.md § Layout: documents and folders move by drag and drop). */
sealed interface DragItem {
    val name: String

    data class Document(val document: FydeDocument) : DragItem {
        override val name: String get() = document.name
    }

    data class Folder(val folder: FydeFolder) : DragItem {
        override val name: String get() = folder.name
    }
}

/**
 * The vault's drag and drop at Expanded: one item dragged at a time, the
 * pointer's place in root coordinates, and every drop target's bounds.
 * In-app rather than the platform's drag and drop, whose transfer data
 * differs on every platform and which nothing outside the app needs.
 */
internal class VaultDrag(private val folders: () -> VaultFolders, private val onDrop: (DragItem, FolderChoice) -> Unit) {
    var item by mutableStateOf<DragItem?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    private val targets = mutableStateMapOf<Any, Pair<Rect, FolderChoice>>()

    /** The target under the pointer, when [item] can go there. */
    val hovered: FolderChoice?
        get() {
            val dragged = item ?: return null
            val choice = targets.values.firstOrNull { (bounds, _) -> bounds.contains(pointer) }?.second ?: return null
            return choice.takeIf { accepts(dragged, it) }
        }

    /**
     * Whether [item] can be dropped on [choice]: never where it already is,
     * nor a folder into itself or below it; À classer takes documents only.
     */
    fun accepts(item: DragItem, choice: FolderChoice): Boolean = when (item) {
        is DragItem.Document -> item.document.folderId != choice.folderId
        // À classer only takes documents: a folder goes to the top level from D8.
        is DragItem.Folder -> choice.folderId != null && item.folder.parentId != choice.folderId &&
            choice.folderId !in folders().withDescendants(item.folder.id)
    }

    fun start(item: DragItem, at: Offset) {
        this.item = item
        pointer = at
    }

    fun moveBy(delta: Offset) {
        pointer += delta
    }

    fun drop() {
        val dragged = item
        val target = hovered
        item = null
        if (dragged != null && target != null) onDrop(dragged, target)
    }

    fun cancel() {
        item = null
    }

    fun register(key: Any, bounds: Rect, choice: FolderChoice) {
        targets[key] = bounds to choice
    }

    fun unregister(key: Any) {
        targets.remove(key)
    }
}

/** The open vault's [VaultDrag], `null` below Expanded and outside the vault: no dragging there. */
internal val LocalVaultDrag = staticCompositionLocalOf<VaultDrag?> { null }

/** Lets [item] be picked up and dragged (Expanded only); a click still clicks. */
@Composable
internal fun Modifier.vaultDragSource(item: DragItem): Modifier {
    val drag = LocalVaultDrag.current ?: return this
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return onGloballyPositioned { coordinates = it }
        .pointerInput(item) {
            detectDragGestures(
                onDragStart = { offset -> drag.start(item, (coordinates?.positionInRoot() ?: Offset.Zero) + offset) },
                onDrag = { change, amount ->
                    change.consume()
                    drag.moveBy(amount)
                },
                onDragEnd = { drag.drop() },
                onDragCancel = { drag.cancel() },
            )
        }
}

/** Makes this a place [choice] something can be dropped on, outlined in `primary` while it would take it. */
@Composable
internal fun Modifier.vaultDropTarget(choice: FolderChoice): Modifier {
    val drag = LocalVaultDrag.current ?: return this
    val key = remember { Any() }
    DisposableEffect(drag, key) { onDispose { drag.unregister(key) } }
    val hovered = drag.item != null && drag.hovered == choice
    return onGloballyPositioned { drag.register(key, it.boundsInRoot(), choice) }
        .then(if (hovered) Modifier.border(2.dp, AppColors.primary, AppShapes.card) else Modifier)
}

/**
 * Draws [content], then the dragged item's name under the pointer while a
 * drag runs. Its position is the pointer's in root coordinates, translated
 * into this box's.
 */
@Composable
internal fun VaultDragLayer(drag: VaultDrag?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        content()
        val item = drag?.item ?: return@Box
        val at = drag.pointer - origin
        Text(
            text = item.name,
            style = AppTypography.bodyEmphasis,
            color = AppColors.text,
            maxLines = 1,
            modifier = Modifier
                .offset { IntOffset((at.x + 12f).roundToInt(), (at.y + 12f).roundToInt()) }
                .shadow(6.dp, AppShapes.card)
                .clip(AppShapes.card)
                .background(AppColors.surfaceElevated)
                .padding(horizontal = AppSpacing.space4, vertical = AppSpacing.space2),
        )
    }
}
