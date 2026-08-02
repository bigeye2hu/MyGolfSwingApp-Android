package com.mygolfswing.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegalAssetsReleaseTest {

    @Test
    fun androidLegalAssetsArePresentAndNonPlaceholder() {
        legalAssets().forEach { file ->
            val text = file.readText()
            assertTrue("${file.path} should be a substantive legal document", text.length > 2_000)
            assertFalse("${file.path} still contains placeholder copy", text.contains("TODO", ignoreCase = true))
            assertFalse("${file.path} still contains placeholder copy", text.contains("待补充"))
            assertFalse("${file.path} still contains placeholder copy", text.contains("占位"))
        }
    }

    @Test
    fun androidLegalAssetsUseGooglePlayPurchaseLanguage() {
        val privacy = legalAsset("privacy_policy.md").readText()
        val terms = legalAsset("terms_of_service.md").readText()

        assertTrue(privacy.contains("Google Play"))
        assertTrue(privacy.contains("购买记录"))
        assertTrue(privacy.contains("商品编号"))
        assertTrue(privacy.contains("订单凭据"))
        assertTrue(privacy.contains("腾讯云 CloudBase"))
        assertTrue(privacy.contains("腾讯云 IM"))
        assertTrue(terms.contains("Google Play 退款政策"))

        val combined = privacy + "\n" + terms
        listOf("Apple", "App Store", "StoreKit", "iOS", "LeanCloud").forEach { phrase ->
            assertFalse("Android legal assets should not contain $phrase", combined.contains(phrase))
        }
    }

    @Test
    fun legalAssetDatesMatchAndroidReleaseCopy() {
        val privacy = legalAsset("privacy_policy.md").readText()
        val terms = legalAsset("terms_of_service.md").readText()

        assertTrue(privacy.contains("更新日期：2026年7月28日"))
        assertTrue(privacy.contains("生效日期：2026年7月28日"))
        assertTrue(terms.contains("更新日期：2026年7月28日"))
        assertTrue(terms.contains("生效日期：2026年7月28日"))
    }

    private companion object {
        fun legalAssets(): List<File> = listOf(
            legalAsset("privacy_policy.md"),
            legalAsset("terms_of_service.md"),
        )

        fun legalAsset(name: String): File =
            listOf(
                File("src/main/assets/legal/$name"),
                File("app/src/main/assets/legal/$name"),
            ).first { it.isFile }
    }
}
