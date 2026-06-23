package com.mygolfswing.app.ui.golfcam.model

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.serialization.json.Json
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
    val defaultColorHex: Long?,
)

object TrackStyleCloudConfigLoader {
    private const val PREFS_NAME = "golf_cam_track_style_config"
    private const val CACHE_KEY_CONFIG_JSON = "config_json"

    suspend fun fetch(): Result<TrackStyleCloudConfig> {
        val remote = runCatching {
            cacheConfig(fetchRemoteConfig())
        }
        if (remote.isSuccess) return remote

        val cached = readCachedConfig()
        if (cached != null) return Result.success(cached)

        return Result.success(localFallback())
    }

    private suspend fun fetchRemoteConfig(): JsonObject {
        val response = ServiceLocator.net.cloudApi.videoPlaza(
            JsonObject(mapOf("action" to JsonPrimitive("getTrackStyleConfig"))),
        )
        val success = response["success"]?.jsonPrimitive?.booleanOrNull ?: false
        require(success) { response["error"]?.jsonPrimitive?.contentOrNull ?: "getTrackStyleConfig failed" }
        return response["config"]?.jsonObject ?: error("missing track style config")
    }

    private fun cacheConfig(config: JsonObject): TrackStyleCloudConfig {
        ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(CACHE_KEY_CONFIG_JSON, config.toString())
            .apply()
        return parse(config)
    }

    private fun readCachedConfig(): TrackStyleCloudConfig? {
        val raw = ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(CACHE_KEY_CONFIG_JSON, null)
            ?: return null
        return runCatching {
            parse(Json.parseToJsonElement(raw).jsonObject)
        }.getOrNull()
    }

    internal fun parse(config: JsonObject): TrackStyleCloudConfig {
        val defaultColorHex = config.string("defaultColorHex")?.hexToArgbLong()
        val cloudPresets = config["presets"]
            ?.let { it as? JsonArray }
            ?.mapNotNull(::parsePreset)
            ?.takeIf { it.isNotEmpty() }
        val presets = cloudPresets
            ?: defaultColorHex?.let {
                listOf(TrackColorPreset("默认颜色", listOf(it, it, it), 30f, 0.68f, 1f))
            }
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
            colorHex = defaultColorHex,
            colorAlpha = defaultOpacity.coerceIn(0.0, 1.0),
            glowOpacity = defaultPreset.glowAlpha.toDouble(),
            widthPct = (styleDefaults?.double("widthPct") ?: 40.0).coerceIn(5.0, 160.0),
            tailHalfLife = (styleDefaults?.double("tailHalfLife") ?: 70.0).coerceIn(5.0, 100.0),
            showDistance = styleDefaults?.boolean("showDistance") ?: false,
            totalDistance = GolfCamDistanceConfig.clamp(styleDefaults?.double("totalDistance") ?: 150.0),
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
            defaultColorHex = defaultColorHex,
        )
    }

    internal fun localFallback(): TrackStyleCloudConfig {
        val fallbackColor = 0xFF55F3D8
        val fallbackPreset = defaultTrackColorPresets.firstOrNull { it.name == "青蓝薄荷" }
            ?: defaultTrackColorPresets.first()
        return TrackStyleCloudConfig(
            presets = defaultTrackColorPresets,
            defaultColorHex = fallbackColor,
            defaultStyle = TrackStyle(
                colorPreset = fallbackPreset,
                colorHex = fallbackColor,
                colorAlpha = 0.75,
                glowOpacity = fallbackPreset.glowAlpha.toDouble(),
                widthPct = 40.0,
                tailHalfLife = 70.0,
                showDistance = false,
                totalDistance = 150.0,
                distanceUnit = DistanceUnit.YARDS,
                speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL,
                renderTuning = TrackRenderTuning(),
            ),
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
            glowAlpha = (obj.float("glowAlpha") ?: 0.68f).coerceIn(0f, 1f),
            bandAlpha = (obj.float("bandAlpha") ?: 1f).coerceIn(0f, 1f),
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
            highlightWhiteLiftBase = obj.float("highlightWhiteLiftBase") ?: 0.00f,
            highlightWhiteLiftScale = obj.float("highlightWhiteLiftScale") ?: 0.00f,
            highlightStart = obj.float("highlightStart") ?: 0.00f,
            highlightEnd = obj.float("highlightEnd") ?: 0.96f,
            highlightPeak = obj.float("highlightPeak") ?: 0.35f,
            highlightStrength = obj.float("highlightStrength") ?: 1.6f,
            widthTaperPower = obj.float("widthTaperPower") ?: 1.36f,
            apexThinStrength = obj.float("apexThinStrength") ?: 0.60f,
            apexThinRadius = obj.float("apexThinRadius") ?: 0.20f,
            highlightFalloffPower = obj.float("highlightFalloffPower") ?: 0.82f,
        ).clamped()
    }

    private fun TrackRenderTuning.clamped(): TrackRenderTuning {
        var start = highlightStart
        var end = highlightEnd
        var peak = highlightPeak
        if (kotlin.math.abs(start - 0.20f) < 0.001f &&
            kotlin.math.abs(end - 0.60f) < 0.001f &&
            kotlin.math.abs(peak - 0.42f) < 0.001f
        ) {
            start = 0.00f
            end = 0.96f
            peak = 0.32f
        }
        if (kotlin.math.abs(start - 0.18f) < 0.001f &&
            kotlin.math.abs(end - 0.96f) < 0.001f
        ) {
            start = 0.00f
        }

        start = start.coerceIn(0.00f, 0.90f)
        end = end.coerceIn(0.05f, 1.00f)
        if (end <= start + 0.05f) {
            end = minOf(1.0f, start + 0.05f)
        }
        peak = peak.coerceIn(start, end)

        return copy(
            bandAlphaMultiplier = bandAlphaMultiplier.coerceIn(0.00f, 1.40f),
            bodyAlphaBase = bodyAlphaBase.coerceIn(0.00f, 0.16f),
            bodyAlphaScale = bodyAlphaScale.coerceIn(0.50f, 1.20f),
            glowBlurMultiplier = glowBlurMultiplier.coerceIn(0.25f, 1.20f),
            outerGlowMultiplier = outerGlowMultiplier.coerceIn(0.20f, 1.00f),
            fillGlowMultiplier = fillGlowMultiplier.coerceIn(0.08f, 0.45f),
            highlightWidthRatio = highlightWidthRatio.coerceIn(0.30f, 0.85f),
            highlightAlphaMax = highlightAlphaMax.coerceIn(0.00f, 0.90f),
            highlightWhiteLiftBase = highlightWhiteLiftBase.coerceIn(0.00f, 0.00f),
            highlightWhiteLiftScale = highlightWhiteLiftScale.coerceIn(0.00f, 0.00f),
            highlightStart = start,
            highlightEnd = end,
            highlightPeak = peak,
            highlightStrength = highlightStrength.coerceIn(0.00f, 1.60f),
            widthTaperPower = widthTaperPower.coerceIn(0.70f, 2.20f),
            apexThinStrength = apexThinStrength.coerceIn(0.00f, 0.85f),
            apexThinRadius = apexThinRadius.coerceIn(0.04f, 0.35f),
            highlightFalloffPower = highlightFalloffPower.coerceIn(0.45f, 1.80f),
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
