package com.mygolfswing.app.ui.golfcam.model

import android.net.Uri
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.golfcam.normalizedVideoRotation
import com.mygolfswing.app.ui.golfcam.render.GolfCamCurveSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object GolfCamWorkflowStateStore {
    private const val PREFS_NAME = "golf_cam_workflow_state"
    private const val KEY_PREFIX = "flight_path:"
    private const val MIN_RESTORED_TRIM_DURATION_MS = 500L

    private fun finiteDouble(value: Double?, fallback: Double): Double {
        return value?.takeIf { it.isFinite() } ?: fallback
    }

    private fun finiteDoubleIn(value: Double?, min: Double, max: Double, fallback: Double): Double {
        return finiteDouble(value, fallback).coerceIn(min, max)
    }

    private fun finiteFloat(value: Float?, fallback: Float): Float {
        return value?.takeIf { it.isFinite() } ?: fallback
    }

    private fun finiteFloatIn(value: Float?, min: Float, max: Float, fallback: Float): Float {
        return finiteFloat(value, fallback).coerceIn(min, max)
    }

    fun save(videoUri: Uri, state: GolfCamWorkflowState) {
        ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(storageKey(videoUri), encode(state).toString())
            .apply()
    }

    fun clear(videoUri: Uri) {
        ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .remove(storageKey(videoUri))
            .apply()
    }

    fun restore(videoUri: Uri, target: GolfCamWorkflowState): Boolean {
        val prefs = ServiceLocator.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val key = storageKey(videoUri)
        val raw = prefs.getString(key, null)
            ?: return false
        val restored = runCatching {
            decode(Json.parseToJsonElement(raw).jsonObject)
        }.getOrNull() ?: run {
            prefs.edit().remove(key).apply()
            return false
        }
        target.copyFrom(restored)
        target.videoUri = videoUri
        return true
    }

    internal fun storageKey(videoUri: Uri): String = storageKey(videoUri.toString())

    internal fun storageKey(videoUri: String): String = KEY_PREFIX + videoUri

    internal fun encode(state: GolfCamWorkflowState): JsonObject {
        return jsonObject(
            "videoConfig" to state.videoConfig?.let {
                jsonObject(
                    "durationMs" to JsonPrimitive(it.durationMs),
                    "frameRate" to JsonPrimitive(it.frameRate),
                    "visualWidth" to JsonPrimitive(it.visualWidth),
                    "visualHeight" to JsonPrimitive(it.visualHeight),
                    "rotationDegrees" to JsonPrimitive(it.rotationDegrees),
                )
            },
            "trimResult" to state.trimResult?.let {
                jsonObject("startMs" to JsonPrimitive(it.startMs), "endMs" to JsonPrimitive(it.endMs))
            },
            "impactFrame" to state.impactFrame?.let { impact ->
                jsonObject(
                    "frameIndex" to JsonPrimitive(impact.frameIndex),
                    "timeMs" to JsonPrimitive(impact.timeMs),
                    "candidateFrames" to JsonArray(
                        impact.candidateFrames.map {
                            jsonObject(
                                "frameIndex" to JsonPrimitive(it.frameIndex),
                                "confidence" to JsonPrimitive(it.confidence),
                                "timeSec" to JsonPrimitive(it.timeSec),
                            )
                        },
                    ),
                )
            },
            "impactPosition" to state.impactPosition?.let {
                pointObject(it.normalizedX, it.normalizedY)
            },
            "landing" to state.landing?.let {
                jsonObject(
                    "normalizedX" to JsonPrimitive(it.normalizedX),
                    "normalizedY" to JsonPrimitive(it.normalizedY),
                    "flightTimeSec" to JsonPrimitive(it.flightTimeSec),
                )
            },
            "flightCurve" to state.flightCurve?.let {
                jsonObject(
                    "controlPoint1" to pointObject(it.controlPoint1.x, it.controlPoint1.y),
                    "controlPoint2" to pointObject(it.controlPoint2.x, it.controlPoint2.y),
                )
            },
            "trackStyle" to state.trackStyle?.style?.let(::encodeTrackStyle),
        )
    }

    internal fun decode(json: JsonObject): GolfCamWorkflowState {
        val state = GolfCamWorkflowState()
        val videoConfigJson = json.obj("videoConfig")
        state.videoConfig = videoConfigJson?.videoConfigOrNull()
        state.trimResult = json.obj("trimResult")?.trimResultOrNull()
        state.impactFrame = json.obj("impactFrame")?.let { impact ->
            val rawTimeMs = (impact.long("timeMs") ?: 0L).coerceAtLeast(0L)
            val trim = state.trimResult
            val restoredFrameRate = videoConfigJson?.double("frameRate")?.takeIf { it.isFinite() && it in 1.0..120.0 }
            val restoredTimeMs = trim?.let {
                clampRestoredImpactTimeMs(
                    rawTimeMs = rawTimeMs,
                    trim = it,
                    restoredFrameRate = restoredFrameRate,
                )
            } ?: rawTimeMs
            val rawCandidateFrames = impact.array("candidateFrames")?.mapNotNull { candidate ->
                candidate.jsonObjectOrNull()?.let {
                    val frameIndex = it.int("frameIndex") ?: return@let null
                    if (frameIndex < 0) return@let null
                    CandidateImpactFrame(
                        frameIndex = frameIndex,
                        confidence = finiteFloatIn(it.float("confidence"), 0f, 1f, fallback = 0f),
                        timeSec = finiteDouble(it.double("timeSec"), fallback = 0.0).coerceAtLeast(0.0),
                    )
                }
            }.orEmpty()
            ImpactFrameSelection(
                frameIndex = restoredImpactFrameIndex(
                    rawFrameIndex = impact.int("frameIndex") ?: 0,
                    restoredTimeMs = restoredTimeMs,
                    trim = trim,
                    frameRate = restoredFrameRate,
                ),
                timeMs = restoredTimeMs,
                candidateFrames = restoredCandidateFramesForTrimRange(
                    candidateFrames = rawCandidateFrames,
                    trim = trim,
                    frameRate = restoredFrameRate,
                ),
            )
        }
        state.impactPosition = json.obj("impactPosition")?.let {
            ImpactPositionSelection(it.normalizedFloat("x"), it.normalizedFloat("y"))
        }
        state.landing = json.obj("landing")?.let {
            LandingOutput(
                normalizedX = it.normalizedFloat("normalizedX"),
                normalizedY = it.normalizedFloat("normalizedY"),
                flightTimeSec = finiteDoubleIn(it.double("flightTimeSec"), 0.5, 8.0, fallback = 4.5),
            )
        }
        state.flightCurve = json.obj("flightCurve")?.let { curve ->
            val impact = state.impactPosition
            val landing = state.landing
            if (impact != null && landing != null) {
                val suggested = GolfCamCurveSpec.suggestedControlPoints(
                    start = NormalizedPoint(impact.normalizedX, impact.normalizedY),
                    end = NormalizedPoint(landing.normalizedX, landing.normalizedY),
                )
                val cp1 = curve.obj("controlPoint1")?.point() ?: suggested.first
                val cp2 = curve.obj("controlPoint2")?.point() ?: suggested.second
                FlightCurveSelection(impact, landing, cp1, cp2)
            } else {
                null
            }
        }
        state.trackStyle = json.obj("trackStyle")?.let { styleJson ->
            state.flightCurve?.let {
                clampedTrackStyleSelection(TrackStyleSelection(decodeTrackStyle(styleJson), it))
            }
        }
        state.pruneInvalidDownstreamState()
        return state
    }

    private fun encodeTrackStyle(style: TrackStyle): JsonObject {
        return jsonObject(
            "colorPreset" to jsonObject(
                "name" to JsonPrimitive(style.colorPreset.name),
                "gradientColors" to JsonArray(style.colorPreset.gradientColors.map { JsonPrimitive(it) }),
                "glowBlur" to JsonPrimitive(style.colorPreset.glowBlur),
                "glowAlpha" to JsonPrimitive(style.colorPreset.glowAlpha),
                "bandAlpha" to JsonPrimitive(style.colorPreset.bandAlpha),
            ),
            "colorHex" to style.colorHex?.let { JsonPrimitive(it) },
            "colorAlpha" to JsonPrimitive(style.colorAlpha),
            "glowOpacity" to JsonPrimitive(style.glowOpacity),
            "widthPct" to JsonPrimitive(style.widthPct),
            "taperPower" to JsonPrimitive(style.taperPower),
            "tailHalfLife" to JsonPrimitive(style.tailHalfLife),
            "minTail" to JsonPrimitive(style.minTail),
            "showDistance" to JsonPrimitive(style.showDistance),
            "totalDistance" to JsonPrimitive(style.totalDistance),
            "distanceUnit" to JsonPrimitive(style.distanceUnit.name),
            "speedLevel" to JsonPrimitive(style.speedLevel),
            "activeTrajectoryAlgorithm" to JsonPrimitive(style.activeTrajectoryAlgorithm.name),
            "compareTrajectoryAlgorithms" to JsonPrimitive(style.compareTrajectoryAlgorithms),
            "trajectoryPreset" to JsonPrimitive(style.trajectoryPreset.name),
            "motionLaunchBias" to JsonPrimitive(style.motionLaunchBias),
            "motionDrag" to JsonPrimitive(style.motionDrag),
            "motionLiftHold" to JsonPrimitive(style.motionLiftHold),
            "motionDescentAcceleration" to JsonPrimitive(style.motionDescentAcceleration),
            "motionTailBoost" to JsonPrimitive(style.motionTailBoost),
            "motionAnchorTime" to style.motionAnchorTime?.let { JsonPrimitive(it) },
            "v4VMin" to style.v4VMin?.let { JsonPrimitive(it) },
            "v4VMax" to style.v4VMax?.let { JsonPrimitive(it) },
            "v4PLeft" to style.v4PLeft?.let { JsonPrimitive(it) },
            "v4PRight" to style.v4PRight?.let { JsonPrimitive(it) },
            "v4CustomSPeak" to style.v4CustomSPeak?.let { JsonPrimitive(it) },
            "effectType" to JsonPrimitive(style.effectType.name),
            "renderTuning" to encodeRenderTuning(style.renderTuning),
        )
    }

    private fun decodeTrackStyle(json: JsonObject): TrackStyle {
        val preset = json.obj("colorPreset")?.let(::decodePreset) ?: defaultGolfCamTrackColorPreset()
        return TrackStyle(
            colorPreset = preset,
            colorHex = json.long("colorHex"),
            colorAlpha = finiteDoubleIn(json.double("colorAlpha"), 0.0, 1.0, fallback = 0.75),
            glowOpacity = finiteDoubleIn(json.double("glowOpacity"), 0.0, 1.0, fallback = preset.glowAlpha.toDouble()),
            widthPct = finiteDoubleIn(json.double("widthPct"), 5.0, 160.0, fallback = 40.0),
            taperPower = finiteFloat(json.float("taperPower"), fallback = 1.8f),
            tailHalfLife = finiteDoubleIn(json.double("tailHalfLife"), 5.0, 100.0, fallback = 70.0),
            minTail = finiteFloat(json.float("minTail"), fallback = 0.12f),
            showDistance = json.boolean("showDistance") ?: false,
            totalDistance = GolfCamDistanceConfig.clamp(json.double("totalDistance") ?: GolfCamDistanceConfig.DEFAULT_DISTANCE),
            distanceUnit = parseGolfCamDistanceUnit(json.string("distanceUnit")),
            speedLevel = GolfCamSpeedConfig.clamp(json.double("speedLevel") ?: GolfCamSpeedConfig.DEFAULT_LEVEL),
            activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
            compareTrajectoryAlgorithms = false,
            trajectoryPreset = TrajectoryPresetKind.GOLF_CAM_LIKE,
            motionLaunchBias = json.double("motionLaunchBias") ?: TrajectoryMotionParameters.standard.launchBias,
            motionDrag = json.double("motionDrag") ?: TrajectoryMotionParameters.standard.drag,
            motionLiftHold = json.double("motionLiftHold") ?: TrajectoryMotionParameters.standard.liftHold,
            motionDescentAcceleration = json.double("motionDescentAcceleration") ?: TrajectoryMotionParameters.standard.descentAcceleration,
            motionTailBoost = json.double("motionTailBoost") ?: TrajectoryMotionParameters.standard.tailBoost,
            motionAnchorTime = json.double("motionAnchorTime"),
            v4VMin = json.double("v4VMin"),
            v4VMax = json.double("v4VMax"),
            v4PLeft = json.double("v4PLeft"),
            v4PRight = json.double("v4PRight"),
            v4CustomSPeak = json.double("v4CustomSPeak"),
            effectType = enumValue(json.string("effectType"), TrackEffectType.GRADIENT),
            renderTuning = json.obj("renderTuning")?.let(::decodeRenderTuning) ?: TrackRenderTuning(),
        )
    }

    private fun decodePreset(json: JsonObject): TrackColorPreset {
        return TrackColorPreset(
            name = json.string("name") ?: "颜色",
            gradientColors = json.array("gradientColors")?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(0xFFFFFFFF),
            glowBlur = finiteFloat(json.float("glowBlur"), fallback = 30f).coerceAtLeast(0f),
            glowAlpha = finiteFloatIn(json.float("glowAlpha"), 0f, 1f, fallback = 0.68f),
            bandAlpha = finiteFloatIn(json.float("bandAlpha"), 0f, 1f, fallback = 1f),
        )
    }

    private fun encodeRenderTuning(tuning: TrackRenderTuning): JsonObject {
        return jsonObject(
            "showOuterGlow" to JsonPrimitive(tuning.showOuterGlow),
            "showBodyBand" to JsonPrimitive(tuning.showBodyBand),
            "showHighlightCore" to JsonPrimitive(tuning.showHighlightCore),
            "bandAlphaMultiplier" to JsonPrimitive(tuning.bandAlphaMultiplier),
            "bodyAlphaBase" to JsonPrimitive(tuning.bodyAlphaBase),
            "bodyAlphaScale" to JsonPrimitive(tuning.bodyAlphaScale),
            "glowBlurMultiplier" to JsonPrimitive(tuning.glowBlurMultiplier),
            "outerGlowMultiplier" to JsonPrimitive(tuning.outerGlowMultiplier),
            "fillGlowMultiplier" to JsonPrimitive(tuning.fillGlowMultiplier),
            "highlightWidthRatio" to JsonPrimitive(tuning.highlightWidthRatio),
            "highlightAlphaMax" to JsonPrimitive(tuning.highlightAlphaMax),
            "highlightWhiteLiftBase" to JsonPrimitive(tuning.highlightWhiteLiftBase),
            "highlightWhiteLiftScale" to JsonPrimitive(tuning.highlightWhiteLiftScale),
            "highlightStart" to JsonPrimitive(tuning.highlightStart),
            "highlightEnd" to JsonPrimitive(tuning.highlightEnd),
            "highlightPeak" to JsonPrimitive(tuning.highlightPeak),
            "highlightStrength" to JsonPrimitive(tuning.highlightStrength),
            "widthTaperPower" to JsonPrimitive(tuning.widthTaperPower),
            "apexThinStrength" to JsonPrimitive(tuning.apexThinStrength),
            "apexThinRadius" to JsonPrimitive(tuning.apexThinRadius),
            "highlightFalloffPower" to JsonPrimitive(tuning.highlightFalloffPower),
        )
    }

    private fun decodeRenderTuning(json: JsonObject): TrackRenderTuning {
        return TrackRenderTuning(
            showOuterGlow = json.boolean("showOuterGlow") ?: false,
            showBodyBand = json.boolean("showBodyBand") ?: true,
            showHighlightCore = json.boolean("showHighlightCore") ?: true,
            bandAlphaMultiplier = json.float("bandAlphaMultiplier") ?: 1.0f,
            bodyAlphaBase = json.float("bodyAlphaBase") ?: 0.10f,
            bodyAlphaScale = json.float("bodyAlphaScale") ?: 0.98f,
            glowBlurMultiplier = json.float("glowBlurMultiplier") ?: 0.82f,
            outerGlowMultiplier = json.float("outerGlowMultiplier") ?: 0.62f,
            fillGlowMultiplier = json.float("fillGlowMultiplier") ?: 0.22f,
            highlightWidthRatio = json.float("highlightWidthRatio") ?: 0.50f,
            highlightAlphaMax = json.float("highlightAlphaMax") ?: 0.90f,
            highlightWhiteLiftBase = json.float("highlightWhiteLiftBase") ?: 0.00f,
            highlightWhiteLiftScale = json.float("highlightWhiteLiftScale") ?: 0.00f,
            highlightStart = json.float("highlightStart") ?: 0.00f,
            highlightEnd = json.float("highlightEnd") ?: 0.96f,
            highlightPeak = json.float("highlightPeak") ?: 0.35f,
            highlightStrength = json.float("highlightStrength") ?: 1.6f,
            widthTaperPower = json.float("widthTaperPower") ?: 1.36f,
            apexThinStrength = json.float("apexThinStrength") ?: 0.60f,
            apexThinRadius = json.float("apexThinRadius") ?: 0.20f,
            highlightFalloffPower = json.float("highlightFalloffPower") ?: 0.82f,
        ).clampedForGolfCamProduction()
    }

    private fun GolfCamWorkflowState.copyFrom(other: GolfCamWorkflowState) {
        videoConfig = other.videoConfig
        trimResult = other.trimResult
        impactFrame = other.impactFrame
        impactPosition = other.impactPosition
        landing = other.landing
        flightCurve = other.flightCurve
        trackStyle = other.trackStyle
    }

    private fun pointObject(x: Float, y: Float): JsonObject {
        return jsonObject("x" to JsonPrimitive(x), "y" to JsonPrimitive(y))
    }

    private fun JsonObject.point(): NormalizedPoint {
        return NormalizedPoint(
            x = normalizedFloat("x"),
            y = finiteFloatIn(float("y"), -1.0f, 1.5f, fallback = 0.5f),
        )
    }

    private fun JsonObject.trimResultOrNull(): TrimResult? {
        val startMs = long("startMs") ?: return null
        val endMs = long("endMs") ?: return null
        if (startMs < 0L || endMs - startMs < MIN_RESTORED_TRIM_DURATION_MS) return null
        return TrimResult(startMs = startMs, endMs = endMs)
    }

    private fun clampRestoredImpactTimeMs(
        rawTimeMs: Long,
        trim: TrimResult,
        restoredFrameRate: Double?,
    ): Long {
        val maxTimeMs = restoredFrameRate?.let { fps ->
            GolfCamFrameTimeline.lastFrameTimeMsForRange(trim.startMs, trim.endMs, fps)
        } ?: (trim.endMs - 1L).coerceAtLeast(trim.startMs)
        return rawTimeMs.coerceIn(trim.startMs, maxTimeMs)
    }

    private fun restoredImpactFrameIndex(
        rawFrameIndex: Int,
        restoredTimeMs: Long,
        trim: TrimResult?,
        frameRate: Double?,
    ): Int {
        if (trim == null || frameRate == null) return rawFrameIndex.coerceAtLeast(0)
        val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trim.startMs, trim.endMs, frameRate)
        val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trim.startMs, trim.endMs, frameRate)
        if (restoredTimeMs >= lastAnchorMs) return lastFrame
        val relativeSeconds = GolfCamFrameTimeline.nonNegativeDeltaMs(trim.startMs, restoredTimeMs) / 1000.0
        return GolfCamFrameTimeline.frameIndexAt(relativeSeconds, frameRate).coerceIn(0, lastFrame)
    }

    private fun restoredCandidateFramesForTrimRange(
        candidateFrames: List<CandidateImpactFrame>,
        trim: TrimResult?,
        frameRate: Double?,
    ): List<CandidateImpactFrame> {
        if (trim == null || frameRate == null) return emptyList()
        val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trim.startMs, trim.endMs, frameRate)
        val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trim.startMs, trim.endMs, frameRate)
        val maxRelativeSec = GolfCamFrameTimeline.nonNegativeDeltaMs(trim.startMs, lastAnchorMs) / 1000.0
        return candidateFrames.mapNotNull { candidate ->
            val relTime = candidate.timeSec
            if (!relTime.isFinite()) return@mapNotNull null
            if (relTime !in 0.0..maxRelativeSec) return@mapNotNull null
            val frameIndex = if (relTime >= maxRelativeSec) {
                lastFrame
            } else {
                GolfCamFrameTimeline.frameIndexAt(relTime, frameRate).coerceIn(0, lastFrame)
            }
            CandidateImpactFrame(
                frameIndex = frameIndex,
                confidence = finiteFloatIn(candidate.confidence, 0f, 1f, fallback = 0f),
                timeSec = relTime,
            )
        }
    }

    private fun JsonObject.videoConfigOrNull(): GolfCamVideoConfig? {
        val durationMs = (long("durationMs") ?: 1L).coerceAtLeast(1L)
        val rawFrameRate = double("frameRate")
        val frameRate = rawFrameRate?.takeIf { it in 1.0..120.0 } ?: 0.0
        val visualWidth = (int("visualWidth") ?: 1).coerceAtLeast(1)
        val visualHeight = (int("visualHeight") ?: 1).coerceAtLeast(1)
        val rotation = normalizedVideoRotation(int("rotationDegrees") ?: 0)
        return GolfCamVideoConfig(
            durationMs = durationMs,
            frameRate = frameRate,
            visualWidth = visualWidth,
            visualHeight = visualHeight,
            rotationDegrees = rotation,
        )
    }

    private fun JsonObject.normalizedFloat(key: String): Float {
        return finiteFloatIn(float(key), 0f, 1f, fallback = 0.5f)
    }

    private fun jsonObject(vararg pairs: Pair<String, JsonElement?>): JsonObject {
        return JsonObject(pairs.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.float(key: String): Float? = (this[key] as? JsonPrimitive)?.floatOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T {
        return raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }
}
