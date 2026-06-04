package com.mygolfswing.app.data.repo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2ProfileCloudConsistencyAuditRepositoryTest {

    @Test
    fun parseAuditMillisAcceptsPrimitiveAndMongoDateShapes() {
        assertEquals(1_717_488_000_000L, parseAuditMillis(JsonPrimitive(1_717_488_000_000L)))
        assertEquals(1_717_488_000_000L, parseAuditMillis(JsonPrimitive(1_717_488_000_000.0)))
        assertEquals(
            1_717_488_000_000L,
            parseAuditMillis(JsonObject(mapOf("\$date" to JsonPrimitive(1_717_488_000_000L)))),
        )
        assertEquals(
            1_717_488_000_000L,
            parseAuditMillis(JsonObject(mapOf("\$date" to JsonPrimitive("2024-06-04T08:00:00Z")))),
        )
    }

    @Test
    fun parseAuditMillisRejectsBlankOrMalformedValues() {
        assertNull(parseAuditMillis(JsonPrimitive(" ")))
        assertNull(parseAuditMillis(JsonPrimitive("not-a-date")))
        assertNull(parseAuditMillis(JsonObject(emptyMap())))
    }
}
