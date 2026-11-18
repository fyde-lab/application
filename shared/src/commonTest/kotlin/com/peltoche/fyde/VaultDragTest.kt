package com.peltoche.fyde

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultDragTest {

    private fun folder(id: String, parentId: String? = null) =
        FydeFolder(id = id, parentId = parentId, name = id, appearance = null)

    private val folders = VaultFolders(listOf(folder("housing"), folder("energy", "housing"), folder("work")))

    private fun document(folderId: String?) = FydeDocument(
        id = "d",
        name = "Facture",
        contentType = "application/pdf",
        createdAt = 0,
        size = 0,
        documentType = null,
        folderId = folderId,
    )

    private val drops = mutableListOf<Pair<DragItem, FolderChoice>>()
    private val drag = VaultDrag(folders = { folders }) { item, choice -> drops += item to choice }

    @Test
    fun aDocumentDroppedOnAnotherFolderMovesThere() {
        drag.register("work", Rect(100f, 0f, 200f, 50f), FolderChoice("work"))
        val item = DragItem.Document(document("energy"))
        drag.start(item, Offset(10f, 10f))
        drag.moveBy(Offset(140f, 10f))

        assertEquals(FolderChoice("work"), drag.hovered)
        drag.drop()

        assertEquals(listOf<Pair<DragItem, FolderChoice>>(item to FolderChoice("work")), drops)
        assertNull(drag.item)
    }

    @Test
    fun aDropOutsideAnyTargetDoesNothing() {
        drag.register("work", Rect(100f, 0f, 200f, 50f), FolderChoice("work"))
        drag.start(DragItem.Document(document(null)), Offset(10f, 10f))
        drag.drop()

        assertEquals(emptyList(), drops)
    }

    @Test
    fun nothingGoesWhereItAlreadyIs() {
        assertEquals(false, drag.accepts(DragItem.Document(document("work")), FolderChoice("work")))
        assertEquals(false, drag.accepts(DragItem.Folder(folder("energy", "housing")), FolderChoice("housing")))
    }

    @Test
    fun aFolderNeverGoesIntoItselfBelowItselfOrToUnfiled() {
        val housing = DragItem.Folder(folder("housing"))

        assertEquals(false, drag.accepts(housing, FolderChoice("housing")))
        assertEquals(false, drag.accepts(housing, FolderChoice("energy")))
        assertEquals(false, drag.accepts(housing, FolderChoice(null)))
        assertEquals(true, drag.accepts(housing, FolderChoice("work")))
        assertEquals(true, drag.accepts(DragItem.Document(document("work")), FolderChoice(null)))
    }
}
