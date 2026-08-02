package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2CategoryRepositoryTest {

    @Test
    fun parseCategoryMillisAcceptsPrimitiveAndMongoDateShapes() {
        assertEquals(1_717_488_000_000L, parseCategoryMillis(JsonPrimitive(1_717_488_000_000L)))
        assertEquals(1_717_488_000_000L, parseCategoryMillis(JsonPrimitive(1_717_488_000_000.0)))
        assertEquals(
            1_717_488_000_000L,
            parseCategoryMillis(JsonObject(mapOf("\$date" to JsonPrimitive(1_717_488_000_000L)))),
        )
        assertEquals(
            1_717_488_000_000L,
            parseCategoryMillis(JsonObject(mapOf("\$date" to JsonPrimitive("2024-06-04T08:00:00Z")))),
        )
    }

    @Test
    fun parseCategoryMillisRejectsBlankOrMalformedValues() {
        assertNull(parseCategoryMillis(JsonPrimitive(" ")))
        assertNull(parseCategoryMillis(JsonPrimitive("not-a-date")))
        assertNull(parseCategoryMillis(JsonObject(emptyMap())))
    }

    @Test
    fun runCategorySyncCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("category sync failed")
        val result = runCategorySyncCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runCategorySyncCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runCategorySyncCatching<Unit> { throw CancellationException("category sync disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("category sync disposed", e.message)
        }
    }

    @Test
    fun runCategoryCloudBooleanCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        assertFalse(
            runCategoryCloudBooleanCatching {
                throw IllegalStateException("category cloud write failed")
            },
        )
    }

    @Test
    fun runCategoryCloudBooleanCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runCategoryCloudBooleanCatching { throw CancellationException("category cloud disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("category cloud disposed", e.message)
        }
    }
}
