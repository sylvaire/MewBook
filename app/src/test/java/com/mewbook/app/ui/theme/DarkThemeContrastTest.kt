package com.mewbook.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkThemeContrastTest {
    private val SurfaceDark: Color get() = themeColor("SurfaceDark")
    private val BackgroundDark: Color get() = themeColor("BackgroundDark")
    private val SurfaceVariantDark: Color get() = themeColor("SurfaceVariantDark")
    private val OutlineVariantDark: Color get() = themeColor("OutlineVariantDark")
    private val OnBackgroundDark: Color get() = themeColor("OnBackgroundDark")
    private val OnSurfaceDark: Color get() = themeColor("OnSurfaceDark")
    private val OnSurfaceVariantDark: Color get() = themeColor("OnSurfaceVariantDark")
    private val OnPrimaryDark: Color get() = themeColor("OnPrimaryDark")
    private val PrimaryDark: Color get() = themeColor("PrimaryDark")
    private val OnPrimaryContainerDark: Color get() = themeColor("OnPrimaryContainerDark")
    private val PrimaryContainerDark: Color get() = themeColor("PrimaryContainerDark")
    private val OnSecondaryDark: Color get() = themeColor("OnSecondaryDark")
    private val SecondaryDark: Color get() = themeColor("SecondaryDark")
    private val OnSecondaryContainerDark: Color get() = themeColor("OnSecondaryContainerDark")
    private val SecondaryContainerDark: Color get() = themeColor("SecondaryContainerDark")
    private val OnTertiaryDark: Color get() = themeColor("OnTertiaryDark")
    private val TertiaryDark: Color get() = themeColor("TertiaryDark")
    private val OnTertiaryContainerDark: Color get() = themeColor("OnTertiaryContainerDark")
    private val TertiaryContainerDark: Color get() = themeColor("TertiaryContainerDark")
    private val OnErrorDark: Color get() = themeColor("OnErrorDark")
    private val ErrorDark: Color get() = themeColor("ErrorDark")
    private val OnErrorContainerDark: Color get() = themeColor("OnErrorContainerDark")
    private val ErrorContainerDark: Color get() = themeColor("ErrorContainerDark")
    private val IncomeGreenDark: Color get() = themeColor("IncomeGreenDark")
    private val ExpenseRedDark: Color get() = themeColor("ExpenseRedDark")
    private val BudgetWarningDark: Color get() = themeColor("BudgetWarningDark")
    private val BudgetDangerDark: Color get() = themeColor("BudgetDangerDark")
    private val BudgetSafeDark: Color get() = themeColor("BudgetSafeDark")
    private val IncomeGreenDarkContainer: Color get() = themeColor("IncomeGreenDarkContainer")
    private val ExpenseRedDarkContainer: Color get() = themeColor("ExpenseRedDarkContainer")
    private val BudgetWarningDarkContainer: Color get() = themeColor("BudgetWarningDarkContainer")

    @Test
    fun darkCardSurfaceStaysVisuallySeparateFromBackground() {
        assertTrue(
            "Dark card surface should be lighter than the page background.",
            SurfaceDark.luminance() > BackgroundDark.luminance()
        )
        assertContrastAtLeast(
            label = "dark card surface against page background",
            foreground = SurfaceDark,
            background = BackgroundDark,
            minimum = 1.20f
        )
        assertContrastAtLeast(
            label = "dark elevated surface against card surface",
            foreground = SurfaceVariantDark,
            background = SurfaceDark,
            minimum = 1.20f
        )
        assertContrastAtLeast(
            label = "dark card edge against card surface",
            foreground = OutlineVariantDark,
            background = SurfaceDark,
            minimum = 1.80f
        )
    }

    @Test
    fun darkTextPairsStayReadable() {
        assertContrastAtLeast("on dark background", OnBackgroundDark, BackgroundDark, 4.50f)
        assertContrastAtLeast("on dark surface", OnSurfaceDark, SurfaceDark, 4.50f)
        assertContrastAtLeast("on dark surface variant", OnSurfaceVariantDark, SurfaceVariantDark, 4.50f)
        assertContrastAtLeast("on dark primary", OnPrimaryDark, PrimaryDark, 4.50f)
        assertContrastAtLeast("on dark primary container", OnPrimaryContainerDark, PrimaryContainerDark, 4.50f)
        assertContrastAtLeast("on dark secondary", OnSecondaryDark, SecondaryDark, 4.50f)
        assertContrastAtLeast("on dark secondary container", OnSecondaryContainerDark, SecondaryContainerDark, 4.50f)
        assertContrastAtLeast("on dark tertiary", OnTertiaryDark, TertiaryDark, 4.50f)
        assertContrastAtLeast("on dark tertiary container", OnTertiaryContainerDark, TertiaryContainerDark, 4.50f)
        assertContrastAtLeast("on dark error", OnErrorDark, ErrorDark, 4.50f)
        assertContrastAtLeast("on dark error container", OnErrorContainerDark, ErrorContainerDark, 4.50f)
    }

    @Test
    fun darkSemanticColorsStayConsistentAndReadable() {
        assertContrastAtLeast("dark income on card", IncomeGreenDark, SurfaceDark, 4.50f)
        assertContrastAtLeast("dark expense on card", ExpenseRedDark, SurfaceDark, 4.50f)
        assertContrastAtLeast("dark budget warning on card", BudgetWarningDark, SurfaceDark, 4.50f)
        assertContrastAtLeast("dark budget danger on card", BudgetDangerDark, SurfaceDark, 4.50f)
        assertContrastAtLeast("dark budget safe on card", BudgetSafeDark, SurfaceDark, 4.50f)

        assertContrastAtLeast("dark income chip fill", IncomeGreenDarkContainer, SurfaceDark, 1.35f)
        assertContrastAtLeast("dark expense chip fill", ExpenseRedDarkContainer, SurfaceDark, 1.35f)
        assertContrastAtLeast("dark warning chip fill", BudgetWarningDarkContainer, SurfaceDark, 1.35f)
    }

    private fun assertContrastAtLeast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Float
    ) {
        val contrast = contrastRatio(foreground, background)
        assertTrue(
            "$label contrast was $contrast, expected at least $minimum.",
            contrast >= minimum
        )
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance()) + 0.05f
        val darker = minOf(first.luminance(), second.luminance()) + 0.05f
        return lighter / darker
    }

    private fun themeColor(name: String): Color {
        val value = Class
            .forName("com.mewbook.app.ui.theme.ColorKt")
            .getDeclaredMethod("get$name")
            .invoke(null)
        return when (value) {
            is Color -> value
            is ULong -> Color(value)
            is Long -> Color(value.toULong())
            else -> error("Unexpected color value for $name: ${value?.javaClass?.name}")
        }
    }
}
