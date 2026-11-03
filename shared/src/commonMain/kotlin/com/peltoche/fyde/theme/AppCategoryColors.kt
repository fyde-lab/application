package com.peltoche.fyde.theme

import androidx.compose.ui.graphics.Color

/** One category's pair from design-system.md § Category colors: the card's [fill], and the [ink] its icon and count are drawn in. */
data class AppCategoryColor(val fill: Color, val ink: Color)

/**
 * design-system.md § Category colors — one [AppCategoryColor] per category,
 * plus [unclassified] for a document no category was given. Read through
 * [AppColors.category]; screens map a category to its entry
 * (`FydeCategory.colors()`), never pick a hex themselves.
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
    subscriptions = AppCategoryColor(Color(0xFF2B2436), Color(0xFFC9B3E8)),
    donations = AppCategoryColor(Color(0xFF26301C), Color(0xFFBCD69A)),
    unclassified = AppCategoryColor(Color(0xFF262A28), Color(0xFFB7B9B4)),
)
