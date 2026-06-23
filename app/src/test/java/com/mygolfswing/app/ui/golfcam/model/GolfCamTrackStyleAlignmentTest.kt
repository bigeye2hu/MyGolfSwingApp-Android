package com.mygolfswing.app.ui.golfcam.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GolfCamTrackStyleAlignmentTest {

    @Test
    fun bezierArcProgressMapsThroughArcLengthTable() {
        val bezier = BezierPath(
            p0 = NormalizedPoint(0.05f, 0.85f),
            p1 = NormalizedPoint(0.20f, -0.20f),
            p2 = NormalizedPoint(0.75f, 0.15f),
            p3 = NormalizedPoint(0.95f, 0.72f),
        )
        val arcTable = ArcLengthTable(bezier)
        val uAtHalfArc = arcTable.uAtArcLength(arcTable.totalLength * 0.5f)
        val pointAtHalfArc = bezier.pointAtArcProgress(0.5f, arcTable)
        val rawPointAtHalfT = bezier.point(0.5f)
        val expected = bezier.point(uAtHalfArc)

        assertTrue(abs(uAtHalfArc - 0.5f) > 0.001f)
        assertEquals(expected.x, pointAtHalfArc.x, 1e-6f)
        assertEquals(expected.y, pointAtHalfArc.y, 1e-6f)
        assertTrue(abs(pointAtHalfArc.x - rawPointAtHalfT.x) > 0.001f || abs(pointAtHalfArc.y - rawPointAtHalfT.y) > 0.001f)
    }

    @Test
    fun displayDistanceUsesIosCadenceFloorAndRoundedFinalValue() {
        val totalDistance = 150.6
        val totalDurationSec = 3.0
        val speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL

        val sampledAtFrame30 = GolfCamSpeedConfig.displayDistance(
            totalDistance = totalDistance,
            elapsedSec = 1.00,
            totalDurationSec = totalDurationSec,
            speedLevel = speedLevel,
        )
        val stillFrame30 = GolfCamSpeedConfig.displayDistance(
            totalDistance = totalDistance,
            elapsedSec = 1.04,
            totalDurationSec = totalDurationSec,
            speedLevel = speedLevel,
        )

        assertEquals(sampledAtFrame30, stillFrame30)
        assertTrue(sampledAtFrame30 < totalDistance.toInt())
        assertEquals(
            151,
            GolfCamSpeedConfig.displayDistance(
                totalDistance = totalDistance,
                elapsedSec = totalDurationSec,
                totalDurationSec = totalDurationSec,
                speedLevel = speedLevel,
            ),
        )
    }

    @Test
    fun cloudConfigConsumesDefaultsPresetsAndRenderTuningWithIosClamps() {
        val config = JsonObject(
            mapOf(
                "defaultPresetName" to JsonPrimitive("测试B"),
                "defaultColorHex" to JsonPrimitive("#55F3D8"),
                "defaultOpacity" to JsonPrimitive(1.5),
                "styleDefaults" to JsonObject(
                    mapOf(
                        "widthPct" to JsonPrimitive(300),
                        "tailHalfLife" to JsonPrimitive(1),
                        "speedLevel" to JsonPrimitive(2.0),
                        "showDistance" to JsonPrimitive(true),
                        "totalDistance" to JsonPrimitive(1200),
                        "distanceUnit" to JsonPrimitive("meters"),
                    ),
                ),
                "renderTuning" to JsonObject(
                    mapOf(
                        "showOuterGlow" to JsonPrimitive(true),
                        "bandAlphaMultiplier" to JsonPrimitive(3.0),
                        "bodyAlphaBase" to JsonPrimitive(0.4),
                        "bodyAlphaScale" to JsonPrimitive(0.1),
                        "highlightAlphaMax" to JsonPrimitive(1.5),
                        "highlightWhiteLiftBase" to JsonPrimitive(0.8),
                        "highlightWhiteLiftScale" to JsonPrimitive(0.8),
                        "highlightStart" to JsonPrimitive(0.20),
                        "highlightEnd" to JsonPrimitive(0.60),
                        "highlightPeak" to JsonPrimitive(0.42),
                        "widthTaperPower" to JsonPrimitive(5.0),
                        "apexThinRadius" to JsonPrimitive(0.01),
                    ),
                ),
                "presets" to JsonArray(
                    listOf(
                        preset("测试A", "#111111", "#222222", "#333333", glowAlpha = 0.4, bandAlpha = 0.8),
                        preset("测试B", "#010203", "#040506", "#070809", glowAlpha = 0.7, bandAlpha = 0.9),
                    ),
                ),
            ),
        )

        val parsed = TrackStyleCloudConfigLoader.parse(config)
        val style = parsed.defaultStyle

        assertEquals(0xFF55F3D8, parsed.defaultColorHex)
        assertEquals("测试B", style.colorPreset.name)
        assertEquals(0xFF010203, style.colorPreset.gradientColors.first())
        assertEquals(1.0, style.colorAlpha, 0.0)
        assertEquals(0.7, style.glowOpacity, 1e-6)
        assertEquals(160.0, style.widthPct, 0.0)
        assertEquals(5.0, style.tailHalfLife, 0.0)
        assertEquals(GolfCamSpeedConfig.MAX_LEVEL, style.speedLevel, 0.0)
        assertTrue(style.showDistance)
        assertEquals(1000.0, style.totalDistance, 0.0)
        assertEquals(DistanceUnit.METERS, style.distanceUnit)

        val tuning = style.renderTuning
        assertTrue(tuning.showOuterGlow)
        assertEquals(1.40f, tuning.bandAlphaMultiplier, 0.0f)
        assertEquals(0.16f, tuning.bodyAlphaBase, 0.0f)
        assertEquals(0.50f, tuning.bodyAlphaScale, 0.0f)
        assertEquals(0.90f, tuning.highlightAlphaMax, 0.0f)
        assertEquals(0.00f, tuning.highlightWhiteLiftBase, 0.0f)
        assertEquals(0.00f, tuning.highlightWhiteLiftScale, 0.0f)
        assertEquals(0.00f, tuning.highlightStart, 0.0f)
        assertEquals(0.96f, tuning.highlightEnd, 0.0f)
        assertEquals(0.32f, tuning.highlightPeak, 0.0f)
        assertEquals(2.20f, tuning.widthTaperPower, 0.0f)
        assertEquals(0.04f, tuning.apexThinRadius, 0.0f)
    }

    @Test
    fun defaultColorHexBecomesFallbackPresetWhenCloudPresetsAreMissing() {
        val parsed = TrackStyleCloudConfigLoader.parse(
            JsonObject(
                mapOf(
                    "defaultColorHex" to JsonPrimitive("#112233"),
                    "defaultOpacity" to JsonPrimitive(0.65),
                    "styleDefaults" to JsonObject(emptyMap()),
                ),
            ),
        )

        assertEquals(1, parsed.presets.size)
        assertEquals("默认颜色", parsed.defaultStyle.colorPreset.name)
        assertTrue(parsed.defaultStyle.colorPreset.gradientColors.all { it == 0xFF112233 })
        assertEquals(0.65, parsed.defaultStyle.colorAlpha, 0.0)
        assertFalse(parsed.defaultStyle.showDistance)
    }

    private fun preset(
        name: String,
        start: String,
        middle: String,
        end: String,
        glowAlpha: Double,
        bandAlpha: Double,
    ): JsonObject {
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "gradientColors" to JsonArray(listOf(JsonPrimitive(start), JsonPrimitive(middle), JsonPrimitive(end))),
                "glowBlur" to JsonPrimitive(28),
                "glowAlpha" to JsonPrimitive(glowAlpha),
                "bandAlpha" to JsonPrimitive(bandAlpha),
            ),
        )
    }
}
