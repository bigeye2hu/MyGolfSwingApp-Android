package com.mygolfswing.app.data.repo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
