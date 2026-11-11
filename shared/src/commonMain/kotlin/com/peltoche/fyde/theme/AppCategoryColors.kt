package com.peltoche.fyde.theme

import androidx.compose.ui.graphics.Color

/** One folder colour's pair from design-system.md § Folder colours: the card's [fill], and the [ink] its icon and count are drawn in. */
data class AppCategoryColor(val fill: Color, val ink: Color)

/**
 * design-system.md § Folder colours — one [AppCategoryColor] per colour a
 * top-level folder can take, named after the proposed folder that wears it,
 * plus [unclassified] for À classer. Read through [AppColors.category];
 * screens map a folder's appearance to its entry
 * (`FydeFolderAppearance.colors()`), never pick a hex themselves.
 */
data class AppCategoryColors(
    val housing: AppCategoryColor,
    val money: AppCategoryColor,
    val work: AppCategoryColor,
    val taxes: AppCategoryColor,
    val health: AppCategoryColor,
    val telecom: AppCategoryColor,
    val transport: AppCategoryColor,
    val shopping: AppCategoryColor,
    val family: AppCategoryColor,
    val identity: AppCategoryColor,
    val legal: AppCategoryColor,
    val education: AppCategoryColor,
    val pets: AppCategoryColor,
    val leisure: AppCategoryColor,
    val subscriptions: AppCategoryColor,
    val donations: AppCategoryColor,
    val unclassified: AppCategoryColor,
)

internal val lightCategoryColors = AppCategoryColors(
    housing = AppCategoryColor(Color(0xFFE8DCC6), Color(0xFF6E5328)),
    money = AppCategoryColor(Color(0xFFD7E8DA), Color(0xFF2B6E45)),
    work = AppCategoryColor(Color(0xFFD4E6E3), Color(0xFF1D5C55)),
    taxes = AppCategoryColor(Color(0xFFEBD8D6), Color(0xFF8A3B36)),
    health = AppCategoryColor(Color(0xFFEFDAE3), Color(0xFF8E3459)),
    telecom = AppCategoryColor(Color(0xFFD8E3EF), Color(0xFF2C5582)),
    transport = AppCategoryColor(Color(0xFFDDDBEF), Color(0xFF45428F)),
    shopping = AppCategoryColor(Color(0xFFF0E1CF), Color(0xFF86501C)),
    family = AppCategoryColor(Color(0xFFE9E3CB), Color(0xFF6A5B1E)),
    identity = AppCategoryColor(Color(0xFFD3E7EB), Color(0xFF1B5866)),
    legal = AppCategoryColor(Color(0xFFDDE1E7), Color(0xFF3F4A5C)),
    education = AppCategoryColor(Color(0xFFDAEAD3), Color(0xFF3A6A2A)),
    pets = AppCategoryColor(Color(0xFFEFDCD2), Color(0xFF8A4325)),
    leisure = AppCategoryColor(Color(0xFFEDD9EE), Color(0xFF7A3580)),
    subscriptions = AppCategoryColor(Color(0xFFE3DCEC), Color(0xFF5D3F80)),
    donations = AppCategoryColor(Color(0xFFDDE8CF), Color(0xFF4A6326)),
    unclassified = AppCategoryColor(Color(0xFFE6E3DB), Color(0xFF5B574F)),
)

internal val darkCategoryColors = AppCategoryColors(
    housing = AppCategoryColor(Color(0xFF3A3121), Color(0xFFE3C58F)),
    money = AppCategoryColor(Color(0xFF1E3326), Color(0xFF9ED5AE)),
    work = AppCategoryColor(Color(0xFF1B3331), Color(0xFF8FD0C6)),
    taxes = AppCategoryColor(Color(0xFF3A2422), Color(0xFFEBA49E)),
    health = AppCategoryColor(Color(0xFF3A2230), Color(0xFFEDA3C4)),
    telecom = AppCategoryColor(Color(0xFF1F2B3A), Color(0xFF9DBDE3)),
    transport = AppCategoryColor(Color(0xFF26253B), Color(0xFFB4B1EC)),
    shopping = AppCategoryColor(Color(0xFF3A2C1D), Color(0xFFE9B887)),
    family = AppCategoryColor(Color(0xFF33301C), Color(0xFFDCCB8A)),
    identity = AppCategoryColor(Color(0xFF1B3138), Color(0xFF8DCCDB)),
    legal = AppCategoryColor(Color(0xFF252A33), Color(0xFFB5C0D2)),
    education = AppCategoryColor(Color(0xFF213019), Color(0xFFA9D696)),
    pets = AppCategoryColor(Color(0xFF3A261D), Color(0xFFEDAF92)),
    leisure = AppCategoryColor(Color(0xFF35223A), Color(0xFFE2A8E8)),
    subscriptions = AppCategoryColor(Color(0xFF2B2436), Color(0xFFC9B3E8)),
    donations = AppCategoryColor(Color(0xFF26301C), Color(0xFFBCD69A)),
    unclassified = AppCategoryColor(Color(0xFF262A28), Color(0xFFB7B9B4)),
)
