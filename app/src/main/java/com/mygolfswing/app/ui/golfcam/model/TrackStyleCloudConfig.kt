package com.mygolfswing.app.ui.golfcam.model

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull

data class TrackStyleCloudConfig(
    val presets: List<TrackColorPreset>,
    val defaultStyle: TrackStyle,
    val defaultColorHex: Long?,
)

internal fun TrackStyle.refreshedWithCloudPreset(presets: List<TrackColorPreset>): TrackStyle {
    val matchedPreset = presets.firstOrNull { it.name == colorPreset.name } ?: return this
    val oldPresetColor = colorPreset.bandOuterColor()
    val colorWasPresetDerived = colorHex == null || colorHex == oldPresetColor
    return copy(
        colorPreset = matchedPreset,
        colorHex = if (colorWasPresetDerived) matchedPreset.bandOuterColor() else colorHex,
        glowOpacity = matchedPreset.glowAlpha.toDouble(),
    )
}

internal fun TrackStyle.refreshedWithCloudConfig(config: TrackStyleCloudConfig): TrackStyle {
    return refreshedWithCloudPreset(config.presets).copy(
        renderTuning = config.defaultStyle.renderTuning.clampedForGolfCamProduction(),
    )
}

internal fun trackStyleAfterCloudConfigLoaded(
    currentStyle: TrackStyle,
    initialFallbackStyle: TrackStyle,
    restoredStyle: TrackStyle?,
    config: TrackStyleCloudConfig,
): TrackStyle {
    return if (restoredStyle == null && currentStyle == initialFallbackStyle) {
        config.defaultStyle
    } else {
        currentStyle.refreshedWithCloudConfig(config)
    }
}

object TrackStyleCloudConfigLoader {
    private const val PREFS_NAME = "golf_cam_track_style_config"
    private const val CACHE_KEY_CONFIG_JSON = "config_json"

    private fun finiteDoubleIn(value: Double?, min: Double, max: Double, fallback: Double): Double {
        return value?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
    }

    private fun finiteFloatIn(value: Float?, min: Float, max: Float, fallback: Float): Float {
        return value?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
    }

    private fun finiteFloatAtLeast(value: Float?, min: Float, fallback: Float): Float {
        return value?.takeIf { it.isFinite() }?.coerceAtLeast(min) ?: fallback.coerceAtLeast(min)
    }

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
        return parseRemoteConfigResponse(response)
    }

    internal fun parseRemoteConfigResponse(response: JsonObject): JsonObject {
        val success = response.booleanLiteral("success") ?: false
        require(success) { response.string("error") ?: "getTrackStyleConfig failed" }
        return parseRemoteConfigPayload(response["config"])
    }

    private fun parseRemoteConfigPayload(config: JsonElement?): JsonObject {
        return when (config) {
            is JsonObject -> config
            is JsonPrimitive -> config.contentOrNull
                ?.let { raw ->
                    runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
                }
            else -> null
        } ?: error("missing track style config")
    }

    private fun cacheConfig(config: JsonObject): TrackStyleCloudConfig {
        val parsed = parse(config)
        ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(CACHE_KEY_CONFIG_JSON, config.toString())
            .apply()
        return parsed
    }

    private fun readCachedConfig(): TrackStyleCloudConfig? {
        val raw = ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(CACHE_KEY_CONFIG_JSON, null)
            ?: return null
        return runCatching {
            parse(Json.parseToJsonElement(raw) as? JsonObject ?: return@runCatching null)
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
            ?: presets.firstOrNull { it.name == GOLF_CAM_DEFAULT_PRESET_NAME }
            ?: presets.firstOrNull()
            ?: TrackStyle().colorPreset
        val defaultOpacity = finiteDoubleIn(config.double("defaultOpacity"), 0.0, 1.0, fallback = 0.75)
        val styleDefaults = config["styleDefaults"] as? JsonObject
        val renderTuning = (config["renderTuning"] as? JsonObject)?.let(::parseRenderTuning) ?: TrackRenderTuning()

        val defaultStyle = TrackStyle(
            colorPreset = defaultPreset,
            colorHex = defaultColorHex,
            colorAlpha = defaultOpacity,
            glowOpacity = defaultPreset.glowAlpha.toDouble(),
            widthPct = finiteDoubleIn(styleDefaults?.double("widthPct"), 5.0, 160.0, fallback = 40.0),
            tailHalfLife = finiteDoubleIn(styleDefaults?.double("tailHalfLife"), 5.0, 100.0, fallback = 70.0),
            showDistance = styleDefaults?.boolean("showDistance") ?: false,
            totalDistance = GolfCamDistanceConfig.clamp(styleDefaults?.double("totalDistance") ?: GolfCamDistanceConfig.DEFAULT_DISTANCE),
            distanceUnit = parseGolfCamDistanceUnit(styleDefaults?.string("distanceUnit")),
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
        val fallbackColor = GOLF_CAM_DEFAULT_COLOR_HEX
        val fallbackPreset = defaultGolfCamTrackColorPreset()
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
            ?.let { it as? JsonArray }
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.hexToArgbLong() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return TrackColorPreset(
            name = obj.string("name")?.takeIf { it.isNotBlank() } ?: "颜色",
            gradientColors = colors,
            glowBlur = finiteFloatAtLeast(obj.float("glowBlur"), min = 0f, fallback = 30f),
            glowAlpha = finiteFloatIn(obj.float("glowAlpha"), 0f, 1f, fallback = 0.68f),
            bandAlpha = finiteFloatIn(obj.float("bandAlpha"), 0f, 1f, fallback = 1f),
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
        ).clampedForGolfCamProduction()
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.float(key: String): Float? = (this[key] as? JsonPrimitive)?.floatOrNull
    private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.booleanLiteral(key: String): Boolean? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return if (primitive.isString) null else primitive.booleanOrNull
    }

    private fun String.hexToArgbLong(): Long? {
        val clean = trim().removePrefix("#")
        if (clean.length != 6) return null
        return clean.toLongOrNull(16)?.or(0xFF000000)
    }
}
