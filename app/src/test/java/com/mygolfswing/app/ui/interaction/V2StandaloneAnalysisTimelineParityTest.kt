package com.mygolfswing.app.ui.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.mygolfswing.app.ui.player.StandaloneTimelineScrubFeel
import com.mygolfswing.app.ui.player.StandaloneTimelineScrubTuning
import com.mygolfswing.app.ui.player.standaloneTimelineEffectiveDpPerSecond
import com.mygolfswing.app.ui.player.standaloneTimelineInertiaFactor
import com.mygolfswing.app.ui.player.standaloneTimelineTimeAfterDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V2StandaloneAnalysisTimelineParityTest {
    @Test
    fun iosScrubFeelPresetsKeepTheirExactSensitivityEndpoints() {
        val expected = mapOf(
            StandaloneTimelineScrubFeel.BALANCED to (380.0 to 160.0),
            StandaloneTimelineScrubFeel.PRECISE to (520.0 to 260.0),
            StandaloneTimelineScrubFeel.FRAME to (760.0 to 420.0),
        )

        expected.forEach { (feel, endpoints) ->
            val tuning = StandaloneTimelineScrubTuning.recommended(feel)
            assertEquals(endpoints.first, standaloneTimelineEffectiveDpPerSecond(tuning, 0.0), 0.001)
            assertEquals(
                endpoints.second,
                standaloneTimelineEffectiveDpPerSecond(tuning, tuning.flickVelocityThreshold),
                0.001,
            )
            assertEquals(
                tuning.normalInertiaFactor,
                standaloneTimelineInertiaFactor(tuning, 0.0),
                0.001,
            )
            assertEquals(
                tuning.flickInertiaFactor,
                standaloneTimelineInertiaFactor(tuning, tuning.flickVelocityThreshold),
                0.001,
            )
        }
    }

    @Test
    fun timelineDragDirectionAndBoundsMatchIosScrollSemantics() {
        assertEquals(
            2_000L,
            standaloneTimelineTimeAfterDelta(
                currentMs = 1_000L,
                contentDeltaPx = 160f,
                effectivePixelsPerSecond = 160f,
                durationMs = 10_000L,
            ),
        )
        assertEquals(
            0L,
            standaloneTimelineTimeAfterDelta(
                currentMs = 400L,
                contentDeltaPx = -160f,
                effectivePixelsPerSecond = 160f,
                durationMs = 10_000L,
            ),
        )
        assertEquals(
            10_000L,
            standaloneTimelineTimeAfterDelta(
                currentMs = 9_800L,
                contentDeltaPx = 160f,
                effectivePixelsPerSecond = 160f,
                durationMs = 10_000L,
            ),
        )
    }

    @Test
    fun preciseTimeBubbleUsesIosHundredthSecondFormat() {
        assertEquals("00.00", formatStandaloneAnalysisPreciseTime(0L))
        assertEquals("02.50", formatStandaloneAnalysisPreciseTime(2_500L))
        assertEquals("62.35", formatStandaloneAnalysisPreciseTime(62_345L))
        assertTrue(formatStandaloneAnalysisPreciseTime(-1L).startsWith("00.00"))
    }

    @Test
    fun selfiePreviewUsesTheSameIosSafeDragBounds() {
        val base = Offset(82f, 508f)
        val container = Size(400f, 800f)
        val common = { offset: Offset ->
            standaloneSelfieClampedPosition(
                basePosition = base,
                proposedOffset = offset,
                containerSize = container,
                halfPreviewWidth = 63f,
                halfPreviewHeight = 89f,
                horizontalMargin = 8f,
                topMargin = 58f,
                bottomMargin = 138f,
            )
        }

        assertEquals(base, common(Offset.Zero))
        assertEquals(Offset(329f, 573f), common(Offset(1_000f, 1_000f)))
        assertEquals(Offset(71f, 147f), common(Offset(-1_000f, -1_000f)))
    }
}
