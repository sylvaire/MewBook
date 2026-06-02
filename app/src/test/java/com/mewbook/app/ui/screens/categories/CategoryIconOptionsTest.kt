package com.mewbook.app.ui.screens.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class CategoryIconOptionsTest {
    @Suppress("UNCHECKED_CAST")
    private val availableIcons: List<String>
        get() = Class
            .forName("com.mewbook.app.ui.screens.categories.CategoriesScreenKt")
            .getDeclaredMethod("getAvailableIcons")
            .invoke(null) as List<String>

    @Test
    fun availableIcons_includesExpandedDailyLifeOptions() {
        val expectedIcons = setOf(
            "car_repair",
            "electric_car",
            "local_car_wash",
            "directions_subway",
            "tram",
            "airport_shuttle",
            "shopping_basket",
            "local_grocery_store",
            "local_convenience_store",
            "storefront",
            "medical_services",
            "local_pharmacy",
            "vaccines",
            "apartment",
            "home_repair_service",
            "electrical_services",
            "laptop",
            "science",
            "wallet",
            "credit_card"
        )

        assertTrue(availableIcons.containsAll(expectedIcons))
    }

    @Test
    fun availableIcons_hasNoDuplicateNames() {
        assertEquals(availableIcons.size, availableIcons.toSet().size)
    }

    @Test
    fun availableIcons_omitsVisualAliasIcons() {
        assertFalse(availableIcons.contains("nutrition"))
    }

    @Test
    fun availableIcons_haveExplicitUniqueVisualMappings() {
        val iconMappings = readCategoryIconMappings()
        val unmappedIcons = availableIcons.filterNot(iconMappings::containsKey)
        val duplicateVisualMappings = availableIcons
            .groupBy { iconMappings[it] }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }

        assertTrue(
            "Every selectable category icon should have an explicit visual mapping. Missing: $unmappedIcons",
            unmappedIcons.isEmpty()
        )
        assertTrue(
            "Selectable category icons should not map to duplicate visuals: $duplicateVisualMappings",
            duplicateVisualMappings.isEmpty()
        )
    }

    private fun readCategoryIconMappings(): Map<String, String> {
        val recordItemPath = listOf(
            Paths.get("app/src/main/java/com/mewbook/app/ui/components/RecordItem.kt"),
            Paths.get("../app/src/main/java/com/mewbook/app/ui/components/RecordItem.kt")
        ).firstOrNull(Files::exists) ?: error("RecordItem.kt not found from test working directory.")

        val mappingLine = Regex("""^\s*((?:"[^"]+"\s*,\s*)*"[^"]+")\s*->\s*([^\r\n]+)""", RegexOption.MULTILINE)
        val iconName = Regex("\"([^\"]+)\"")
        val source = recordItemPath.toFile().readText()

        return mappingLine.findAll(source)
            .flatMap { match ->
                val visualTarget = match.groupValues[2].trim().removeSuffix(",")
                iconName.findAll(match.groupValues[1]).map { it.groupValues[1] to visualTarget }
            }
            .toMap()
    }
}
