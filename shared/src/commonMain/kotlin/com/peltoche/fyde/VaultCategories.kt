package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.peltoche.fyde.theme.AppCategoryColor
import com.peltoche.fyde.theme.AppColors

/**
 * design-system.md Principle 1's fixed order: categories never move with
 * their counts, so a category is always where it was. Not [FydeCategory]'s
 * own order, which follows the SDK.
 */
internal val CategoryDisplayOrder: List<FydeCategory> = listOf(
    FydeCategory.HOUSING,
    FydeCategory.MONEY,
    FydeCategory.WORK,
    FydeCategory.TAXES,
    FydeCategory.HEALTH,
    FydeCategory.TELECOM,
    FydeCategory.TRANSPORT,
    FydeCategory.SHOPPING,
    FydeCategory.FAMILY,
    FydeCategory.SUBSCRIPTIONS,
    FydeCategory.DONATIONS,
)

/** This category's fill and ink; a document with none takes "À classer"'s. */
@Composable
internal fun FydeCategory?.colors(): AppCategoryColor {
    val palette = AppColors.category
    return when (this) {
        FydeCategory.HOUSING -> palette.housing
        FydeCategory.MONEY -> palette.money
        FydeCategory.WORK -> palette.work
        FydeCategory.TAXES -> palette.taxes
        FydeCategory.HEALTH -> palette.health
        FydeCategory.TELECOM -> palette.telecom
        FydeCategory.TRANSPORT -> palette.transport
        FydeCategory.SHOPPING -> palette.shopping
        FydeCategory.FAMILY -> palette.family
        FydeCategory.SUBSCRIPTIONS -> palette.subscriptions
        FydeCategory.DONATIONS -> palette.donations
        null -> palette.unclassified
    }
}

/** design-system.md § Category colours' icon for [category], the document glyph for none. */
@Composable
internal fun CategoryGlyph(category: FydeCategory?, tint: Color, modifier: Modifier = Modifier) {
    when (category) {
        FydeCategory.HOUSING -> HomeGlyph(modifier, tint)
        FydeCategory.MONEY -> WalletGlyph(modifier, tint)
        FydeCategory.WORK -> BriefcaseGlyph(modifier, tint)
        FydeCategory.TAXES -> LandmarkGlyph(modifier, tint)
        FydeCategory.HEALTH -> HeartGlyph(modifier, tint)
        FydeCategory.TELECOM -> PhoneGlyph(modifier, tint)
        FydeCategory.TRANSPORT -> TrainGlyph(modifier, tint)
        FydeCategory.SHOPPING -> BagGlyph(modifier, tint)
        FydeCategory.FAMILY -> UsersGlyph(modifier, tint)
        FydeCategory.SUBSCRIPTIONS -> RepeatGlyph(modifier, tint)
        FydeCategory.DONATIONS -> GiftGlyph(modifier, tint)
        null -> DocumentGlyph(modifier, tint)
    }
}
