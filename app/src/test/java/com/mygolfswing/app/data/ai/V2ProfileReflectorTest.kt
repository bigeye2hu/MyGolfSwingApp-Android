package com.mygolfswing.app.data.ai

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class V2ProfileReflectorTest {

    @Test
    fun reflectionFailureResultKeepsOrdinaryFailuresHandled() {
        val result = profileReflectionFailureResult(
            IllegalStateException("provider returned malformed profile json"),
        )

        assertEquals(
            V2ProfileReflector.Result.Failed("provider returned malformed profile json"),
            result,
        )
    }

    @Test
    fun reflectionFailureResultPreservesCoroutineCancellation() {
        try {
            profileReflectionFailureResult(CancellationException("left AI screen"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("left AI screen", e.message)
        }
    }

    @Test
    fun reflectionCallersUseSharedFailureResultHelper() {
        listOf(
            "app/src/main/java/com/mygolfswing/app/data/store/V2AIChatStore.kt",
            "app/src/main/java/com/mygolfswing/app/ui/profile/V2AIArchiveScreen.kt",
        ).forEach { path ->
            val source = repoFile(path).readText()
            assertTrue(
                "$path should route reflection failures through profileReflectionFailureResult",
                source.contains("profileReflectionFailureResult(e)"),
            )
            assertFalse(
                "$path should not hand-roll Result.Failed from caught Throwable message",
                source.contains("Result.Failed(e.message"),
            )
        }
    }

    private fun repoFile(path: String): File =
        listOf(File(path), File("../$path")).first { it.exists() }
}
