package com.mygolfswing.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class V2CreditsPurchaseAvailabilityTest {

    @Test
    fun loadingProductsDoesNotShowUnavailableCard() {
        assertEquals(
            V2PurchaseAvailabilityCard.None,
            purchaseAvailabilityCard(
                loading = true,
                hasActivity = false,
                totalProducts = 3,
                purchasableProducts = 0,
            ),
        )
    }

    @Test
    fun missingActivityShowsDedicatedRecoveryCard() {
        assertEquals(
            V2PurchaseAvailabilityCard.MissingActivity,
            purchaseAvailabilityCard(
                loading = false,
                hasActivity = false,
                totalProducts = 3,
                purchasableProducts = 3,
            ),
        )
    }

    @Test
    fun noPlayProductsShowsUnavailableCard() {
        assertEquals(
            V2PurchaseAvailabilityCard.NoProducts,
            purchaseAvailabilityCard(
                loading = false,
                hasActivity = true,
                totalProducts = 3,
                purchasableProducts = 0,
            ),
        )
    }

    @Test
    fun partialPlayProductConfigurationShowsPartialCard() {
        assertEquals(
            V2PurchaseAvailabilityCard.PartialProducts,
            purchaseAvailabilityCard(
                loading = false,
                hasActivity = true,
                totalProducts = 3,
                purchasableProducts = 2,
            ),
        )
    }

    @Test
    fun fullyLoadedPlayProductsShowNoRecoveryCard() {
        assertEquals(
            V2PurchaseAvailabilityCard.None,
            purchaseAvailabilityCard(
                loading = false,
                hasActivity = true,
                totalProducts = 3,
                purchasableProducts = 3,
            ),
        )
    }
}
