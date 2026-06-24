package com.mygolfswing.app.ui.golfcam.model

import androidx.compose.ui.graphics.StrokeCap
import com.mygolfswing.app.ui.golfcam.ImpactCandidateDetectionKey
import com.mygolfswing.app.ui.golfcam.effectiveExportFrameRotation
import com.mygolfswing.app.ui.golfcam.exportTrimRange
import com.mygolfswing.app.ui.golfcam.exportVideoDimensions
import com.mygolfswing.app.ui.golfcam.isAudioMimeType
import com.mygolfswing.app.ui.golfcam.clampedCurveControlPoint
import com.mygolfswing.app.ui.golfcam.normalizedLandingCoordinate
import com.mygolfswing.app.ui.golfcam.normalizedVideoCoordinate
import com.mygolfswing.app.ui.golfcam.shouldReuseImpactCandidates
import com.mygolfswing.app.ui.golfcam.steps.clampTrimEndFraction
import com.mygolfswing.app.ui.golfcam.steps.clampTrimStartFraction
import com.mygolfswing.app.ui.golfcam.steps.defaultTrimEndFraction
import com.mygolfswing.app.ui.golfcam.steps.initialTrimFractions
import com.mygolfswing.app.ui.golfcam.steps.initialImpactFrameIndex
import com.mygolfswing.app.ui.golfcam.steps.impactFrameDisplayText
import com.mygolfswing.app.ui.golfcam.steps.minimumTrimFraction
import com.mygolfswing.app.ui.golfcam.steps.trimPlayheadFraction
import com.mygolfswing.app.ui.golfcam.steps.trimPlayheadSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.trimSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.thumbnailRotationDegrees
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayFrameSampler
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayRenderSpec
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelRenderSpec
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelTextMetrics
import com.mygolfswing.app.ui.golfcam.render.GolfCamCurveSpec
import com.mygolfswing.app.ui.golfcam.render.TrajectoryRibbonRenderer
import com.mygolfswing.app.ui.golfcam.render.VideoDisplayGeometry
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
    fun trajectorySegmentCountMatchesIosTrailV3Config() {
        assertEquals(360, GolfCamTrailGeometry.SEGMENT_COUNT)
    }

    @Test
    fun flightCurveDefaultsMatchIosGolfCamLikeControlPointSpec() {
        val (p1, p2) = GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(0.10f, 0.80f),
            end = NormalizedPoint(0.90f, 0.62f),
        )

        assertEquals(0.164f, p1.x, 1e-6f)
        assertEquals(0.43712f, p1.y, 1e-6f)
        assertEquals(0.356f, p2.x, 1e-6f)
        assertEquals(0.152f, p2.y, 1e-6f)
    }

    @Test
    fun flightCurveClampAndStrokeMatchIosGolfCamCurveStrokeSpec() {
        val clamped = GolfCamCurveSpec.clampPoint(NormalizedPoint(-1f, -2f))
        val last = GolfCamCurveSpec.SEGMENTS - 1
        val startColor = GolfCamCurveSpec.segmentColor(0)
        val endColor = GolfCamCurveSpec.segmentColor(last)

        assertEquals(0.02f, clamped.x, 0f)
        assertEquals(-0.5f, clamped.y, 0f)
        assertEquals(180, GolfCamCurveSpec.SEGMENTS)
        assertEquals(StrokeCap.Butt, GolfCamCurveSpec.STROKE_CAP)
        assertEquals(0.14f, GolfCamCurveSpec.SHADOW_ALPHA, 0f)
        assertEquals(1.1f, GolfCamCurveSpec.SHADOW_WIDTH_ADD, 0f)
        assertEquals(6.50f, GolfCamCurveSpec.foregroundWidth(0), 0.001f)
        assertEquals(1.15f, GolfCamCurveSpec.foregroundWidth(last), 0.001f)
        assertTrue(GolfCamCurveSpec.foregroundWidth(0) > GolfCamCurveSpec.foregroundWidth(last))
        assertEquals(0.08f, startColor.red, 0.003f)
        assertEquals(0.23f, startColor.green, 0.003f)
        assertEquals(0.18f, startColor.blue, 0.003f)
        assertEquals(0.24f, startColor.alpha, 0.003f)
        assertEquals(0.55f, endColor.red, 0.003f)
        assertEquals(0.96f, endColor.green, 0.003f)
        assertEquals(0.82f, endColor.blue, 0.003f)
        assertEquals(0.96f, endColor.alpha, 0.003f)
    }

    @Test
    fun trackStylePrimaryColorMatchesIosDefaultColorAndBandOuterRules() {
        val preset = TrackColorPreset(
            name = "测试色",
            gradientColors = listOf(0xFF000000, 0xFF646464, 0xFFC8C8C8),
            glowBlur = 20f,
            glowAlpha = 0.4f,
            bandAlpha = 0.8f,
        )

        assertEquals(0xFF8C8C8C, preset.bandOuterColor())
        assertEquals(0xFF123456, TrackStyle(colorPreset = preset, colorHex = 0xFF123456).primaryColorArgb())
        assertEquals(0xFF8C8C8C, TrackStyle(colorPreset = preset).primaryColorArgb())
    }

    @Test
    fun trackStyleProductionDefaultsUseIosV5AndCloudFallbackWidth() {
        val style = TrackStyle()
        val fallback = TrackStyleCloudConfigLoader.localFallback().defaultStyle
        val parsedMissingDefaults = TrackStyleCloudConfigLoader.parse(JsonObject(emptyMap())).defaultStyle

        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, style.activeTrajectoryAlgorithm)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, fallback.activeTrajectoryAlgorithm)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, parsedMissingDefaults.activeTrajectoryAlgorithm)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(40.0, fallback.widthPct, 0.0)
        assertEquals(40.0, parsedMissingDefaults.widthPct, 0.0)
        assertEquals(0xFF55F3D8, fallback.colorHex)
        assertEquals(0.75, fallback.colorAlpha, 0.0)
        assertFalse(fallback.showDistance)
        assertEquals(DistanceUnit.YARDS, fallback.distanceUnit)
    }

    @Test
    fun ribbonRendererConsumesCloudPresetBandAlphaWithColorOpacityAndTuning() {
        val preset = TrackColorPreset(
            name = "半透明轨迹",
            gradientColors = listOf(0xFF102030, 0xFF405060, 0xFF708090),
            glowAlpha = 0.5f,
            bandAlpha = 0.5f,
        )
        val style = TrackStyle(
            colorPreset = preset,
            colorHex = 0xFF405060,
            colorAlpha = 0.8,
            renderTuning = TrackRenderTuning(bandAlphaMultiplier = 1.25f),
        )

        assertEquals(0.5f, style.colorPreset.bandAlpha, 0f)
        assertEquals(0.5f, style.glowOpacity.toFloat(), 0f)
        assertEquals(0.5f, TrajectoryRibbonRenderer.bodyBandAlpha(style), 1e-6f)
        assertEquals(
            0.5f,
            TrajectoryRibbonRenderer.bodyBandAlpha(
                style.copy(colorAlpha = 1.0, renderTuning = TrackRenderTuning()),
            ),
            1e-6f,
        )
        assertEquals(
            1.0f,
            TrajectoryRibbonRenderer.bodyBandAlpha(
                style.copy(colorAlpha = 1.0, renderTuning = TrackRenderTuning(bandAlphaMultiplier = 3f)),
            ),
            1e-6f,
        )
    }

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
    fun displayDistanceCadenceHonorsVideoFrameRate() {
        val thirtyFps = GolfCamSpeedConfig.displayDistance(
            totalDistance = 150.0,
            elapsedSec = 1.04,
            totalDurationSec = 3.0,
            speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL,
            frameRate = 30.0,
        )
        val sixtyFps = GolfCamSpeedConfig.displayDistance(
            totalDistance = 150.0,
            elapsedSec = 1.04,
            totalDurationSec = 3.0,
            speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL,
            frameRate = 60.0,
        )

        assertTrue(sixtyFps > thirtyFps)
    }

    @Test
    fun distanceConfigMatchesIosCloudAndUiRange() {
        assertEquals(10.0, GolfCamDistanceConfig.clamp(-10.0), 0.0)
        assertEquals(10.0, GolfCamDistanceConfig.clamp(1.0), 0.0)
        assertEquals(1000.0, GolfCamDistanceConfig.clamp(1200.0), 0.0)
        assertEquals("1", GolfCamDistanceConfig.inputText("1"))
        assertEquals("1000", GolfCamDistanceConfig.inputText("10000"))
        assertEquals("150", GolfCamDistanceConfig.inputText("150yd"))
        assertEquals("", GolfCamDistanceConfig.inputText(""))
        assertEquals("1000", GolfCamDistanceConfig.displayText(1000.9))
        assertEquals(91.44, GolfCamDistanceConfig.convert(100.0, DistanceUnit.YARDS, DistanceUnit.METERS), 1e-6)
        assertEquals(1000.0, GolfCamDistanceConfig.convert(1000.0, DistanceUnit.METERS, DistanceUnit.YARDS), 0.0)
    }

    @Test
    fun headGlowAlphaSpecIsSharedByPreviewAndExport() {
        val alphas = TrajectoryOverlayRenderSpec.headGlowAlphas(0.7f)
        val style = TrackStyle(widthPct = 40.0)

        assertEquals(0.245f, alphas.outer, 1e-6f)
        assertEquals(0.525f, alphas.body, 1e-6f)
        assertEquals(0.63f, alphas.core, 1e-6f)
        assertTrue(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(0.999f))
        assertFalse(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(1f))
        assertEquals(
            GolfCamTrailGeometry.strokeWidth(
                widthPct = style.widthPct,
                videoHeight = 1080f,
                globalProgress = 0.5f,
                apexProgress = 0.4f,
                tuning = style.renderTuning,
            ) * 1.25f,
            TrajectoryOverlayRenderSpec.headGlowRadius(
                style = style,
                videoHeight = 1080f,
                headProgress = 0.5f,
                apexProgress = 0.4f,
            ),
            1e-6f,
        )
    }

    @Test
    fun videoDisplayGeometryMatchesIosResizeAspectFit() {
        val portraitInTallScreen = VideoDisplayGeometry.fit(
            containerWidth = 1440f,
            containerHeight = 3120f,
            videoAspectRatio = 9f / 16f,
        )
        val portraitInShortBox = VideoDisplayGeometry.fit(
            containerWidth = 1440f,
            containerHeight = 1000f,
            videoAspectRatio = 9f / 16f,
        )

        assertEquals(1440f, portraitInTallScreen.width, 0.01f)
        assertEquals(2560f, portraitInTallScreen.height, 0.01f)
        assertEquals(562.5f, portraitInShortBox.width, 0.01f)
        assertEquals(1000f, portraitInShortBox.height, 0.01f)
    }

    @Test
    fun interactiveOverlayCoordinatesClampToFullIosNormalizedRange() {
        assertEquals(0f, normalizedVideoCoordinate(-20f, 1000f), 0f)
        assertEquals(0f, normalizedVideoCoordinate(10f, 0f), 0f)
        assertEquals(0.5f, normalizedVideoCoordinate(500f, 1000f), 0f)
        assertEquals(1f, normalizedVideoCoordinate(1200f, 1000f), 0f)
    }

    @Test
    fun landingMarkerAnchorCompensatesIosCustomImageOffset() {
        assertEquals(0.506f, normalizedLandingCoordinate(500f, 1000f, markerSizePx = 24f), 1e-6f)
        assertEquals(1f, normalizedLandingCoordinate(998f, 1000f, markerSizePx = 24f), 0f)
        assertEquals(0f, normalizedLandingCoordinate(-10f, 1000f, markerSizePx = 24f), 0f)
    }

    @Test
    fun curveControlDragClampMatchesIosUnifiedFlowRange() {
        val topLeft = clampedCurveControlPoint(-500f, -1200f, 1000f, 1000f)
        val bottomRight = clampedCurveControlPoint(1600f, 1800f, 1000f, 1000f)

        assertEquals(0f, topLeft.x, 0f)
        assertEquals(-1f, topLeft.y, 0f)
        assertEquals(1f, bottomRight.x, 0f)
        assertEquals(1.5f, bottomRight.y, 0f)
    }

    @Test
    fun exportVideoDimensionsUseVisualOrientationForRotatedPhoneVideo() {
        val landscape = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 0)
        val portrait90 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 90)
        val portrait270 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 270)

        assertEquals(1920, landscape.width)
        assertEquals(1080, landscape.height)
        assertEquals(1080, portrait90.width)
        assertEquals(1920, portrait90.height)
        assertEquals(1080, portrait270.width)
        assertEquals(1920, portrait270.height)
    }

    @Test
    fun exportFrameRotationAvoidsDoubleRotatingRetrieverVisualFrames() {
        assertEquals(
            0,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 0,
                frameWidth = 1920,
                frameHeight = 1080,
                exportWidth = 1920,
                exportHeight = 1080,
            ),
        )
        assertEquals(
            90,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 90,
                frameWidth = 1920,
                frameHeight = 1080,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
        assertEquals(
            0,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 90,
                frameWidth = 1080,
                frameHeight = 1920,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
        assertEquals(
            0,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 270,
                frameWidth = 1080,
                frameHeight = 1920,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
    }

    @Test
    fun exportAudioTrackDetectionMatchesAndroidMuxerMimeRules() {
        assertTrue(isAudioMimeType("audio/mp4a-latm"))
        assertTrue(isAudioMimeType("audio/3gpp"))
        assertFalse(isAudioMimeType("video/avc"))
        assertFalse(isAudioMimeType(null))
    }

    @Test
    fun exportTrimRangeMatchesIosTrimmedDurationRules() {
        val full = exportTrimRange(trimStartMs = 0L, trimEndMs = null, sourceDurationMs = 60_000L)
        val clipped = exportTrimRange(trimStartMs = 10_000L, trimEndMs = 25_000L, sourceDurationMs = 60_000L)
        val clamped = exportTrimRange(trimStartMs = 70_000L, trimEndMs = 90_000L, sourceDurationMs = 60_000L)

        assertEquals(0L, full.startMs)
        assertEquals(60_000L, full.endMs)
        assertEquals(60_000L, full.durationMs)
        assertEquals(10_000L, clipped.startMs)
        assertEquals(25_000L, clipped.endMs)
        assertEquals(15_000L, clipped.durationMs)
        assertEquals(59_999L, clamped.startMs)
        assertEquals(60_000L, clamped.endMs)
        assertEquals(1L, clamped.durationMs)
    }

    @Test
    fun frameTimelineUsesActualVideoFrameRateForEditingSteps() {
        assertEquals(18, GolfCamFrameTimeline.frameIndexAt(relativeSeconds = 0.30, frameRate = 60.0))
        assertEquals(9, GolfCamFrameTimeline.frameIndexAt(relativeSeconds = 0.30, frameRate = 30.0))
        assertEquals(1250L, GolfCamFrameTimeline.timeMsForFrame(trimStartMs = 1000L, frameIndex = 15, frameRate = 60.0))
        assertEquals(540, GolfCamFrameTimeline.totalFrames(durationMs = 9000L, frameRate = 60.0))
        assertEquals(270, GolfCamFrameTimeline.totalFrames(durationMs = 9000L, frameRate = 30.0))
    }

    @Test
    fun trimDefaultsAndMinimumDurationMatchIosCustomTrimmer() {
        assertEquals(1f, defaultTrimEndFraction(durationMs = 20_000L), 0f)
        assertEquals(0.5f, defaultTrimEndFraction(durationMs = 60_000L), 0f)
        assertEquals(0.025f, minimumTrimFraction(durationMs = 20_000L), 1e-6f)

        assertEquals(0.475f, clampTrimStartFraction(newFraction = 0.90f, endFraction = 0.50f, durationMs = 20_000L), 1e-6f)
        assertEquals(0.525f, clampTrimEndFraction(newFraction = 0.10f, startFraction = 0.50f, durationMs = 20_000L), 1e-6f)
        assertEquals(0f, clampTrimStartFraction(newFraction = 0.90f, endFraction = 1.0f, durationMs = 300L), 0f)
        assertEquals(1f, clampTrimEndFraction(newFraction = 0.10f, startFraction = 0f, durationMs = 300L), 0f)
    }

    @Test
    fun trimStepRestoresSavedRangeBeforeFallingBackToIosDefault() {
        val restored = initialTrimFractions(
            durationMs = 60_000L,
            saved = TrimResult(startMs = 8_000L, endMs = 24_000L),
        )
        val missing = initialTrimFractions(durationMs = 60_000L, saved = null)
        val invalidReversed = initialTrimFractions(
            durationMs = 60_000L,
            saved = TrimResult(startMs = 24_000L, endMs = 8_000L),
        )
        val invalidTooShort = initialTrimFractions(
            durationMs = 60_000L,
            saved = TrimResult(startMs = 8_000L, endMs = 8_200L),
        )

        assertEquals(8_000f / 60_000f, restored.first, 1e-6f)
        assertEquals(24_000f / 60_000f, restored.second, 1e-6f)
        assertEquals(0f, missing.first, 0f)
        assertEquals(0.5f, missing.second, 0f)
        assertEquals(0f, invalidReversed.first, 0f)
        assertEquals(0.5f, invalidReversed.second, 0f)
        assertEquals(0f, invalidTooShort.first, 0f)
        assertEquals(0.5f, invalidTooShort.second, 0f)
    }

    @Test
    fun trimHandleSeekTimeUsesIosFractionOfVideoDuration() {
        assertEquals(0L, trimSeekTimeMs(durationMs = 20_000L, fraction = -0.25f))
        assertEquals(5_000L, trimSeekTimeMs(durationMs = 20_000L, fraction = 0.25f))
        assertEquals(20_000L, trimSeekTimeMs(durationMs = 20_000L, fraction = 1.25f))
        assertEquals(0L, trimSeekTimeMs(durationMs = 0L, fraction = 0.5f))
    }

    @Test
    fun trimPlayheadMatchesIosSelectedRangeRules() {
        assertEquals(0.30f, trimPlayheadFraction(20_000L, 6_000L, 0.25f, 0.75f) ?: -1f, 1e-6f)
        assertEquals(null, trimPlayheadFraction(20_000L, 4_000L, 0.25f, 0.75f))
        assertEquals(null, trimPlayheadFraction(20_000L, 16_000L, 0.25f, 0.75f))

        assertEquals(5_000L, trimPlayheadSeekTimeMs(20_000L, 0.10f, 0.25f, 0.75f))
        assertEquals(8_000L, trimPlayheadSeekTimeMs(20_000L, 0.40f, 0.25f, 0.75f))
        assertEquals(15_000L, trimPlayheadSeekTimeMs(20_000L, 0.90f, 0.25f, 0.75f))
    }

    @Test
    fun trimThumbnailsUseVisualOrientationLikeIosPreferredTrackTransform() {
        assertEquals(
            90,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 90,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1920,
                frameHeight = 1080,
            ),
        )
        assertEquals(
            0,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 90,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1080,
                frameHeight = 1920,
            ),
        )
        assertEquals(
            270,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 270,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1920,
                frameHeight = 1080,
            ),
        )
        assertEquals(
            180,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 180,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1920,
                frameHeight = 1080,
            ),
        )
    }

    @Test
    fun initialImpactFramePrefersSavedThenBestCandidateThenMiddleFrame() {
        val candidates = listOf(
            CandidateImpactFrame(frameIndex = 120, confidence = 0.95f, timeSec = 2.0),
            CandidateImpactFrame(frameIndex = 48, confidence = 0.80f, timeSec = 0.8),
        )

        assertEquals(42, initialImpactFrameIndex(savedFrameIndex = 42, candidateFrames = candidates, totalFrames = 180))
        assertEquals(120, initialImpactFrameIndex(savedFrameIndex = null, candidateFrames = candidates, totalFrames = 180))
        assertEquals(90, initialImpactFrameIndex(savedFrameIndex = null, candidateFrames = emptyList(), totalFrames = 180))
        assertEquals(179, initialImpactFrameIndex(savedFrameIndex = 999, candidateFrames = emptyList(), totalFrames = 180))
        assertEquals(0, initialImpactFrameIndex(savedFrameIndex = null, candidateFrames = candidates, totalFrames = 0))
    }

    @Test
    fun impactFrameDisplayUsesTrimRelativeFrameCountLikeIos() {
        assertEquals("1/180", impactFrameDisplayText(currentFrame = 0, totalFrames = 180))
        assertEquals("43/180", impactFrameDisplayText(currentFrame = 42, totalFrames = 180))
        assertEquals("180/180", impactFrameDisplayText(currentFrame = 999, totalFrames = 180))
        assertEquals("1/1", impactFrameDisplayText(currentFrame = 0, totalFrames = 0))
    }

    @Test
    fun asyncCandidateCanReplaceAutoMiddleFrameWhenNoSavedImpactFrameOnEntry() {
        val candidates = listOf(
            CandidateImpactFrame(frameIndex = 24, confidence = 0.98f, timeSec = 0.4),
        )
        val autoMiddleFrameAlreadyWrittenToState = 90

        assertEquals(90, autoMiddleFrameAlreadyWrittenToState)
        assertEquals(24, initialImpactFrameIndex(savedFrameIndex = null, candidateFrames = candidates, totalFrames = 180))
        assertEquals(90, initialImpactFrameIndex(savedFrameIndex = autoMiddleFrameAlreadyWrittenToState, candidateFrames = candidates, totalFrames = 180))
    }

    @Test
    fun impactCandidateDetectionCacheIsScopedToTrimRangeAndFrameRate() {
        val oldKey = ImpactCandidateDetectionKey(trimStartMs = 1_000L, trimEndMs = 6_000L, frameRate = 60.0)
        val sameKey = ImpactCandidateDetectionKey(trimStartMs = 1_000L, trimEndMs = 6_000L, frameRate = 60.0)
        val changedTrim = ImpactCandidateDetectionKey(trimStartMs = 2_000L, trimEndMs = 7_000L, frameRate = 60.0)
        val changedFrameRate = ImpactCandidateDetectionKey(trimStartMs = 1_000L, trimEndMs = 6_000L, frameRate = 30.0)
        val candidates = listOf(
            CandidateImpactFrame(frameIndex = 12, confidence = 0.90f, timeSec = 0.2),
        )

        assertTrue(shouldReuseImpactCandidates(oldKey, sameKey, candidates))
        assertFalse(shouldReuseImpactCandidates(oldKey, changedTrim, candidates))
        assertFalse(shouldReuseImpactCandidates(oldKey, changedFrameRate, candidates))
        assertFalse(shouldReuseImpactCandidates(oldKey, sameKey, emptyList()))
    }

    @Test
    fun trajectoryOverlaySamplerCoversPreviewExportAcceptanceFrames() {
        val bezier = BezierPath(
            p0 = NormalizedPoint(0.08f, 0.78f),
            p1 = NormalizedPoint(0.18f, 0.20f),
            p2 = NormalizedPoint(0.55f, 0.10f),
            p3 = NormalizedPoint(0.90f, 0.58f),
        )
        val arcTable = ArcLengthTable(bezier)
        val timing = TrajectoryTiming(impactTimeMs = 0L, landingTimeMs = 3000L, fps = 60.0)
        val style = TrackStyle(
            speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL,
            tailHalfLife = 70.0,
            showDistance = true,
            totalDistance = 150.0,
            distanceUnit = DistanceUnit.YARDS,
        )
        val sampleTimes = listOf(
            15.0 / timing.fps,
            timing.totalDurationSec * 0.5,
            timing.totalDurationSec - (1.0 / timing.fps),
        )

        val samples = sampleTimes.map { elapsed ->
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = style,
                elapsedSec = elapsed,
            ) ?: error("missing trajectory sample")
        }

        samples.zip(sampleTimes).forEach { (sample, elapsed) ->
            assertEquals(
                GolfCamSpeedConfig.progress(elapsed / timing.totalDurationSec, style.speedLevel).toFloat(),
                sample.headProgress,
                1e-6f,
            )
            assertEquals(
                GolfCamSpeedConfig.displayDistance(
                    totalDistance = style.totalDistance,
                    elapsedSec = elapsed,
                    totalDurationSec = timing.totalDurationSec,
                    speedLevel = style.speedLevel,
                    frameRate = timing.fps,
                ),
                sample.distance,
            )
            assertTrue(sample.tailProgress <= sample.headProgress)
            assertTrue(sample.headPoint.x in 0f..1f)
        }

        assertTrue(samples[0].headProgress < samples[1].headProgress)
        assertTrue(samples[1].headProgress < samples[2].headProgress)
        assertTrue((samples.last().distance ?: 0) <= style.totalDistance.toInt())
        assertEquals(
            style.totalDistance.toInt(),
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = style,
                elapsedSec = timing.totalDurationSec,
            )?.distance,
        )
    }

    @Test
    fun trajectoryOverlaySamplerUsesLegacyMotionOnlyForOldAlgorithm() {
        val bezier = BezierPath(
            p0 = NormalizedPoint(0.10f, 0.80f),
            p1 = NormalizedPoint(0.18f, 0.30f),
            p2 = NormalizedPoint(0.60f, 0.20f),
            p3 = NormalizedPoint(0.90f, 0.60f),
        )
        val arcTable = ArcLengthTable(bezier)
        val timing = TrajectoryTiming(impactTimeMs = 0L, landingTimeMs = 3000L, fps = 60.0)

        val golfCamLike = TrajectoryOverlayFrameSampler.sample(
            bezier = bezier,
            arcTable = arcTable,
            timing = timing,
            style = TrackStyle(activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE),
            elapsedSec = 1.0,
            legacyProgressAtTime = { 0.01 },
        ) ?: error("missing V5 sample")
        val legacy = TrajectoryOverlayFrameSampler.sample(
            bezier = bezier,
            arcTable = arcTable,
            timing = timing,
            style = TrackStyle(activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5),
            elapsedSec = 1.0,
            legacyProgressAtTime = { 0.25 },
        ) ?: error("missing legacy sample")

        assertEquals(GolfCamSpeedConfig.progress(1.0 / 3.0, GolfCamSpeedConfig.DEFAULT_LEVEL).toFloat(), golfCamLike.headProgress, 1e-6f)
        assertEquals(0.25f, legacy.headProgress, 1e-6f)
    }

    @Test
    fun distanceLabelRenderSpecMatchesIosOffsetAndStackedTextRules() {
        val height = 1080f
        val metrics = DistanceLabelTextMetrics(
            unitWidth = 30f,
            unitHeight = 12f,
            valueWidth = 42f,
            valueHeight = 26f,
        )
        val flyingLeft = DistanceLabelRenderSpec.layout(
            headX = 500f,
            headY = 400f,
            videoHeight = height,
            isFlyingLeft = true,
            metrics = metrics,
        )
        val flyingRight = DistanceLabelRenderSpec.layout(
            headX = 500f,
            headY = 400f,
            videoHeight = height,
            isFlyingLeft = false,
            metrics = metrics,
        )

        assertEquals(11f, DistanceLabelRenderSpec.unitFontPx(height), 0.0f)
        assertEquals(24f, DistanceLabelRenderSpec.valueFontPx(height), 0.0f)
        assertEquals(37.8f, height * DistanceLabelRenderSpec.OFFSET_RATIO, 0.001f)
        assertEquals(1f, DistanceLabelRenderSpec.shadowOffsetPx(height), 0.0f)
        assertEquals(3f, DistanceLabelRenderSpec.shadowBlurRadiusPx(height), 0.0f)
        assertEquals((500f - 37.8f) - metrics.unitWidth / 2f, flyingLeft.unitLeft, 0.001f)
        assertEquals((500f + 37.8f) - metrics.unitWidth / 2f, flyingRight.unitLeft, 0.001f)
        assertEquals(flyingLeft.unitTop + metrics.unitHeight + 2f, flyingLeft.valueTop, 0.001f)
        assertTrue(DistanceLabelRenderSpec.isFlyingLeft(currentX = 10f, previousX = 20f))
        assertFalse(DistanceLabelRenderSpec.isFlyingLeft(currentX = 20f, previousX = 10f))
    }

    @Test
    fun visibleTailStartProgressMatchesIosTailPercentRules() {
        assertEquals(
            0.0,
            GolfCamSpeedConfig.visibleTailStartProgress(
                headProgress = 0.8,
                tailHalfLife = 100.0,
                activeAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
                apexProgress = 0.6,
            ),
            1e-9,
        )
        assertEquals(
            0.20,
            GolfCamSpeedConfig.visibleTailStartProgress(
                headProgress = 0.90,
                tailHalfLife = 70.0,
                activeAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
                apexProgress = 0.6,
            ),
            1e-9,
        )
        assertEquals(
            0.60,
            GolfCamSpeedConfig.visibleTailStartProgress(
                headProgress = 0.80,
                tailHalfLife = 20.0,
                activeAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5,
                apexProgress = 0.6,
            ),
            1e-9,
        )
        assertEquals(
            0.24,
            GolfCamSpeedConfig.visibleTailStartProgress(
                headProgress = 0.40,
                tailHalfLife = 5.0,
                activeAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
                apexProgress = 0.6,
            ),
            1e-9,
        )
    }

    @Test
    fun trailStrokeWidthUsesIosHeightScaledRibbonFormula() {
        val tuning = TrackRenderTuning()
        val tailWidth1080 = GolfCamTrailGeometry.strokeWidth(
            widthPct = 40.0,
            videoHeight = 1080f,
            globalProgress = 0f,
            apexProgress = 0.6f,
            tuning = tuning,
        )
        val headWidth1080 = GolfCamTrailGeometry.strokeWidth(
            widthPct = 40.0,
            videoHeight = 1080f,
            globalProgress = 1f,
            apexProgress = 0.6f,
            tuning = tuning,
        )
        val tailWidth720 = GolfCamTrailGeometry.strokeWidth(
            widthPct = 40.0,
            videoHeight = 720f,
            globalProgress = 0f,
            apexProgress = 0.6f,
            tuning = tuning,
        )
        val apexWidth = GolfCamTrailGeometry.strokeWidth(
            widthPct = 40.0,
            videoHeight = 1080f,
            globalProgress = 0.6f,
            apexProgress = 0.6f,
            tuning = tuning,
        )
        val nearApexWidth = GolfCamTrailGeometry.strokeWidth(
            widthPct = 40.0,
            videoHeight = 1080f,
            globalProgress = 0.6f,
            apexProgress = 0.95f,
            tuning = tuning,
        )

        assertEquals(15.80f, tailWidth1080, 0.02f)
        assertEquals(2.17f, headWidth1080, 0.02f)
        assertEquals(tailWidth1080 * (720f / 1080f), tailWidth720, 0.02f)
        assertTrue(tailWidth1080 > headWidth1080)
        assertTrue(apexWidth < nearApexWidth)
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
        assertEquals(0xFF55F3D8, style.colorHex)
        assertEquals(0xFF55F3D8, style.primaryColorArgb())
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
        assertEquals(0xFF112233, parsed.defaultStyle.colorHex)
        assertEquals(0xFF112233, parsed.defaultStyle.primaryColorArgb())
        assertEquals(0.65, parsed.defaultStyle.colorAlpha, 0.0)
        assertFalse(parsed.defaultStyle.showDistance)
    }

    @Test
    fun cloudConfigParsesProductionTrackStylePayloadShape() {
        val parsed = TrackStyleCloudConfigLoader.parse(
            JsonObject(
                mapOf(
                    "version" to JsonPrimitive("1781963575066"),
                    "defaultPresetName" to JsonPrimitive("青蓝薄荷"),
                    "defaultColorHex" to JsonPrimitive("#55F3D8"),
                    "defaultOpacity" to JsonPrimitive(0.75),
                    "styleDefaults" to JsonObject(
                        mapOf(
                            "widthPct" to JsonPrimitive(40),
                            "tailHalfLife" to JsonPrimitive(70),
                            "speedLevel" to JsonPrimitive(0.45),
                            "showDistance" to JsonPrimitive(false),
                            "totalDistance" to JsonPrimitive(150),
                            "distanceUnit" to JsonPrimitive("yards"),
                        ),
                    ),
                    "renderTuning" to JsonObject(
                        mapOf(
                            "showOuterGlow" to JsonPrimitive(false),
                            "showBodyBand" to JsonPrimitive(true),
                            "showHighlightCore" to JsonPrimitive(true),
                            "bandAlphaMultiplier" to JsonPrimitive(1.0),
                            "bodyAlphaBase" to JsonPrimitive(0.10),
                            "bodyAlphaScale" to JsonPrimitive(0.98),
                            "glowBlurMultiplier" to JsonPrimitive(0.82),
                            "outerGlowMultiplier" to JsonPrimitive(0.62),
                            "fillGlowMultiplier" to JsonPrimitive(0.22),
                            "highlightWidthRatio" to JsonPrimitive(0.50),
                            "highlightAlphaMax" to JsonPrimitive(0.90),
                            "highlightWhiteLiftBase" to JsonPrimitive(0.00),
                            "highlightWhiteLiftScale" to JsonPrimitive(0.00),
                            "highlightStart" to JsonPrimitive(0.00),
                            "highlightEnd" to JsonPrimitive(0.96),
                            "highlightPeak" to JsonPrimitive(0.35),
                            "highlightStrength" to JsonPrimitive(1.6),
                            "widthTaperPower" to JsonPrimitive(1.36),
                            "apexThinStrength" to JsonPrimitive(0.60),
                            "apexThinRadius" to JsonPrimitive(0.20),
                            "highlightFalloffPower" to JsonPrimitive(0.82),
                        ),
                    ),
                    "presets" to JsonArray(
                        listOf(
                            preset("玫瑰荧光", "#FF3F91", "#FF5FA8", "#FF8EC3", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("青蓝薄荷", "#35DFC7", "#55F3D8", "#8FFFF0", glowAlpha = 0.70, bandAlpha = 1.0),
                        ),
                    ),
                ),
            ),
        )
        val style = parsed.defaultStyle

        assertEquals(2, parsed.presets.size)
        assertEquals("青蓝薄荷", style.colorPreset.name)
        assertEquals(0xFF55F3D8, parsed.defaultColorHex)
        assertEquals(0xFF55F3D8, style.colorHex)
        assertEquals(0.75, style.colorAlpha, 0.0)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(70.0, style.tailHalfLife, 0.0)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL, style.speedLevel, 0.0)
        assertFalse(style.showDistance)
        assertEquals(150.0, style.totalDistance, 0.0)
        assertEquals(DistanceUnit.YARDS, style.distanceUnit)
        assertTrue(style.renderTuning.showBodyBand)
        assertTrue(style.renderTuning.showHighlightCore)
        assertEquals(0.82f, style.renderTuning.glowBlurMultiplier, 0.0f)
        assertEquals(0.20f, style.renderTuning.apexThinRadius, 0.0f)
    }

    @Test
    fun localTrackStyleFallbackMirrorsCloudProductionDefaults() {
        val fallback = TrackStyleCloudConfigLoader.localFallback()
        val style = fallback.defaultStyle

        assertEquals(16, fallback.presets.size)
        assertEquals(0xFF55F3D8, fallback.defaultColorHex)
        assertEquals("青蓝薄荷", style.colorPreset.name)
        assertEquals(0xFF55F3D8, style.colorHex)
        assertEquals(0.75, style.colorAlpha, 0.0)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL, style.speedLevel, 0.0)
        assertEquals(70.0, style.tailHalfLife, 0.0)
        assertFalse(style.showDistance)
        assertEquals(150.0, style.totalDistance, 0.0)
        assertEquals(DistanceUnit.YARDS, style.distanceUnit)
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
