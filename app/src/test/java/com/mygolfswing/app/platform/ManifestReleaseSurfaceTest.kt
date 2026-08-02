package com.mygolfswing.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestReleaseSurfaceTest {

    @Test
    fun launchManifestKeepsReleaseSecurityDefaults() {
        val manifest = parseManifest()
        val application = manifest.singleElement("application")

        assertEquals(".V2App", application.android("name"))
        assertEquals("false", application.android("usesCleartextTraffic"))
        assertEquals("false", application.android("allowBackup"))
        assertFalse(application.android("debuggable").equals("true", ignoreCase = true))
        assertFalse(application.android("testOnly").equals("true", ignoreCase = true))

        val permissions = manifest.elements("uses-permission").map { it.android("name") }.toSet()
        assertTrue(permissions.contains("android.permission.INTERNET"))
        assertTrue(permissions.contains("android.permission.CAMERA"))
        assertFalse(permissions.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
        assertFalse(permissions.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))

        val cameraFeature = manifest.elements("uses-feature")
            .single { it.android("name") == "android.hardware.camera" }
        assertEquals("false", cameraFeature.android("required"))
    }

    @Test
    fun onlyMainActivityIsExportedForLaunchAndShareEntry() {
        val application = parseManifest().singleElement("application")
        val exportedComponents = application.elements("activity") + application.elements("provider")

        assertEquals(
            listOf(".MainActivity"),
            exportedComponents.filter { it.android("exported") == "true" }.map { it.android("name") },
        )

        val mainActivity = application.elements("activity").single { it.android("name") == ".MainActivity" }
        assertEquals("singleTop", mainActivity.android("launchMode"))
        assertTrue(mainActivity.intentFilters().any { filter ->
            filter.actions().contains("android.intent.action.MAIN") &&
                filter.categories().contains("android.intent.category.LAUNCHER")
        })
        assertTrue(mainActivity.intentFilters().any { filter ->
            filter.actions().contains("android.intent.action.SEND") &&
                filter.categories().contains("android.intent.category.DEFAULT") &&
                filter.dataMimeTypes().contains("text/plain")
        })
    }

    @Test
    fun fileProviderIsPrivateAndGrantsOnlyDeclaredPaths() {
        val application = parseManifest().singleElement("application")
        val provider = application.elements("provider")
            .single { it.android("name") == "androidx.core.content.FileProvider" }

        assertEquals("${'$'}{applicationId}.fileprovider", provider.android("authorities"))
        assertEquals("false", provider.android("exported"))
        assertEquals("true", provider.android("grantUriPermissions"))

        val metaData = provider.elements("meta-data").single()
        assertEquals("android.support.FILE_PROVIDER_PATHS", metaData.android("name"))
        assertEquals("@xml/file_paths", metaData.android("resource"))
    }

    private fun parseManifest(): Element {
        val file = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).first { it.isFile }
        return DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
    }

    private fun Element.singleElement(name: String): Element = elements(name).single()

    private fun Element.elements(name: String): List<Element> =
        (0 until childNodes.length).mapNotNull { index ->
            val node = childNodes.item(index)
            node.takeIf { it.nodeType == Node.ELEMENT_NODE && it.nodeName == name } as? Element
        }

    private fun Element.android(name: String): String = getAttribute("android:$name")

    private fun Element.intentFilters(): List<Element> = elements("intent-filter")

    private fun Element.actions(): Set<String> =
        elements("action").map { it.android("name") }.toSet()

    private fun Element.categories(): Set<String> =
        elements("category").map { it.android("name") }.toSet()

    private fun Element.dataMimeTypes(): Set<String> =
        elements("data").map { it.android("mimeType") }.toSet()
}
