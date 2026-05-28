package com.mewbook.app.ui.screens.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconOptionsTest {

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
}
