package com.mygolfswing.app.platform

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class FileProviderPathsTest {
    @Test
    fun fileProviderPathsCoverLaunchCriticalLocalMedia() {
        val paths = parseFileProviderPaths()

        assertTrue(paths.contains(PathEntry(tag = "cache-path", name = "videos", path = "videos/")))
        assertTrue(paths.contains(PathEntry(tag = "cache-path", name = "avatars", path = "avatars/")))
        assertTrue(paths.contains(PathEntry(tag = "files-path", name = "user_videos", path = "users/")))
    }

    private fun parseFileProviderPaths(): Set<PathEntry> {
        val file = listOf(
            File("src/main/res/xml/file_paths.xml"),
            File("app/src/main/res/xml/file_paths.xml"),
        ).first { it.isFile }
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val root = document.documentElement
        return (0 until root.childNodes.length).mapNotNull { index ->
            val node = root.childNodes.item(index)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) return@mapNotNull null
            val attributes = node.attributes
            PathEntry(
                tag = node.nodeName,
                name = attributes.getNamedItem("name")?.nodeValue.orEmpty(),
                path = attributes.getNamedItem("path")?.nodeValue.orEmpty(),
            )
        }.toSet()
    }

    private data class PathEntry(
        val tag: String,
        val name: String,
        val path: String,
    )
}
