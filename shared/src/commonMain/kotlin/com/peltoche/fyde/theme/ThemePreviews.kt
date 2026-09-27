package com.peltoche.fyde.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A token-only "style guide" screen — no application screens or business
 * logic, just [AppColors]/[AppTypography]/[AppSpacing]/[AppShapes] rendered
 * so the foundational layer can be reviewed and previewed on its own. Not
 * wired into [com.peltoche.fyde.App]'s navigation.
 */
@Composable
fun ThemeGallery() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.space5),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space8),
    ) {
        Text("Fyde design tokens", style = AppTypography.display, color = AppColors.text)
        ColorSchemeSection()
        TypographySection()
        SpacingSection()
        ShapesSection()
    }
}

@Composable
private fun GallerySectionTitle(text: String) {
    Text(
        text = text,
        style = AppTypography.section,
        color = AppColors.text,
        modifier = Modifier.padding(bottom = AppSpacing.space3),
    )
}

@Composable
private fun ColorSchemeSection() {
    Column {
        GallerySectionTitle("Color scheme")
        ColorSwatch("background", AppColors.background)
        ColorSwatch("surface", AppColors.surface)
        ColorSwatch("surfaceElevated", AppColors.surfaceElevated)
        ColorSwatch("border", AppColors.border)
        ColorSwatch("borderStrong", AppColors.borderStrong)
        ColorSwatch("primary", AppColors.primary)
        ColorSwatch("secondary", AppColors.secondary)
        ColorSwatch("error", AppColors.error)
        ColorSwatch("warning", AppColors.warning)
        ColorSwatch("success", AppColors.success)
        ColorSwatch("selectionSurface", AppColors.selectionSurface)
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = AppSpacing.space1),
    ) {
        Box(modifier = Modifier.size(32.dp).clip(AppRadius.sm).background(color))
        Spacer(Modifier.width(AppSpacing.space3))
        Text(text = label, style = AppTypography.data, color = AppColors.text)
    }
}

@Composable
private fun TypographySection() {
    Column {
        GallerySectionTitle("Typography")
        Text("Display — Fyde", style = AppTypography.display, color = AppColors.text)
        Text("Title — Documents", style = AppTypography.title, color = AppColors.text)
        Text("Section — Server", style = AppTypography.section, color = AppColors.text)
        Text("Body emphasis — invoice.pdf", style = AppTypography.bodyEmphasis, color = AppColors.text)
        Text("Body — Documents you import will show up here.", style = AppTypography.body, color = AppColors.text)
        Text("Caption — 2.1 MB · 2026-08-01", style = AppTypography.caption, color = AppColors.textMuted)
        Text("MICRO — STATUS", style = AppTypography.micro, color = AppColors.textMuted)
        Text("Label — Upload document", style = AppTypography.label, color = AppColors.text)
        Text("Data — 128,412 B", style = AppTypography.data, color = AppColors.text)
    }
}

@Composable
private fun SpacingSection() {
    Column {
        GallerySectionTitle("Spacing")
        val steps = listOf(
            "space1 · 2dp" to AppSpacing.space1,
            "space2 · 4dp" to AppSpacing.space2,
            "space3 · 8dp" to AppSpacing.space3,
            "space4 · 12dp" to AppSpacing.space4,
            "space5 · 16dp" to AppSpacing.space5,
            "space6 · 20dp" to AppSpacing.space6,
            "space7 · 24dp" to AppSpacing.space7,
            "space8 · 32dp" to AppSpacing.space8,
            "space9 · 40dp" to AppSpacing.space9,
            "space10 · 56dp" to AppSpacing.space10,
        )
        steps.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = AppSpacing.space1),
            ) {
                Box(modifier = Modifier.width(value).height(8.dp).background(AppColors.primary))
                Spacer(Modifier.width(AppSpacing.space3))
                Text(text = label, style = AppTypography.data, color = AppColors.textMuted)
            }
        }
    }
}

@Composable
private fun ShapesSection() {
    Column {
        GallerySectionTitle("Shapes")
        val radii = listOf(
            "none" to AppRadius.none,
            "xs" to AppRadius.xs,
            "sm" to AppRadius.sm,
            "md" to AppRadius.md,
            "lg" to AppRadius.lg,
            "full" to AppRadius.full,
        )
        Row {
            radii.forEach { (label, shape) ->
                ShapeSwatch(label, shape, modifier = Modifier.padding(end = AppSpacing.space4))
            }
        }
    }
}

@Composable
private fun ShapeSwatch(label: String, shape: Shape, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.size(48.dp).clip(shape).background(AppColors.secondary))
        Spacer(Modifier.height(AppSpacing.space2))
        Text(text = label, style = AppTypography.data, color = AppColors.textMuted)
    }
}

@Preview
@Composable
fun ThemeGalleryLightPreview() {
    AppTheme(darkTheme = false) {
        ThemeGallery()
    }
}

@Preview
@Composable
fun ThemeGalleryDarkPreview() {
    AppTheme(darkTheme = true) {
        ThemeGallery()
    }
}
