package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultFoldersTest {

    private val home = FydeFolderAppearance(FydeFolderColor.SAND, FydeFolderIcon.HOME)

    private fun folder(id: String, name: String, parentId: String? = null, appearance: FydeFolderAppearance? = null) =
        FydeFolder(id = id, parentId = parentId, name = name, description = "", appearance = appearance)

    private val housing = folder("housing", "Logement", appearance = home)
    private val energy = folder("energy", "Énergie", parentId = "housing")
    private val bills = folder("bills", "Factures", parentId = "energy")
    private val education = folder("education", "Éducation", appearance = home)
    private val work = folder("work", "travail", appearance = home)
    private val folders = VaultFolders(listOf(work, bills, housing, education, energy))

    @Test
    fun eachLevelSortsByNameIgnoringCaseAndAccents() {
        assertEquals(listOf(education, housing, work), folders.topLevel)
        assertEquals(listOf(energy), folders.children("housing"))
        assertEquals(emptyList(), folders.children("work"))
    }

    @Test
    fun pathRunsFromTheTopLevelFolderDown() {
        assertEquals(listOf(housing, energy, bills), folders.path("bills"))
        assertEquals(emptyList(), folders.path(null))
        assertEquals(emptyList(), folders.path("unknown"))
    }

    @Test
    fun aSubfolderShowsItsTopLevelFoldersAppearance() {
        assertEquals(housing, folders.topLevelOf("bills"))
        assertEquals(home, folders.appearanceOf("bills"))
        assertNull(folders.appearanceOf(null))
    }

    @Test
    fun aFolderWhoseParentIsntSyncedIsListedAtTheTopLevel() {
        val orphan = folder("orphan", "Orphelin", parentId = "gone")

        assertEquals(listOf(orphan), VaultFolders(listOf(orphan)).topLevel)
    }

    @Test
    fun aCycleEndsThePath() {
        val a = folder("a", "A", parentId = "b")
        val b = folder("b", "B", parentId = "a")

        assertEquals(listOf(a, b), VaultFolders(listOf(a, b)).path("b"))
    }

    @Test
    fun foldersAreEqualForTheSameList() {
        assertEquals(VaultFolders(listOf(housing)), VaultFolders(listOf(housing)))
    }
}
