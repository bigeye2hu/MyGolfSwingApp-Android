package com.mygolfswing.app.data.memo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2MemoBridgeTest {

    @Test
    fun localVideoAbsolutePathAcceptsFileUrlAndPlainPath() {
        assertEquals("/tmp/swing.mp4", V2MemoBridge.localVideoAbsolutePath("file:///tmp/swing.mp4"))
        assertEquals("/tmp/swing.mp4", V2MemoBridge.localVideoAbsolutePath("/tmp/swing.mp4"))
    }

    @Test
    fun localVideoAbsolutePathRejectsRemoteUrls() {
        assertNull(V2MemoBridge.localVideoAbsolutePath("https://example.com/swing.mp4"))
        assertNull(V2MemoBridge.localVideoAbsolutePath("http://example.com/swing.mp4"))
    }

    @Test
    fun coilImageModelAddsFileSchemeForPlainPaths() {
        assertEquals("file:///tmp/thumb.jpg", V2MemoBridge.coilImageModel("/tmp/thumb.jpg"))
        assertEquals("https://example.com/thumb.jpg", V2MemoBridge.coilImageModel("https://example.com/thumb.jpg"))
    }
}
