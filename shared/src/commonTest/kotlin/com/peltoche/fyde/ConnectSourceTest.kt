package com.peltoche.fyde

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectSourceTest {

    private fun emphasized(text: String, domains: List<String>): List<String> {
        val annotated = withDomainsEmphasized(text, domains)
        return annotated.spanStyles
            .filter { it.item.fontWeight == FontWeight.SemiBold }
            .map { annotated.text.substring(it.start, it.end) }
    }

    @Test
    fun emphasizesEachDomainInTheNote() {
        val text = "Fyde se connectera uniquement à edf.fr et particulier.edf.fr, depuis vos appareils."
        assertEquals(listOf("edf.fr", "particulier.edf.fr"), emphasized(text, listOf("edf.fr", "particulier.edf.fr")))
    }

    @Test
    fun emphasizesADomainContainedInAnEarlierOneAtItsOwnPlace() {
        // "edf.fr" also appears inside "particulier.edf.fr": it's matched after it, not inside it.
        val text = "Only particulier.edf.fr and edf.fr."
        val annotated = withDomainsEmphasized(text, listOf("particulier.edf.fr", "edf.fr"))
        val starts = annotated.spanStyles.map { it.start }
        assertEquals(listOf(text.indexOf("particulier"), text.lastIndexOf("edf.fr")), starts)
    }

    @Test
    fun leavesTheTextAsIsWhenADomainIsMissing() {
        val text = "Fyde will only connect to impots.gouv.fr."
        assertEquals(listOf("impots.gouv.fr"), emphasized(text, listOf("ameli.fr", "impots.gouv.fr")))
        assertEquals(text, withDomainsEmphasized(text, listOf("ameli.fr")).text)
    }
}
