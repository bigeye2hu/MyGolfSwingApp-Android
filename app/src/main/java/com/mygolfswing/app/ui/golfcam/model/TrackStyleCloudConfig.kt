package com.mygolfswing.app.ui.golfcam.model

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TrackStyleCloudConfig(
    val presets: List<TrackColorPreset>,
    val defaultStyle: TrackStyle,
)

object TrackStyleCloudConfigLoader {
    suspend fun fetch(): Result<TrackStyleCloudConfig> = runCatching {
        val response = ServiceLocator.net.cloudApi.videoPlaza(
            JsonObject(mapOf("action" to JsonPrimitive("getTrackStyleConfig"))),
        )
        val success = response["success"]?.jsonPrimitive?.booleanOrNull ?: false
        require(success) { response["error"]?.jsonPrimitive?.contentOrNull ?: "getTrackStyleConfig failed" }
        val config = response["config"]?.jsonObject ?: error("missing track style config")
        parse(config)
    }

    private fun parse(config: JsonObject): TrackStyleCloudConfig {
        val presets = config["presets"]
            ?.let { it as? JsonArray }
            ?.mapNotNull(::parsePreset)
            ?.takeIf { it.isNotEmpty() }
            ?: defaultTrackColorPresets

        val defaultPresetName = config.string("defaultPresetName")
        val defaultPreset = presets.firstOrNull { it.name == defaultPresetName }
            ?: presets.firstOrNull()
            ?: TrackStyle().colorPreset
        val defaultOpacity = config.double("defaultOpacity") ?: 0.75
        val styleDefaults = config["styleDefaults"]?.jsonObject
        val renderTuning = config["renderTuning"]?.jsonObject?.let(::parseRenderTuning) ?: TrackRenderTuning()

        val defaultStyle = TrackStyle(
            colorPreset = defaultPreset,
            colorAlpha = defaultOpacity.coerceIn(0.0, 1.0),
            glowOpacity = defaultPreset.glowAlpha.toDouble(),
            widthPct = (styleDefaults?.double("widthPct") ?: 70.0).coerceIn(5.0, 160.0),
            tailHalfLife = (styleDefaults?.double("tailHalfLife") ?: 70.0).coerceIn(5.0, 100.0),
            showDistance = styleDefaults?.boolean("showDistance") ?: false,
            totalDistance = (styleDefaults?.double("totalDistance") ?: 150.0).coerceIn(0.0, 1000.0),
            distanceUnit = if (styleDefaults?.string("distanceUnit") == "meters") {
                DistanceUnit.METERS
            } else {
                DistanceUnit.YARDS
            },
            speedLevel = GolfCamSpeedConfig.clamp(styleDefaults?.double("speedLevel") ?: GolfCamSpeedConfig.DEFAULT_LEVEL),
            renderTuning = renderTuning,
        )

        return TrackStyleCloudConfig(
            presets = presets,
            defaultStyle = defaultStyle,
        )
    }

    private fun parsePreset(element: kotlinx.serialization.json.JsonElement): TrackColorPreset? {
        val obj = element as? JsonObject ?: return null
        val colors = obj["gradientColors"]
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull?.hexToArgbLong() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return TrackColorPreset(
            name = obj.string("name")?.takeIf { it.isNotBlank() } ?: "颜色",
            gradientColors = colors,
            glowBlur = obj.float("glowBlur") ?: 30f,
            glowAlpha = obj.float("glowAlpha") ?: 0.68f,
            bandAlpha = obj.float("bandAlpha") ?: 1f,
        )
    }

    private fun parseRenderTuning(obj: JsonObject): TrackRenderTuning {
        return TrackRenderTuning(
            showOuterGlow = obj.boolean("showOuterGlow") ?: false,
            showBodyBand = obj.boolean("showBodyBand") ?: true,
            showHighlightCore = obj.boolean("showHighlightCore") ?: true,
            bandAlphaMultiplier = obj.float("bandAlphaMultiplier") ?: 1.0f,
            bodyAlphaBase = obj.float("bodyAlphaBase") ?: 0.10f,
            bodyAlphaScale = obj.float("bodyAlphaScale") ?: 0.98f,
            glowBlurMultiplier = obj.float("glowBlurMultiplier") ?: 0.82f,
            outerGlowMultiplier = obj.float("outerGlowMultiplier") ?: 0.62f,
            fillGlowMultiplier = obj.float("fillGlowMultiplier") ?: 0.22f,
            highlightWidthRatio = obj.float("highlightWidthRatio") ?: 0.50f,
            highlightAlphaMax = obj.float("highlightAlphaMax") ?: 0.90f,
            highlightStart = obj.float("highlightStart") ?: 0.00f,
            highlightEnd = obj.float("highlightEnd") ?: 0.96f,
            highlightPeak = obj.float("highlightPeak") ?: 0.35f,
            highlightStrength = obj.float("highlightStrength") ?: 1.6f,
            widthTaperPower = obj.float("widthTaperPower") ?: 1.36f,
            apexThinStrength = obj.float("apexThinStrength") ?: 0.60f,
            apexThinRadius = obj.float("apexThinRadius") ?: 0.20f,
            highlightFalloffPower = obj.float("highlightFalloffPower") ?: 0.82f,
        )
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.float(key: String): Float? = this[key]?.jsonPrimitive?.floatOrNull
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun String.hexToArgbLong(): Long? {
        val clean = trim().removePrefix("#")
        if (clean.length != 6) return null
        return clean.toLongOrNull(16)?.or(0xFF000000)
    }
}
