package com.mygolfswing.app.ui.golfcam.model

import android.media.MediaCodec
import android.media.MediaExtractor
import androidx.compose.ui.graphics.StrokeCap
import com.mygolfswing.app.ui.golfcam.ImpactCandidateDetectionKey
import com.mygolfswing.app.ui.golfcam.ExportResolution
import com.mygolfswing.app.ui.golfcam.ExportTrimRange
import com.mygolfswing.app.ui.golfcam.EXPORT_SETTINGS_4K_SOURCE_REQUIRED_REASON
import com.mygolfswing.app.ui.golfcam.GOLF_CAM_IMPORT_DETECT_MESSAGE
import com.mygolfswing.app.ui.golfcam.GOLF_CAM_IMPORT_GENERATE_PREVIEW_MESSAGE
import com.mygolfswing.app.ui.golfcam.GOLF_CAM_IMPORT_RESTORE_MESSAGE
import com.mygolfswing.app.ui.golfcam.GOLF_CAM_IMPORT_VIDEO_CONFIG_ERROR_MESSAGE
import com.mygolfswing.app.ui.golfcam.deleteExportPreviewTempFile
import com.mygolfswing.app.ui.golfcam.IMPACT_DRAG_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.IMPACT_MARKER_SIZE_DP
import com.mygolfswing.app.ui.golfcam.LANDING_DRAG_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.LANDING_MARKER_SIZE_DP
import com.mygolfswing.app.ui.golfcam.PREVIEW_BOTTOM_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.PREVIEW_VIDEO_CARD_BORDER_ALPHA
import com.mygolfswing.app.ui.golfcam.PREVIEW_VIDEO_CARD_BORDER_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.PREVIEW_VIDEO_CARD_SHADOW_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.PreviewPanelAction
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_BACKGROUND_ALPHA
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_CARD_CORNER_DP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_CONTENT_SPACING_DP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_TEXT_FONT_SP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_SUBTITLE_FONT_SP
import com.mygolfswing.app.ui.golfcam.THUMBNAIL_PRELOAD_OVERLAY_TITLE_FONT_SP
import com.mygolfswing.app.ui.golfcam.AudioSampleCopyAction
import com.mygolfswing.app.ui.golfcam.audioCandidateBaseTimeSeconds
import com.mygolfswing.app.ui.golfcam.audioCopyBufferSize
import com.mygolfswing.app.ui.golfcam.audioMuxerPresentationTimeUs
import com.mygolfswing.app.ui.golfcam.audioMuxerSampleFlags
import com.mygolfswing.app.ui.golfcam.audioSampleCopyAction
import com.mygolfswing.app.ui.golfcam.audioSamplePresentationTimeUs
import com.mygolfswing.app.ui.golfcam.coerceExportResolutionForSource
import com.mygolfswing.app.ui.golfcam.coerceCurrentWorkflowStep
import com.mygolfswing.app.ui.golfcam.effectiveExportFrameRotation
import com.mygolfswing.app.ui.golfcam.estimateFrameRateFromPresentationTimes
import com.mygolfswing.app.ui.golfcam.exportTrimRange
import com.mygolfswing.app.ui.golfcam.exportTrimRangeUs
import com.mygolfswing.app.ui.golfcam.exportSaveSuccessDisposition
import com.mygolfswing.app.ui.golfcam.exportTrajectoryElapsedSeconds
import com.mygolfswing.app.ui.golfcam.exportTrajectoryElapsedSecondsFromUs
import com.mygolfswing.app.ui.golfcam.exportFailureMessage
import com.mygolfswing.app.ui.golfcam.exportFramePresentationTimeUs
import com.mygolfswing.app.ui.golfcam.exportMillisecondsToMicroseconds
import com.mygolfswing.app.ui.golfcam.exportPreviewSaveButtonAlpha
import com.mygolfswing.app.ui.golfcam.exportProgressFraction
import com.mygolfswing.app.ui.golfcam.exportSaveFailureMessage
import com.mygolfswing.app.ui.golfcam.exportSettingsUiState
import com.mygolfswing.app.ui.golfcam.exportSourceFrameTimeUs
import com.mygolfswing.app.ui.golfcam.exportTotalFrameCount
import com.mygolfswing.app.ui.golfcam.exportVideoDimensions
import com.mygolfswing.app.ui.golfcam.formatPreviewTime
import com.mygolfswing.app.ui.golfcam.golfCamVideoConfigFromMetadata
import com.mygolfswing.app.ui.golfcam.golfCamContainerThumbnailPreloadDurationMs
import com.mygolfswing.app.ui.golfcam.golfCamThumbnailPreloadDurationMs
import com.mygolfswing.app.ui.golfcam.isValidExportPreviewFile
import com.mygolfswing.app.ui.golfcam.canOpenWorkflowStep
import com.mygolfswing.app.ui.golfcam.curveFrameSeekTimeMs
import com.mygolfswing.app.ui.golfcam.hasCompletedWorkflowStep
import com.mygolfswing.app.ui.golfcam.impactCandidatesForTrimRange
import com.mygolfswing.app.ui.golfcam.isAudioMimeType
import com.mygolfswing.app.ui.golfcam.mediaStorePendingPublishSucceeded
import com.mygolfswing.app.ui.golfcam.metadataIntOrDefault
import com.mygolfswing.app.ui.golfcam.metadataLongOrDefault
import com.mygolfswing.app.ui.golfcam.clampedCurveControlPoint
import com.mygolfswing.app.ui.golfcam.normalizedLandingCoordinate
import com.mygolfswing.app.ui.golfcam.normalizedCoordinateOrCenter
import com.mygolfswing.app.ui.golfcam.normalizedVideoCoordinate
import com.mygolfswing.app.ui.golfcam.previewImpactTimeMs
import com.mygolfswing.app.ui.golfcam.previewPlaybackClip
import com.mygolfswing.app.ui.golfcam.previewPanelActionStates
import com.mygolfswing.app.ui.golfcam.previewProgressDisplayFraction
import com.mygolfswing.app.ui.golfcam.previewProgressFraction
import com.mygolfswing.app.ui.golfcam.previewTrajectoryTiming
import com.mygolfswing.app.ui.golfcam.resolveExportVideoTarget
import com.mygolfswing.app.ui.golfcam.shouldReuseImpactCandidates
import com.mygolfswing.app.ui.golfcam.shouldClearImpactCandidatesForNewDetection
import com.mygolfswing.app.ui.golfcam.shouldStartTrimImpactThumbnailPreloadGate
import com.mygolfswing.app.ui.golfcam.shouldDetectGolfCamVideoConfig
import com.mygolfswing.app.ui.golfcam.shouldOpenExportPreview
import com.mygolfswing.app.ui.golfcam.shouldAllowExportSave
import com.mygolfswing.app.ui.golfcam.thumbnailPreloadProgressPercentText
import com.mygolfswing.app.ui.golfcam.shouldAllowExportPreviewDismiss
import com.mygolfswing.app.ui.golfcam.shouldCleanupFailedGalleryInsert
import com.mygolfswing.app.ui.golfcam.sourceSupportsExportResolution
import com.mygolfswing.app.ui.golfcam.shouldKeepDecodedAudioSample
import com.mygolfswing.app.ui.golfcam.targetExportVideoBitrate
import com.mygolfswing.app.ui.golfcam.targetExportVideoDimensions
import com.mygolfswing.app.ui.golfcam.trimmedAudioPresentationTimeUs
import com.mygolfswing.app.ui.golfcam.trustedPreviewVideoConfigOrUnknown
import com.mygolfswing.app.ui.golfcam.unifiedPreviewElapsedMs
import com.mygolfswing.app.ui.golfcam.videoConfigWithPlayerTiming
import com.mygolfswing.app.ui.golfcam.visibleCurveHitCenter
import com.mygolfswing.app.ui.golfcam.normalizedVideoRotation
import com.mygolfswing.app.ui.golfcam.normalizedPreviewExportSelection
import com.mygolfswing.app.ui.golfcam.previewStateFromWorkflow
import com.mygolfswing.app.ui.golfcam.visualVideoDimensions
import com.mygolfswing.app.ui.golfcam.steps.clampTrimEndFraction
import com.mygolfswing.app.ui.golfcam.steps.clampTrimStartFraction
import com.mygolfswing.app.ui.golfcam.steps.defaultTrimEndFraction
import com.mygolfswing.app.ui.golfcam.steps.formatImpactPositionPercent
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_COORD_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_COORD_VALUE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_HINT_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_HINT_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_HINT_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_TOOL_MARK_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_TOOL_MARK_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_TOOL_TITLE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_TOOL_VERTICAL_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.formatLandingPositionLabel
import com.mygolfswing.app.ui.golfcam.steps.formatLandingTimeLabel
import com.mygolfswing.app.ui.golfcam.steps.commitTrackStyleDistanceInput
import com.mygolfswing.app.ui.golfcam.steps.formatTrackStyleInteger
import com.mygolfswing.app.ui.golfcam.steps.formatTrackStyleOpacityPercent
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_HORIZONTAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_TAB_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CATEGORY_TOP_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PICKER_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PRESET_CHECK_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PRESET_OUTER_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PRESET_SELECTED_STROKE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PRESET_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_PRESET_SWATCH_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_COLOR_ROW_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CONTENT_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_CONFIRM_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_INPUT_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_INPUT_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_INPUT_HORIZONTAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_INPUT_VERTICAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_INPUT_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_UNIT_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_UNIT_TOGGLE_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_UNIT_TOGGLE_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_UNIT_TOGGLE_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_UNIT_TOGGLE_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_DISTANCE_VERTICAL_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_SLIDER_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_SLIDER_VERTICAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.formatTrimTimeLabel
import com.mygolfswing.app.ui.golfcam.steps.initialTrimFractions
import com.mygolfswing.app.ui.golfcam.steps.trackStyleSliderValue
import com.mygolfswing.app.ui.golfcam.steps.trackStyleDistanceVisibilityChange
import com.mygolfswing.app.ui.golfcam.steps.visibleTrackStyleCategoryLabels
import com.mygolfswing.app.ui.golfcam.steps.formatImpactFrameTimeLabel
import com.mygolfswing.app.ui.golfcam.steps.GOLF_CAM_THUMBNAIL_CACHE_COUNT
import com.mygolfswing.app.ui.golfcam.steps.GOLF_CAM_THUMBNAIL_PRELOAD_COUNT
import com.mygolfswing.app.ui.golfcam.steps.GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT
import com.mygolfswing.app.ui.golfcam.steps.golfCamFillMissingThumbnailSlotsFromNearest
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailLoadOrder
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailPriorityIndex
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailPrioritySampleTimeMs
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailRetrieverTimeUs
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailSampleTimesMs
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailTimeDistanceMs
import com.mygolfswing.app.ui.golfcam.steps.golfCamTrimImpactPreloadProgressFraction
import com.mygolfswing.app.ui.golfcam.steps.golfCamTrimImpactPreloadSampleTimesMs
import com.mygolfswing.app.ui.golfcam.steps.golfCamVideoPreloadSampleTimesMs
import com.mygolfswing.app.ui.golfcam.steps.isVideoThumbnailPreloadActiveForCurrentVideo
import com.mygolfswing.app.ui.golfcam.steps.nextThumbnailVideoGeneration
import com.mygolfswing.app.ui.golfcam.steps.shouldClearActiveVideoThumbnailPreload
import com.mygolfswing.app.ui.golfcam.steps.shouldDropStaleVideoThumbnailPreload
import com.mygolfswing.app.ui.golfcam.steps.shouldEmitCachedThumbnailCallback
import com.mygolfswing.app.ui.golfcam.steps.shouldContinueThumbnailLoadForVideo
import com.mygolfswing.app.ui.golfcam.steps.shouldStartVideoThumbnailPreload
import com.mygolfswing.app.ui.golfcam.steps.shouldWaitForTrimImpactThumbnailPreload
import com.mygolfswing.app.ui.golfcam.steps.golfCamFrameIndexDistance
import com.mygolfswing.app.ui.golfcam.steps.initialImpactFrameIndex
import com.mygolfswing.app.ui.golfcam.steps.asyncImpactCandidateReplacementFrame
import com.mygolfswing.app.ui.golfcam.steps.impactFrameSelectionForFrame
import com.mygolfswing.app.ui.golfcam.steps.impactFrameThumbnailFrameIndices
import com.mygolfswing.app.ui.golfcam.steps.impactFrameThumbnailSampleTimeMs
import com.mygolfswing.app.ui.golfcam.steps.impactFrameDisplayText
import com.mygolfswing.app.ui.golfcam.steps.impactFrameStepFrame
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_CANDIDATE_DOT_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_CANDIDATE_DOT_TOP_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_NAV_BUTTON_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_NAV_BUTTON_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_CELL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_CELL_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_GAP_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_SELECTED_BORDER_DP
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_SELECTED_SCALE
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_THUMBNAIL_UNSELECTED_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_TIMELINE_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.nearestImpactFrameThumbnailIndex
import com.mygolfswing.app.ui.golfcam.steps.shouldUpdateImpactFrameSelection
import com.mygolfswing.app.ui.golfcam.steps.validImpactCandidateFramesForTrimRange
import com.mygolfswing.app.ui.golfcam.steps.isLandingVirtualSegment
import com.mygolfswing.app.ui.golfcam.steps.landingFlightTimeSeconds
import com.mygolfswing.app.ui.golfcam.steps.landingOutputForPositionChange
import com.mygolfswing.app.ui.golfcam.steps.landingOutputForTimeChange
import com.mygolfswing.app.ui.golfcam.steps.landingSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.landingTailSeconds
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TIMELINE_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TIMELINE_THUMB_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TIMELINE_PIXELS_PER_SECOND_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TIMELINE_TRACK_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_CENTER_INDICATOR_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_CENTER_INDICATOR_BAR_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_CENTER_INDICATOR_DOT_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_CENTER_INDICATOR_VERTICAL_INSET_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_HINT_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_HINT_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_HINT_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_MIN_TIME_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_MIN_TIME_MARKER_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_THUMB_COUNT
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_MARK_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_MARK_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_TITLE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_VALUE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_TOOL_VERTICAL_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_BADGE_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_BADGE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_BADGE_HORIZONTAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_BADGE_VERTICAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_OVERLAY_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.LANDING_VIRTUAL_THUMBNAIL_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.minimumTrimFraction
import com.mygolfswing.app.ui.golfcam.steps.TRIM_BAR_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_DIM_OVERLAY_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.TRIM_HANDLE_TOUCH_EXTRA_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_HANDLE_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_SAFE_MARGIN_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_SELECTION_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_SELECTION_BORDER_STROKE_PX
import com.mygolfswing.app.ui.golfcam.steps.TRIM_SELECTION_FILL_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.TRIM_STRIP_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.TRIM_THUMBNAIL_COUNT
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_HIT_TARGET_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_BOTTOM_ROW_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_DOT_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_DOT_STROKE_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_READOUT_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_VALUE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_FRAME_BUTTON_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_FRAME_BUTTON_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_FRAME_BUTTON_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_HINT_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_HINT_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_IMPACT_CHIP_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_IMPACT_CHIP_HORIZONTAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_IMPACT_CHIP_ICON_SIZE_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_IMPACT_CHIP_LABEL_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_IMPACT_CHIP_VERTICAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_INFO_TIME_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_INFO_TITLE_FONT_SP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_CELL_WIDTH_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_CELL_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_IMAGE_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_IMAGE_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_SELECTION_CORNER_RADIUS_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_SELECTION_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TIMELINE_UNSELECTED_ALPHA
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TOOL_VERTICAL_PADDING_DP
import com.mygolfswing.app.ui.golfcam.steps.CURVE_TOOL_VERTICAL_SPACING_DP
import com.mygolfswing.app.ui.golfcam.steps.trimThumbnailSampleTimesUs
import com.mygolfswing.app.ui.golfcam.steps.trimFractionOrDefault
import com.mygolfswing.app.ui.golfcam.steps.trimPlayheadFraction
import com.mygolfswing.app.ui.golfcam.steps.trimPlayheadSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.trimResultFromFractions
import com.mygolfswing.app.ui.golfcam.steps.trimSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.toggleTrackStyleDistanceUnit
import com.mygolfswing.app.ui.golfcam.steps.curveThumbnailFrameIndices
import com.mygolfswing.app.ui.golfcam.steps.curveThumbnailSampleTimeMs
import com.mygolfswing.app.ui.golfcam.steps.curveTimelineFrameIndex
import com.mygolfswing.app.ui.golfcam.steps.formatCurveControlPointLabel
import com.mygolfswing.app.ui.golfcam.steps.formatCurveTimeLabel
import com.mygolfswing.app.ui.golfcam.steps.flightCurveReadoutControlPoints
import com.mygolfswing.app.ui.golfcam.steps.nearestCurveThumbnailIndex
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
import java.io.File
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
    fun flightCurveToolReadoutUsesIosSuggestedDefaultsBeforeCurveStateExists() {
        val state = GolfCamWorkflowState().apply {
            impactPosition = ImpactPositionSelection(normalizedX = 0.10f, normalizedY = 0.80f)
            landing = LandingOutput(normalizedX = 0.90f, normalizedY = 0.62f)
        }
        val expected = GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(0.10f, 0.80f),
            end = NormalizedPoint(0.90f, 0.62f),
        )

        assertEquals(expected, flightCurveReadoutControlPoints(state))
    }

    @Test
    fun flightCurveToolReadoutPrefersSavedCurveControls() {
        val savedP1 = NormalizedPoint(0.31f, -0.12f)
        val savedP2 = NormalizedPoint(0.62f, 1.20f)
        val state = GolfCamWorkflowState().apply {
            impactPosition = ImpactPositionSelection(normalizedX = 0.10f, normalizedY = 0.80f)
            landing = LandingOutput(normalizedX = 0.90f, normalizedY = 0.62f)
            flightCurve = FlightCurveSelection(
                impactPosition = impactPosition!!,
                landingPoint = landing!!,
                controlPoint1 = savedP1,
                controlPoint2 = savedP2,
            )
        }

        assertEquals(savedP1 to savedP2, flightCurveReadoutControlPoints(state))
    }

    @Test
    fun flightCurveClampAndStrokeMatchIosGolfCamCurveStrokeSpec() {
        val clamped = GolfCamCurveSpec.clampPoint(NormalizedPoint(-1f, -2f))
        val malformedSuggestedClamp = GolfCamCurveSpec.clampPoint(NormalizedPoint(Float.NaN, Float.POSITIVE_INFINITY))
        val malformedDraggedClamp = GolfCamCurveSpec.clampDraggedControlPoint(NormalizedPoint(Float.NaN, Float.NEGATIVE_INFINITY))
        val malformedSuggested = GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(Float.NaN, Float.POSITIVE_INFINITY),
            end = NormalizedPoint(Float.NEGATIVE_INFINITY, Float.NaN),
        )
        val last = GolfCamCurveSpec.SEGMENTS - 1
        val startColor = GolfCamCurveSpec.segmentColor(0)
        val endColor = GolfCamCurveSpec.segmentColor(last)
        val beforeStartColor = GolfCamCurveSpec.segmentColor(-8)
        val afterEndColor = GolfCamCurveSpec.segmentColor(last + 80)

        assertEquals(0.02f, clamped.x, 0f)
        assertEquals(-0.5f, clamped.y, 0f)
        assertEquals(0.5f, malformedSuggestedClamp.x, 0f)
        assertEquals(0f, malformedSuggestedClamp.y, 0f)
        assertEquals(0.5f, malformedDraggedClamp.x, 0f)
        assertEquals(0f, malformedDraggedClamp.y, 0f)
        assertTrue(malformedSuggested.first.x.isFinite())
        assertTrue(malformedSuggested.first.y.isFinite())
        assertTrue(malformedSuggested.second.x.isFinite())
        assertTrue(malformedSuggested.second.y.isFinite())
        assertEquals(180, GolfCamCurveSpec.SEGMENTS)
        assertEquals(StrokeCap.Butt, GolfCamCurveSpec.STROKE_CAP)
        assertEquals(0.14f, GolfCamCurveSpec.SHADOW_ALPHA, 0f)
        assertEquals(1.1f, GolfCamCurveSpec.SHADOW_WIDTH_ADD, 0f)
        assertEquals(6.50f, GolfCamCurveSpec.foregroundWidth(0), 0.001f)
        assertEquals(1.15f, GolfCamCurveSpec.foregroundWidth(last), 0.001f)
        assertEquals(GolfCamCurveSpec.foregroundWidth(0), GolfCamCurveSpec.foregroundWidth(-8), 0.001f)
        assertEquals(GolfCamCurveSpec.foregroundWidth(last), GolfCamCurveSpec.foregroundWidth(last + 80), 0.001f)
        assertTrue(GolfCamCurveSpec.foregroundWidth(0) > GolfCamCurveSpec.foregroundWidth(last))
        assertEquals(0.08f, startColor.red, 0.003f)
        assertEquals(0.23f, startColor.green, 0.003f)
        assertEquals(0.18f, startColor.blue, 0.003f)
        assertEquals(0.24f, startColor.alpha, 0.003f)
        assertEquals(startColor.red, beforeStartColor.red, 0.003f)
        assertEquals(startColor.green, beforeStartColor.green, 0.003f)
        assertEquals(startColor.blue, beforeStartColor.blue, 0.003f)
        assertEquals(startColor.alpha, beforeStartColor.alpha, 0.003f)
        assertEquals(0.55f, endColor.red, 0.003f)
        assertEquals(0.96f, endColor.green, 0.003f)
        assertEquals(0.82f, endColor.blue, 0.003f)
        assertEquals(0.96f, endColor.alpha, 0.003f)
        assertEquals(endColor.red, afterEndColor.red, 0.003f)
        assertEquals(endColor.green, afterEndColor.green, 0.003f)
        assertEquals(endColor.blue, afterEndColor.blue, 0.003f)
        assertEquals(endColor.alpha, afterEndColor.alpha, 0.003f)
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
        assertEquals("青蓝薄荷", style.colorPreset.name)
        assertEquals("青蓝薄荷", parsedMissingDefaults.colorPreset.name)
        assertEquals(0xFF55F3D8, style.primaryColorArgb())
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
            0.7f,
            TrajectoryRibbonRenderer.bodyBandAlpha(
                style.copy(colorAlpha = 1.0, renderTuning = TrackRenderTuning(bandAlphaMultiplier = 3f)),
            ),
            1e-6f,
        )
        assertEquals(0.50f, TrajectoryRibbonRenderer.highlightWidthRatio(TrackRenderTuning(highlightWidthRatio = 0.50f)), 0.0f)
        assertEquals(0.30f, TrajectoryRibbonRenderer.highlightWidthRatio(TrackRenderTuning(highlightWidthRatio = 0.30f)), 0.0f)
        assertEquals(0.85f, TrajectoryRibbonRenderer.highlightWidthRatio(TrackRenderTuning(highlightWidthRatio = 0.85f)), 0.0f)
        assertEquals(0.0f, TrajectoryRibbonRenderer.bodyBandAlpha(style.copy(colorAlpha = Double.NaN)), 0.0f)
        assertEquals(
            1.0f,
            TrajectoryRibbonRenderer.bodyBandAlpha(
                style.copy(colorPreset = preset.copy(bandAlpha = Float.NaN)),
            ),
            0.0f,
        )
        assertEquals(
            0.5f,
            TrajectoryRibbonRenderer.highlightWidthRatio(TrackRenderTuning(highlightWidthRatio = Float.NaN)),
            0.0f,
        )
    }

    @Test
    fun sharedRendererGeometryRejectsNonFinitePreviewExportInputs() {
        val tuning = TrackRenderTuning(
            widthTaperPower = Float.NaN,
            apexThinRadius = Float.NaN,
            apexThinStrength = Float.NaN,
        )
        val headGlow = TrajectoryOverlayRenderSpec.headGlowAlphas(Float.NaN)

        val malformedWidth = GolfCamTrailGeometry.strokeWidth(
            widthPct = Double.NaN,
            videoHeight = Float.NaN,
            globalProgress = Float.NaN,
            apexProgress = Float.POSITIVE_INFINITY,
            tuning = tuning,
        )
        val lowOpacityGlow = TrajectoryOverlayRenderSpec.headGlowAlphas(-0.5f)
        val highOpacityGlow = TrajectoryOverlayRenderSpec.headGlowAlphas(Float.POSITIVE_INFINITY)

        assertTrue(malformedWidth.isFinite())
        assertTrue(malformedWidth >= 0.10f)
        assertEquals(0.0f, headGlow.outer, 0.0f)
        assertEquals(0.0f, headGlow.body, 0.0f)
        assertEquals(0.0f, headGlow.core, 0.0f)
        assertEquals(0.0f, lowOpacityGlow.outer, 0.0f)
        assertEquals(0.0f, lowOpacityGlow.body, 0.0f)
        assertEquals(0.0f, lowOpacityGlow.core, 0.0f)
        assertEquals(0.0f, highOpacityGlow.outer, 0.0f)
        assertEquals(0.0f, highOpacityGlow.body, 0.0f)
        assertEquals(0.0f, highOpacityGlow.core, 0.0f)
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
    fun bezierGeometryRejectsNonFiniteDirectPreviewExportInputs() {
        val malformedBezier = BezierPath(
            p0 = NormalizedPoint(Float.NaN, Float.POSITIVE_INFINITY),
            p1 = NormalizedPoint(0.20f, -1.0f),
            p2 = NormalizedPoint(Float.NEGATIVE_INFINITY, 1.5f),
            p3 = NormalizedPoint(0.95f, Float.NaN),
        )
        val arcTable = ArcLengthTable(malformedBezier)
        val zeroSampleArcTable = ArcLengthTable(malformedBezier, sampleCount = 0)
        val negativeSampleArcTable = ArcLengthTable(malformedBezier, sampleCount = -8)
        val directPoint = malformedBezier.point(Float.NaN)
        val headPoint = malformedBezier.pointAtArcProgress(Float.POSITIVE_INFINITY, arcTable)
        val offset = NormalizedPoint(Float.NaN, Float.POSITIVE_INFINITY).toOffset(Float.NaN, Float.NEGATIVE_INFINITY)

        assertTrue(arcTable.totalLength.isFinite())
        assertTrue(arcTable.uAtArcLength(Float.NaN).isFinite())
        assertTrue(arcTable.arcLengthAtU(Float.NaN).isFinite())
        assertTrue(arcTable.apexProgress().isFinite())
        assertTrue(zeroSampleArcTable.totalLength.isFinite())
        assertTrue(zeroSampleArcTable.uAtArcLength(Float.NaN).isFinite())
        assertTrue(negativeSampleArcTable.totalLength.isFinite())
        assertTrue(negativeSampleArcTable.arcLengthAtU(Float.POSITIVE_INFINITY).isFinite())
        assertTrue(directPoint.x.isFinite())
        assertTrue(directPoint.y.isFinite())
        assertTrue(headPoint.x.isFinite())
        assertTrue(headPoint.y.isFinite())
        assertTrue(offset.x.isFinite())
        assertTrue(offset.y.isFinite())
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
    fun speedAndDistanceSamplingRejectNonFiniteInputsBeforePreviewExport() {
        assertEquals(0.0, GolfCamSpeedConfig.progress(Double.NaN, GolfCamSpeedConfig.DEFAULT_LEVEL), 0.0)
        assertEquals(
            GolfCamSpeedConfig.DEFAULT_LEVEL,
            GolfCamSpeedConfig.clamp(Double.POSITIVE_INFINITY),
            0.0,
        )
        assertEquals(
            0.0,
            GolfCamSpeedConfig.displayProgress(
                elapsedSec = Double.NaN,
                totalDurationSec = Double.NaN,
                speedLevel = Double.NaN,
                frameRate = Double.NaN,
            ),
            0.0,
        )
        assertEquals(
            GolfCamDistanceConfig.DEFAULT_DISTANCE.toInt(),
            GolfCamSpeedConfig.displayDistance(
                totalDistance = Double.NaN,
                elapsedSec = 10.0,
                totalDurationSec = Double.NaN,
                speedLevel = Double.NaN,
                frameRate = Double.NaN,
            ),
        )
        assertEquals(
            0.0,
            GolfCamSpeedConfig.visibleTailStartProgress(
                headProgress = Double.NaN,
                tailHalfLife = Double.NaN,
                activeAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
                apexProgress = Double.NaN,
            ),
            0.0,
        )
    }

    @Test
    fun distanceConfigMatchesIosCloudAndUiRange() {
        assertEquals(10.0, GolfCamDistanceConfig.clamp(-10.0), 0.0)
        assertEquals(10.0, GolfCamDistanceConfig.clamp(1.0), 0.0)
        assertEquals(1000.0, GolfCamDistanceConfig.clamp(1200.0), 0.0)
        assertEquals(GolfCamDistanceConfig.DEFAULT_DISTANCE, GolfCamDistanceConfig.clamp(Double.NaN), 0.0)
        assertEquals("1", GolfCamDistanceConfig.inputText("1"))
        assertEquals("10000", GolfCamDistanceConfig.inputText("10000"))
        assertEquals("150", GolfCamDistanceConfig.inputText("150yd"))
        assertEquals("", GolfCamDistanceConfig.inputText(""))
        assertEquals("1000", GolfCamDistanceConfig.displayText(1000.9))
        assertEquals(91.44, GolfCamDistanceConfig.convert(100.0, DistanceUnit.YARDS, DistanceUnit.METERS), 1e-6)
        assertEquals(1000.0, GolfCamDistanceConfig.convert(1000.0, DistanceUnit.METERS, DistanceUnit.YARDS), 0.0)
        assertEquals(DistanceUnit.YARDS, parseGolfCamDistanceUnit("yards"))
        assertEquals(DistanceUnit.YARDS, parseGolfCamDistanceUnit("YARDS"))
        assertEquals(DistanceUnit.METERS, parseGolfCamDistanceUnit("meters"))
        assertEquals(DistanceUnit.METERS, parseGolfCamDistanceUnit("METERS"))
        assertEquals(DistanceUnit.YARDS, parseGolfCamDistanceUnit("unknown"))
    }

    @Test
    fun trackStyleDistanceInputClampsOnlyOnCommitOrUnitToggle() {
        val style = TrackStyle(
            showDistance = true,
            totalDistance = 150.0,
            distanceUnit = DistanceUnit.YARDS,
        )

        assertEquals("1", GolfCamDistanceConfig.inputText("1"))

        val committedSmall = commitTrackStyleDistanceInput(style, rawText = "1")
        assertEquals(10.0, committedSmall.style.totalDistance, 0.0)
        assertEquals(DistanceUnit.YARDS, committedSmall.style.distanceUnit)
        assertEquals("10", committedSmall.displayText)

        val committedEmpty = commitTrackStyleDistanceInput(style, rawText = "")
        assertEquals(150.0, committedEmpty.style.totalDistance, 0.0)
        assertEquals("150", committedEmpty.displayText)

        val toggledSmall = toggleTrackStyleDistanceUnit(style, rawText = "1")
        assertEquals(DistanceUnit.METERS, toggledSmall.style.distanceUnit)
        assertEquals(10.0, toggledSmall.style.totalDistance, 0.0)
        assertEquals("10", toggledSmall.displayText)

        val toggledLargeMeters = toggleTrackStyleDistanceUnit(
            style.copy(distanceUnit = DistanceUnit.METERS, totalDistance = 900.0),
            rawText = "2000",
        )
        assertEquals(DistanceUnit.YARDS, toggledLargeMeters.style.distanceUnit)
        assertEquals(1000.0, toggledLargeMeters.style.totalDistance, 0.0)
        assertEquals("1000", toggledLargeMeters.displayText)
    }

    @Test
    fun trackStyleDistanceVisibilityChangeKeepsFinalDistanceInIosRange() {
        val enabledFromZero = trackStyleDistanceVisibilityChange(
            TrackStyle(showDistance = false, totalDistance = 0.0),
            enabled = true,
        )
        val enabledFromNaN = trackStyleDistanceVisibilityChange(
            TrackStyle(showDistance = false, totalDistance = Double.NaN),
            enabled = true,
        )
        assertTrue(enabledFromZero.showDistance)
        assertEquals(150.0, enabledFromZero.totalDistance, 0.0)
        assertTrue(enabledFromNaN.showDistance)
        assertEquals(GolfCamDistanceConfig.DEFAULT_DISTANCE, enabledFromNaN.totalDistance, 0.0)

        val enabledFromSmallLegacyValue = trackStyleDistanceVisibilityChange(
            TrackStyle(showDistance = false, totalDistance = 2.0),
            enabled = true,
        )
        assertTrue(enabledFromSmallLegacyValue.showDistance)
        assertEquals(10.0, enabledFromSmallLegacyValue.totalDistance, 0.0)

        val disabledFromLargeLegacyValue = trackStyleDistanceVisibilityChange(
            TrackStyle(showDistance = true, totalDistance = 1200.0),
            enabled = false,
        )
        assertFalse(disabledFromLargeLegacyValue.showDistance)
        assertEquals(1000.0, disabledFromLargeLegacyValue.totalDistance, 0.0)
    }

    @Test
    fun trackStyleToolNumberLabelsUseStableIosIntegerAndPercentFormatting() {
        assertEquals("40", formatTrackStyleInteger(40.4))
        assertEquals("70", formatTrackStyleInteger(70.0))
        assertEquals("0", formatTrackStyleInteger(Double.NaN))
        assertEquals("0%", formatTrackStyleOpacityPercent(-0.2))
        assertEquals("75%", formatTrackStyleOpacityPercent(0.75))
        assertEquals("75%", formatTrackStyleOpacityPercent(Double.NaN))
        assertEquals("100%", formatTrackStyleOpacityPercent(1.4))
    }

    @Test
    fun trackStyleSliderValuesStayFiniteInsideIosRangesForMalformedDirectState() {
        assertEquals(0.75f, trackStyleSliderValue(Double.NaN, 0f, 1f, 0.75f), 0f)
        assertEquals(1f, trackStyleSliderValue(Double.POSITIVE_INFINITY, 0f, 1f, 2f), 0f)
        assertEquals(0f, trackStyleSliderValue(-0.2, 0f, 1f, 0.75f), 0f)
        assertEquals(1f, trackStyleSliderValue(1.4, 0f, 1f, 0.75f), 0f)
        assertEquals(40f, trackStyleSliderValue(Double.NaN, 5f, 160f, 40f), 0f)
        assertEquals(160f, trackStyleSliderValue(240.0, 5f, 160f, 40f), 0f)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL.toFloat(), trackStyleSliderValue(Double.NaN, 0.15f, 0.75f, 0.45f), 0f)
        assertEquals(0f, trackStyleSliderValue(0.4, Float.NaN, Float.NEGATIVE_INFINITY, Float.NaN), 0f)
    }

    @Test
    fun trackStyleToolExposesOnlyIosProductionVisibleCategories() {
        val labels = visibleTrackStyleCategoryLabels()

        assertEquals(listOf("颜色", "宽度", "速度", "拖尾", "距离"), labels)
        assertFalse(labels.contains("弹道"))
        assertFalse(labels.contains("调光"))
        assertFalse(labels.contains("特效"))
    }

    @Test
    fun trackStyleToolVisualSpecMatchesIosCompactControls() {
        assertEquals(36, TRACK_STYLE_CATEGORY_HEIGHT_DP)
        assertEquals(108, TRACK_STYLE_CONTENT_HEIGHT_DP)
        assertEquals(12, TRACK_STYLE_CATEGORY_HORIZONTAL_PADDING_DP)
        assertEquals(2, TRACK_STYLE_CATEGORY_TOP_PADDING_DP)
        assertEquals(32, TRACK_STYLE_CATEGORY_TAB_HEIGHT_DP)
        assertEquals(13, TRACK_STYLE_CATEGORY_ICON_SIZE_DP)
        assertEquals(10, TRACK_STYLE_CATEGORY_LABEL_FONT_SP)
        assertEquals(34, TRACK_STYLE_COLOR_PICKER_SIZE_DP)
        assertEquals(12, TRACK_STYLE_COLOR_ROW_SPACING_DP)
        assertEquals(16, TRACK_STYLE_CONTENT_HORIZONTAL_PADDING_DP)
        assertEquals(44, TRACK_STYLE_COLOR_PRESET_OUTER_DP)
        assertEquals(38, TRACK_STYLE_COLOR_PRESET_SWATCH_DP)
        assertEquals(10, TRACK_STYLE_COLOR_PRESET_SPACING_DP)
        assertEquals(2, TRACK_STYLE_COLOR_PRESET_SELECTED_STROKE_DP)
        assertEquals(11, TRACK_STYLE_COLOR_PRESET_CHECK_SIZE_DP)
        assertEquals(14, TRACK_STYLE_SLIDER_VERTICAL_PADDING_DP)
        assertEquals(11, TRACK_STYLE_SLIDER_LABEL_FONT_SP)
        assertEquals(10, TRACK_STYLE_DISTANCE_VERTICAL_SPACING_DP)
        assertEquals(28, TRACK_STYLE_DISTANCE_INPUT_FONT_SP)
        assertEquals(80, TRACK_STYLE_DISTANCE_INPUT_WIDTH_DP)
        assertEquals(12, TRACK_STYLE_DISTANCE_INPUT_CORNER_RADIUS_DP)
        assertEquals(14, TRACK_STYLE_DISTANCE_INPUT_HORIZONTAL_PADDING_DP)
        assertEquals(8, TRACK_STYLE_DISTANCE_INPUT_VERTICAL_PADDING_DP)
        assertEquals(14, TRACK_STYLE_DISTANCE_UNIT_FONT_SP)
        assertEquals(20, TRACK_STYLE_DISTANCE_CONFIRM_ICON_SIZE_DP)
        assertEquals(56, TRACK_STYLE_DISTANCE_UNIT_TOGGLE_SIZE_DP)
        assertEquals(12, TRACK_STYLE_DISTANCE_UNIT_TOGGLE_CORNER_RADIUS_DP)
        assertEquals(13, TRACK_STYLE_DISTANCE_UNIT_TOGGLE_LABEL_FONT_SP)
        assertEquals(10, TRACK_STYLE_DISTANCE_UNIT_TOGGLE_ICON_SIZE_DP)
    }

    @Test
    fun trackStyleStateUpdateClampsStyleAndUsesCurrentCurve() {
        val currentCurve = FlightCurveSelection(
            impactPosition = ImpactPositionSelection(normalizedX = 0.3f, normalizedY = 0.7f),
            landingPoint = LandingOutput(normalizedX = 0.8f, normalizedY = 0.4f, flightTimeSec = 4.5),
            controlPoint1 = NormalizedPoint(0.3f, 0.2f),
            controlPoint2 = NormalizedPoint(0.6f, 0.1f),
        )
        val staleCurve = currentCurve.copy(
            landingPoint = LandingOutput(normalizedX = 0.6f, normalizedY = 0.6f, flightTimeSec = 4.5),
        )
        val rawStyle = TrackStyle(
            colorPreset = TrackColorPreset(
                name = "越界颜色",
                gradientColors = listOf(0xFF123456),
                glowBlur = -4f,
                glowAlpha = 1.5f,
                bandAlpha = -0.5f,
            ),
            colorAlpha = 1.4,
            glowOpacity = -0.2,
            widthPct = 320.0,
            tailHalfLife = 1.0,
            totalDistance = 2.0,
            speedLevel = 2.0,
            activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5,
            compareTrajectoryAlgorithms = true,
            trajectoryPreset = TrajectoryPresetKind.HIGH_ARC,
            motionLaunchBias = 99.0,
            motionDrag = -99.0,
            motionLiftHold = 99.0,
            motionDescentAcceleration = -99.0,
            motionTailBoost = 99.0,
            motionAnchorTime = 99.0,
            v4VMin = -99.0,
            v4VMax = 99.0,
            v4PLeft = -99.0,
            v4PRight = 99.0,
            v4CustomSPeak = 99.0,
            effectType = TrackEffectType.FIRE,
            renderTuning = TrackRenderTuning(
                bandAlphaMultiplier = 9.0f,
                highlightStart = 0.95f,
                highlightEnd = 0.01f,
                highlightPeak = 2.0f,
                widthTaperPower = 9.0f,
            ),
        )
        val clamped = clampedTrackStyleSelection(
            next = TrackStyleSelection(style = rawStyle, flightCurve = staleCurve),
            currentFlightCurve = currentCurve,
        )
        val state = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = currentCurve.impactPosition
            landing = currentCurve.landingPoint
            flightCurve = currentCurve
        }

        assertEquals(currentCurve, clamped.flightCurve)
        assertEquals(1.0, clamped.style.colorAlpha, 0.0)
        assertEquals(0.0, clamped.style.glowOpacity, 0.0)
        assertEquals(160.0, clamped.style.widthPct, 0.0)
        assertEquals(5.0, clamped.style.tailHalfLife, 0.0)
        assertEquals(10.0, clamped.style.totalDistance, 0.0)
        assertEquals(GolfCamSpeedConfig.MAX_LEVEL, clamped.style.speedLevel, 0.0)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, clamped.style.activeTrajectoryAlgorithm)
        assertFalse(clamped.style.compareTrajectoryAlgorithms)
        assertEquals(TrajectoryPresetKind.GOLF_CAM_LIKE, clamped.style.trajectoryPreset)
        assertEquals(TrajectoryMotionParameters.standard.launchBias, clamped.style.motionLaunchBias, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.drag, clamped.style.motionDrag, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.liftHold, clamped.style.motionLiftHold, 0.0)
        assertEquals(
            TrajectoryMotionParameters.standard.descentAcceleration,
            clamped.style.motionDescentAcceleration,
            0.0,
        )
        assertEquals(TrajectoryMotionParameters.standard.tailBoost, clamped.style.motionTailBoost, 0.0)
        assertEquals(null, clamped.style.motionAnchorTime)
        assertEquals(null, clamped.style.v4VMin)
        assertEquals(null, clamped.style.v4VMax)
        assertEquals(null, clamped.style.v4PLeft)
        assertEquals(null, clamped.style.v4PRight)
        assertEquals(null, clamped.style.v4CustomSPeak)
        assertEquals(TrackEffectType.GRADIENT, clamped.style.effectType)
        assertEquals(0f, clamped.style.colorPreset.glowBlur, 0f)
        assertEquals(1f, clamped.style.colorPreset.glowAlpha, 0f)
        assertEquals(0f, clamped.style.colorPreset.bandAlpha, 0f)
        assertEquals(1.40f, clamped.style.renderTuning.bandAlphaMultiplier, 0f)
        assertEquals(0.90f, clamped.style.renderTuning.highlightStart, 0f)
        assertEquals(0.95f, clamped.style.renderTuning.highlightEnd, 0f)
        assertEquals(0.95f, clamped.style.renderTuning.highlightPeak, 0f)
        assertEquals(2.20f, clamped.style.renderTuning.widthTaperPower, 0f)

        state.updateTrackStyle(TrackStyleSelection(style = rawStyle, flightCurve = staleCurve))
        assertEquals(currentCurve, state.trackStyle?.flightCurve)
        assertEquals(160.0, state.trackStyle?.style?.widthPct ?: -1.0, 0.0)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, state.trackStyle?.style?.activeTrajectoryAlgorithm)
        assertTrue(state.hasCompletedTrackStyle)
    }

    @Test
    fun workflowStateClampRejectsNonFiniteCoordinatesBeforePreviewExport() {
        val clampedImpact = clampedImpactPositionSelection(
            ImpactPositionSelection(normalizedX = Float.NaN, normalizedY = Float.POSITIVE_INFINITY),
        )
        val clampedLanding = clampedLandingOutput(
            LandingOutput(normalizedX = Float.NaN, normalizedY = Float.NEGATIVE_INFINITY, flightTimeSec = Double.NaN),
        )
        val clampedCurve = clampedFlightCurveSelection(
            FlightCurveSelection(
                impactPosition = ImpactPositionSelection(Float.NaN, Float.POSITIVE_INFINITY),
                landingPoint = LandingOutput(Float.NEGATIVE_INFINITY, Float.NaN, Double.POSITIVE_INFINITY),
                controlPoint1 = NormalizedPoint(Float.NaN, Float.POSITIVE_INFINITY),
                controlPoint2 = NormalizedPoint(Float.NEGATIVE_INFINITY, Float.NaN),
            ),
        )

        assertEquals(0.5f, clampedImpact.normalizedX, 0f)
        assertEquals(0.5f, clampedImpact.normalizedY, 0f)
        assertEquals(0.5f, clampedLanding.normalizedX, 0f)
        assertEquals(0.5f, clampedLanding.normalizedY, 0f)
        assertEquals(4.5, clampedLanding.flightTimeSec, 0.0)
        assertEquals(0.5f, clampedCurve.impactPosition.normalizedX, 0f)
        assertEquals(0.5f, clampedCurve.impactPosition.normalizedY, 0f)
        assertEquals(0.5f, clampedCurve.landingPoint.normalizedX, 0f)
        assertEquals(0.5f, clampedCurve.landingPoint.normalizedY, 0f)
        assertEquals(4.5, clampedCurve.landingPoint.flightTimeSec, 0.0)
        assertEquals(0.5f, clampedCurve.controlPoint1.x, 0f)
        assertEquals(0f, clampedCurve.controlPoint1.y, 0f)
        assertEquals(0.5f, clampedCurve.controlPoint2.x, 0f)
        assertEquals(0f, clampedCurve.controlPoint2.y, 0f)
    }

    @Test
    fun trackStyleClampRejectsNonFiniteProductionParameters() {
        val clamped = clampedTrackStyle(
            TrackStyle(
                colorPreset = TrackColorPreset(
                    name = "非有限颜色",
                    gradientColors = listOf(0xFF123456),
                    glowBlur = Float.NaN,
                    glowAlpha = Float.POSITIVE_INFINITY,
                    bandAlpha = Float.NaN,
                ),
                colorAlpha = Double.NaN,
                glowOpacity = Double.POSITIVE_INFINITY,
                widthPct = Double.NaN,
                tailHalfLife = Double.NEGATIVE_INFINITY,
                totalDistance = Double.NaN,
                speedLevel = Double.NaN,
                renderTuning = TrackRenderTuning(
                    bandAlphaMultiplier = Float.NaN,
                    bodyAlphaBase = Float.NaN,
                    bodyAlphaScale = Float.POSITIVE_INFINITY,
                    glowBlurMultiplier = Float.NaN,
                    outerGlowMultiplier = Float.NaN,
                    fillGlowMultiplier = Float.NaN,
                    highlightWidthRatio = Float.NaN,
                    highlightAlphaMax = Float.NaN,
                    highlightStart = Float.NaN,
                    highlightEnd = Float.NaN,
                    highlightPeak = Float.NaN,
                    highlightStrength = Float.NaN,
                    widthTaperPower = Float.NaN,
                    apexThinStrength = Float.NaN,
                    apexThinRadius = Float.NaN,
                    highlightFalloffPower = Float.NaN,
                ),
            ),
        )

        assertEquals(0.75, clamped.colorAlpha, 0.0)
        assertEquals(0.7, clamped.glowOpacity, 0.0)
        assertEquals(40.0, clamped.widthPct, 0.0)
        assertEquals(70.0, clamped.tailHalfLife, 0.0)
        assertEquals(GolfCamDistanceConfig.DEFAULT_DISTANCE, clamped.totalDistance, 0.0)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL, clamped.speedLevel, 0.0)
        assertEquals(0f, clamped.colorPreset.glowBlur, 0f)
        assertEquals(0.7f, clamped.colorPreset.glowAlpha, 0f)
        assertEquals(1f, clamped.colorPreset.bandAlpha, 0f)
        assertEquals(TrackRenderTuning().bandAlphaMultiplier, clamped.renderTuning.bandAlphaMultiplier, 0f)
        assertEquals(TrackRenderTuning().bodyAlphaBase, clamped.renderTuning.bodyAlphaBase, 0f)
        assertEquals(TrackRenderTuning().bodyAlphaScale, clamped.renderTuning.bodyAlphaScale, 0f)
        assertEquals(TrackRenderTuning().glowBlurMultiplier, clamped.renderTuning.glowBlurMultiplier, 0f)
        assertEquals(TrackRenderTuning().highlightStart, clamped.renderTuning.highlightStart, 0f)
        assertEquals(TrackRenderTuning().highlightEnd, clamped.renderTuning.highlightEnd, 0f)
        assertEquals(TrackRenderTuning().highlightPeak, clamped.renderTuning.highlightPeak, 0f)
        assertEquals(TrackRenderTuning().widthTaperPower, clamped.renderTuning.widthTaperPower, 0f)
    }

    @Test
    fun headGlowAlphaSpecIsSharedByPreviewAndExport() {
        val alphas = TrajectoryOverlayRenderSpec.headGlowAlphas(0.7f)
        val style = TrackStyle(widthPct = 40.0)

        assertEquals(0.245f, alphas.outer, 1e-6f)
        assertEquals(0.525f, alphas.body, 1e-6f)
        assertEquals(0.63f, alphas.core, 1e-6f)
        assertEquals(0, TrajectoryOverlayRenderSpec.alphaByte(Float.NaN))
        assertEquals(0, TrajectoryOverlayRenderSpec.alphaByte(Float.NEGATIVE_INFINITY))
        assertEquals(0, TrajectoryOverlayRenderSpec.alphaByte(-0.5f))
        assertEquals(127, TrajectoryOverlayRenderSpec.alphaByte(0.5f))
        assertEquals(255, TrajectoryOverlayRenderSpec.alphaByte(1.5f))
        assertEquals(0, TrajectoryOverlayRenderSpec.alphaByte(Float.POSITIVE_INFINITY))
        assertTrue(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(0.999f))
        assertFalse(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(1f))
        assertFalse(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(Float.NaN))
        assertFalse(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(Float.POSITIVE_INFINITY))
        assertFalse(TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(-0.01f))
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
    fun videoDisplayGeometryFallsBackFromNonFiniteInputsBeforePreviewExport() {
        val malformedContainer = VideoDisplayGeometry.fit(
            containerWidth = Float.NaN,
            containerHeight = Float.POSITIVE_INFINITY,
            videoAspectRatio = Float.NaN,
        )
        val malformedAspect = VideoDisplayGeometry.fit(
            containerWidth = 1200f,
            containerHeight = 800f,
            videoAspectRatio = Float.POSITIVE_INFINITY,
        )

        assertEquals(1f, malformedContainer.width, 0.0f)
        assertEquals(1f, malformedContainer.height, 0.0f)
        assertEquals(800f, malformedAspect.width, 0.0f)
        assertEquals(800f, malformedAspect.height, 0.0f)
        assertTrue(malformedContainer.width.isFinite())
        assertTrue(malformedContainer.height.isFinite())
        assertTrue(malformedAspect.width.isFinite())
        assertTrue(malformedAspect.height.isFinite())
    }

    @Test
    fun interactiveOverlayCoordinatesClampToFullIosNormalizedRange() {
        assertEquals(0.5f, normalizedCoordinateOrCenter(null), 0f)
        assertEquals(0.5f, normalizedCoordinateOrCenter(Float.NaN), 0f)
        assertEquals(0.5f, normalizedCoordinateOrCenter(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, normalizedCoordinateOrCenter(-0.25f), 0f)
        assertEquals(0.25f, normalizedCoordinateOrCenter(0.25f), 0f)
        assertEquals(1f, normalizedCoordinateOrCenter(1.25f), 0f)
        assertEquals(0f, normalizedVideoCoordinate(-20f, 1000f), 0f)
        assertEquals(0.5f, normalizedVideoCoordinate(10f, 0f), 0f)
        assertEquals(0.5f, normalizedVideoCoordinate(Float.NaN, 1000f), 0f)
        assertEquals(0.5f, normalizedVideoCoordinate(10f, Float.NaN), 0f)
        assertEquals(0.5f, normalizedVideoCoordinate(500f, 1000f), 0f)
        assertEquals(1f, normalizedVideoCoordinate(1200f, 1000f), 0f)
    }

    @Test
    fun impactPositionMarkerAndLabelsMatchIosOverlaySpec() {
        assertEquals(24f, IMPACT_MARKER_SIZE_DP, 0f)
        assertEquals(50f, IMPACT_DRAG_RADIUS_DP, 0f)
        assertEquals(8, IMPACT_POSITION_TOOL_VERTICAL_SPACING_DP)
        assertEquals(11, IMPACT_POSITION_COORD_LABEL_FONT_SP)
        assertEquals(14, IMPACT_POSITION_COORD_VALUE_FONT_SP)
        assertEquals(16, IMPACT_POSITION_TOOL_MARK_SIZE_DP)
        assertEquals(6, IMPACT_POSITION_TOOL_MARK_SPACING_DP)
        assertEquals(13, IMPACT_POSITION_TOOL_TITLE_FONT_SP)
        assertEquals(14, IMPACT_POSITION_HINT_ICON_SIZE_DP)
        assertEquals(8, IMPACT_POSITION_HINT_SPACING_DP)
        assertEquals(12, IMPACT_POSITION_HINT_FONT_SP)
        assertEquals("0.0%", formatImpactPositionPercent(-4f))
        assertEquals("36.5%", formatImpactPositionPercent(36.54f))
        assertEquals("50.0%", formatImpactPositionPercent(Float.NaN))
        assertEquals("100.0%", formatImpactPositionPercent(140f))
    }

    @Test
    fun impactPositionStateUpdateClampsP0ToVideoBounds() {
        val clamped = clampedImpactPositionSelection(
            ImpactPositionSelection(normalizedX = -0.25f, normalizedY = 1.25f),
        )
        val state = GolfCamWorkflowState().apply {
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            landing = LandingOutput(normalizedX = 0.8f, normalizedY = 0.4f, flightTimeSec = 4.5)
            flightCurve = FlightCurveSelection(
                impactPosition = ImpactPositionSelection(0.4f, 0.6f),
                landingPoint = landing!!,
                controlPoint1 = NormalizedPoint(0.3f, 0.2f),
                controlPoint2 = NormalizedPoint(0.6f, 0.1f),
            )
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = flightCurve!!)
        }

        assertEquals(0f, clamped.normalizedX, 0f)
        assertEquals(1f, clamped.normalizedY, 0f)

        state.updateImpactPosition(ImpactPositionSelection(normalizedX = -0.25f, normalizedY = 1.25f))
        assertEquals(0f, state.impactPosition?.normalizedX ?: -1f, 0f)
        assertEquals(1f, state.impactPosition?.normalizedY ?: -1f, 0f)
        assertEquals(ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L), state.impactFrame)
        assertEquals(null, state.landing)
        assertEquals(null, state.flightCurve)
        assertEquals(null, state.trackStyle)
    }

    @Test
    fun landingMarkerAnchorUsesDirectIosP3CoordinateWithoutVisualOffset() {
        assertEquals(24f, LANDING_MARKER_SIZE_DP, 0f)
        assertEquals(50f, LANDING_DRAG_RADIUS_DP, 0f)
        assertEquals(56, LANDING_TIMELINE_HEIGHT_DP)
        assertEquals(32, LANDING_TIMELINE_THUMB_WIDTH_DP)
        assertEquals(80, LANDING_TIMELINE_PIXELS_PER_SECOND_DP)
        assertEquals(4, LANDING_TIMELINE_TRACK_CORNER_RADIUS_DP)
        assertEquals(20, LANDING_CENTER_INDICATOR_WIDTH_DP)
        assertEquals(4, LANDING_CENTER_INDICATOR_BAR_WIDTH_DP)
        assertEquals(8, LANDING_CENTER_INDICATOR_DOT_SIZE_DP)
        assertEquals(4, LANDING_CENTER_INDICATOR_VERTICAL_INSET_DP)
        assertEquals(12, LANDING_THUMB_COUNT)
        assertEquals(0.6f, LANDING_VIRTUAL_THUMBNAIL_ALPHA, 0f)
        assertEquals(0.4f, LANDING_VIRTUAL_OVERLAY_ALPHA, 0f)
        assertEquals(2, LANDING_MIN_TIME_MARKER_WIDTH_DP)
        assertEquals(9, LANDING_MIN_TIME_LABEL_FONT_SP)
        assertEquals(6, LANDING_TOOL_VERTICAL_SPACING_DP)
        assertEquals(11, LANDING_TOOL_LABEL_FONT_SP)
        assertEquals(14, LANDING_TOOL_VALUE_FONT_SP)
        assertEquals(16, LANDING_TOOL_MARK_SIZE_DP)
        assertEquals(6, LANDING_TOOL_MARK_SPACING_DP)
        assertEquals(13, LANDING_TOOL_TITLE_FONT_SP)
        assertEquals(10, LANDING_VIRTUAL_BADGE_FONT_SP)
        assertEquals(4, LANDING_VIRTUAL_BADGE_CORNER_RADIUS_DP)
        assertEquals(6, LANDING_VIRTUAL_BADGE_HORIZONTAL_PADDING_DP)
        assertEquals(2, LANDING_VIRTUAL_BADGE_VERTICAL_PADDING_DP)
        assertEquals(14, LANDING_HINT_ICON_SIZE_DP)
        assertEquals(8, LANDING_HINT_SPACING_DP)
        assertEquals(12, LANDING_HINT_FONT_SP)
        assertEquals(0.5f, normalizedLandingCoordinate(500f, 1000f, markerSizePx = 24f), 0f)
        assertEquals(1f, normalizedLandingCoordinate(1000f, 1000f, markerSizePx = 24f), 0f)
        assertEquals(0.998f, normalizedLandingCoordinate(998f, 1000f, markerSizePx = 24f), 0f)
        assertEquals(1f, normalizedLandingCoordinate(1200f, 1000f, markerSizePx = 24f), 0f)
        assertEquals(0f, normalizedLandingCoordinate(-10f, 1000f, markerSizePx = 24f), 0f)
    }

    @Test
    fun landingTimeRulesMatchIosDefaultRangeAndVirtualSegment() {
        assertEquals(4.5, landingFlightTimeSeconds(null), 0.0)
        assertEquals(0.5, landingFlightTimeSeconds(LandingOutput(0.5f, 0.5f, flightTimeSec = 0.1)), 0.0)
        assertEquals(8.0, landingFlightTimeSeconds(LandingOutput(0.5f, 0.5f, flightTimeSec = 12.0)), 0.0)
        assertEquals(3.2, landingFlightTimeSeconds(LandingOutput(0.5f, 0.5f, flightTimeSec = 3.2)), 0.0)
        assertEquals(4.5, landingFlightTimeSeconds(LandingOutput(0.5f, 0.5f, flightTimeSec = Double.NaN)), 0.0)

        assertEquals(0.5, landingTailSeconds(impactTimeMs = 4_800L, clipEndMs = 5_000L), 0.0)
        assertEquals(4.5, landingTailSeconds(impactTimeMs = 1_500L, clipEndMs = 6_000L), 0.0)
        assertEquals(8.0, landingTailSeconds(impactTimeMs = 1_000L, clipEndMs = 15_000L), 0.0)

        assertFalse(isLandingVirtualSegment(flightTimeSec = 4.5, tailSeconds = 4.5))
        assertTrue(isLandingVirtualSegment(flightTimeSec = 4.5001, tailSeconds = 4.5))
        assertEquals("0.5s", formatLandingTimeLabel(0.1))
        assertEquals("4.5s", formatLandingTimeLabel(4.54))
        assertEquals("4.5s", formatLandingTimeLabel(Double.NaN))
        assertEquals("8.0s", formatLandingTimeLabel(12.0))
        assertEquals("0%, 100%", formatLandingPositionLabel(-10f, 130f))
        assertEquals("36%, 72%", formatLandingPositionLabel(36.4f, 72.2f))
        assertEquals("50%, 50%", formatLandingPositionLabel(Float.NaN, Float.POSITIVE_INFINITY))
    }

    @Test
    fun landingOutputUpdatesPreserveCoordinateAndTimeLikeIosWorkflowState() {
        val current = LandingOutput(normalizedX = 0.82f, normalizedY = 0.44f, flightTimeSec = 3.2)
        val timeChanged = landingOutputForTimeChange(current, flightTimeSec = 6.4)
        val positionChanged = landingOutputForPositionChange(current, normalizedX = 0.12f, normalizedY = 0.91f)
        val initialTime = landingOutputForTimeChange(current = null, flightTimeSec = 4.5)
        val initialPosition = landingOutputForPositionChange(current = null, normalizedX = 0.25f, normalizedY = 0.75f)
        val clampedEarlyTime = landingOutputForTimeChange(current, flightTimeSec = 0.1)
        val clampedLateTime = landingOutputForTimeChange(current, flightTimeSec = 12.0)
        val defaultedNaNTime = landingOutputForTimeChange(current, flightTimeSec = Double.NaN)
        val clampedPosition = landingOutputForPositionChange(current, normalizedX = -0.2f, normalizedY = 1.2f)
        val defaultedNaNPosition = landingOutputForPositionChange(current, normalizedX = Float.NaN, normalizedY = Float.POSITIVE_INFINITY)

        assertEquals(0.82f, timeChanged.normalizedX, 0f)
        assertEquals(0.44f, timeChanged.normalizedY, 0f)
        assertEquals(6.4, timeChanged.flightTimeSec, 0.0)

        assertEquals(0.12f, positionChanged.normalizedX, 0f)
        assertEquals(0.91f, positionChanged.normalizedY, 0f)
        assertEquals(3.2, positionChanged.flightTimeSec, 0.0)

        assertEquals(LandingOutput(0.5f, 0.5f, 4.5), initialTime)
        assertEquals(LandingOutput(0.25f, 0.75f, 4.5), initialPosition)
        assertEquals(0.5, clampedEarlyTime.flightTimeSec, 0.0)
        assertEquals(8.0, clampedLateTime.flightTimeSec, 0.0)
        assertEquals(4.5, defaultedNaNTime.flightTimeSec, 0.0)
        assertEquals(0f, clampedPosition.normalizedX, 0f)
        assertEquals(1f, clampedPosition.normalizedY, 0f)
        assertEquals(3.2, clampedPosition.flightTimeSec, 0.0)
        assertEquals(0.5f, defaultedNaNPosition.normalizedX, 0f)
        assertEquals(0.5f, defaultedNaNPosition.normalizedY, 0f)
        assertEquals(3.2, defaultedNaNPosition.flightTimeSec, 0.0)
    }

    @Test
    fun landingStateUpdateClampsP3AndFlightTimeToIosBounds() {
        val clamped = clampedLandingOutput(
            LandingOutput(normalizedX = -0.25f, normalizedY = 1.25f, flightTimeSec = 12.0),
        )
        val state = GolfCamWorkflowState().apply {
            impactPosition = ImpactPositionSelection(0.3f, 0.7f)
            flightCurve = FlightCurveSelection(
                impactPosition = impactPosition!!,
                landingPoint = LandingOutput(normalizedX = 0.8f, normalizedY = 0.4f, flightTimeSec = 4.5),
                controlPoint1 = NormalizedPoint(0.3f, 0.2f),
                controlPoint2 = NormalizedPoint(0.6f, 0.1f),
            )
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = flightCurve!!)
        }

        assertEquals(0f, clamped.normalizedX, 0f)
        assertEquals(1f, clamped.normalizedY, 0f)
        assertEquals(8.0, clamped.flightTimeSec, 0.0)

        state.updateLanding(LandingOutput(normalizedX = -0.25f, normalizedY = 1.25f, flightTimeSec = 12.0))
        assertEquals(0f, state.landing?.normalizedX ?: -1f, 0f)
        assertEquals(1f, state.landing?.normalizedY ?: -1f, 0f)
        assertEquals(8.0, state.landing?.flightTimeSec ?: -1.0, 0.0)
        assertEquals(null, state.flightCurve)
        assertEquals(null, state.trackStyle)
    }

    @Test
    fun landingSeekClampUsesActualFrameRateInsteadOfFixedThirtyFpsOffset() {
        assertEquals(
            6_983L,
            landingSeekTimeMs(
                trimStartMs = 1_000L,
                impactTimeMs = 2_000L,
                flightTimeSec = 8.0,
                clipEndMs = 7_000L,
                frameRate = 60.0,
            ),
        )
        assertEquals(
            6_966L,
            landingSeekTimeMs(
                trimStartMs = 1_000L,
                impactTimeMs = 2_000L,
                flightTimeSec = 8.0,
                clipEndMs = 7_000L,
                frameRate = 30.0,
            ),
        )
        assertEquals(
            3_500L,
            landingSeekTimeMs(
                trimStartMs = 1_000L,
                impactTimeMs = 2_000L,
                flightTimeSec = 1.5,
                clipEndMs = 7_000L,
                frameRate = 60.0,
            ),
        )
        assertEquals(
            1_000L,
            landingSeekTimeMs(
                trimStartMs = 1_000L,
                impactTimeMs = 900L,
                flightTimeSec = 0.0,
                clipEndMs = 1_001L,
                frameRate = 120.0,
            ),
        )
    }

    @Test
    fun curveControlDragClampMatchesIosUnifiedFlowRange() {
        val topLeft = clampedCurveControlPoint(-500f, -1200f, 1000f, 1000f)
        val bottomRight = clampedCurveControlPoint(1600f, 1800f, 1000f, 1000f)
        val defaulted = clampedCurveControlPoint(Float.NaN, Float.POSITIVE_INFINITY, 1000f, 1000f)

        assertEquals(0f, topLeft.x, 0f)
        assertEquals(-1f, topLeft.y, 0f)
        assertEquals(1f, bottomRight.x, 0f)
        assertEquals(1.5f, bottomRight.y, 0f)
        assertEquals(0.5f, defaulted.x, 0f)
        assertEquals(0.5f, defaulted.y, 0f)
        assertEquals(88f, CURVE_CONTROL_HIT_TARGET_DP, 0f)
        assertEquals(6, CURVE_TOOL_VERTICAL_SPACING_DP)
        assertEquals(8, CURVE_TOOL_VERTICAL_PADDING_DP)
        assertEquals(11, CURVE_INFO_TITLE_FONT_SP)
        assertEquals(10, CURVE_INFO_TIME_FONT_SP)
        assertEquals(6, CURVE_CONTROL_DOT_SIZE_DP)
        assertEquals(1, CURVE_CONTROL_DOT_STROKE_DP)
        assertEquals(4, CURVE_CONTROL_READOUT_SPACING_DP)
        assertEquals(10, CURVE_CONTROL_LABEL_FONT_SP)
        assertEquals(9, CURVE_CONTROL_VALUE_FONT_SP)
        assertEquals(50, CURVE_TIMELINE_HEIGHT_DP)
        assertEquals(32, CURVE_TIMELINE_CELL_WIDTH_DP)
        assertEquals(45, CURVE_TIMELINE_IMAGE_HEIGHT_DP)
        assertEquals(0, CURVE_TIMELINE_CELL_SPACING_DP)
        assertEquals(2, CURVE_TIMELINE_IMAGE_CORNER_RADIUS_DP)
        assertEquals(3, CURVE_TIMELINE_SELECTION_HEIGHT_DP)
        assertEquals(1, CURVE_TIMELINE_SELECTION_CORNER_RADIUS_DP)
        assertEquals(0.7f, CURVE_TIMELINE_UNSELECTED_ALPHA, 0f)
        assertEquals(50, CURVE_FRAME_BUTTON_WIDTH_DP)
        assertEquals(12, CURVE_FRAME_BUTTON_ICON_SIZE_DP)
        assertEquals(9, CURVE_FRAME_BUTTON_LABEL_FONT_SP)
        assertEquals(8, CURVE_BOTTOM_ROW_SPACING_DP)
        assertEquals(12, CURVE_HINT_ICON_SIZE_DP)
        assertEquals(11, CURVE_HINT_FONT_SP)
        assertEquals(7, CURVE_IMPACT_CHIP_CORNER_RADIUS_DP)
        assertEquals(9, CURVE_IMPACT_CHIP_HORIZONTAL_PADDING_DP)
        assertEquals(5, CURVE_IMPACT_CHIP_VERTICAL_PADDING_DP)
        assertEquals(9, CURVE_IMPACT_CHIP_ICON_SIZE_DP)
        assertEquals(10, CURVE_IMPACT_CHIP_LABEL_FONT_SP)
        assertEquals("0.00s", formatCurveTimeLabel(-0.25))
        assertEquals("0.00s", formatCurveTimeLabel(Double.NaN))
        assertEquals("1.25s", formatCurveTimeLabel(1.25))
        assertEquals("(0%,-100%)", formatCurveControlPointLabel(-20f, -180f))
        assertEquals("(42%,58%)", formatCurveControlPointLabel(42.4f, 58.2f))
        assertEquals("(50%,0%)", formatCurveControlPointLabel(Float.NaN, Float.POSITIVE_INFINITY))
        assertEquals("(100%,150%)", formatCurveControlPointLabel(130f, 180f))
    }

    @Test
    fun flightCurveStateUpdateClampsControlsAndUsesCurrentAnchors() {
        val currentImpact = ImpactPositionSelection(normalizedX = 0.3f, normalizedY = 0.7f)
        val currentLanding = LandingOutput(normalizedX = 0.8f, normalizedY = 0.4f, flightTimeSec = 4.5)
        val staleImpact = ImpactPositionSelection(normalizedX = 0.1f, normalizedY = 0.9f)
        val staleLanding = LandingOutput(normalizedX = 0.6f, normalizedY = 0.6f, flightTimeSec = 12.0)
        val rawCurve = FlightCurveSelection(
            impactPosition = staleImpact,
            landingPoint = staleLanding,
            controlPoint1 = NormalizedPoint(-0.25f, -2.0f),
            controlPoint2 = NormalizedPoint(1.25f, 2.0f),
        )
        val clamped = clampedFlightCurveSelection(
            next = rawCurve,
            currentImpactPosition = currentImpact,
            currentLanding = currentLanding,
        )
        val state = GolfCamWorkflowState().apply {
            impactPosition = currentImpact
            landing = currentLanding
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = rawCurve)
        }

        assertEquals(currentImpact, clamped.impactPosition)
        assertEquals(currentLanding, clamped.landingPoint)
        assertEquals(0f, clamped.controlPoint1.x, 0f)
        assertEquals(-1f, clamped.controlPoint1.y, 0f)
        assertEquals(1f, clamped.controlPoint2.x, 0f)
        assertEquals(1.5f, clamped.controlPoint2.y, 0f)

        state.updateFlightCurve(rawCurve)
        assertEquals(currentImpact, state.flightCurve?.impactPosition)
        assertEquals(currentLanding, state.flightCurve?.landingPoint)
        assertEquals(0f, state.flightCurve?.controlPoint1?.x ?: -1f, 0f)
        assertEquals(-1f, state.flightCurve?.controlPoint1?.y ?: 0f, 0f)
        assertEquals(1f, state.flightCurve?.controlPoint2?.x ?: -1f, 0f)
        assertEquals(1.5f, state.flightCurve?.controlPoint2?.y ?: -1f, 0f)
        assertEquals(null, state.trackStyle)
    }

    @Test
    fun curveThumbnailTimelineIncludesLandingFrameWhileRespectingMaxCount() {
        val longTimeline = curveThumbnailFrameIndices(totalFrames = 360)
        val shortTimeline = curveThumbnailFrameIndices(totalFrames = 12)

        assertEquals(60, longTimeline.size)
        assertEquals(0, longTimeline.first())
        assertEquals(359, longTimeline.last())
        assertTrue(longTimeline.zipWithNext().all { (previous, next) -> next > previous })
        assertEquals((0 until 12).toList(), shortTimeline)
        assertEquals(0, nearestCurveThumbnailIndex(frameIndex = 0, frameIndices = longTimeline))
        assertEquals(longTimeline.lastIndex, nearestCurveThumbnailIndex(frameIndex = 359, frameIndices = longTimeline))
        assertEquals(Int.MAX_VALUE.toLong() - Int.MIN_VALUE.toLong(), golfCamFrameIndexDistance(Int.MIN_VALUE, Int.MAX_VALUE))
        assertEquals(
            1,
            nearestCurveThumbnailIndex(
                frameIndex = Int.MAX_VALUE,
                frameIndices = listOf(Int.MIN_VALUE, Int.MAX_VALUE),
            ),
        )
    }

    @Test
    fun curveTimelineFrameIndexClampsProgrammaticJumpsToTrimmedFrameRange() {
        assertEquals(0, curveTimelineFrameIndex(frameIndex = -12, totalFrames = 360))
        assertEquals(42, curveTimelineFrameIndex(frameIndex = 42, totalFrames = 360))
        assertEquals(359, curveTimelineFrameIndex(frameIndex = 999, totalFrames = 360))
        assertEquals(0, curveTimelineFrameIndex(frameIndex = 12, totalFrames = 0))
    }

    @Test
    fun curveThumbnailSampleTimeUsesHalfOpenTrimRange() {
        val frames = curveThumbnailFrameIndices(totalFrames = 360)

        assertEquals(1_000L, curveThumbnailSampleTimeMs(trimStartMs = 1_000L, trimEndMs = 7_000L, frameRate = 60.0, frameIndex = frames.first()))
        assertEquals(6_983L, curveThumbnailSampleTimeMs(trimStartMs = 1_000L, trimEndMs = 7_000L, frameRate = 60.0, frameIndex = frames.last()))
        assertEquals(6_983L, curveThumbnailSampleTimeMs(trimStartMs = 1_000L, trimEndMs = 7_000L, frameRate = 60.0, frameIndex = 999))
    }

    @Test
    fun curveFrameSeekUsesHalfOpenTrimRangeAndActualFrameRate() {
        assertEquals(
            1_000L,
            curveFrameSeekTimeMs(trimStartMs = 1_000L, trimEndMs = 7_000L, frameIndex = -10, frameRate = 60.0),
        )
        assertEquals(
            6_983L,
            curveFrameSeekTimeMs(trimStartMs = 1_000L, trimEndMs = 7_000L, frameIndex = 999, frameRate = 60.0),
        )
        assertEquals(
            15_983L,
            curveFrameSeekTimeMs(trimStartMs = 10_000L, trimEndMs = 16_000L, frameIndex = 999, frameRate = 60.0),
        )
        assertEquals(
            1_000L,
            curveFrameSeekTimeMs(trimStartMs = 1_000L, trimEndMs = 1_001L, frameIndex = 10, frameRate = 120.0),
        )
    }

    @Test
    fun curveControlHitTargetStaysReachableWhenControlPointIsOffscreen() {
        val targetSizePx = CURVE_CONTROL_HIT_TARGET_DP
        val inside = visibleCurveHitCenter(
            point = NormalizedPoint(0.50f, 0.50f),
            widthPx = 1000f,
            heightPx = 800f,
            targetSizePx = targetSizePx,
        )
        val above = visibleCurveHitCenter(
            point = NormalizedPoint(0.20f, -0.80f),
            widthPx = 1000f,
            heightPx = 800f,
            targetSizePx = targetSizePx,
        )
        val belowRight = visibleCurveHitCenter(
            point = NormalizedPoint(1.20f, 1.40f),
            widthPx = 1000f,
            heightPx = 800f,
            targetSizePx = targetSizePx,
        )
        val tinyCanvas = visibleCurveHitCenter(
            point = NormalizedPoint(0.20f, -0.80f),
            widthPx = 60f,
            heightPx = 40f,
            targetSizePx = targetSizePx,
        )

        assertEquals(500f, inside.x, 0.0f)
        assertEquals(400f, inside.y, 0.0f)
        assertEquals(200f, above.x, 0.0f)
        assertEquals(44f, above.y, 0.0f)
        assertEquals(956f, belowRight.x, 0.0f)
        assertEquals(756f, belowRight.y, 0.0f)
        assertEquals(30f, tinyCanvas.x, 0.0f)
        assertEquals(20f, tinyCanvas.y, 0.0f)
    }

    @Test
    fun exportVideoDimensionsUseVisualOrientationForRotatedPhoneVideo() {
        val landscape = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 0)
        val portrait90 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 90)
        val portrait270 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 270)
        val portrait450 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = 450)
        val portraitNegative90 = exportVideoDimensions(rawWidth = 1920, rawHeight = 1080, rotationDegrees = -90)

        assertEquals(1920, landscape.width)
        assertEquals(1080, landscape.height)
        assertEquals(1080, portrait90.width)
        assertEquals(1920, portrait90.height)
        assertEquals(1080, portrait270.width)
        assertEquals(1920, portrait270.height)
        assertEquals(1080, portrait450.width)
        assertEquals(1920, portrait450.height)
        assertEquals(1080, portraitNegative90.width)
        assertEquals(1920, portraitNegative90.height)
    }

    @Test
    fun exportMetadataParsingFallsBackInsteadOfFailingOnBadValues() {
        assertEquals(1920, metadataIntOrDefault("1920", defaultValue = 1280))
        assertEquals(1280, metadataIntOrDefault(null, defaultValue = 1280))
        assertEquals(1280, metadataIntOrDefault("not-a-number", defaultValue = 1280))
        assertEquals(-90, metadataIntOrDefault("-90", defaultValue = 0))
        assertEquals(60_000L, metadataLongOrDefault("60000", defaultValue = 3000L))
        assertEquals(3000L, metadataLongOrDefault(null, defaultValue = 3000L))
        assertEquals(3000L, metadataLongOrDefault("bad-duration", defaultValue = 3000L))

        val fallbackTarget = resolveExportVideoTarget(
            selected = ExportResolution.UHD_4K,
            rawWidth = metadataIntOrDefault("bad-width", defaultValue = 1280),
            rawHeight = metadataIntOrDefault("bad-height", defaultValue = 720),
            rotationDegrees = metadataIntOrDefault("bad-rotation", defaultValue = 0),
        )

        assertEquals(ExportResolution.HD_1080P, fallbackTarget.resolution)
        assertEquals(1920, fallbackTarget.dimensions.width)
        assertEquals(1080, fallbackTarget.dimensions.height)
    }

    @Test
    fun targetExportResolutionMatchesIosOrientationRules() {
        val landscape1080 = targetExportVideoDimensions(ExportResolution.HD_1080P, isPortrait = false)
        val portrait1080 = targetExportVideoDimensions(ExportResolution.HD_1080P, isPortrait = true)
        val landscape4k = targetExportVideoDimensions(ExportResolution.UHD_4K, isPortrait = false)
        val portrait4k = targetExportVideoDimensions(ExportResolution.UHD_4K, isPortrait = true)

        assertEquals(1920, landscape1080.width)
        assertEquals(1080, landscape1080.height)
        assertEquals(1080, portrait1080.width)
        assertEquals(1920, portrait1080.height)
        assertEquals(3840, landscape4k.width)
        assertEquals(2160, landscape4k.height)
        assertEquals(2160, portrait4k.width)
        assertEquals(3840, portrait4k.height)
    }

    @Test
    fun sourceResolutionGateMatchesIos4kAvailabilityRules() {
        assertTrue(sourceSupportsExportResolution(ExportResolution.HD_1080P, visualWidth = 720, visualHeight = 1280))
        assertTrue(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 3840, visualHeight = 2160))
        assertTrue(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 2160, visualHeight = 3840))
        assertFalse(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 3840, visualHeight = 1080))
        assertFalse(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 1080, visualHeight = 3840))
        assertFalse(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 3839, visualHeight = 2160))
        assertFalse(sourceSupportsExportResolution(ExportResolution.UHD_4K, visualWidth = 2160, visualHeight = 3839))
    }

    @Test
    fun selectedExportResolutionFallsBackWhenSourceCannotProvide4k() {
        assertEquals(
            ExportResolution.HD_1080P,
            coerceExportResolutionForSource(
                selected = ExportResolution.UHD_4K,
                visualWidth = 1080,
                visualHeight = 1920,
            ),
        )
        assertEquals(
            ExportResolution.UHD_4K,
            coerceExportResolutionForSource(
                selected = ExportResolution.UHD_4K,
                visualWidth = 2160,
                visualHeight = 3840,
            ),
        )
        assertEquals(
            ExportResolution.HD_1080P,
            coerceExportResolutionForSource(
                selected = ExportResolution.HD_1080P,
                visualWidth = 720,
                visualHeight = 1280,
            ),
        )
    }

    @Test
    fun exportSettingsWaitForTrustedVideoConfigBeforeOffering4k() {
        val unknown = exportSettingsUiState(
            selectedResolution = ExportResolution.UHD_4K,
            videoConfig = GolfCamVideoConfig(),
        )
        val trusted1080 = exportSettingsUiState(
            selectedResolution = ExportResolution.UHD_4K,
            videoConfig = GolfCamVideoConfig(
                durationMs = 18_000L,
                frameRate = 59.94,
                visualWidth = 1080,
                visualHeight = 1920,
            ),
        )
        val trusted4k = exportSettingsUiState(
            selectedResolution = ExportResolution.UHD_4K,
            videoConfig = GolfCamVideoConfig(
                durationMs = 18_000L,
                frameRate = 59.94,
                visualWidth = 2160,
                visualHeight = 3840,
            ),
        )

        assertEquals(ExportResolution.HD_1080P, unknown.selectedResolution)
        assertFalse(unknown.isSource4K)
        assertFalse(unknown.isPortrait)
        assertEquals("1920 × 1080", unknown.selectedResolution.displaySize(unknown.isPortrait))
        assertEquals(ExportResolution.HD_1080P, trusted1080.selectedResolution)
        assertFalse(trusted1080.isSource4K)
        assertTrue(trusted1080.isPortrait)
        assertEquals("1080 × 1920", trusted1080.selectedResolution.displaySize(trusted1080.isPortrait))
        assertEquals(ExportResolution.UHD_4K, trusted4k.selectedResolution)
        assertTrue(trusted4k.isSource4K)
        assertTrue(trusted4k.isPortrait)
        assertEquals("需要4K源视频", EXPORT_SETTINGS_4K_SOURCE_REQUIRED_REASON)
    }

    @Test
    fun overlayPreviewVisualSpecMatchesIosOverviewCardAndPanel() {
        assertEquals(18, PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP)
        assertEquals(1, PREVIEW_VIDEO_CARD_BORDER_WIDTH_DP)
        assertEquals(0.35f, PREVIEW_VIDEO_CARD_BORDER_ALPHA, 0f)
        assertEquals(16, PREVIEW_VIDEO_CARD_SHADOW_RADIUS_DP)
        assertEquals(190, PREVIEW_BOTTOM_PANEL_HEIGHT_DP)
    }

    @Test
    fun exportTargetUsesSourceMetadataVisualOrientationInsteadOfStalePreviewState() {
        val rotated4kPortrait = resolveExportVideoTarget(
            selected = ExportResolution.UHD_4K,
            rawWidth = 3840,
            rawHeight = 2160,
            rotationDegrees = 90,
        )
        assertEquals(ExportResolution.UHD_4K, rotated4kPortrait.resolution)
        assertTrue(rotated4kPortrait.isPortrait)
        assertEquals(2160, rotated4kPortrait.visualDimensions.width)
        assertEquals(3840, rotated4kPortrait.visualDimensions.height)
        assertEquals(2160, rotated4kPortrait.dimensions.width)
        assertEquals(3840, rotated4kPortrait.dimensions.height)

        val rotatedNon4kPortrait = resolveExportVideoTarget(
            selected = ExportResolution.UHD_4K,
            rawWidth = 1920,
            rawHeight = 1080,
            rotationDegrees = 90,
        )
        assertEquals(ExportResolution.HD_1080P, rotatedNon4kPortrait.resolution)
        assertTrue(rotatedNon4kPortrait.isPortrait)
        assertEquals(1080, rotatedNon4kPortrait.dimensions.width)
        assertEquals(1920, rotatedNon4kPortrait.dimensions.height)
    }

    @Test
    fun targetExportBitrateMatchesIosHdrVideoProcessorResolutionBands() {
        val landscape1080 = targetExportVideoDimensions(ExportResolution.HD_1080P, isPortrait = false)
        val portrait1080 = targetExportVideoDimensions(ExportResolution.HD_1080P, isPortrait = true)
        val landscape4k = targetExportVideoDimensions(ExportResolution.UHD_4K, isPortrait = false)
        val portrait4k = targetExportVideoDimensions(ExportResolution.UHD_4K, isPortrait = true)

        assertEquals(15_000_000, targetExportVideoBitrate(landscape1080))
        assertEquals(15_000_000, targetExportVideoBitrate(portrait1080))
        assertEquals(30_000_000, targetExportVideoBitrate(landscape4k))
        assertEquals(30_000_000, targetExportVideoBitrate(portrait4k))
    }

    @Test
    fun exportedFileOpensPreviewBeforeGallerySave() {
        val emptyFile = File.createTempFile("golfcam-empty-preview", ".mp4")
        val validFile = File.createTempFile("golfcam-valid-preview", ".mp4")
        try {
            validFile.writeBytes(byteArrayOf(0x10, 0x20))

            assertFalse(shouldOpenExportPreview(null))
            assertFalse(shouldOpenExportPreview(File("GolfSwing_preview.mp4")))
            assertFalse(shouldOpenExportPreview(emptyFile))
            assertTrue(shouldOpenExportPreview(validFile))
        } finally {
            deleteExportPreviewTempFile(emptyFile)
            deleteExportPreviewTempFile(validFile)
        }
    }

    @Test
    fun exportPreviewCannotDismissWhileGallerySaveIsRunning() {
        assertTrue(shouldAllowExportPreviewDismiss(saving = false))
        assertFalse(shouldAllowExportPreviewDismiss(saving = true))
    }

    @Test
    fun exportPreviewSystemBackUsesSameSavingGateAsCloseButton() {
        assertTrue(shouldAllowExportPreviewDismiss(saving = false))
        assertFalse(shouldAllowExportPreviewDismiss(saving = true))
    }

    @Test
    fun exportPreviewSaveButtonWaitsForSavingOrFailureMessageToClear() {
        val emptyFile = File.createTempFile("golfcam-empty-save-preview", ".mp4")
        val validFile = File.createTempFile("golfcam-valid-save-preview", ".mp4")
        try {
            validFile.writeBytes(byteArrayOf(0x10, 0x20))

            assertTrue(shouldAllowExportSave(saving = false, saveMessage = null, previewFile = validFile))
            assertFalse(shouldAllowExportSave(saving = true, saveMessage = null, previewFile = validFile))
            assertFalse(
                shouldAllowExportSave(
                    saving = false,
                    saveMessage = "保存失败：权限被拒绝",
                    previewFile = validFile,
                ),
            )
            assertFalse(
                shouldAllowExportSave(
                    saving = true,
                    saveMessage = "保存失败：权限被拒绝",
                    previewFile = validFile,
                ),
            )
            assertFalse(shouldAllowExportSave(saving = false, saveMessage = null, previewFile = null))
            assertFalse(shouldAllowExportSave(saving = false, saveMessage = null, previewFile = File("missing-preview.mp4")))
            assertFalse(shouldAllowExportSave(saving = false, saveMessage = null, previewFile = emptyFile))
        } finally {
            deleteExportPreviewTempFile(emptyFile)
            deleteExportPreviewTempFile(validFile)
        }
    }

    @Test
    fun exportPreviewSaveButtonVisualStateUsesSameGateAsSaveAction() {
        val validFile = File.createTempFile("golfcam-valid-save-alpha", ".mp4")
        try {
            validFile.writeBytes(byteArrayOf(0x10, 0x20))

            assertEquals(
                1f,
                exportPreviewSaveButtonAlpha(saving = false, saveMessage = null, previewFile = validFile),
                0.0f,
            )
            assertEquals(
                0.42f,
                exportPreviewSaveButtonAlpha(saving = true, saveMessage = null, previewFile = validFile),
                0.0f,
            )
            assertEquals(
                0.42f,
                exportPreviewSaveButtonAlpha(
                    saving = false,
                    saveMessage = "保存失败：权限被拒绝",
                    previewFile = validFile,
                ),
                0.0f,
            )
            assertEquals(
                0.42f,
                exportPreviewSaveButtonAlpha(saving = false, saveMessage = null, previewFile = null),
                0.0f,
            )
        } finally {
            deleteExportPreviewTempFile(validFile)
        }
    }

    @Test
    fun exportAndSaveFailureMessagesKeepReadableFallbacks() {
        assertEquals("导出失败：视频导出失败，请稍后重试", exportFailureMessage(RuntimeException()))
        assertEquals("导出失败：视频导出失败，请稍后重试", exportFailureMessage(RuntimeException("   ")))
        assertEquals("导出失败：当前设备暂时无法完成视频编码，请降低清晰度后重试", exportFailureMessage(RuntimeException("编码器不可用")))
        assertEquals("保存失败：保存到相册失败，请稍后重试", exportSaveFailureMessage(RuntimeException()))
        assertEquals("保存失败：保存到相册失败，请稍后重试", exportSaveFailureMessage(RuntimeException("\n\t")))
        assertEquals("保存失败：系统相册暂时不可写，请检查存储空间或权限后重试", exportSaveFailureMessage(RuntimeException("权限被拒绝")))
    }

    @Test
    fun exportSaveSuccessClosesPreviewAndOverviewLikeIosOnSaveComplete() {
        val disposition = exportSaveSuccessDisposition()

        assertTrue(disposition.markExportComplete)
        assertTrue(disposition.clearPreviewFile)
        assertTrue(disposition.clearSaveMessage)
        assertTrue(disposition.closeOverview)
    }

    @Test
    fun previewBottomPanelKeepsDeferredIosEntriesDisabled() {
        val idle = previewPanelActionStates(exporting = false, exportDone = false)

        assertEquals(
            listOf(
                PreviewPanelAction.FLIGHT_PATH,
                PreviewPanelAction.CLUB_PATH,
                PreviewPanelAction.TRIM,
                PreviewPanelAction.EXPORT,
            ),
            idle.map { it.action },
        )
        assertTrue(idle.first { it.action == PreviewPanelAction.FLIGHT_PATH }.enabled)
        assertFalse(idle.first { it.action == PreviewPanelAction.FLIGHT_PATH }.deferredBadge)
        assertFalse(idle.first { it.action == PreviewPanelAction.CLUB_PATH }.enabled)
        assertTrue(idle.first { it.action == PreviewPanelAction.CLUB_PATH }.deferredBadge)
        assertFalse(idle.first { it.action == PreviewPanelAction.TRIM }.enabled)
        assertTrue(idle.first { it.action == PreviewPanelAction.TRIM }.deferredBadge)
        assertTrue(idle.first { it.action == PreviewPanelAction.EXPORT }.enabled)
        assertFalse(idle.first { it.action == PreviewPanelAction.EXPORT }.deferredBadge)

        assertFalse(
            previewPanelActionStates(
                exporting = false,
                exportDone = false,
                videoConfigTrusted = false,
            ).first { it.action == PreviewPanelAction.EXPORT }.enabled,
        )
        assertFalse(previewPanelActionStates(exporting = true, exportDone = false).first { it.action == PreviewPanelAction.EXPORT }.enabled)
        assertFalse(previewPanelActionStates(exporting = false, exportDone = true).first { it.action == PreviewPanelAction.EXPORT }.enabled)
    }

    @Test
    fun previewTimeLabelUsesStableSecondsFormat() {
        assertEquals("0.0s", formatPreviewTime(-100L))
        assertEquals("1.2s", formatPreviewTime(1_234L))
    }

    @Test
    fun exportPreviewTempFileIsCleanedWhenPreviewIsDismissedOrSaved() {
        val tempFile = File.createTempFile("golfcam-export-preview", ".mp4")
        assertTrue(tempFile.exists())

        assertTrue(deleteExportPreviewTempFile(tempFile))
        assertFalse(tempFile.exists())
        assertFalse(deleteExportPreviewTempFile(tempFile))
        assertFalse(deleteExportPreviewTempFile(null))
    }

    @Test
    fun exportPreviewFileMustExistAndBeNonEmptyBeforeGallerySave() {
        val missingFile = File("missing-golfcam-export-preview.mp4")
        val emptyFile = File.createTempFile("golfcam-empty-export-preview", ".mp4")
        val nonEmptyFile = File.createTempFile("golfcam-nonempty-export-preview", ".mp4")
        try {
            nonEmptyFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))

            assertFalse(isValidExportPreviewFile(null))
            assertFalse(isValidExportPreviewFile(missingFile))
            assertFalse(isValidExportPreviewFile(emptyFile))
            assertTrue(isValidExportPreviewFile(nonEmptyFile))
        } finally {
            deleteExportPreviewTempFile(emptyFile)
            deleteExportPreviewTempFile(nonEmptyFile)
        }
    }

    @Test
    fun mediaStorePendingPublishMustSucceedOnAndroidQAndLater() {
        assertTrue(mediaStorePendingPublishSucceeded(sdkInt = 28, updatedRows = 0))
        assertTrue(mediaStorePendingPublishSucceeded(sdkInt = 29, updatedRows = 1))
        assertFalse(mediaStorePendingPublishSucceeded(sdkInt = 29, updatedRows = 0))
        assertFalse(mediaStorePendingPublishSucceeded(sdkInt = 35, updatedRows = -1))
    }

    @Test
    fun failedGallerySaveCleansInsertedMediaStoreRowOnlyBeforeSuccess() {
        assertFalse(shouldCleanupFailedGalleryInsert(insertedUriPresent = false, saveSucceeded = false))
        assertTrue(shouldCleanupFailedGalleryInsert(insertedUriPresent = true, saveSucceeded = false))
        assertFalse(shouldCleanupFailedGalleryInsert(insertedUriPresent = true, saveSucceeded = true))
    }

    @Test
    fun exportSaveFailuresKeepPreviewOpenWithUserVisibleMessage() {
        assertEquals(
            "保存失败：系统相册暂时不可写，请检查存储空间或权限后重试",
            exportSaveFailureMessage(IllegalStateException("权限被拒绝")),
        )
        assertEquals("保存失败：保存到相册失败，请稍后重试", exportSaveFailureMessage(Throwable()))
    }

    @Test
    fun exportFailuresReturnToPreviewWithRetryableMessage() {
        assertEquals(
            "导出失败：当前设备暂时无法完成视频编码，请降低清晰度后重试",
            exportFailureMessage(IllegalStateException("编码器不可用")),
        )
        assertEquals("导出失败：视频导出失败，请稍后重试", exportFailureMessage(Throwable()))
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
                metadataRotationDegrees = 90,
                frameWidth = 2160,
                frameHeight = 3840,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
        assertEquals(
            0,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 270,
                frameWidth = 3840,
                frameHeight = 2160,
                exportWidth = 1920,
                exportHeight = 1080,
            ),
        )
        assertEquals(
            90,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 450,
                frameWidth = 1920,
                frameHeight = 1080,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
        assertEquals(
            270,
            effectiveExportFrameRotation(
                metadataRotationDegrees = -90,
                frameWidth = 1920,
                frameHeight = 1080,
                exportWidth = 1080,
                exportHeight = 1920,
            ),
        )
        assertEquals(
            0,
            effectiveExportFrameRotation(
                metadataRotationDegrees = 45,
                frameWidth = 1920,
                frameHeight = 1080,
                exportWidth = 1920,
                exportHeight = 1080,
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
    fun exportAudioCopyBufferUsesStableFallbackAndMinimumSize() {
        assertEquals(256 * 1024, audioCopyBufferSize(null))
        assertEquals(16 * 1024, audioCopyBufferSize(0))
        assertEquals(16 * 1024, audioCopyBufferSize(4 * 1024))
        assertEquals(96 * 1024, audioCopyBufferSize(96 * 1024))
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
    fun previewPlaybackClipUsesSameTrimClampAsExportWhenSourceDurationIsKnown() {
        val normal = previewPlaybackClip(
            trimStartMs = 10_000L,
            trimEndMs = 25_000L,
            sourceDurationMs = 60_000L,
        )
        val clamped = previewPlaybackClip(
            trimStartMs = 70_000L,
            trimEndMs = 90_000L,
            sourceDurationMs = 60_000L,
        )
        val unknownDuration = previewPlaybackClip(
            trimStartMs = 10_000L,
            trimEndMs = null,
            sourceDurationMs = 1L,
            playerDurationMs = 15_000L,
        )

        assertEquals(10_000L, normal.startMs)
        assertEquals(25_000L, normal.endMs)
        assertEquals(15_000L, normal.durationMs)
        assertEquals(59_999L, clamped.startMs)
        assertEquals(60_000L, clamped.endMs)
        assertEquals(1L, clamped.durationMs)
        assertEquals(10_000L, unknownDuration.startMs)
        assertEquals(null, unknownDuration.endMs)
        assertEquals(15_000L, unknownDuration.durationMs)

        val extremeUnknownDuration = previewPlaybackClip(
            trimStartMs = Long.MAX_VALUE,
            trimEndMs = Long.MAX_VALUE,
            sourceDurationMs = null,
            playerDurationMs = null,
        )
        assertEquals(Long.MAX_VALUE, extremeUnknownDuration.startMs)
        assertEquals(Long.MAX_VALUE, extremeUnknownDuration.endMs)
        assertEquals(1L, extremeUnknownDuration.durationMs)
    }

    @Test
    fun previewTimelineMarkersUseClippedPlaybackTimebase() {
        val clip = previewPlaybackClip(
            trimStartMs = 70_000L,
            trimEndMs = 90_000L,
            sourceDurationMs = 60_000L,
        )

        assertEquals(0L, previewImpactTimeMs(absoluteImpactTimeMs = 12_000L, clipStartMs = clip.startMs))
        assertEquals(Long.MAX_VALUE, previewImpactTimeMs(absoluteImpactTimeMs = Long.MAX_VALUE, clipStartMs = Long.MIN_VALUE))
        assertEquals(1f, previewProgressFraction(positionMs = 2_000L, durationMs = clip.durationMs), 0f)
        assertEquals(0.5f, previewProgressFraction(positionMs = 5_000L, durationMs = 10_000L), 0f)
        assertEquals(0f, previewProgressFraction(positionMs = -200L, durationMs = 10_000L), 0f)
    }

    @Test
    fun trackStylePreviewTimingUsesSharedSafeFrameClock() {
        val normal = previewTrajectoryTiming(impactTimeMs = 10_000L, flightTimeSec = 4.5, fps = 59.94)
        val malformed = previewTrajectoryTiming(impactTimeMs = 10_000L, flightTimeSec = Double.NaN, fps = Double.POSITIVE_INFINITY)
        val extreme = previewTrajectoryTiming(impactTimeMs = Long.MAX_VALUE - 5L, flightTimeSec = 4.5, fps = 0.0)

        assertEquals(10_000L, normal.impactTimeMs)
        assertEquals(14_500L, normal.landingTimeMs)
        assertEquals(59.94, normal.fps, 0.0)
        assertEquals(13_000L, malformed.landingTimeMs)
        assertEquals(30.0, malformed.fps, 0.0)
        assertEquals(Long.MAX_VALUE, extreme.landingTimeMs)
        assertEquals(1.0, extreme.fps, 0.0)
    }

    @Test
    fun previewProgressDisplayFractionRejectsMalformedProgressBeforeUiLayout() {
        assertEquals(0f, previewProgressDisplayFraction(Float.NaN), 0f)
        assertEquals(0f, previewProgressDisplayFraction(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, previewProgressDisplayFraction(Float.NEGATIVE_INFINITY), 0f)
        assertEquals(0f, previewProgressDisplayFraction(-0.25f), 0f)
        assertEquals(0.5f, previewProgressDisplayFraction(0.5f), 0f)
        assertEquals(1f, previewProgressDisplayFraction(1.25f), 0f)
    }

    @Test
    fun exportProgressReportsCompletedFrameFractionThroughFinalFrame() {
        assertEquals(0f, exportProgressFraction(completedFrames = 0, totalFrames = 120), 0f)
        assertEquals(1f / 120f, exportProgressFraction(completedFrames = 1, totalFrames = 120), 1e-6f)
        assertEquals(0.5f, exportProgressFraction(completedFrames = 60, totalFrames = 120), 0f)
        assertEquals(1f, exportProgressFraction(completedFrames = 120, totalFrames = 120), 0f)
        assertEquals(1f, exportProgressFraction(completedFrames = 121, totalFrames = 120), 0f)
        assertEquals(0f, exportProgressFraction(completedFrames = -1, totalFrames = 120), 0f)
        assertEquals(0f, exportProgressFraction(completedFrames = 1, totalFrames = 0), 0f)
    }

    @Test
    fun exportFrameCountCoversFullClippedTimeline() {
        assertEquals(30, exportTotalFrameCount(durationMs = 1_000L, fps = 30))
        assertEquals(31, exportTotalFrameCount(durationMs = 1_001L, fps = 30))
        assertEquals(300, exportTotalFrameCount(durationMs = 10_010L, fps = 29.97))
        assertEquals(1, exportTotalFrameCount(durationMs = 1L, fps = 30))
        assertEquals(1, exportTotalFrameCount(durationMs = 0L, fps = 0))
        assertEquals(30, exportTotalFrameCount(durationMs = 1_000L, fps = Double.NaN))
        assertEquals(Int.MAX_VALUE, exportTotalFrameCount(durationMs = Long.MAX_VALUE, fps = 120.0))
    }

    @Test
    fun exportSourceFrameTimeStaysInsideHalfOpenTrimRange() {
        assertEquals(
            10_000_000L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 11_000L, frameIndex = 0, fps = 30.0),
        )
        assertEquals(
            10_033_333L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 11_000L, frameIndex = 1, fps = 30.0),
        )
        assertEquals(
            10_999_999L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 11_000L, frameIndex = 999, fps = 30.0),
        )
        assertEquals(
            10_000_000L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 11_000L, frameIndex = -2, fps = 30.0),
        )
        assertEquals(
            10_000_999L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 9_000L, frameIndex = 10, fps = 60.0),
        )
        assertEquals(
            10_033_333L,
            exportSourceFrameTimeUs(trimStartMs = 10_000L, trimEndMs = 11_000L, frameIndex = 1, fps = Double.NaN),
        )
        assertEquals(Long.MAX_VALUE, exportMillisecondsToMicroseconds(Long.MAX_VALUE))
        assertEquals(0L, exportMillisecondsToMicroseconds(Long.MIN_VALUE))
        assertEquals(
            Long.MAX_VALUE,
            exportSourceFrameTimeUs(
                trimStartMs = Long.MAX_VALUE,
                trimEndMs = Long.MAX_VALUE,
                frameIndex = Int.MAX_VALUE,
                fps = 1.0,
            ),
        )
    }

    @Test
    fun exportFramePresentationTimeUsesMicrosecondPrecisionForRealFrameRate() {
        assertEquals(0L, exportFramePresentationTimeUs(frameIndex = 0, fps = 60))
        assertEquals(16_666L, exportFramePresentationTimeUs(frameIndex = 1, fps = 60))
        assertEquals(33_333L, exportFramePresentationTimeUs(frameIndex = 2, fps = 60))
        assertEquals(983_333L, exportFramePresentationTimeUs(frameIndex = 59, fps = 60))
        assertEquals(1_000_000L, exportFramePresentationTimeUs(frameIndex = 30, fps = 30))
        assertEquals(10_010_010L, exportFramePresentationTimeUs(frameIndex = 300, fps = 29.97))
        assertEquals(0L, exportFramePresentationTimeUs(frameIndex = -5, fps = 60))
        assertEquals(33_333L, exportFramePresentationTimeUs(frameIndex = 1, fps = Double.NaN))

        val delayedEncoderOutputPts = (0..3).map { sampleIndex ->
            exportFramePresentationTimeUs(frameIndex = sampleIndex, fps = 60)
        }
        assertEquals(listOf(0L, 16_666L, 33_333L, 50_000L), delayedEncoderOutputPts)
        assertEquals(Long.MIN_VALUE / 1000.0, exportTrajectoryElapsedSeconds(outputFrameTimeMs = Long.MIN_VALUE, previewImpactTimeMs = Long.MAX_VALUE), 1.0)
        assertEquals(Long.MAX_VALUE / 1000.0, exportTrajectoryElapsedSeconds(outputFrameTimeMs = Long.MAX_VALUE, previewImpactTimeMs = Long.MIN_VALUE), 1.0)
        assertEquals(-Long.MAX_VALUE / 1_000_000.0, exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs = 0L, previewImpactTimeMs = Long.MAX_VALUE), 1.0)
    }

    @Test
    fun audioTrimPresentationTimeUsesHalfOpenClipRangeAndZeroOrigin() {
        val trimStartUs = 10_000_000L
        val trimEndUs = 25_000_000L
        val trimRangeUs = exportTrimRangeUs(ExportTrimRange(startMs = 10_000L, endMs = 25_000L))
        val extremeTrimRangeUs = exportTrimRangeUs(ExportTrimRange(startMs = Long.MAX_VALUE - 1L, endMs = Long.MAX_VALUE))

        assertEquals(trimStartUs, trimRangeUs.startUs)
        assertEquals(trimEndUs, trimRangeUs.endUs)
        assertEquals(Long.MAX_VALUE, extremeTrimRangeUs.startUs)
        assertEquals(Long.MAX_VALUE, extremeTrimRangeUs.endUs)
        assertEquals(null, trimmedAudioPresentationTimeUs(9_999_999L, trimStartUs, trimEndUs))
        assertEquals(0L, trimmedAudioPresentationTimeUs(10_000_000L, trimStartUs, trimEndUs))
        assertEquals(5_000_000L, trimmedAudioPresentationTimeUs(15_000_000L, trimStartUs, trimEndUs))
        assertEquals(null, trimmedAudioPresentationTimeUs(25_000_000L, trimStartUs, trimEndUs))
        assertEquals(Long.MAX_VALUE, trimmedAudioPresentationTimeUs(0L, Long.MIN_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun audioMuxerSamplesRequirePositiveSizeAndHalfOpenTrimPts() {
        val trimStartUs = 10_000_000L
        val trimEndUs = 25_000_000L

        assertEquals(null, audioMuxerPresentationTimeUs(9_999_999L, sampleSize = 1024, trimStartUs, trimEndUs))
        assertEquals(0L, audioMuxerPresentationTimeUs(10_000_000L, sampleSize = 1024, trimStartUs, trimEndUs))
        assertEquals(5_000_000L, audioMuxerPresentationTimeUs(15_000_000L, sampleSize = 1024, trimStartUs, trimEndUs))
        assertEquals(null, audioMuxerPresentationTimeUs(25_000_000L, sampleSize = 1024, trimStartUs, trimEndUs))
        assertEquals(null, audioMuxerPresentationTimeUs(15_000_000L, sampleSize = 0, trimStartUs, trimEndUs))
        assertEquals(null, audioMuxerPresentationTimeUs(15_000_000L, sampleSize = -1, trimStartUs, trimEndUs))
    }

    @Test
    fun audioCopySkipsEmptySamplesButEndsOnExtractorEos() {
        assertEquals(AudioSampleCopyAction.WRITE, audioSampleCopyAction(1024))
        assertEquals(AudioSampleCopyAction.SKIP, audioSampleCopyAction(0))
        assertEquals(AudioSampleCopyAction.END, audioSampleCopyAction(-1))
    }

    @Test
    fun audioMuxerSampleFlagsTranslateExtractorConstantsForBufferInfo() {
        assertEquals(0, audioMuxerSampleFlags(0))
        assertEquals(
            MediaCodec.BUFFER_FLAG_KEY_FRAME,
            audioMuxerSampleFlags(MediaExtractor.SAMPLE_FLAG_SYNC),
        )
        assertEquals(
            MediaCodec.BUFFER_FLAG_PARTIAL_FRAME,
            audioMuxerSampleFlags(MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME),
        )
        assertEquals(
            MediaCodec.BUFFER_FLAG_KEY_FRAME or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME,
            audioMuxerSampleFlags(MediaExtractor.SAMPLE_FLAG_SYNC or MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME),
        )
        assertEquals(0, audioMuxerSampleFlags(MediaExtractor.SAMPLE_FLAG_ENCRYPTED))
    }

    @Test
    fun audioImpactDetectionKeepsDecodedPcmInsideTrimWindowOnly() {
        val trimStartUs = 10_000_000L
        val trimEndUs = 25_000_000L

        assertEquals(10_000_000L, audioSamplePresentationTimeUs(10_000_000L, frameIndex = 0, sampleRate = 48_000))
        assertEquals(10_000_500L, audioSamplePresentationTimeUs(10_000_000L, frameIndex = 24, sampleRate = 48_000))
        assertFalse(shouldKeepDecodedAudioSample(9_999_999L, trimStartUs, trimEndUs))
        assertTrue(shouldKeepDecodedAudioSample(10_000_000L, trimStartUs, trimEndUs))
        assertTrue(shouldKeepDecodedAudioSample(24_999_999L, trimStartUs, trimEndUs))
        assertFalse(shouldKeepDecodedAudioSample(25_000_000L, trimStartUs, trimEndUs))
        assertEquals(10.0, audioCandidateBaseTimeSeconds(10.0, firstKeptSampleTimeUs = null), 0.0)
        assertEquals(10.0005, audioCandidateBaseTimeSeconds(10.0, firstKeptSampleTimeUs = 10_000_500L), 0.0)
        assertEquals(10.0, audioCandidateBaseTimeSeconds(10.0, firstKeptSampleTimeUs = 9_999_000L), 0.0)
        assertEquals(0.0, audioCandidateBaseTimeSeconds(Double.NaN, firstKeptSampleTimeUs = null), 0.0)
        assertEquals(0.0, audioCandidateBaseTimeSeconds(Double.NEGATIVE_INFINITY, firstKeptSampleTimeUs = null), 0.0)
    }

    @Test
    fun exportTrajectoryElapsedUsesClippedPreviewTimelineLikeIos() {
        val sourceTrimStartMs = 10_000L
        val absoluteImpactMs = 12_000L
        val previewImpactMs = absoluteImpactMs - sourceTrimStartMs

        assertEquals(-2.0, exportTrajectoryElapsedSeconds(outputFrameTimeMs = 0L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(0.0, exportTrajectoryElapsedSeconds(outputFrameTimeMs = 2_000L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(1.5, exportTrajectoryElapsedSeconds(outputFrameTimeMs = 3_500L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(-2.0, exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs = 0L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(0.0, exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs = 2_000_000L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(1.5, exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs = 3_500_000L, previewImpactTimeMs = previewImpactMs), 0.0)
        assertEquals(
            16_666.0 / 1_000_000.0,
            exportTrajectoryElapsedSecondsFromUs(
                outputFrameTimeUs = exportFramePresentationTimeUs(frameIndex = 121, fps = 60),
                previewImpactTimeMs = previewImpactMs,
            ),
            1e-9,
        )
    }

    @Test
    fun unifiedPreviewElapsedUsesCurrentPlayerPositionEvenWhenPaused() {
        val impactTimeMs = 12_000L

        assertEquals(0L, unifiedPreviewElapsedMs(currentPositionMs = 11_500L, impactTimeMs = impactTimeMs))
        assertEquals(0L, unifiedPreviewElapsedMs(currentPositionMs = 12_000L, impactTimeMs = impactTimeMs))
        assertEquals(1_250L, unifiedPreviewElapsedMs(currentPositionMs = 13_250L, impactTimeMs = impactTimeMs))
    }

    @Test
    fun frameTimelineUsesActualVideoFrameRateForEditingSteps() {
        assertEquals(18, GolfCamFrameTimeline.frameIndexAt(relativeSeconds = 0.30, frameRate = 60.0))
        assertEquals(9, GolfCamFrameTimeline.frameIndexAt(relativeSeconds = 0.30, frameRate = 30.0))
        assertEquals(1250L, GolfCamFrameTimeline.timeMsForFrame(trimStartMs = 1000L, frameIndex = 15, frameRate = 60.0))
        assertEquals(540, GolfCamFrameTimeline.totalFrames(durationMs = 9000L, frameRate = 60.0))
        assertEquals(270, GolfCamFrameTimeline.totalFrames(durationMs = 9000L, frameRate = 30.0))
        assertEquals(31, GolfCamFrameTimeline.totalFrames(durationMs = 1001L, frameRate = 30.0))
        assertEquals(300, GolfCamFrameTimeline.totalFrames(durationMs = 10_010L, frameRate = 29.97))
    }

    @Test
    fun frameTimelineUsesHalfOpenTrimRangeForLastImpactAnchor() {
        assertEquals(359, GolfCamFrameTimeline.lastFrameIndexForRange(startMs = 1_000L, endMs = 7_000L, frameRate = 60.0))
        assertEquals(6_983L, GolfCamFrameTimeline.lastFrameTimeMsForRange(startMs = 1_000L, endMs = 7_000L, frameRate = 60.0))
        assertEquals(1_000L, GolfCamFrameTimeline.lastFrameTimeMsForRange(startMs = 1_000L, endMs = 1_001L, frameRate = 120.0))
    }

    @Test
    fun impactFrameThumbnailTimelineCoversLastValidFrameWithoutSamplingTrimEnd() {
        val frames = impactFrameThumbnailFrameIndices(totalFrames = 360, maxThumbs = 60)

        assertEquals(60, frames.size)
        assertEquals(0, frames.first())
        assertEquals(359, frames.last())
        assertTrue(frames.zipWithNext().all { (a, b) -> b > a })
        assertEquals(
            1,
            nearestImpactFrameThumbnailIndex(
                frameIndex = Int.MAX_VALUE,
                totalFrames = Int.MAX_VALUE,
                thumbnailFrames = listOf(Int.MIN_VALUE, Int.MAX_VALUE),
            ),
        )
        assertEquals(
            1_000L,
            impactFrameThumbnailSampleTimeMs(
                trimStartMs = 1_000L,
                trimEndMs = 7_000L,
                frameRate = 60.0,
                thumbIndex = 0,
                maxThumbs = 60,
            ),
        )
        assertEquals(
            6_983L,
            impactFrameThumbnailSampleTimeMs(
                trimStartMs = 1_000L,
                trimEndMs = 7_000L,
                frameRate = 60.0,
                thumbIndex = 59,
                maxThumbs = 60,
            ),
        )
    }

    @Test
    fun thumbnailPreloadCacheHelpersMatchIosCacheFirstFillPosture() {
        val sampleTimes = golfCamThumbnailSampleTimesMs(startMs = 1_000L, endMs = 7_000L, maxThumbnails = 5)
        val loadOrder = golfCamThumbnailLoadOrder(sampleTimes, priorityTimeMs = 4_200L)
        val startPriorityOrder = golfCamThumbnailLoadOrder(sampleTimes, priorityTimeMs = 1_000L)
        val endPriorityOrder = golfCamThumbnailLoadOrder(sampleTimes, priorityTimeMs = 7_000L)
        val filled = golfCamFillMissingThumbnailSlotsFromNearest(listOf<String?>(null, "a", null, null, "b"))

        assertEquals(listOf(1_000L, 2_500L, 4_000L, 5_500L, 7_000L), sampleTimes)
        assertEquals(listOf(2, 3, 1, 4, 0), loadOrder)
        assertEquals(0, startPriorityOrder.first())
        assertEquals(4, endPriorityOrder.first())
        assertTrue(startPriorityOrder != endPriorityOrder)
        assertEquals(2, golfCamThumbnailPriorityIndex(sampleTimes, priorityTimeMs = 4_200L))
        assertEquals(2, golfCamThumbnailPriorityIndex(sampleTimes, priorityTimeMs = 4_700L))
        assertEquals(3, golfCamThumbnailPriorityIndex(sampleTimes, priorityTimeMs = 4_800L))
        assertEquals(-1, golfCamThumbnailPriorityIndex(emptyList(), priorityTimeMs = 4_200L))
        assertEquals(4_000L, golfCamThumbnailPrioritySampleTimeMs(sampleTimes, priorityTimeMs = 4_200L))
        assertEquals(4_000L, golfCamThumbnailPrioritySampleTimeMs(sampleTimes, priorityTimeMs = 4_700L))
        assertEquals(5_500L, golfCamThumbnailPrioritySampleTimeMs(sampleTimes, priorityTimeMs = 4_800L))
        assertEquals(4_200L, golfCamThumbnailPrioritySampleTimeMs(emptyList(), priorityTimeMs = 4_200L))
        assertEquals(0L, golfCamThumbnailPrioritySampleTimeMs(emptyList(), priorityTimeMs = -20L))
        assertEquals(listOf("a", "a", "a", "b", "b"), filled)
        assertTrue(golfCamThumbnailSampleTimesMs(0L, 10_000L, maxThumbnails = 120).size <= 120)
        assertEquals(120, golfCamThumbnailSampleTimesMs(0L, 10_000L, maxThumbnails = Int.MAX_VALUE).size)
        assertEquals(emptyList<Long>(), golfCamThumbnailSampleTimesMs(0L, 10_000L, maxThumbnails = 0))
        assertEquals(listOf(0L, 500L, 1_000L), golfCamThumbnailSampleTimesMs(startMs = -1_000L, endMs = 1_000L, maxThumbnails = 3))
        assertEquals(
            listOf(Long.MAX_VALUE),
            golfCamThumbnailSampleTimesMs(startMs = Long.MAX_VALUE, endMs = Long.MAX_VALUE, maxThumbnails = 3),
        )
        assertEquals(emptyList<Long>(), golfCamVideoPreloadSampleTimesMs(durationMs = 0L))
        assertEquals(listOf(0L), golfCamVideoPreloadSampleTimesMs(durationMs = 1L, maxThumbnails = 5))
        assertEquals(120, golfCamVideoPreloadSampleTimesMs(10_000L, maxThumbnails = Int.MAX_VALUE).size)
        assertTrue(golfCamVideoPreloadSampleTimesMs(10_000L).last() < 10_000L)
        assertEquals(120, GOLF_CAM_THUMBNAIL_PRELOAD_COUNT)
        assertEquals(60, GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT)
        assertTrue(
            GOLF_CAM_THUMBNAIL_CACHE_COUNT >=
                GOLF_CAM_THUMBNAIL_PRELOAD_COUNT + GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT + TRIM_THUMBNAIL_COUNT,
        )
        assertTrue(
            shouldStartVideoThumbnailPreload(
                exactCachedCount = 0,
                totalCount = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
                isPreloadActive = false,
            ),
        )
        assertFalse(
            shouldStartVideoThumbnailPreload(
                exactCachedCount = 0,
                totalCount = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
                isPreloadActive = true,
            ),
        )
        assertFalse(
            shouldStartVideoThumbnailPreload(
                exactCachedCount = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
                totalCount = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
                isPreloadActive = false,
            ),
        )
        assertFalse(
            shouldStartVideoThumbnailPreload(
                exactCachedCount = 0,
                totalCount = 0,
                isPreloadActive = false,
            ),
        )
        assertTrue(
            isVideoThumbnailPreloadActiveForCurrentVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = null,
                isPreloadActiveForVideo = true,
            ),
        )
        assertTrue(
            isVideoThumbnailPreloadActiveForCurrentVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-a",
                isPreloadActiveForVideo = true,
            ),
        )
        assertFalse(
            isVideoThumbnailPreloadActiveForCurrentVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-b",
                isPreloadActiveForVideo = true,
            ),
        )
        assertFalse(
            isVideoThumbnailPreloadActiveForCurrentVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-a",
                isPreloadActiveForVideo = false,
            ),
        )
        assertTrue(
            shouldDropStaleVideoThumbnailPreload(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-b",
                isPreloadActiveForVideo = true,
            ),
        )
        assertFalse(
            shouldDropStaleVideoThumbnailPreload(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-a",
                isPreloadActiveForVideo = true,
            ),
        )
        assertFalse(
            shouldDropStaleVideoThumbnailPreload(
                loadingVideoKey = "video-a",
                currentVideoKey = null,
                isPreloadActiveForVideo = true,
            ),
        )
        assertFalse(
            shouldDropStaleVideoThumbnailPreload(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-b",
                isPreloadActiveForVideo = false,
            ),
        )
        assertTrue(
            shouldClearActiveVideoThumbnailPreload(
                loadingGeneration = 3L,
                activeGeneration = 3L,
            ),
        )
        assertFalse(
            shouldClearActiveVideoThumbnailPreload(
                loadingGeneration = 1L,
                activeGeneration = 3L,
            ),
        )
        assertFalse(
            shouldClearActiveVideoThumbnailPreload(
                loadingGeneration = 1L,
                activeGeneration = null,
            ),
        )
        assertEquals(1L, nextThumbnailVideoGeneration(0L))
        assertEquals(1L, nextThumbnailVideoGeneration(Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, golfCamThumbnailTimeDistanceMs(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, golfCamThumbnailTimeDistanceMs(0L, Long.MAX_VALUE))
        assertEquals(
            listOf(2, 0, 1),
            golfCamThumbnailLoadOrder(
                sampleTimesMs = listOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE),
                priorityTimeMs = Long.MAX_VALUE,
            ),
        )
        assertEquals(0L, golfCamThumbnailRetrieverTimeUs(-1_000L))
        assertEquals(1_000_000L, golfCamThumbnailRetrieverTimeUs(1_000L))
        assertEquals(Long.MAX_VALUE / 1000L * 1000L, golfCamThumbnailRetrieverTimeUs(Long.MAX_VALUE))
    }

    @Test
    fun trimToImpactThumbnailPreloadMatchesIosSixtyFrameGate() {
        val sampleTimes = golfCamTrimImpactPreloadSampleTimesMs(
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            frameRate = 60.0,
        )
        val shortRangeSamples = golfCamTrimImpactPreloadSampleTimesMs(
            trimStartMs = 1_000L,
            trimEndMs = 1_100L,
            frameRate = 60.0,
        )

        assertEquals(60, sampleTimes.size)
        assertEquals(1_000L, sampleTimes.first())
        assertEquals(6_983L, sampleTimes.last())
        assertTrue(sampleTimes.all { it in 1_000L until 7_000L })
        assertEquals(6, shortRangeSamples.size)
        assertEquals(1_000L, shortRangeSamples.first())
        assertEquals(1_083L, shortRangeSamples.last())
        assertEquals(emptyList<Long>(), golfCamTrimImpactPreloadSampleTimesMs(1_000L, 7_000L, 60.0, maxThumbnails = 0))
        assertEquals(
            listOf(1_000L),
            golfCamTrimImpactPreloadSampleTimesMs(1_000L, 7_000L, 60.0, maxThumbnails = 1),
        )
        assertEquals(
            60,
            golfCamTrimImpactPreloadSampleTimesMs(
                trimStartMs = 1_000L,
                trimEndMs = 7_000L,
                frameRate = 60.0,
                maxThumbnails = Int.MAX_VALUE,
            ).size,
        )
    }

    @Test
    fun trimToImpactThumbnailPreloadProgressAndOverlayMatchIosGate() {
        assertTrue(shouldWaitForTrimImpactThumbnailPreload(exactCachedCount = 0, totalCount = 60))
        assertTrue(shouldWaitForTrimImpactThumbnailPreload(exactCachedCount = 59, totalCount = 60))
        assertFalse(shouldWaitForTrimImpactThumbnailPreload(exactCachedCount = 60, totalCount = 60))
        assertFalse(shouldWaitForTrimImpactThumbnailPreload(exactCachedCount = 0, totalCount = 0))
        assertTrue(
            shouldStartTrimImpactThumbnailPreloadGate(
                currentStep = GolfCamStep.TRIM,
                nextStep = GolfCamStep.IMPACT_FRAME,
                isGateRunning = false,
            ),
        )
        assertFalse(
            shouldStartTrimImpactThumbnailPreloadGate(
                currentStep = GolfCamStep.TRIM,
                nextStep = GolfCamStep.IMPACT_FRAME,
                isGateRunning = true,
            ),
        )
        assertFalse(
            shouldStartTrimImpactThumbnailPreloadGate(
                currentStep = GolfCamStep.IMPACT_FRAME,
                nextStep = GolfCamStep.IMPACT_POSITION,
                isGateRunning = false,
            ),
        )
        assertEquals(0f, golfCamTrimImpactPreloadProgressFraction(exactCachedCount = -4, totalCount = 60), 0f)
        assertEquals(0.5f, golfCamTrimImpactPreloadProgressFraction(exactCachedCount = 30, totalCount = 60), 0f)
        assertEquals(1f, golfCamTrimImpactPreloadProgressFraction(exactCachedCount = 90, totalCount = 60), 0f)
        assertEquals(1f, golfCamTrimImpactPreloadProgressFraction(exactCachedCount = 0, totalCount = 0), 0f)
        assertEquals(0.70f, THUMBNAIL_PRELOAD_OVERLAY_BACKGROUND_ALPHA, 0f)
        assertEquals(20, THUMBNAIL_PRELOAD_OVERLAY_CARD_CORNER_DP)
        assertEquals(60, THUMBNAIL_PRELOAD_OVERLAY_ICON_SIZE_DP)
        assertEquals(14, THUMBNAIL_PRELOAD_OVERLAY_CONTENT_SPACING_DP)
        assertEquals(18, THUMBNAIL_PRELOAD_OVERLAY_TITLE_FONT_SP)
        assertEquals(14, THUMBNAIL_PRELOAD_OVERLAY_SUBTITLE_FONT_SP)
        assertEquals(200, THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_WIDTH_DP)
        assertEquals(8, THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP)
        assertEquals(16, THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_TEXT_FONT_SP)
        assertEquals("0%", thumbnailPreloadProgressPercentText(-0.25f))
        assertEquals("50%", thumbnailPreloadProgressPercentText(0.5f))
        assertEquals("100%", thumbnailPreloadProgressPercentText(1.25f))
    }

    @Test
    fun thumbnailLoaderSkipsOnlyAlreadyDisplayedExactCacheCallbacks() {
        val alreadyDisplayedExactSamples = setOf(0L, 1_000L)

        assertFalse(shouldEmitCachedThumbnailCallback(0L, alreadyDisplayedExactSamples))
        assertFalse(shouldEmitCachedThumbnailCallback(-50L, alreadyDisplayedExactSamples))
        assertFalse(shouldEmitCachedThumbnailCallback(1_000L, alreadyDisplayedExactSamples))
        assertTrue(shouldEmitCachedThumbnailCallback(500L, alreadyDisplayedExactSamples))
        assertTrue(shouldEmitCachedThumbnailCallback(1_500L, alreadyDisplayedExactSamples))
        assertTrue(shouldEmitCachedThumbnailCallback(0L, emptySet()))
    }

    @Test
    fun thumbnailLoaderStopsStaleVideoTasksAfterVideoSwitch() {
        assertTrue(shouldContinueThumbnailLoadForVideo(loadingVideoKey = "video-a", currentVideoKey = null))
        assertTrue(shouldContinueThumbnailLoadForVideo(loadingVideoKey = "video-a", currentVideoKey = "video-a"))
        assertFalse(shouldContinueThumbnailLoadForVideo(loadingVideoKey = "video-a", currentVideoKey = "video-b"))
        assertTrue(
            shouldContinueThumbnailLoadForVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-a",
                loadingGeneration = 3L,
                currentGeneration = 3L,
            ),
        )
        assertFalse(
            shouldContinueThumbnailLoadForVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-a",
                loadingGeneration = 1L,
                currentGeneration = 3L,
            ),
        )
        assertFalse(
            shouldContinueThumbnailLoadForVideo(
                loadingVideoKey = "video-a",
                currentVideoKey = "video-b",
                loadingGeneration = 3L,
                currentGeneration = 3L,
            ),
        )
    }

    @Test
    fun entryStartsVideoLevelThumbnailPreloadAfterUsableDurationIsKnown() {
        assertEquals(null, golfCamThumbnailPreloadDurationMs(null))
        assertEquals(
            null,
            golfCamThumbnailPreloadDurationMs(
                GolfCamVideoConfig(
                    durationMs = 499L,
                    frameRate = 60.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                    rotationDegrees = 90,
                ),
            ),
        )
        assertEquals(
            18_000L,
            golfCamThumbnailPreloadDurationMs(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 60.0,
                    visualWidth = 1,
                    visualHeight = 1,
                    rotationDegrees = 90,
                ),
            ),
        )
        assertEquals(
            null,
            golfCamContainerThumbnailPreloadDurationMs(durationMs = 499L, videoConfig = null),
        )
        assertEquals(
            18_000L,
            golfCamContainerThumbnailPreloadDurationMs(durationMs = 18_000L, videoConfig = null),
        )
        assertEquals(
            18_000L,
            golfCamContainerThumbnailPreloadDurationMs(
                durationMs = 0L,
                videoConfig = GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 60.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                    rotationDegrees = 90,
                ),
            ),
        )
    }

    @Test
    fun trajectoryImportMessagesMatchIosStepZeroAuthority() {
        assertEquals("正在恢复上次编辑进度", GOLF_CAM_IMPORT_RESTORE_MESSAGE)
        assertEquals("正在识别视频比例与帧率", GOLF_CAM_IMPORT_DETECT_MESSAGE)
        assertEquals("正在生成预览", GOLF_CAM_IMPORT_GENERATE_PREVIEW_MESSAGE)
        assertEquals("无法读取视频参数，请换一个视频或稍后重试", GOLF_CAM_IMPORT_VIDEO_CONFIG_ERROR_MESSAGE)
    }

    @Test
    fun videoConfigurationDetectionNormalizesRotationAndVisualDimensions() {
        assertEquals(0, normalizedVideoRotation(13))
        assertEquals(90, normalizedVideoRotation(90))
        assertEquals(90, normalizedVideoRotation(450))
        assertEquals(270, normalizedVideoRotation(-90))
        assertEquals(1920 to 1080, visualVideoDimensions(rawWidth = 1080, rawHeight = 1920, rotationDegrees = 90))
        assertEquals(1920 to 1080, visualVideoDimensions(rawWidth = 1080, rawHeight = 1920, rotationDegrees = 450))
        assertEquals(1920 to 1080, visualVideoDimensions(rawWidth = 1080, rawHeight = 1920, rotationDegrees = -90))
        assertEquals(3840 to 2160, visualVideoDimensions(rawWidth = 3840, rawHeight = 2160, rotationDegrees = 0))

        val portrait = GolfCamVideoConfig(
            durationMs = 12_000L,
            frameRate = 59.94,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )
        assertTrue(portrait.isPortrait)
        assertEquals(1080f / 1920f, portrait.aspectRatio, 0f)
    }

    @Test
    fun workflowStateStoreNormalizesRestoredVideoRotationLikeDetectionAndExport() {
        fun restoredRotation(rawRotation: Int): Int? {
            return GolfCamWorkflowStateStore.decode(
                JsonObject(
                    mapOf(
                        "videoConfig" to JsonObject(
                            mapOf(
                                "durationMs" to JsonPrimitive(12_000L),
                                "frameRate" to JsonPrimitive(60.0),
                                "visualWidth" to JsonPrimitive(1080),
                                "visualHeight" to JsonPrimitive(1920),
                                "rotationDegrees" to JsonPrimitive(rawRotation),
                            ),
                        ),
                    ),
                ),
            ).videoConfig?.rotationDegrees
        }

        assertEquals(90, restoredRotation(450))
        assertEquals(270, restoredRotation(-90))
        assertEquals(0, restoredRotation(13))
    }

    @Test
    fun entryDetectsVideoConfigWhenRestoredCacheOnlyHasInferredFrameRate() {
        val unknown = GolfCamVideoConfig(frameRate = 60.0)

        assertTrue(shouldDetectGolfCamVideoConfig(null))
        assertEquals(1, unknown.visualWidth)
        assertEquals(1, unknown.visualHeight)
        assertTrue(shouldDetectGolfCamVideoConfig(unknown))
        assertTrue(
            shouldDetectGolfCamVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 0.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                ),
            ),
        )
        assertTrue(
            shouldDetectGolfCamVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 240.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                ),
            ),
        )
        assertTrue(
            shouldDetectGolfCamVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = Double.NaN,
                    visualWidth = 1080,
                    visualHeight = 1920,
                ),
            ),
        )
        assertTrue(
            shouldDetectGolfCamVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 60.0,
                    visualWidth = 1,
                    visualHeight = 1,
                ),
            ),
        )
        assertFalse(
            shouldDetectGolfCamVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 18_000L,
                    frameRate = 59.94,
                    visualWidth = 1080,
                    visualHeight = 1920,
                    rotationDegrees = 90,
                ),
            ),
        )
    }

    @Test
    fun detectedVideoConfigDoesNotInventVisualDimensionsWhenMetadataIsMissing() {
        val complete = golfCamVideoConfigFromMetadata(
            rawDurationMs = "18000",
            rawWidth = "1920",
            rawHeight = "1080",
            rawRotationDegrees = "90",
            frameRate = 59.94,
        )
        val missingWidth = golfCamVideoConfigFromMetadata(
            rawDurationMs = "18000",
            rawWidth = null,
            rawHeight = "1080",
            rawRotationDegrees = "90",
            frameRate = 59.94,
        )
        val missingHeight = golfCamVideoConfigFromMetadata(
            rawDurationMs = "18000",
            rawWidth = "1920",
            rawHeight = null,
            rawRotationDegrees = "0",
            frameRate = 59.94,
        )
        val malformedDuration = golfCamVideoConfigFromMetadata(
            rawDurationMs = "bad-duration",
            rawWidth = "1920",
            rawHeight = "1080",
            rawRotationDegrees = "-90",
            frameRate = 200.0,
        )
        val malformedFrameRate = golfCamVideoConfigFromMetadata(
            rawDurationMs = "18000",
            rawWidth = "1920",
            rawHeight = "1080",
            rawRotationDegrees = "0",
            frameRate = Double.NaN,
        )

        assertEquals(1080, complete.visualWidth)
        assertEquals(1920, complete.visualHeight)
        assertEquals(90, complete.rotationDegrees)
        assertFalse(shouldDetectGolfCamVideoConfig(complete))

        assertEquals(1080, missingWidth.visualWidth)
        assertEquals(1, missingWidth.visualHeight)
        assertTrue(shouldDetectGolfCamVideoConfig(missingWidth))

        assertEquals(1920, missingHeight.visualWidth)
        assertEquals(1, missingHeight.visualHeight)
        assertTrue(shouldDetectGolfCamVideoConfig(missingHeight))

        assertEquals(1L, malformedDuration.durationMs)
        assertEquals(270, malformedDuration.rotationDegrees)
        assertEquals(120.0, malformedDuration.frameRate, 0.0)
        assertTrue(shouldDetectGolfCamVideoConfig(malformedDuration))
        assertEquals(30.0, malformedFrameRate.frameRate, 0.0)
        assertFalse(shouldDetectGolfCamVideoConfig(malformedFrameRate))
    }

    @Test
    fun playerTimingBackfillDoesNotInventVisualVideoDimensions() {
        val timingOnly = videoConfigWithPlayerTiming(
            current = null,
            durationMs = 18_000L,
            frameRate = 59.94,
        )
        val detected = videoConfigWithPlayerTiming(
            current = GolfCamVideoConfig(
                durationMs = 12_000L,
                frameRate = 30.0,
                visualWidth = 1080,
                visualHeight = 1920,
                rotationDegrees = 90,
            ),
            durationMs = 18_000L,
            frameRate = 59.94,
        )
        val malformedTiming = videoConfigWithPlayerTiming(
            current = null,
            durationMs = 18_000L,
            frameRate = Double.POSITIVE_INFINITY,
        )

        assertEquals(18_000L, timingOnly.durationMs)
        assertEquals(59.94, timingOnly.frameRate, 0.0)
        assertEquals(1, timingOnly.visualWidth)
        assertEquals(1, timingOnly.visualHeight)
        assertTrue(shouldDetectGolfCamVideoConfig(timingOnly))

        assertEquals(18_000L, detected.durationMs)
        assertEquals(59.94, detected.frameRate, 0.0)
        assertEquals(1080, detected.visualWidth)
        assertEquals(1920, detected.visualHeight)
        assertEquals(90, detected.rotationDegrees)
        assertFalse(shouldDetectGolfCamVideoConfig(detected))
        assertEquals(30.0, malformedTiming.frameRate, 0.0)
        assertTrue(shouldDetectGolfCamVideoConfig(malformedTiming))
    }

    @Test
    fun previewScreenDoesNotTrustInvalidInitialVideoConfigBeforeDetection() {
        val invalidInitial = GolfCamVideoConfig(
            durationMs = 1L,
            frameRate = 0.0,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )
        val trustedInitial = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 59.94,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )

        val invalidFallback = trustedPreviewVideoConfigOrUnknown(invalidInitial)
        val nullFallback = trustedPreviewVideoConfigOrUnknown(null)

        assertEquals(1L, invalidFallback.durationMs)
        assertEquals(1, invalidFallback.visualWidth)
        assertEquals(1, invalidFallback.visualHeight)
        assertEquals(0, invalidFallback.rotationDegrees)
        assertTrue(shouldDetectGolfCamVideoConfig(invalidFallback))
        assertEquals(invalidFallback, nullFallback)

        assertEquals(trustedInitial, trustedPreviewVideoConfigOrUnknown(trustedInitial))
        assertFalse(shouldDetectGolfCamVideoConfig(trustedPreviewVideoConfigOrUnknown(trustedInitial)))
    }

    @Test
    fun videoFrameRateEstimationUsesPresentationTimestampsForFractionalFps() {
        val ntscTimes = (0..90).map { index ->
            (index * 1_000_000.0 * 1001.0 / 30_000.0).toLong()
        }

        assertEquals(29.970, estimateFrameRateFromPresentationTimes(ntscTimes) ?: -1.0, 0.002)
        assertEquals(59.94, estimateFrameRateFromPresentationTimes(listOf(0L, 16_683L, 33_366L, 50_050L)) ?: -1.0, 0.02)
        assertEquals(
            60.0,
            estimateFrameRateFromPresentationTimes(listOf(0L, 33_334L, 16_667L, 16_667L, 50_001L)) ?: -1.0,
            0.01,
        )
        assertEquals(
            60.0,
            estimateFrameRateFromPresentationTimes(
                listOf(0L, 16_667L, 33_334L, 433_334L, 450_001L, 466_668L, 483_335L),
            ) ?: -1.0,
            0.01,
        )
        assertEquals(null, estimateFrameRateFromPresentationTimes(listOf(1_000L, 1_000L, 1_000L)))
        assertEquals(
            null,
            estimateFrameRateFromPresentationTimes(
                listOf(0L, Long.MAX_VALUE - 3L, Long.MAX_VALUE - 2L, Long.MAX_VALUE - 1L),
            ),
        )
    }

    @Test
    fun flightCurveTimelineUsesTrimmedDurationLikeIos() {
        val sourceDurationMs = 60_000L
        val trimStartMs = 10_000L
        val trimEndMs = 16_000L
        val frameRate = 60.0

        assertEquals(360, GolfCamFrameTimeline.totalFramesForRange(trimStartMs, trimEndMs, frameRate))
        assertEquals(3_600, GolfCamFrameTimeline.totalFrames(sourceDurationMs, frameRate))
        assertEquals(10_000L, GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex = 0, frameRate))
        assertEquals(15_983L, GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex = 359, frameRate))
    }

    @Test
    fun frameTimelineUsesFiniteFallbackForMalformedFrameRate() {
        assertEquals(30.0, GolfCamFrameTimeline.safeFrameRate(Double.NaN), 0.0)
        assertEquals(30.0, GolfCamFrameTimeline.safeFrameRate(Double.POSITIVE_INFINITY), 0.0)
        assertEquals(30.0, GolfCamFrameTimeline.safeFrameRate(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals(1.0, GolfCamFrameTimeline.safeFrameRate(0.0), 0.0)
        assertEquals(120.0, GolfCamFrameTimeline.safeFrameRate(240.0), 0.0)
        assertEquals(0, GolfCamFrameTimeline.frameIndexAt(relativeSeconds = Double.POSITIVE_INFINITY, frameRate = 60.0))
        assertEquals(60, GolfCamFrameTimeline.totalFrames(durationMs = 2_000L, frameRate = Double.NaN))
        assertEquals(10_033L, GolfCamFrameTimeline.timeMsForFrame(10_000L, frameIndex = 1, frameRate = Double.NaN))
        assertEquals(10_000L, GolfCamFrameTimeline.lastFrameTimeMsForRange(10_000L, 10_001L, Double.NaN))
        assertEquals(10_001L, GolfCamFrameTimeline.endExclusiveMsAfterStart(10_000L, 9_000L))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.endExclusiveMsAfterStart(Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(1L, GolfCamFrameTimeline.durationMsForRange(10_000L, 9_000L))
        assertEquals(0L, GolfCamFrameTimeline.nonNegativeDeltaMs(10_000L, 9_000L))
        assertEquals(0L, GolfCamFrameTimeline.nonNegativeDeltaMs(10_000L, 10_000L))
        assertEquals(250L, GolfCamFrameTimeline.nonNegativeDeltaMs(10_000L, 10_250L))
        assertEquals(10_250L, GolfCamFrameTimeline.timeMsAfter(10_000L, 250L))
        assertEquals(10_000L, GolfCamFrameTimeline.timeMsAfter(10_000L, -250L))
        assertEquals(10_250L, GolfCamFrameTimeline.timeMsAfterSeconds(10_000L, 0.25))
        assertEquals(10_000L, GolfCamFrameTimeline.timeMsAfterSeconds(10_000L, Double.NaN))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.durationMsForRange(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.nonNegativeDeltaMs(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, GolfCamFrameTimeline.totalFrames(durationMs = Long.MAX_VALUE, frameRate = 120.0))
        assertEquals(Int.MAX_VALUE, GolfCamFrameTimeline.totalFramesForRange(Long.MIN_VALUE, Long.MAX_VALUE, frameRate = 120.0))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.timeMsForFrame(Long.MAX_VALUE - 5L, frameIndex = 120, frameRate = 1.0))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.timeMsAfter(Long.MAX_VALUE - 5L, 120L))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.timeMsAfterSeconds(Long.MAX_VALUE - 5L, 120.0))
        assertEquals(Long.MAX_VALUE, GolfCamFrameTimeline.lastFrameTimeMsForRange(Long.MAX_VALUE, Long.MAX_VALUE, 60.0))
    }

    @Test
    fun trimDefaultsAndMinimumDurationMatchIosCustomTrimmer() {
        assertEquals(1f, defaultTrimEndFraction(durationMs = 20_000L), 0f)
        assertEquals(0.5f, defaultTrimEndFraction(durationMs = 60_000L), 0f)
        assertEquals(0.025f, minimumTrimFraction(durationMs = 20_000L), 1e-6f)
        assertEquals(0.25f, trimFractionOrDefault(Float.NaN, fallback = 0.25f), 0f)
        assertEquals(0f, trimFractionOrDefault(Float.POSITIVE_INFINITY, fallback = Float.NaN), 0f)
        assertEquals(1f, trimFractionOrDefault(1.5f, fallback = 0f), 0f)

        assertEquals(0.475f, clampTrimStartFraction(newFraction = 0.90f, endFraction = 0.50f, durationMs = 20_000L), 1e-6f)
        assertEquals(0.525f, clampTrimEndFraction(newFraction = 0.10f, startFraction = 0.50f, durationMs = 20_000L), 1e-6f)
        assertEquals(0f, clampTrimStartFraction(newFraction = 0.90f, endFraction = 1.0f, durationMs = 300L), 0f)
        assertEquals(1f, clampTrimEndFraction(newFraction = 0.10f, startFraction = 0f, durationMs = 300L), 0f)
        assertEquals(0f, clampTrimStartFraction(newFraction = Float.NaN, endFraction = 0.50f, durationMs = 20_000L), 0f)
        assertEquals(0.49166667f, clampTrimStartFraction(newFraction = 0.90f, endFraction = Float.NaN, durationMs = 60_000L), 1e-6f)
        assertEquals(1f, clampTrimEndFraction(newFraction = Float.NaN, startFraction = 0.50f, durationMs = 20_000L), 0f)
        assertEquals(0.10f, clampTrimEndFraction(newFraction = 0.10f, startFraction = Float.POSITIVE_INFINITY, durationMs = 20_000L), 1e-6f)
    }

    @Test
    fun trimResultOutputAlwaysRespectsIosRangeAndMinimumDuration() {
        assertEquals(TrimResult(startMs = 0L, endMs = 30_000L), trimResultFromFractions(60_000L, 0f, 0.5f))
        assertEquals(TrimResult(startMs = 9_500L, endMs = 10_000L), trimResultFromFractions(20_000L, 0.90f, 0.50f))
        assertEquals(TrimResult(startMs = 0L, endMs = 500L), trimResultFromFractions(20_000L, -1f, -0.5f))
        assertEquals(TrimResult(startMs = 19_500L, endMs = 20_000L), trimResultFromFractions(20_000L, 1.2f, 1.5f))
        assertEquals(TrimResult(startMs = 0L, endMs = 300L), trimResultFromFractions(300L, 0.9f, 0.1f))
        assertEquals(TrimResult(startMs = 0L, endMs = 30_000L), trimResultFromFractions(60_000L, Float.NaN, Float.POSITIVE_INFINITY))
        assertEquals(TrimResult(startMs = 0L, endMs = 20_000L), trimResultFromFractions(20_000L, Float.NEGATIVE_INFINITY, Float.NaN))
        assertEquals(null, trimResultFromFractions(0L, 0f, 1f))
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
    fun trimTimeLabelsUseStableIosMinuteSecondTenthsFormat() {
        assertEquals("0:00.0", formatTrimTimeLabel(-250L))
        assertEquals("0:05.0", formatTrimTimeLabel(5_000L))
        assertEquals("1:05.4", formatTrimTimeLabel(65_400L))
        assertEquals("12:03.9", formatTrimTimeLabel(723_900L))
    }

    @Test
    fun trimVisualSpecMatchesIosDimMaskSelectionBorderAndHandleGeometry() {
        assertEquals(28, TRIM_SAFE_MARGIN_DP)
        assertEquals(12, TRIM_HANDLE_WIDTH_DP)
        assertEquals(56, TRIM_BAR_HEIGHT_DP)
        assertEquals(16, TRIM_HANDLE_TOUCH_EXTRA_DP)
        assertEquals(0.60f, TRIM_DIM_OVERLAY_ALPHA, 0f)
        assertEquals(1.5f, TRIM_SELECTION_BORDER_STROKE_PX, 0f)
        assertEquals(0.08f, TRIM_SELECTION_FILL_ALPHA, 0f)
        assertEquals(6, TRIM_SELECTION_CORNER_RADIUS_DP)
        assertEquals(4, TRIM_STRIP_CORNER_RADIUS_DP)
    }

    @Test
    fun trimThumbnailSamplingMatchesIosFifteenEvenlySpacedFrames() {
        val sampleTimes = trimThumbnailSampleTimesUs(durationMs = 28_000L)

        assertEquals(15, TRIM_THUMBNAIL_COUNT)
        assertEquals(TRIM_THUMBNAIL_COUNT, sampleTimes.size)
        assertEquals(0L, sampleTimes.first())
        assertEquals(28_000_000L, sampleTimes.last())
        assertEquals(2_000_000L, sampleTimes[1] - sampleTimes[0])
        assertTrue(sampleTimes.zipWithNext().all { (previous, next) -> next > previous })
        assertEquals(emptyList<Long>(), trimThumbnailSampleTimesUs(durationMs = 0L))

        val extremeSamples = trimThumbnailSampleTimesUs(durationMs = Long.MAX_VALUE)
        assertEquals(TRIM_THUMBNAIL_COUNT, extremeSamples.size)
        assertEquals(0L, extremeSamples.first())
        assertTrue(extremeSamples.all { it in 0L..(Long.MAX_VALUE / 1000L * 1000L) })
        assertTrue(extremeSamples.zipWithNext().all { (previous, next) -> next >= previous })
        assertEquals(120, trimThumbnailSampleTimesUs(durationMs = 10_000L, thumbnailCount = Int.MAX_VALUE).size)
    }

    @Test
    fun trimHandleSeekTimeUsesIosFractionOfVideoDuration() {
        assertEquals(0L, trimSeekTimeMs(durationMs = 20_000L, fraction = -0.25f))
        assertEquals(5_000L, trimSeekTimeMs(durationMs = 20_000L, fraction = 0.25f))
        assertEquals(20_000L, trimSeekTimeMs(durationMs = 20_000L, fraction = 1.25f))
        assertEquals(0L, trimSeekTimeMs(durationMs = 20_000L, fraction = Float.NaN))
        assertEquals(0L, trimSeekTimeMs(durationMs = 20_000L, fraction = Float.POSITIVE_INFINITY))
        assertEquals(0L, trimSeekTimeMs(durationMs = 0L, fraction = 0.5f))
    }

    @Test
    fun trimPlayheadMatchesIosSelectedRangeRules() {
        assertEquals(0.30f, trimPlayheadFraction(20_000L, 6_000L, 0.25f, 0.75f) ?: -1f, 1e-6f)
        assertEquals(null, trimPlayheadFraction(20_000L, 4_000L, 0.25f, 0.75f))
        assertEquals(null, trimPlayheadFraction(20_000L, 16_000L, 0.25f, 0.75f))
        assertEquals(0.30f, trimPlayheadFraction(20_000L, 6_000L, Float.NaN, Float.POSITIVE_INFINITY) ?: -1f, 1e-6f)

        assertEquals(5_000L, trimPlayheadSeekTimeMs(20_000L, 0.10f, 0.25f, 0.75f))
        assertEquals(8_000L, trimPlayheadSeekTimeMs(20_000L, 0.40f, 0.25f, 0.75f))
        assertEquals(15_000L, trimPlayheadSeekTimeMs(20_000L, 0.90f, 0.25f, 0.75f))
        assertEquals(0L, trimPlayheadSeekTimeMs(20_000L, Float.NaN, Float.NaN, Float.POSITIVE_INFINITY))
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
            0,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 90,
                rawWidth = 3840,
                rawHeight = 2160,
                frameWidth = 540,
                frameHeight = 960,
            ),
        )
        assertEquals(
            90,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 450,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1920,
                frameHeight = 1080,
            ),
        )
        assertEquals(
            270,
            thumbnailRotationDegrees(
                metadataRotationDegrees = -90,
                rawWidth = 1920,
                rawHeight = 1080,
                frameWidth = 1920,
                frameHeight = 1080,
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
        assertEquals(
            0,
            thumbnailRotationDegrees(
                metadataRotationDegrees = 45,
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
            CandidateImpactFrame(frameIndex = 48, confidence = 0.80f, timeSec = 0.8),
            CandidateImpactFrame(frameIndex = 120, confidence = 0.95f, timeSec = 2.0),
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
    fun impactFrameVisualSpecMatchesIosThumbnailTimelineAndNavigation() {
        assertEquals(32, IMPACT_FRAME_THUMBNAIL_CELL_WIDTH_DP)
        assertEquals(45, IMPACT_FRAME_THUMBNAIL_CELL_HEIGHT_DP)
        assertEquals(50, IMPACT_FRAME_TIMELINE_HEIGHT_DP)
        assertEquals(2, IMPACT_FRAME_THUMBNAIL_GAP_DP)
        assertEquals(4, IMPACT_FRAME_THUMBNAIL_CORNER_RADIUS_DP)
        assertEquals(2, IMPACT_FRAME_THUMBNAIL_SELECTED_BORDER_DP)
        assertEquals(1.1f, IMPACT_FRAME_THUMBNAIL_SELECTED_SCALE, 0f)
        assertEquals(0.6f, IMPACT_FRAME_THUMBNAIL_UNSELECTED_ALPHA, 0f)
        assertEquals(6, IMPACT_FRAME_CANDIDATE_DOT_SIZE_DP)
        assertEquals(2, IMPACT_FRAME_CANDIDATE_DOT_TOP_PADDING_DP)
        assertEquals(44, IMPACT_FRAME_NAV_BUTTON_WIDTH_DP)
        assertEquals(50, IMPACT_FRAME_NAV_BUTTON_HEIGHT_DP)
    }

    @Test
    fun impactFrameStepButtonsMoveOneFrameAndClampAtBounds() {
        assertEquals(41, impactFrameStepFrame(currentFrame = 42, totalFrames = 180, deltaFrames = -1))
        assertEquals(43, impactFrameStepFrame(currentFrame = 42, totalFrames = 180, deltaFrames = 1))
        assertEquals(0, impactFrameStepFrame(currentFrame = 0, totalFrames = 180, deltaFrames = -1))
        assertEquals(179, impactFrameStepFrame(currentFrame = 179, totalFrames = 180, deltaFrames = 1))
        assertEquals(0, impactFrameStepFrame(currentFrame = 0, totalFrames = 0, deltaFrames = 1))
        assertEquals(179, impactFrameStepFrame(currentFrame = 10_000, totalFrames = 180, deltaFrames = 1))
    }

    @Test
    fun impactFrameTimeLabelUsesStableSecondsFormat() {
        assertEquals("0.00s", formatImpactFrameTimeLabel(-0.25))
        assertEquals("1.23s", formatImpactFrameTimeLabel(1.234))
        assertEquals("0.00s", formatImpactFrameTimeLabel(Double.NaN))
        assertEquals("0.00s", formatImpactFrameTimeLabel(Double.POSITIVE_INFINITY))
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
        assertEquals(
            24,
            asyncImpactCandidateReplacementFrame(
                savedFrameIndexOnEntry = null,
                currentFrame = autoMiddleFrameAlreadyWrittenToState,
                candidateFrames = candidates,
                totalFrames = 180,
            ),
        )
        assertEquals(
            null,
            asyncImpactCandidateReplacementFrame(
                savedFrameIndexOnEntry = autoMiddleFrameAlreadyWrittenToState,
                currentFrame = autoMiddleFrameAlreadyWrittenToState,
                candidateFrames = candidates,
                totalFrames = 180,
            ),
        )
    }

    @Test
    fun asyncCandidateDoesNotOverrideManualImpactFrameSelection() {
        val candidates = listOf(
            CandidateImpactFrame(frameIndex = 24, confidence = 0.98f, timeSec = 0.4),
            CandidateImpactFrame(frameIndex = 360, confidence = 0.97f, timeSec = 6.0),
        )

        assertEquals(
            null,
            asyncImpactCandidateReplacementFrame(
                savedFrameIndexOnEntry = null,
                currentFrame = 91,
                candidateFrames = candidates,
                totalFrames = 180,
            ),
        )
        assertEquals(
            179,
            asyncImpactCandidateReplacementFrame(
                savedFrameIndexOnEntry = null,
                currentFrame = 90,
                candidateFrames = listOf(CandidateImpactFrame(frameIndex = 999, confidence = 0.99f, timeSec = 12.0)),
                totalFrames = 180,
            ),
        )
    }

    @Test
    fun impactFrameSelectionRefreshesWhenAsyncCandidatesArrive() {
        val candidates = listOf(CandidateImpactFrame(frameIndex = 24, confidence = 0.98f, timeSec = 0.4))

        assertTrue(
            shouldUpdateImpactFrameSelection(
                currentFrame = 90,
                durationMs = 6_000L,
                frameRate = 60.0,
                candidateFrames = candidates,
            ),
        )
        assertFalse(
            shouldUpdateImpactFrameSelection(
                currentFrame = 90,
                durationMs = 0L,
                frameRate = 60.0,
                candidateFrames = candidates,
            ),
        )
        assertFalse(
            shouldUpdateImpactFrameSelection(
                currentFrame = 90,
                durationMs = 6_000L,
                frameRate = Double.NaN,
                candidateFrames = candidates,
            ),
        )
        assertFalse(
            shouldUpdateImpactFrameSelection(
                currentFrame = 90,
                durationMs = 6_000L,
                frameRate = Double.POSITIVE_INFINITY,
                candidateFrames = candidates,
            ),
        )
        assertTrue(
            shouldUpdateImpactFrameSelection(
                currentFrame = 90,
                durationMs = 6_000L,
                frameRate = 60.0,
                candidateFrames = listOf(CandidateImpactFrame(frameIndex = -1, confidence = 0.98f, timeSec = 0.4)),
            ),
        )
    }

    @Test
    fun impactFrameSelectionSanitizesCandidateListForCurrentTrimAndFrameRate() {
        val rawCandidates = listOf(
            CandidateImpactFrame(frameIndex = -1, confidence = 0.98f, timeSec = -0.1),
            CandidateImpactFrame(frameIndex = 999, confidence = 1.4f, timeSec = 0.5),
            CandidateImpactFrame(frameIndex = 12, confidence = -0.2f, timeSec = 5.983),
            CandidateImpactFrame(frameIndex = 24, confidence = 0.6f, timeSec = 6.1),
        )

        val sanitized = validImpactCandidateFramesForTrimRange(
            candidateFrames = rawCandidates,
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            frameRate = 60.0,
        )
        val selection = impactFrameSelectionForFrame(
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            currentFrame = 90,
            durationMs = 12_000L,
            frameRate = 60.0,
            candidateFrames = rawCandidates,
        )

        assertEquals(2, sanitized.size)
        assertEquals(30, sanitized[0].frameIndex)
        assertEquals(0.5, sanitized[0].timeSec, 0.0)
        assertEquals(1.0f, sanitized[0].confidence, 0.0f)
        assertEquals(359, sanitized[1].frameIndex)
        assertEquals(5.983, sanitized[1].timeSec, 0.0)
        assertEquals(0.0f, sanitized[1].confidence, 0.0f)
        assertEquals(sanitized, selection?.candidateFrames)

        val singleFrameCandidates = validImpactCandidateFramesForTrimRange(
            candidateFrames = listOf(
                CandidateImpactFrame(frameIndex = 0, confidence = 0.9f, timeSec = 0.0),
                CandidateImpactFrame(frameIndex = 1, confidence = 0.8f, timeSec = 0.001),
            ),
            trimStartMs = 1_000L,
            trimEndMs = 1_001L,
            frameRate = 120.0,
        )
        assertEquals(1, singleFrameCandidates.size)
        assertEquals(0.0, singleFrameCandidates.single().timeSec, 0.0)
        assertEquals(0, singleFrameCandidates.single().frameIndex)

        val extremeSelection = impactFrameSelectionForFrame(
            trimStartMs = Long.MAX_VALUE,
            trimEndMs = Long.MAX_VALUE,
            currentFrame = 0,
            durationMs = 1L,
            frameRate = 60.0,
            candidateFrames = emptyList(),
        )
        assertEquals(Long.MAX_VALUE, extremeSelection?.timeMs)
    }

    @Test
    fun impactCandidateSanitizationRejectsNonFiniteTimesAndConfidence() {
        val stepCandidates = validImpactCandidateFramesForTrimRange(
            candidateFrames = listOf(
                CandidateImpactFrame(frameIndex = 4, confidence = Float.NaN, timeSec = 0.75),
                CandidateImpactFrame(frameIndex = 5, confidence = 0.8f, timeSec = Double.POSITIVE_INFINITY),
                CandidateImpactFrame(frameIndex = 6, confidence = Float.POSITIVE_INFINITY, timeSec = 1.0),
            ),
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            frameRate = 60.0,
        )
        val detectorCandidates = impactCandidatesForTrimRange(
            rawCandidates = listOf(
                ImpactCandidate(timeSec = Double.NaN, confidence = 1.0f),
                ImpactCandidate(timeSec = 11.0, confidence = Float.NaN),
                ImpactCandidate(timeSec = Double.POSITIVE_INFINITY, confidence = 0.9f),
            ),
            trimStartMs = 10_000L,
            trimEndMs = 16_000L,
            frameRate = 60.0,
        )

        assertEquals(2, stepCandidates.size)
        assertEquals(45, stepCandidates[0].frameIndex)
        assertEquals(0.0f, stepCandidates[0].confidence, 0.0f)
        assertEquals(60, stepCandidates[1].frameIndex)
        assertEquals(0.0f, stepCandidates[1].confidence, 0.0f)
        assertEquals(1, detectorCandidates.size)
        assertEquals(60, detectorCandidates.first().frameIndex)
        assertEquals(1.0, detectorCandidates.first().timeSec, 0.0)
        assertEquals(0.0f, detectorCandidates.first().confidence, 0.0f)
    }

    @Test
    fun impactFrameSelectionRecomputesAbsoluteTimeWhenTrimRangeChanges() {
        val firstTrimSelection = impactFrameSelectionForFrame(
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            currentFrame = 60,
            durationMs = 12_000L,
            frameRate = 60.0,
            candidateFrames = emptyList(),
        )
        val secondTrimSelection = impactFrameSelectionForFrame(
            trimStartMs = 3_000L,
            trimEndMs = 9_000L,
            currentFrame = 60,
            durationMs = 12_000L,
            frameRate = 60.0,
            candidateFrames = emptyList(),
        )
        val clampedToTrimEnd = impactFrameSelectionForFrame(
            trimStartMs = 3_000L,
            trimEndMs = 3_500L,
            currentFrame = 120,
            durationMs = 12_000L,
            frameRate = 60.0,
            candidateFrames = emptyList(),
        )

        assertEquals(2_000L, firstTrimSelection?.timeMs)
        assertEquals(4_000L, secondTrimSelection?.timeMs)
        assertEquals(3_483L, clampedToTrimEnd?.timeMs)
        assertEquals(29, clampedToTrimEnd?.frameIndex)
    }

    @Test
    fun impactCandidatesClampToLastFrameInsideHalfOpenTrimRange() {
        val candidates = impactCandidatesForTrimRange(
            rawCandidates = listOf(ImpactCandidate(timeSec = 6.999, confidence = 0.98f)),
            trimStartMs = 1_000L,
            trimEndMs = 7_000L,
            frameRate = 60.0,
        )

        assertEquals(1, candidates.size)
        assertEquals(359, candidates.first().frameIndex)
        assertEquals(5.983, candidates.first().timeSec, 0.001)
    }

    @Test
    fun impactCandidatesDiscardDetectorTimesOutsideHalfOpenTrimRange() {
        val candidates = impactCandidatesForTrimRange(
            rawCandidates = listOf(
                ImpactCandidate(timeSec = 9.999, confidence = 1.0f),
                ImpactCandidate(timeSec = 10.0, confidence = 0.8f),
                ImpactCandidate(timeSec = 12.0, confidence = 0.7f),
                ImpactCandidate(timeSec = 16.0, confidence = 0.9f),
                ImpactCandidate(timeSec = 16.001, confidence = 0.6f),
            ),
            trimStartMs = 10_000L,
            trimEndMs = 16_000L,
            frameRate = 60.0,
        )

        assertEquals(2, candidates.size)
        assertEquals(0, candidates[0].frameIndex)
        assertEquals(0.0, candidates[0].timeSec, 0.0)
        assertEquals(120, candidates[1].frameIndex)
        assertEquals(2.0, candidates[1].timeSec, 0.0)
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
        assertFalse(shouldClearImpactCandidatesForNewDetection(oldKey, sameKey, candidates))
        assertTrue(shouldClearImpactCandidatesForNewDetection(oldKey, changedTrim, candidates))
        assertTrue(shouldClearImpactCandidatesForNewDetection(oldKey, changedFrameRate, candidates))
        assertFalse(shouldClearImpactCandidatesForNewDetection(oldKey, changedTrim, emptyList()))
    }

    @Test
    fun impactCandidatesConvertAbsoluteDetectorTimesIntoTrimRelativeFrames() {
        val candidates = impactCandidatesForTrimRange(
            rawCandidates = listOf(
                ImpactCandidate(timeSec = 10.0, confidence = 0.4f),
                ImpactCandidate(timeSec = 11.25, confidence = 1.4f),
                ImpactCandidate(timeSec = 15.999, confidence = -0.2f),
            ),
            trimStartMs = 10_000L,
            trimEndMs = 16_000L,
            frameRate = 60.0,
        )

        assertEquals(3, candidates.size)
        assertEquals(75, candidates[0].frameIndex)
        assertEquals(1.25, candidates[0].timeSec, 0.0)
        assertEquals(1.0f, candidates[0].confidence, 0.0f)
        assertEquals(0, candidates[1].frameIndex)
        assertEquals(0.0, candidates[1].timeSec, 0.0)
        assertEquals(0.4f, candidates[1].confidence, 0.0f)
        assertEquals(359, candidates[2].frameIndex)
        assertEquals(5.983, candidates[2].timeSec, 0.001)
        assertEquals(0.0f, candidates[2].confidence, 0.0f)
    }

    @Test
    fun impactCandidateConfidencePercentIsClampedForBadDetectorOrCacheValues() {
        assertEquals(0, ImpactCandidate(timeSec = 1.0, confidence = -0.5f).confidencePercent)
        assertEquals(100, ImpactCandidate(timeSec = 1.0, confidence = 1.5f).confidencePercent)
        assertEquals(0, ImpactCandidate(timeSec = 1.0, confidence = Float.NaN).confidencePercent)
        assertEquals(0, ImpactCandidate(timeSec = 1.0, confidence = Float.POSITIVE_INFINITY).confidencePercent)
        assertEquals(0, CandidateImpactFrame(frameIndex = 0, confidence = -0.5f, timeSec = 0.0).confidencePercent)
        assertEquals(100, CandidateImpactFrame(frameIndex = 0, confidence = 1.5f, timeSec = 0.0).confidencePercent)
        assertEquals(0, CandidateImpactFrame(frameIndex = 0, confidence = Float.NaN, timeSec = 0.0).confidencePercent)
        assertEquals(0, CandidateImpactFrame(frameIndex = 0, confidence = Float.POSITIVE_INFINITY, timeSec = 0.0).confidencePercent)
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
        assertEquals(
            null,
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = style,
                elapsedSec = -0.05,
            ),
        )
        assertEquals(
            null,
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = style,
                elapsedSec = Double.NaN,
            ),
        )
        assertEquals(
            GolfCamDistanceConfig.DEFAULT_DISTANCE.toInt(),
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = style.copy(
                    speedLevel = Double.NaN,
                    tailHalfLife = Double.NaN,
                    totalDistance = Double.NaN,
                ),
                elapsedSec = timing.totalDurationSec,
            )?.distance,
        )
        assertFalse(
            TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(
                TrajectoryOverlayFrameSampler.sample(
                    bezier = bezier,
                    arcTable = arcTable,
                    timing = timing,
                    style = style,
                    elapsedSec = timing.totalDurationSec + 0.50,
                )?.headProgress ?: -1f,
            ),
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
        assertEquals(
            null,
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = TrackStyle(activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5),
                elapsedSec = 1.0,
                legacyProgressAtTime = { Double.NaN },
            ),
        )
        assertEquals(
            null,
            TrajectoryOverlayFrameSampler.sample(
                bezier = bezier,
                arcTable = arcTable,
                timing = timing,
                style = TrackStyle(activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5),
                elapsedSec = 1.0,
                legacyProgressAtTime = { Double.POSITIVE_INFINITY },
            ),
        )
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
        assertEquals(11f * (720f / 1080f), DistanceLabelRenderSpec.unitFontPx(720f), 0.001f)
        assertEquals(24f * (720f / 1080f), DistanceLabelRenderSpec.valueFontPx(720f), 0.001f)
        assertEquals(2f * (720f / 1080f), DistanceLabelRenderSpec.lineSpacingPx(720f), 0.001f)
        assertEquals(1f * (720f / 1080f), DistanceLabelRenderSpec.shadowOffsetPx(720f), 0.001f)
        assertEquals((500f - 37.8f) - metrics.unitWidth / 2f, flyingLeft.unitLeft, 0.001f)
        assertEquals((500f + 37.8f) - metrics.unitWidth / 2f, flyingRight.unitLeft, 0.001f)
        assertEquals(flyingLeft.unitTop + metrics.unitHeight + 2f, flyingLeft.valueTop, 0.001f)
        assertTrue(DistanceLabelRenderSpec.isFlyingLeft(currentX = 10f, previousX = 20f))
        assertFalse(DistanceLabelRenderSpec.isFlyingLeft(currentX = 20f, previousX = 10f))
        assertFalse(DistanceLabelRenderSpec.isFlyingLeft(currentX = 0.41f, previousX = 0.40f))
        assertTrue(DistanceLabelRenderSpec.isFlyingLeft(currentX = 0.4002f, previousX = 0.40f))
    }

    @Test
    fun distanceLabelRenderSpecFallsBackFromNonFinitePreviewExportInputs() {
        val layout = DistanceLabelRenderSpec.layout(
            headX = Float.NaN,
            headY = Float.POSITIVE_INFINITY,
            videoHeight = Float.NaN,
            isFlyingLeft = true,
            metrics = DistanceLabelTextMetrics(
                unitWidth = Float.NaN,
                unitHeight = Float.POSITIVE_INFINITY,
                valueWidth = -12f,
                valueHeight = Float.NaN,
            ),
        )

        assertEquals(1f / 1080f, DistanceLabelRenderSpec.scale(Float.NaN), 1e-6f)
        assertEquals(11f / 1080f, DistanceLabelRenderSpec.unitFontPx(Float.NaN), 1e-6f)
        assertEquals(24f / 1080f, DistanceLabelRenderSpec.valueFontPx(Float.POSITIVE_INFINITY), 1e-6f)
        assertTrue(DistanceLabelRenderSpec.isFlyingLeft(currentX = Float.NaN, previousX = 0.4f))
        assertTrue(DistanceLabelRenderSpec.isFlyingLeft(currentX = 0.5f, previousX = Float.NaN))
        assertTrue(layout.unitLeft.isFinite())
        assertTrue(layout.unitTop.isFinite())
        assertTrue(layout.valueLeft.isFinite())
        assertTrue(layout.valueTop.isFinite())
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
    fun cloudPresetRefreshUpdatesPresetDerivedColorHexButPreservesCustomColor() {
        val oldPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF102030, 0xFF405060, 0xFF708090),
            glowBlur = 20f,
            glowAlpha = 0.40f,
            bandAlpha = 0.80f,
        )
        val cloudPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF35DFC7, 0xFF55F3D8, 0xFF8FFFF0),
            glowBlur = 28f,
            glowAlpha = 0.70f,
            bandAlpha = 1.0f,
        )
        val presetDerived = TrackStyle(
            colorPreset = oldPreset,
            colorHex = oldPreset.bandOuterColor(),
            glowOpacity = oldPreset.glowAlpha.toDouble(),
        ).refreshedWithCloudPreset(listOf(cloudPreset))
        val customColor = TrackStyle(
            colorPreset = oldPreset,
            colorHex = 0xFFABCDEF,
            glowOpacity = oldPreset.glowAlpha.toDouble(),
        ).refreshedWithCloudPreset(listOf(cloudPreset))

        assertEquals(cloudPreset, presetDerived.colorPreset)
        assertEquals(cloudPreset.bandOuterColor(), presetDerived.colorHex)
        assertEquals(cloudPreset.glowAlpha.toDouble(), presetDerived.glowOpacity, 0.0)
        assertEquals(cloudPreset, customColor.colorPreset)
        assertEquals(0xFFABCDEF, customColor.colorHex)
        assertEquals(cloudPreset.glowAlpha.toDouble(), customColor.glowOpacity, 0.0)
    }

    @Test
    fun cloudConfigRefreshUpdatesRenderTuningWithoutOverwritingUserStyleChoices() {
        val oldPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF102030, 0xFF405060, 0xFF708090),
            glowBlur = 20f,
            glowAlpha = 0.40f,
            bandAlpha = 0.80f,
        )
        val cloudPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF35DFC7, 0xFF55F3D8, 0xFF8FFFF0),
            glowBlur = 28f,
            glowAlpha = 0.70f,
            bandAlpha = 1.0f,
        )
        val cloudTuning = TrackRenderTuning(
            bandAlphaMultiplier = 1.25f,
            highlightWidthRatio = 0.30f,
            apexThinStrength = 0.80f,
        )
        val existing = TrackStyle(
            colorPreset = oldPreset,
            colorHex = 0xFFABCDEF,
            colorAlpha = 0.42,
            widthPct = 88.0,
            tailHalfLife = 55.0,
            showDistance = true,
            totalDistance = 222.0,
            distanceUnit = DistanceUnit.METERS,
            speedLevel = 0.62,
            renderTuning = TrackRenderTuning(
                bandAlphaMultiplier = 0.75f,
                highlightWidthRatio = 0.85f,
            ),
        )

        val refreshed = existing.refreshedWithCloudConfig(
            TrackStyleCloudConfig(
                presets = listOf(cloudPreset),
                defaultColorHex = 0xFF55F3D8,
                defaultStyle = TrackStyle(
                    colorPreset = cloudPreset,
                    renderTuning = cloudTuning,
                ),
            ),
        )

        assertEquals(cloudPreset, refreshed.colorPreset)
        assertEquals(0xFFABCDEF, refreshed.colorHex)
        assertEquals(0.42, refreshed.colorAlpha, 0.0)
        assertEquals(88.0, refreshed.widthPct, 0.0)
        assertEquals(55.0, refreshed.tailHalfLife, 0.0)
        assertTrue(refreshed.showDistance)
        assertEquals(222.0, refreshed.totalDistance, 0.0)
        assertEquals(DistanceUnit.METERS, refreshed.distanceUnit)
        assertEquals(0.62, refreshed.speedLevel, 0.0)
        assertEquals(cloudTuning, refreshed.renderTuning)
    }

    @Test
    fun cloudConfigRefreshClampsRenderTuningBeforePreviewAndExportUseIt() {
        val cloudPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF35DFC7, 0xFF55F3D8, 0xFF8FFFF0),
            glowBlur = 28f,
            glowAlpha = 0.70f,
            bandAlpha = 1.0f,
        )
        val rawCloudTuning = TrackRenderTuning(
            bandAlphaMultiplier = 9.0f,
            bodyAlphaBase = -1.0f,
            bodyAlphaScale = 9.0f,
            glowBlurMultiplier = -1.0f,
            outerGlowMultiplier = 9.0f,
            fillGlowMultiplier = 9.0f,
            highlightWidthRatio = 9.0f,
            highlightAlphaMax = 9.0f,
            highlightStart = 0.98f,
            highlightEnd = 0.10f,
            highlightPeak = 9.0f,
            highlightStrength = 9.0f,
            widthTaperPower = 9.0f,
            apexThinStrength = 9.0f,
            apexThinRadius = -1.0f,
            highlightFalloffPower = 9.0f,
        )

        val refreshed = TrackStyle().refreshedWithCloudConfig(
            TrackStyleCloudConfig(
                presets = listOf(cloudPreset),
                defaultColorHex = cloudPreset.bandOuterColor(),
                defaultStyle = TrackStyle(
                    colorPreset = cloudPreset,
                    renderTuning = rawCloudTuning,
                ),
            ),
        )
        val tuning = refreshed.renderTuning

        assertEquals(1.40f, tuning.bandAlphaMultiplier, 0f)
        assertEquals(0.00f, tuning.bodyAlphaBase, 0f)
        assertEquals(1.20f, tuning.bodyAlphaScale, 0f)
        assertEquals(0.25f, tuning.glowBlurMultiplier, 0f)
        assertEquals(1.00f, tuning.outerGlowMultiplier, 0f)
        assertEquals(0.45f, tuning.fillGlowMultiplier, 0f)
        assertEquals(0.85f, tuning.highlightWidthRatio, 0f)
        assertEquals(0.90f, tuning.highlightAlphaMax, 0f)
        assertEquals(0.90f, tuning.highlightStart, 0f)
        assertEquals(0.95f, tuning.highlightEnd, 0f)
        assertEquals(0.95f, tuning.highlightPeak, 0f)
        assertEquals(1.60f, tuning.highlightStrength, 0f)
        assertEquals(2.20f, tuning.widthTaperPower, 0f)
        assertEquals(0.85f, tuning.apexThinStrength, 0f)
        assertEquals(0.04f, tuning.apexThinRadius, 0f)
        assertEquals(1.80f, tuning.highlightFalloffPower, 0f)
    }

    @Test
    fun trackStyleCloudConfigMergeDoesNotOverwriteUserEditsThatHappenBeforeFetchCompletes() {
        val fallback = TrackStyleCloudConfigLoader.localFallback().defaultStyle
        val cloudPreset = TrackColorPreset(
            name = "青蓝薄荷",
            gradientColors = listOf(0xFF35DFC7, 0xFF55F3D8, 0xFF8FFFF0),
            glowBlur = 28f,
            glowAlpha = 0.70f,
            bandAlpha = 1.0f,
        )
        val cloudDefault = TrackStyle(
            colorPreset = cloudPreset,
            colorHex = cloudPreset.bandOuterColor(),
            colorAlpha = 0.75,
            widthPct = 40.0,
            tailHalfLife = 70.0,
            speedLevel = GolfCamSpeedConfig.DEFAULT_LEVEL,
            renderTuning = TrackRenderTuning(highlightWidthRatio = 0.30f),
        )
        val config = TrackStyleCloudConfig(
            presets = listOf(cloudPreset),
            defaultColorHex = cloudPreset.bandOuterColor(),
            defaultStyle = cloudDefault,
        )
        val userEditedBeforeFetch = fallback.copy(
            widthPct = 96.0,
            speedLevel = 0.62,
            showDistance = true,
            totalDistance = 222.0,
        )

        val untouched = trackStyleAfterCloudConfigLoaded(
            currentStyle = fallback,
            initialFallbackStyle = fallback,
            restoredStyle = null,
            config = config,
        )
        val edited = trackStyleAfterCloudConfigLoaded(
            currentStyle = userEditedBeforeFetch,
            initialFallbackStyle = fallback,
            restoredStyle = null,
            config = config,
        )

        assertEquals(cloudDefault, untouched)
        assertEquals(96.0, edited.widthPct, 0.0)
        assertEquals(0.62, edited.speedLevel, 0.0)
        assertTrue(edited.showDistance)
        assertEquals(222.0, edited.totalDistance, 0.0)
        assertEquals(cloudPreset, edited.colorPreset)
        assertEquals(cloudDefault.renderTuning, edited.renderTuning)
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
                            "bandAlphaMultiplier" to JsonPrimitive(1.203015100955963),
                            "bodyAlphaBase" to JsonPrimitive(0.10),
                            "bodyAlphaScale" to JsonPrimitive(0.98),
                            "glowBlurMultiplier" to JsonPrimitive(0.82),
                            "outerGlowMultiplier" to JsonPrimitive(0.62),
                            "fillGlowMultiplier" to JsonPrimitive(0.22),
                            "highlightWidthRatio" to JsonPrimitive(0.50),
                            "highlightAlphaMax" to JsonPrimitive(0.5969849467277527),
                            "highlightWhiteLiftBase" to JsonPrimitive(0.00),
                            "highlightWhiteLiftScale" to JsonPrimitive(0.00),
                            "highlightStart" to JsonPrimitive(0.00),
                            "highlightEnd" to JsonPrimitive(0.96),
                            "highlightPeak" to JsonPrimitive(0.26),
                            "highlightStrength" to JsonPrimitive(1.369948673248291),
                            "widthTaperPower" to JsonPrimitive(1.36),
                            "apexThinStrength" to JsonPrimitive(0.60),
                            "apexThinRadius" to JsonPrimitive(0.20),
                            "highlightFalloffPower" to JsonPrimitive(0.82),
                        ),
                    ),
                    "presets" to JsonArray(
                        listOf(
                            preset("玫瑰荧光", "#FF3F91", "#FF5FA8", "#FF8EC3", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("杏橙荧光", "#FF9C24", "#FFB742", "#FFD36B", glowAlpha = 0.66, bandAlpha = 1.0),
                            preset("青蓝薄荷", "#35DFC7", "#55F3D8", "#8FFFF0", glowAlpha = 0.70, bandAlpha = 1.0),
                            preset("炭黑雾光", "#404744", "#656E69", "#9AA39E", glowAlpha = 0.36, bandAlpha = 0.84),
                            preset("荧光青柠", "#A8DC32", "#C4F94C", "#E2FF83", glowAlpha = 0.60, bandAlpha = 0.94),
                            preset("珍珠白", "#C3CCC8", "#F0F6F3", "#FFFFFF", glowAlpha = 0.46, bandAlpha = 0.88),
                            preset("紫莓荧光", "#A855F7", "#C084FC", "#F0ABFC", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("湖蓝荧光", "#21B7FF", "#4FD2FF", "#96ECFF", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("珊瑚蜜桃", "#FF5E72", "#FF7F6E", "#FFB083", glowAlpha = 0.64, bandAlpha = 1.0),
                            preset("金柠荧光", "#FFE143", "#DFFF4E", "#B9FF5A", glowAlpha = 0.66, bandAlpha = 1.0),
                            preset("极光绿", "#00C9A7", "#00E5C0", "#80FFE8", glowAlpha = 0.70, bandAlpha = 1.0),
                            preset("星云紫", "#6A1BFF", "#9B44FF", "#D69AFF", glowAlpha = 0.72, bandAlpha = 1.0),
                            preset("日落晚霞", "#FF4B2B", "#FF8142", "#FFCB77", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("炽金流光", "#FFD700", "#FF9F00", "#FF6B35", glowAlpha = 0.70, bandAlpha = 1.0),
                            preset("烟霞粉紫", "#FF6B9D", "#CC65FE", "#9B5CF6", glowAlpha = 0.68, bandAlpha = 1.0),
                            preset("冰蓝闪电", "#00C3FF", "#4FC8FF", "#B4F0FF", glowAlpha = 0.66, bandAlpha = 1.0),
                        ),
                    ),
                ),
            ),
        )
        val style = parsed.defaultStyle

        assertEquals(16, parsed.presets.size)
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
        assertEquals(1.2030151f, style.renderTuning.bandAlphaMultiplier, 1e-6f)
        assertEquals(0.82f, style.renderTuning.glowBlurMultiplier, 0.0f)
        assertEquals(0.5969849f, style.renderTuning.highlightAlphaMax, 1e-6f)
        assertEquals(0.26f, style.renderTuning.highlightPeak, 0.0f)
        assertEquals(1.3699486f, style.renderTuning.highlightStrength, 1e-6f)
        assertEquals(0.20f, style.renderTuning.apexThinRadius, 0.0f)
    }

    @Test
    fun cloudConfigIgnoresMalformedPresetEntriesAndFallsBackToDefaultColor() {
        val parsed = TrackStyleCloudConfigLoader.parse(
            JsonObject(
                mapOf(
                    "defaultPresetName" to JsonPrimitive("青蓝薄荷"),
                    "defaultColorHex" to JsonPrimitive("#55F3D8"),
                    "presets" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("坏颜色"),
                                    "gradientColors" to JsonPrimitive("not-an-array"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(1, parsed.presets.size)
        assertEquals("默认颜色", parsed.defaultStyle.colorPreset.name)
        assertEquals(0xFF55F3D8, parsed.defaultColorHex)
        assertEquals(0xFF55F3D8, parsed.defaultStyle.colorHex)
    }

    @Test
    fun cloudConfigKeepsUsableFieldsWhenOptionalSectionsHaveWrongJsonTypes() {
        val parsed = TrackStyleCloudConfigLoader.parse(
            JsonObject(
                mapOf(
                    "defaultPresetName" to JsonPrimitive("部分有效"),
                    "defaultColorHex" to JsonPrimitive("#55F3D8"),
                    "defaultOpacity" to JsonPrimitive(0.70),
                    "styleDefaults" to JsonPrimitive("not-an-object"),
                    "renderTuning" to JsonArray(emptyList()),
                    "presets" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("部分有效"),
                                    "gradientColors" to JsonArray(
                                        listOf(
                                            JsonPrimitive("#102030"),
                                            JsonObject(emptyMap()),
                                            JsonPrimitive("#405060"),
                                        ),
                                    ),
                                    "glowBlur" to JsonPrimitive(-12.0),
                                    "glowAlpha" to JsonObject(emptyMap()),
                                    "bandAlpha" to JsonPrimitive(2.0),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val style = parsed.defaultStyle

        assertEquals("部分有效", style.colorPreset.name)
        assertEquals(listOf(0xFF102030, 0xFF405060), style.colorPreset.gradientColors)
        assertEquals(0.0f, style.colorPreset.glowBlur, 0.0f)
        assertEquals(0.68f, style.colorPreset.glowAlpha, 0.0f)
        assertEquals(1.0f, style.colorPreset.bandAlpha, 0.0f)
        assertEquals(0xFF55F3D8, parsed.defaultColorHex)
        assertEquals(0.70, style.colorAlpha, 0.0)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(70.0, style.tailHalfLife, 0.0)
        assertEquals(TrackRenderTuning(), style.renderTuning)
    }

    @Test
    fun cloudConfigFallsBackFromNonFiniteNumericPayloadsToIosDefaults() {
        val parsed = TrackStyleCloudConfigLoader.parse(
            JsonObject(
                mapOf(
                    "defaultPresetName" to JsonPrimitive("非有限颜色"),
                    "defaultColorHex" to JsonPrimitive("#55F3D8"),
                    "defaultOpacity" to JsonPrimitive(Double.NaN),
                    "styleDefaults" to JsonObject(
                        mapOf(
                            "widthPct" to JsonPrimitive(Double.NaN),
                            "tailHalfLife" to JsonPrimitive(Double.POSITIVE_INFINITY),
                            "speedLevel" to JsonPrimitive(Double.NaN),
                            "showDistance" to JsonPrimitive(true),
                            "totalDistance" to JsonPrimitive(Double.NaN),
                            "distanceUnit" to JsonPrimitive("yards"),
                        ),
                    ),
                    "renderTuning" to JsonObject(
                        mapOf(
                            "bandAlphaMultiplier" to JsonPrimitive(Float.NaN),
                            "highlightStart" to JsonPrimitive(Float.NaN),
                            "highlightEnd" to JsonPrimitive(Float.NaN),
                            "highlightPeak" to JsonPrimitive(Float.NaN),
                        ),
                    ),
                    "presets" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("非有限颜色"),
                                    "gradientColors" to JsonArray(listOf(JsonPrimitive("#102030"))),
                                    "glowBlur" to JsonPrimitive(Float.NaN),
                                    "glowAlpha" to JsonPrimitive(Float.NaN),
                                    "bandAlpha" to JsonPrimitive(Float.POSITIVE_INFINITY),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val style = parsed.defaultStyle

        assertEquals("非有限颜色", style.colorPreset.name)
        assertEquals(0.75, style.colorAlpha, 0.0)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(70.0, style.tailHalfLife, 0.0)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL, style.speedLevel, 0.0)
        assertEquals(GolfCamDistanceConfig.DEFAULT_DISTANCE, style.totalDistance, 0.0)
        assertEquals(30f, style.colorPreset.glowBlur, 0f)
        assertEquals(0.68f, style.colorPreset.glowAlpha, 0f)
        assertEquals(1f, style.colorPreset.bandAlpha, 0f)
        assertEquals(TrackRenderTuning().bandAlphaMultiplier, style.renderTuning.bandAlphaMultiplier, 0f)
        assertEquals(TrackRenderTuning().highlightStart, style.renderTuning.highlightStart, 0f)
        assertEquals(TrackRenderTuning().highlightEnd, style.renderTuning.highlightEnd, 0f)
        assertEquals(TrackRenderTuning().highlightPeak, style.renderTuning.highlightPeak, 0f)
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

    @Test
    fun cloudConfigRemoteEnvelopeAcceptsSuccessfulObjectOrStringObjectConfig() {
        val config = JsonObject(mapOf("defaultColorHex" to JsonPrimitive("#55F3D8")))
        assertEquals(
            config,
            TrackStyleCloudConfigLoader.parseRemoteConfigResponse(
                JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "config" to config,
                    ),
                ),
            ),
        )
        assertEquals(
            config,
            TrackStyleCloudConfigLoader.parseRemoteConfigResponse(
                JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "config" to JsonPrimitive(config.toString()),
                    ),
                ),
            ),
        )

        val explicitFailure = runCatching {
            TrackStyleCloudConfigLoader.parseRemoteConfigResponse(
                JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(false),
                        "error" to JsonPrimitive("remote down"),
                    ),
                ),
            )
        }
        assertTrue(explicitFailure.isFailure)
        assertEquals("remote down", explicitFailure.exceptionOrNull()?.message)

        val wrongSuccessType = runCatching {
            TrackStyleCloudConfigLoader.parseRemoteConfigResponse(
                JsonObject(
                    mapOf(
                        "success" to JsonPrimitive("true"),
                        "config" to config,
                    ),
                ),
            )
        }
        assertTrue(wrongSuccessType.isFailure)

        val wrongConfigType = runCatching {
            TrackStyleCloudConfigLoader.parseRemoteConfigResponse(
                JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "config" to JsonPrimitive("not-an-object"),
                    ),
                ),
            )
        }
        assertTrue(wrongConfigType.isFailure)
        assertEquals("missing track style config", wrongConfigType.exceptionOrNull()?.message)
    }

    @Test
    fun workflowStateStoreRestoresCompletedTrackStyleForDirectPreview() {
        val state = GolfCamWorkflowState()
        state.videoConfig = GolfCamVideoConfig(
            durationMs = 28_000L,
            frameRate = 60.0,
            visualWidth = 2160,
            visualHeight = 3840,
            rotationDegrees = 90,
        )
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(
            frameIndex = 72,
            timeMs = 2_200L,
            candidateFrames = listOf(CandidateImpactFrame(frameIndex = 72, confidence = 0.94f, timeSec = 1.2)),
        )
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.24f, normalizedY = 0.78f)
        state.landing = LandingOutput(normalizedX = 0.82f, normalizedY = 0.45f, flightTimeSec = 4.2)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.31f, 0.12f),
            controlPoint2 = NormalizedPoint(0.62f, -0.08f),
        )
        val style = TrackStyle(
            colorPreset = TrackColorPreset(
                name = "线上测试色",
                gradientColors = listOf(0xFF102030, 0xFF405060, 0xFF708090),
                glowBlur = 26f,
                glowAlpha = 0.58f,
                bandAlpha = 0.73f,
            ),
            colorHex = 0xFF405060,
            colorAlpha = 0.66,
            glowOpacity = 0.58,
            widthPct = 88.0,
            tailHalfLife = 55.0,
            showDistance = true,
            totalDistance = 188.0,
            distanceUnit = DistanceUnit.METERS,
            speedLevel = 0.55,
            renderTuning = TrackRenderTuning(
                bandAlphaMultiplier = 1.2f,
                highlightAlphaMax = 0.59f,
                highlightPeak = 0.26f,
            ),
        )
        state.trackStyle = TrackStyleSelection(style = style, flightCurve = state.flightCurve!!)

        val restored = GolfCamWorkflowStateStore.decode(GolfCamWorkflowStateStore.encode(state))

        assertEquals(GolfCamStep.TRACK_STYLE, restored.completedStep)
        assertEquals(28_000L, restored.videoConfig?.durationMs)
        assertEquals(60.0, restored.videoConfig?.frameRate ?: -1.0, 0.0)
        assertEquals(2160, restored.videoConfig?.visualWidth)
        assertEquals(3840, restored.videoConfig?.visualHeight)
        assertEquals(90, restored.videoConfig?.rotationDegrees)
        assertEquals(1_000L, restored.trimResult?.startMs)
        assertEquals(72, restored.impactFrame?.frameIndex)
        assertEquals(0.24f, restored.impactPosition?.normalizedX ?: -1f, 0.0f)
        assertEquals(4.2, restored.landing?.flightTimeSec ?: -1.0, 0.0)
        assertEquals(0.31f, restored.flightCurve?.controlPoint1?.x ?: -1f, 0.0f)
        assertEquals("线上测试色", restored.trackStyle?.style?.colorPreset?.name)
        assertEquals(0xFF405060, restored.trackStyle?.style?.colorHex)
        assertEquals(188.0, restored.trackStyle?.style?.totalDistance ?: -1.0, 0.0)
        assertEquals(DistanceUnit.METERS, restored.trackStyle?.style?.distanceUnit)
        assertEquals(1.2f, restored.trackStyle?.style?.renderTuning?.bandAlphaMultiplier ?: -1f, 0.0f)
    }

    @Test
    fun workflowStateStoreFallsBackFromNonFiniteCachedValuesBeforePreview() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(12_000L),
                            "frameRate" to JsonPrimitive(60.0),
                            "visualWidth" to JsonPrimitive(2160),
                            "visualHeight" to JsonPrimitive(3840),
                            "rotationDegrees" to JsonPrimitive(90),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf("startMs" to JsonPrimitive(1_000L), "endMs" to JsonPrimitive(7_000L)),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                            "candidateFrames" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(60),
                                            "confidence" to JsonPrimitive(Float.NaN),
                                            "timeSec" to JsonPrimitive(Double.NaN),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(Float.NaN), "y" to JsonPrimitive(Float.POSITIVE_INFINITY)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(Float.NaN),
                            "normalizedY" to JsonPrimitive(Float.NEGATIVE_INFINITY),
                            "flightTimeSec" to JsonPrimitive(Double.NaN),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(Float.NaN), "y" to JsonPrimitive(Float.POSITIVE_INFINITY)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(Float.NEGATIVE_INFINITY), "y" to JsonPrimitive(Float.NaN)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "colorPreset" to JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("非有限缓存色"),
                                    "gradientColors" to JsonArray(listOf(JsonPrimitive(0xFF55F3D8))),
                                    "glowBlur" to JsonPrimitive(Float.NaN),
                                    "glowAlpha" to JsonPrimitive(Float.NaN),
                                    "bandAlpha" to JsonPrimitive(Float.POSITIVE_INFINITY),
                                ),
                            ),
                            "colorAlpha" to JsonPrimitive(Double.NaN),
                            "glowOpacity" to JsonPrimitive(Double.POSITIVE_INFINITY),
                            "widthPct" to JsonPrimitive(Double.NaN),
                            "tailHalfLife" to JsonPrimitive(Double.NaN),
                            "totalDistance" to JsonPrimitive(Double.NaN),
                            "speedLevel" to JsonPrimitive(Double.NaN),
                            "renderTuning" to JsonObject(
                                mapOf(
                                    "bandAlphaMultiplier" to JsonPrimitive(Float.NaN),
                                    "highlightStart" to JsonPrimitive(Float.NaN),
                                    "highlightEnd" to JsonPrimitive(Float.NaN),
                                    "highlightPeak" to JsonPrimitive(Float.NaN),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val style = restored.trackStyle?.style ?: error("missing restored style")

        assertTrue(restored.isComplete)
        assertEquals(0.5f, restored.impactPosition?.normalizedX ?: -1f, 0f)
        assertEquals(0.5f, restored.impactPosition?.normalizedY ?: -1f, 0f)
        assertEquals(0.5f, restored.landing?.normalizedX ?: -1f, 0f)
        assertEquals(0.5f, restored.landing?.normalizedY ?: -1f, 0f)
        assertEquals(4.5, restored.landing?.flightTimeSec ?: -1.0, 0.0)
        assertEquals(0.5f, restored.flightCurve?.controlPoint1?.x ?: -1f, 0f)
        assertEquals(0.5f, restored.flightCurve?.controlPoint1?.y ?: -1f, 0f)
        assertEquals(0.5f, restored.flightCurve?.controlPoint2?.x ?: -1f, 0f)
        assertEquals(0.5f, restored.flightCurve?.controlPoint2?.y ?: -1f, 0f)
        assertEquals(0f, restored.impactFrame?.candidateFrames?.firstOrNull()?.confidence ?: -1f, 0f)
        assertEquals(0.0, restored.impactFrame?.candidateFrames?.firstOrNull()?.timeSec ?: -1.0, 0.0)
        assertEquals(0.75, style.colorAlpha, 0.0)
        assertEquals(0.68, style.glowOpacity, 1e-6)
        assertEquals(40.0, style.widthPct, 0.0)
        assertEquals(70.0, style.tailHalfLife, 0.0)
        assertEquals(GolfCamDistanceConfig.DEFAULT_DISTANCE, style.totalDistance, 0.0)
        assertEquals(GolfCamSpeedConfig.DEFAULT_LEVEL, style.speedLevel, 0.0)
        assertEquals(30f, style.colorPreset.glowBlur, 0f)
        assertEquals(0.68f, style.colorPreset.glowAlpha, 0f)
        assertEquals(1f, style.colorPreset.bandAlpha, 0f)
        assertEquals(TrackRenderTuning().bandAlphaMultiplier, style.renderTuning.bandAlphaMultiplier, 0f)
        assertEquals(TrackRenderTuning().highlightStart, style.renderTuning.highlightStart, 0f)
        assertEquals(TrackRenderTuning().highlightEnd, style.renderTuning.highlightEnd, 0f)
        assertEquals(TrackRenderTuning().highlightPeak, style.renderTuning.highlightPeak, 0f)
    }

    @Test
    fun workflowStateStoreRestoresPartialProgressWithoutPretendingPreviewIsReady() {
        val state = GolfCamWorkflowState()
        state.trimResult = TrimResult(startMs = 2_000L, endMs = 12_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 45, timeMs = 3_500L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.36f, normalizedY = 0.72f)

        val restored = GolfCamWorkflowStateStore.decode(GolfCamWorkflowStateStore.encode(state))

        assertEquals(GolfCamStep.IMPACT_POSITION, restored.completedStep)
        assertFalse(restored.isComplete)
        assertEquals(null, restored.trackStyle)
        assertEquals(2_000L, restored.trimResult?.startMs)
        assertEquals(45, restored.impactFrame?.frameIndex)
        assertEquals(0.36f, restored.impactPosition?.normalizedX ?: -1f, 0.0f)
    }

    @Test
    fun workflowStateStoreClampsRestoredImpactTimeInsideHalfOpenTrimRange() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(12_000L),
                            "frameRate" to JsonPrimitive(60.0),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(359),
                            "timeMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(6_983L, restored.impactFrame?.timeMs)
        assertEquals(359, restored.impactFrame?.frameIndex)
        assertTrue(restored.hasCompletedImpactFrame)
        assertEquals(GolfCamStep.IMPACT_FRAME, restored.completedStep)
    }

    @Test
    fun workflowStateStoreRecomputesRestoredImpactFrameIndexFromTimeWhenFpsIsKnown() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(12_000L),
                            "frameRate" to JsonPrimitive(60.0),
                            "visualWidth" to JsonPrimitive(1080),
                            "visualHeight" to JsonPrimitive(1920),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(10),
                            "timeMs" to JsonPrimitive(2_500L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(90, restored.impactFrame?.frameIndex)
        assertEquals(2_500L, restored.impactFrame?.timeMs)
        assertTrue(restored.hasCompletedImpactFrame)
        assertEquals(GolfCamStep.IMPACT_FRAME, restored.completedStep)
    }

    @Test
    fun entryPreviewStateIsDerivedFromCurrentCompletedWorkflowOnly() {
        val state = GolfCamWorkflowState()
        state.videoConfig = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 60.0,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        val selection = TrackStyleSelection(style = TrackStyle(), flightCurve = state.flightCurve!!)
        state.trackStyle = selection

        val preview = previewStateFromWorkflow(state)

        assertEquals(selection, preview?.selection)
        assertEquals(state.videoConfig, preview?.videoConfig)
        assertEquals(2_000L, preview?.impactTimeMs)
        assertEquals(1_000L, preview?.trimStartMs)
        assertEquals(7_000L, preview?.trimEndMs)
    }

    @Test
    fun entryPreviewStateRequiresDetectedVideoConfigBeforeDirectPreview() {
        val state = GolfCamWorkflowState()
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        state.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = state.flightCurve!!)

        assertTrue(state.isComplete)
        assertEquals(null, previewStateFromWorkflow(state))

        state.videoConfig = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 60.0,
            visualWidth = 1,
            visualHeight = 1,
            rotationDegrees = 90,
        )
        assertTrue(shouldDetectGolfCamVideoConfig(state.videoConfig))
        assertEquals(null, previewStateFromWorkflow(state))

        state.videoConfig = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 60.0,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )

        assertEquals(2_000L, previewStateFromWorkflow(state)?.impactTimeMs)
        assertEquals(state.videoConfig, previewStateFromWorkflow(state)?.videoConfig)
    }

    @Test
    fun entryPreviewStateDropsStaleStyleAfterUpstreamReEdit() {
        val state = GolfCamWorkflowState()
        state.videoConfig = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 60.0,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        state.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = state.flightCurve!!)
        assertTrue(state.isComplete)

        state.updateImpactPosition(ImpactPositionSelection(normalizedX = 0.35f, normalizedY = 0.70f))

        assertFalse(state.isComplete)
        assertEquals(null, state.trackStyle)
        assertEquals(null, previewStateFromWorkflow(state))
    }

    @Test
    fun entryPreviewStateRejectsUnclampedTrackStyleBeforeExport() {
        val state = GolfCamWorkflowState()
        state.videoConfig = GolfCamVideoConfig(
            durationMs = 18_000L,
            frameRate = 60.0,
            visualWidth = 1080,
            visualHeight = 1920,
            rotationDegrees = 90,
        )
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        state.trackStyle = TrackStyleSelection(
            style = TrackStyle(
                widthPct = 320.0,
                speedLevel = 2.0,
                activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5,
                renderTuning = TrackRenderTuning(widthTaperPower = 9.0f),
            ),
            flightCurve = state.flightCurve!!,
        )

        assertFalse(state.hasCompletedTrackStyle)
        assertFalse(state.isComplete)
        assertEquals(null, previewStateFromWorkflow(state))

        state.updateTrackStyle(state.trackStyle!!)

        assertTrue(state.hasCompletedTrackStyle)
        assertTrue(state.isComplete)
        assertEquals(160.0, previewStateFromWorkflow(state)?.selection?.style?.widthPct ?: -1.0, 0.0)
        assertEquals(2.20f, previewStateFromWorkflow(state)?.selection?.style?.renderTuning?.widthTaperPower ?: -1f, 0f)
    }

    @Test
    fun previewExportSelectionNormalizesDirectCallerInputBeforeRendering() {
        val rawSelection = TrackStyleSelection(
            style = TrackStyle(
                widthPct = 320.0,
                tailHalfLife = 1.0,
                totalDistance = 2_000.0,
                speedLevel = 2.0,
                activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5,
                compareTrajectoryAlgorithms = true,
                trajectoryPreset = TrajectoryPresetKind.LOW_PIERCING,
                renderTuning = TrackRenderTuning(widthTaperPower = 9.0f),
            ),
            flightCurve = FlightCurveSelection(
                impactPosition = ImpactPositionSelection(normalizedX = -0.25f, normalizedY = 1.25f),
                landingPoint = LandingOutput(normalizedX = 1.25f, normalizedY = -0.25f, flightTimeSec = 12.0),
                controlPoint1 = NormalizedPoint(-0.25f, -2.0f),
                controlPoint2 = NormalizedPoint(1.25f, 2.0f),
            ),
        )

        val normalized = normalizedPreviewExportSelection(rawSelection)

        assertEquals(0f, normalized.flightCurve.impactPosition.normalizedX, 0f)
        assertEquals(1f, normalized.flightCurve.impactPosition.normalizedY, 0f)
        assertEquals(1f, normalized.flightCurve.landingPoint.normalizedX, 0f)
        assertEquals(0f, normalized.flightCurve.landingPoint.normalizedY, 0f)
        assertEquals(8.0, normalized.flightCurve.landingPoint.flightTimeSec, 0.0)
        assertEquals(0f, normalized.flightCurve.controlPoint1.x, 0f)
        assertEquals(-1f, normalized.flightCurve.controlPoint1.y, 0f)
        assertEquals(1f, normalized.flightCurve.controlPoint2.x, 0f)
        assertEquals(1.5f, normalized.flightCurve.controlPoint2.y, 0f)
        assertEquals(160.0, normalized.style.widthPct, 0.0)
        assertEquals(5.0, normalized.style.tailHalfLife, 0.0)
        assertEquals(1000.0, normalized.style.totalDistance, 0.0)
        assertEquals(GolfCamSpeedConfig.MAX_LEVEL, normalized.style.speedLevel, 0.0)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, normalized.style.activeTrajectoryAlgorithm)
        assertFalse(normalized.style.compareTrajectoryAlgorithms)
        assertEquals(TrajectoryPresetKind.GOLF_CAM_LIKE, normalized.style.trajectoryPreset)
        assertEquals(2.20f, normalized.style.renderTuning.widthTaperPower, 0f)
    }

    @Test
    fun previewExportSelectionNormalizationIsIdempotentAcrossPreviewAndExportBoundary() {
        val normalizedOnce = normalizedPreviewExportSelection(
            TrackStyleSelection(
                style = TrackStyle(
                    widthPct = 320.0,
                    tailHalfLife = 1.0,
                    totalDistance = 2_000.0,
                    speedLevel = 2.0,
                    activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.CURRENT_V5,
                    compareTrajectoryAlgorithms = true,
                    trajectoryPreset = TrajectoryPresetKind.LOW_PIERCING,
                    renderTuning = TrackRenderTuning(widthTaperPower = 9.0f),
                ),
                flightCurve = FlightCurveSelection(
                    impactPosition = ImpactPositionSelection(normalizedX = -0.25f, normalizedY = 1.25f),
                    landingPoint = LandingOutput(normalizedX = 1.25f, normalizedY = -0.25f, flightTimeSec = 12.0),
                    controlPoint1 = NormalizedPoint(-0.25f, -2.0f),
                    controlPoint2 = NormalizedPoint(1.25f, 2.0f),
                ),
            ),
        )

        assertEquals(normalizedOnce, normalizedPreviewExportSelection(normalizedOnce))
    }

    @Test
    fun workflowStateCompletionRequiresContinuousIosStepOutputs() {
        val orphanStyleState = GolfCamWorkflowState()
        val impact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        val curve = FlightCurveSelection(
            impactPosition = impact,
            landingPoint = landing,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        orphanStyleState.flightCurve = curve
        orphanStyleState.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)

        assertEquals(GolfCamStep.TRIM, orphanStyleState.completedStep)
        assertFalse(orphanStyleState.isComplete)
        assertFalse(orphanStyleState.hasCompletedTrackStyle)

        val completeState = GolfCamWorkflowState()
        completeState.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        completeState.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        completeState.impactPosition = impact
        completeState.landing = landing
        completeState.flightCurve = curve
        completeState.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)

        assertEquals(GolfCamStep.TRACK_STYLE, completeState.completedStep)
        assertTrue(completeState.isComplete)
        assertTrue(completeState.hasCompletedTrackStyle)
    }

    @Test
    fun workflowStateVideoConfigUpdatePrunesDownstreamProgressWhenTimingBecomesInvalid() {
        fun completeState() = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
            landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
            flightCurve = FlightCurveSelection(
                impactPosition = impactPosition!!,
                landingPoint = landing!!,
                controlPoint1 = NormalizedPoint(0.30f, 0.20f),
                controlPoint2 = NormalizedPoint(0.62f, 0.16f),
            )
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = flightCurve!!)
        }

        val matchingConfig = completeState().apply {
            updateVideoConfig(GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0))
        }
        val mismatchedFrameRate = completeState().apply {
            updateVideoConfig(GolfCamVideoConfig(durationMs = 12_000L, frameRate = 30.0))
        }
        val trimOutsideDetectedDuration = completeState().apply {
            updateVideoConfig(GolfCamVideoConfig(durationMs = 6_000L, frameRate = 60.0))
        }
        val invalidDetectedDuration = completeState().apply {
            updateVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 1L,
                    frameRate = 60.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                ),
            )
        }
        val invalidFpsWouldOtherwiseMatchOneFps = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 2, timeMs = 3_000L)
            impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
            landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
            flightCurve = FlightCurveSelection(
                impactPosition = impactPosition!!,
                landingPoint = landing!!,
                controlPoint1 = NormalizedPoint(0.30f, 0.20f),
                controlPoint2 = NormalizedPoint(0.62f, 0.16f),
            )
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = flightCurve!!)
            updateVideoConfig(
                GolfCamVideoConfig(
                    durationMs = 12_000L,
                    frameRate = 0.0,
                    visualWidth = 1080,
                    visualHeight = 1920,
                ),
            )
        }

        assertTrue(matchingConfig.isComplete)
        assertEquals(GolfCamStep.TRACK_STYLE, matchingConfig.resumeStep)

        assertTrue(mismatchedFrameRate.hasCompletedTrim)
        assertFalse(mismatchedFrameRate.hasCompletedImpactFrame)
        assertEquals(null, mismatchedFrameRate.impactPosition)
        assertEquals(null, mismatchedFrameRate.trackStyle)
        assertEquals(GolfCamStep.IMPACT_FRAME, mismatchedFrameRate.resumeStep)

        assertFalse(trimOutsideDetectedDuration.hasCompletedTrim)
        assertEquals(null, trimOutsideDetectedDuration.impactFrame)
        assertEquals(null, trimOutsideDetectedDuration.trackStyle)
        assertEquals(GolfCamStep.TRIM, trimOutsideDetectedDuration.resumeStep)

        assertFalse(invalidDetectedDuration.hasCompletedTrim)
        assertEquals(null, invalidDetectedDuration.impactFrame)
        assertEquals(null, invalidDetectedDuration.impactPosition)
        assertEquals(null, invalidDetectedDuration.trackStyle)
        assertEquals(GolfCamStep.TRIM, invalidDetectedDuration.resumeStep)
        assertTrue(shouldDetectGolfCamVideoConfig(invalidDetectedDuration.videoConfig))

        assertTrue(invalidFpsWouldOtherwiseMatchOneFps.hasCompletedTrim)
        assertFalse(invalidFpsWouldOtherwiseMatchOneFps.hasCompletedImpactFrame)
        assertEquals(2, invalidFpsWouldOtherwiseMatchOneFps.impactFrame?.frameIndex)
        assertEquals(null, invalidFpsWouldOtherwiseMatchOneFps.impactPosition)
        assertEquals(null, invalidFpsWouldOtherwiseMatchOneFps.trackStyle)
        assertEquals(GolfCamStep.IMPACT_FRAME, invalidFpsWouldOtherwiseMatchOneFps.resumeStep)
    }

    @Test
    fun workflowStateCompletionRejectsInvalidTrimAndImpactFrameAnchors() {
        val tooShortTrim = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 1_200L)
        }
        val impactBeforeTrim = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 900L)
        }
        val impactAfterTrim = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 7_001L)
        }
        val impactExactlyAtTrimEndWithoutFps = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 7_000L)
        }
        val trimBeyondKnownVideoDuration = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 6_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        }
        val frameBeyondTrimAtKnownFps = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 360, timeMs = 2_000L)
        }
        val mismatchedFrameTimeAtKnownFps = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 7_000L)
        }
        val withinOneFrameTolerance = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_016L)
        }
        val validImpact = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 359, timeMs = 6_983L)
        }

        assertFalse(tooShortTrim.hasCompletedTrim)
        assertFalse(tooShortTrim.canOpenWorkflowStep(GolfCamStep.IMPACT_FRAME))
        assertFalse(impactBeforeTrim.hasCompletedImpactFrame)
        assertFalse(impactAfterTrim.hasCompletedImpactFrame)
        assertFalse(impactExactlyAtTrimEndWithoutFps.hasCompletedImpactFrame)
        assertFalse(trimBeyondKnownVideoDuration.hasCompletedTrim)
        assertFalse(trimBeyondKnownVideoDuration.canOpenWorkflowStep(GolfCamStep.IMPACT_FRAME))
        assertFalse(trimBeyondKnownVideoDuration.hasCompletedImpactFrame)
        assertFalse(frameBeyondTrimAtKnownFps.hasCompletedImpactFrame)
        assertFalse(mismatchedFrameTimeAtKnownFps.hasCompletedImpactFrame)
        assertTrue(withinOneFrameTolerance.hasCompletedImpactFrame)
        assertTrue(validImpact.hasCompletedImpactFrame)
    }

    @Test
    fun workflowStateCompletionRejectsInvalidCoordinatesFlightTimeAndCurveControls() {
        val validImpact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val validLanding = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        fun baseState() = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        }

        val invalidImpactPosition = baseState().apply {
            impactPosition = ImpactPositionSelection(normalizedX = 1.2f, normalizedY = 0.75f)
        }
        val invalidLandingCoordinate = baseState().apply {
            impactPosition = validImpact
            landing = LandingOutput(normalizedX = 0.80f, normalizedY = -0.1f, flightTimeSec = 4.5)
        }
        val invalidLandingTime = baseState().apply {
            impactPosition = validImpact
            landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 8.1)
        }
        val invalidCurveControl = baseState().apply {
            impactPosition = validImpact
            landing = validLanding
            flightCurve = FlightCurveSelection(
                impactPosition = validImpact,
                landingPoint = validLanding,
                controlPoint1 = NormalizedPoint(0.30f, -1.1f),
                controlPoint2 = NormalizedPoint(0.62f, 0.16f),
            )
        }
        val validCurve = baseState().apply {
            impactPosition = validImpact
            landing = validLanding
            flightCurve = FlightCurveSelection(
                impactPosition = validImpact,
                landingPoint = validLanding,
                controlPoint1 = NormalizedPoint(0.30f, -1.0f),
                controlPoint2 = NormalizedPoint(1.0f, 1.5f),
            )
        }

        assertFalse(invalidImpactPosition.hasCompletedImpactPosition)
        assertFalse(invalidLandingCoordinate.hasCompletedLanding)
        assertFalse(invalidLandingTime.hasCompletedLanding)
        assertFalse(invalidCurveControl.hasCompletedFlightCurve)
        assertTrue(validCurve.hasCompletedFlightCurve)
    }

    @Test
    fun workflowStateCompletionRequiresCurveAndStyleToMatchCurrentAnchors() {
        val impact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        val staleLanding = LandingOutput(normalizedX = 0.72f, normalizedY = 0.40f, flightTimeSec = 4.5)
        val curve = FlightCurveSelection(
            impactPosition = impact,
            landingPoint = landing,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        val staleCurve = curve.copy(landingPoint = staleLanding)

        val staleCurveState = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = staleCurve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = staleCurve)
        }

        assertEquals(GolfCamStep.LANDING, staleCurveState.completedStep)
        assertFalse(staleCurveState.hasCompletedFlightCurve)
        assertFalse(staleCurveState.isComplete)

        val staleStyleState = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = curve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = staleCurve)
        }

        assertEquals(GolfCamStep.FLIGHT_CURVE, staleStyleState.completedStep)
        assertTrue(staleStyleState.hasCompletedFlightCurve)
        assertFalse(staleStyleState.hasCompletedTrackStyle)
        assertFalse(staleStyleState.isComplete)
    }

    @Test
    fun workflowStateResumeStepStartsAtNextIncompleteIosStep() {
        val impact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        val curve = FlightCurveSelection(
            impactPosition = impact,
            landingPoint = landing,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )

        assertEquals(GolfCamStep.TRIM, GolfCamWorkflowState().resumeStep)

        val trimOnly = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        }
        assertEquals(GolfCamStep.TRIM, trimOnly.completedStep)
        assertEquals(GolfCamStep.IMPACT_FRAME, trimOnly.resumeStep)

        val withImpactFrame = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        }
        assertEquals(GolfCamStep.IMPACT_FRAME, withImpactFrame.completedStep)
        assertEquals(GolfCamStep.IMPACT_POSITION, withImpactFrame.resumeStep)

        val withImpactPosition = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
        }
        assertEquals(GolfCamStep.IMPACT_POSITION, withImpactPosition.completedStep)
        assertEquals(GolfCamStep.LANDING, withImpactPosition.resumeStep)

        val withLanding = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
        }
        assertEquals(GolfCamStep.LANDING, withLanding.completedStep)
        assertEquals(GolfCamStep.FLIGHT_CURVE, withLanding.resumeStep)

        val withCurve = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = curve
        }
        assertEquals(GolfCamStep.FLIGHT_CURVE, withCurve.completedStep)
        assertEquals(GolfCamStep.TRACK_STYLE, withCurve.resumeStep)

        val complete = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = curve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)
        }
        assertEquals(GolfCamStep.TRACK_STYLE, complete.completedStep)
        assertEquals(GolfCamStep.TRACK_STYLE, complete.resumeStep)

        val orphanStyleState = GolfCamWorkflowState().apply {
            flightCurve = curve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)
        }
        assertEquals(GolfCamStep.TRIM, orphanStyleState.completedStep)
        assertEquals(GolfCamStep.TRIM, orphanStyleState.resumeStep)
    }

    @Test
    fun workflowStepBadgesCannotSkipAheadButKeepCompletedStepsVisibleWhenReediting() {
        val emptyState = GolfCamWorkflowState()
        assertTrue(emptyState.canOpenWorkflowStep(GolfCamStep.TRIM))
        assertFalse(emptyState.canOpenWorkflowStep(GolfCamStep.IMPACT_FRAME))
        assertFalse(emptyState.hasCompletedWorkflowStep(GolfCamStep.TRIM))
        assertEquals(GolfCamStep.TRIM, coerceCurrentWorkflowStep(GolfCamStep.TRACK_STYLE, emptyState))

        val trimOnly = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        }
        assertTrue(trimOnly.canOpenWorkflowStep(GolfCamStep.IMPACT_FRAME))
        assertFalse(trimOnly.canOpenWorkflowStep(GolfCamStep.IMPACT_POSITION))
        assertTrue(trimOnly.hasCompletedWorkflowStep(GolfCamStep.TRIM))
        assertFalse(trimOnly.hasCompletedWorkflowStep(GolfCamStep.IMPACT_FRAME))
        assertEquals(GolfCamStep.IMPACT_FRAME, coerceCurrentWorkflowStep(GolfCamStep.TRACK_STYLE, trimOnly))

        val impact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        val curve = FlightCurveSelection(
            impactPosition = impact,
            landingPoint = landing,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        val complete = GolfCamWorkflowState().apply {
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = curve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)
        }

        GolfCamStep.values().forEach { step ->
            assertTrue(complete.canOpenWorkflowStep(step))
            assertTrue(complete.hasCompletedWorkflowStep(step))
            assertEquals(step, coerceCurrentWorkflowStep(step, complete))
        }
    }

    @Test
    fun workflowCurrentStepFallsBackWhenDetectedVideoConfigInvalidatesProgress() {
        val impact = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        val landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        val curve = FlightCurveSelection(
            impactPosition = impact,
            landingPoint = landing,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        val state = GolfCamWorkflowState().apply {
            videoConfig = GolfCamVideoConfig(durationMs = 12_000L, frameRate = 60.0)
            trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
            impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
            impactPosition = impact
            this.landing = landing
            flightCurve = curve
            trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = curve)
        }

        assertEquals(GolfCamStep.TRACK_STYLE, coerceCurrentWorkflowStep(GolfCamStep.TRACK_STYLE, state))

        state.updateVideoConfig(GolfCamVideoConfig(durationMs = 12_000L, frameRate = 30.0))

        assertEquals(GolfCamStep.IMPACT_FRAME, state.resumeStep)
        assertFalse(state.canOpenWorkflowStep(GolfCamStep.TRACK_STYLE))
        assertEquals(GolfCamStep.IMPACT_FRAME, coerceCurrentWorkflowStep(GolfCamStep.TRACK_STYLE, state))
    }

    @Test
    fun workflowStateInvalidatesDownstreamOutputsWhenUpstreamAnchorChanges() {
        val state = GolfCamWorkflowState()
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        state.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = state.flightCurve!!)

        state.updateImpactPosition(ImpactPositionSelection(normalizedX = 0.30f, normalizedY = 0.70f))

        assertEquals(GolfCamStep.IMPACT_POSITION, state.completedStep)
        assertEquals(null, state.landing)
        assertEquals(null, state.flightCurve)
        assertEquals(null, state.trackStyle)
    }

    @Test
    fun workflowStateKeepsDownstreamOutputsWhenImpactFrameOnlyRefreshesCandidates() {
        val state = GolfCamWorkflowState()
        state.trimResult = TrimResult(startMs = 1_000L, endMs = 7_000L)
        state.impactFrame = ImpactFrameSelection(frameIndex = 60, timeMs = 2_000L)
        state.impactPosition = ImpactPositionSelection(normalizedX = 0.25f, normalizedY = 0.75f)
        state.landing = LandingOutput(normalizedX = 0.80f, normalizedY = 0.45f, flightTimeSec = 4.5)
        state.flightCurve = FlightCurveSelection(
            impactPosition = state.impactPosition!!,
            landingPoint = state.landing!!,
            controlPoint1 = NormalizedPoint(0.30f, 0.20f),
            controlPoint2 = NormalizedPoint(0.62f, 0.16f),
        )
        state.trackStyle = TrackStyleSelection(style = TrackStyle(), flightCurve = state.flightCurve!!)

        state.updateImpactFrame(
            ImpactFrameSelection(
                frameIndex = 60,
                timeMs = 2_000L,
                candidateFrames = listOf(CandidateImpactFrame(frameIndex = 60, confidence = 0.95f, timeSec = 1.0)),
            ),
        )

        assertEquals(GolfCamStep.TRACK_STYLE, state.completedStep)
        assertTrue(state.impactFrame?.candidateFrames?.isNotEmpty() == true)
        assertTrue(state.hasCompletedTrackStyle)
    }

    @Test
    fun workflowStateStoreToleratesMalformedArrayFieldsInCachedJson() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(-12L),
                            "frameRate" to JsonPrimitive(400.0),
                            "visualWidth" to JsonPrimitive(0),
                            "visualHeight" to JsonPrimitive(-4),
                            "rotationDegrees" to JsonPrimitive(42),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(18),
                            "timeMs" to JsonPrimitive(1_800L),
                            "candidateFrames" to JsonPrimitive("not-an-array"),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.42f), "y" to JsonPrimitive(0.76f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.82f),
                            "normalizedY" to JsonPrimitive(0.48f),
                            "flightTimeSec" to JsonPrimitive(3.8),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.28f), "y" to JsonPrimitive(0.18f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.58f), "y" to JsonPrimitive(0.12f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "colorPreset" to JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("坏缓存色"),
                                    "gradientColors" to JsonPrimitive("not-an-array"),
                                ),
                            ),
                            "distanceUnit" to JsonPrimitive("UNKNOWN_UNIT"),
                            "activeTrajectoryAlgorithm" to JsonPrimitive("UNKNOWN_ALGORITHM"),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(GolfCamStep.TRIM, restored.completedStep)
        assertFalse(restored.isComplete)
        assertEquals(1L, restored.videoConfig?.durationMs)
        assertEquals(0.0, restored.videoConfig?.frameRate ?: -1.0, 0.0)
        assertEquals(1, restored.videoConfig?.visualWidth)
        assertEquals(1, restored.videoConfig?.visualHeight)
        assertEquals(0, restored.videoConfig?.rotationDegrees)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
        assertEquals(null, restored.impactFrame)
        assertEquals(null, restored.impactPosition)
        assertEquals(null, restored.landing)
        assertEquals(null, restored.flightCurve)
        assertEquals(null, restored.trackStyle)
    }

    @Test
    fun workflowStateStoreMarksWrongFrameRateTypeForRedetectionAndPrunesDownstreamProgress() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(18_000L),
                            "frameRate" to JsonObject(emptyMap()),
                            "visualWidth" to JsonPrimitive(1080),
                            "visualHeight" to JsonPrimitive(1920),
                            "rotationDegrees" to JsonArray(emptyList()),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                            "candidateFrames" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(60),
                                            "confidence" to JsonObject(emptyMap()),
                                            "timeSec" to JsonPrimitive(1.0),
                                        ),
                                    ),
                                    JsonArray(emptyList()),
                                ),
                            ),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.25f), "y" to JsonPrimitive(0.75f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.80f),
                            "normalizedY" to JsonPrimitive(0.45f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.30f), "y" to JsonPrimitive(0.20f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.62f), "y" to JsonPrimitive(0.16f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "colorPreset" to JsonObject(
                                mapOf(
                                    "name" to JsonObject(emptyMap()),
                                    "gradientColors" to JsonArray(
                                        listOf(
                                            JsonPrimitive(0xFF55F3D8),
                                            JsonObject(emptyMap()),
                                        ),
                                    ),
                                    "glowAlpha" to JsonArray(emptyList()),
                                    "bandAlpha" to JsonPrimitive(1.8),
                                ),
                            ),
                            "colorAlpha" to JsonObject(emptyMap()),
                            "widthPct" to JsonPrimitive(42.0),
                            "distanceUnit" to JsonObject(emptyMap()),
                            "activeTrajectoryAlgorithm" to JsonArray(emptyList()),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(GolfCamStep.TRIM, restored.completedStep)
        assertEquals(18_000L, restored.videoConfig?.durationMs)
        assertEquals(0.0, restored.videoConfig?.frameRate ?: -1.0, 0.0)
        assertEquals(1080, restored.videoConfig?.visualWidth)
        assertEquals(1920, restored.videoConfig?.visualHeight)
        assertEquals(0, restored.videoConfig?.rotationDegrees)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
        assertEquals(60, restored.impactFrame?.frameIndex)
        assertFalse(restored.hasCompletedImpactFrame)
        assertEquals(emptyList<CandidateImpactFrame>(), restored.impactFrame?.candidateFrames)
        assertEquals(GolfCamStep.IMPACT_FRAME, restored.resumeStep)
        assertEquals(null, restored.impactPosition)
        assertEquals(null, restored.landing)
        assertEquals(null, restored.flightCurve)
        assertEquals(null, restored.trackStyle)
    }

    @Test
    fun workflowStateStoreNormalizesRestoredImpactCandidatesToCurrentTrimAndFrameRate() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(18_000L),
                            "frameRate" to JsonPrimitive(60.0),
                            "visualWidth" to JsonPrimitive(1080),
                            "visualHeight" to JsonPrimitive(1920),
                            "rotationDegrees" to JsonPrimitive(90),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(90),
                            "timeMs" to JsonPrimitive(2_500L),
                            "candidateFrames" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(-1),
                                            "confidence" to JsonPrimitive(0.9f),
                                            "timeSec" to JsonPrimitive(0.25),
                                        ),
                                    ),
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(999),
                                            "confidence" to JsonPrimitive(1.4f),
                                            "timeSec" to JsonPrimitive(0.5),
                                        ),
                                    ),
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(12),
                                            "confidence" to JsonPrimitive(-0.2f),
                                            "timeSec" to JsonPrimitive(5.983),
                                        ),
                                    ),
                                    JsonObject(
                                        mapOf(
                                            "frameIndex" to JsonPrimitive(24),
                                            "confidence" to JsonPrimitive(0.6f),
                                            "timeSec" to JsonPrimitive(6.1),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val candidates = restored.impactFrame?.candidateFrames.orEmpty()

        assertEquals(2, candidates.size)
        assertEquals(30, candidates[0].frameIndex)
        assertEquals(0.5, candidates[0].timeSec, 0.0)
        assertEquals(1.0f, candidates[0].confidence, 0.0f)
        assertEquals(359, candidates[1].frameIndex)
        assertEquals(5.983, candidates[1].timeSec, 0.0)
        assertEquals(0.0f, candidates[1].confidence, 0.0f)
        assertTrue(restored.hasCompletedImpactFrame)
    }

    @Test
    fun workflowStateStoreDoesNotCreateDefaultVideoConfigWhenLegacyCacheHasNone() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(null, restored.videoConfig)
        assertEquals(GolfCamStep.IMPACT_FRAME, restored.completedStep)
        assertEquals(GolfCamStep.IMPACT_POSITION, restored.resumeStep)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
    }

    @Test
    fun workflowStateStoreDoesNotUseThirtyFpsTailClampWhenLegacyCacheHasNoFrameRate() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(359),
                            "timeMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(null, restored.videoConfig)
        assertEquals(6_999L, restored.impactFrame?.timeMs)
        assertTrue(restored.hasCompletedImpactFrame)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
    }

    @Test
    fun workflowStateStoreMarksLegacyVideoConfigWithoutFrameRateForRedetection() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(18_000L),
                            "visualWidth" to JsonPrimitive(1080),
                            "visualHeight" to JsonPrimitive(1920),
                            "rotationDegrees" to JsonPrimitive(90),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(18_000L, restored.videoConfig?.durationMs)
        assertEquals(0.0, restored.videoConfig?.frameRate ?: -1.0, 0.0)
        assertEquals(1080, restored.videoConfig?.visualWidth)
        assertEquals(1920, restored.videoConfig?.visualHeight)
        assertEquals(90, restored.videoConfig?.rotationDegrees)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
        assertTrue(restored.hasCompletedTrim)
    }

    @Test
    fun workflowStateStoreMarksLegacyVideoConfigWithoutVisualSizeForRedetection() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(18_000L),
                            "frameRate" to JsonPrimitive(59.94),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(18_000L, restored.videoConfig?.durationMs)
        assertEquals(59.94, restored.videoConfig?.frameRate ?: -1.0, 0.0)
        assertEquals(1, restored.videoConfig?.visualWidth)
        assertEquals(1, restored.videoConfig?.visualHeight)
        assertTrue(shouldDetectGolfCamVideoConfig(restored.videoConfig))
    }

    @Test
    fun workflowStateStoreDoesNotTreatInvalidTrimCacheAsPreviewReady() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(5_000L),
                            "endMs" to JsonPrimitive(5_200L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(5_100L),
                            "candidateFrames" to JsonArray(emptyList()),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(1.4f), "y" to JsonPrimitive(-0.2f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(1.2f),
                            "normalizedY" to JsonPrimitive(-0.4f),
                            "flightTimeSec" to JsonPrimitive(12.0),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(1.2f), "y" to JsonPrimitive(-2.0f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(-0.2f), "y" to JsonPrimitive(2.0f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(emptyMap()),
                ),
            ),
        )

        assertEquals(null, restored.trimResult)
        assertEquals(GolfCamStep.TRIM, restored.completedStep)
        assertFalse(restored.isComplete)
        assertEquals(null, restored.impactFrame)
        assertEquals(null, restored.impactPosition)
        assertEquals(null, restored.landing)
        assertEquals(null, restored.flightCurve)
        assertEquals(null, restored.trackStyle)
    }

    @Test
    fun workflowStateStoreDoesNotTreatOutOfDurationTrimCacheAsPreviewReady() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "videoConfig" to JsonObject(
                        mapOf(
                            "durationMs" to JsonPrimitive(6_000L),
                            "frameRate" to JsonPrimitive(60.0),
                        ),
                    ),
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.25f), "y" to JsonPrimitive(0.75f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.80f),
                            "normalizedY" to JsonPrimitive(0.45f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.30f), "y" to JsonPrimitive(0.20f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.62f), "y" to JsonPrimitive(0.16f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(emptyMap()),
                ),
            ),
        )

        assertEquals(GolfCamStep.TRIM, restored.completedStep)
        assertFalse(restored.hasCompletedTrim)
        assertFalse(restored.hasCompletedImpactFrame)
        assertFalse(restored.isComplete)
    }

    @Test
    fun workflowStateStoreRestoresLegacyMissingTrackStyleFieldsToProductionDefaults() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.25f), "y" to JsonPrimitive(0.75f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.80f),
                            "normalizedY" to JsonPrimitive(0.45f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.30f), "y" to JsonPrimitive(0.20f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.62f), "y" to JsonPrimitive(0.16f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "colorAlpha" to JsonPrimitive(2.0),
                            "glowOpacity" to JsonPrimitive(-1.0),
                            "widthPct" to JsonPrimitive(220.0),
                            "tailHalfLife" to JsonPrimitive(2.0),
                            "totalDistance" to JsonPrimitive(2_000.0),
                            "distanceUnit" to JsonPrimitive("meters"),
                            "speedLevel" to JsonPrimitive(2.0),
                        ),
                    ),
                ),
            ),
        )
        val style = restored.trackStyle?.style ?: error("missing restored style")

        assertTrue(restored.isComplete)
        assertEquals("青蓝薄荷", style.colorPreset.name)
        assertEquals(0xFF55F3D8, style.primaryColorArgb())
        assertEquals(1.0, style.colorAlpha, 0.0)
        assertEquals(0.0, style.glowOpacity, 0.0)
        assertEquals(160.0, style.widthPct, 0.0)
        assertEquals(5.0, style.tailHalfLife, 0.0)
        assertEquals(1000.0, style.totalDistance, 0.0)
        assertEquals(DistanceUnit.METERS, style.distanceUnit)
        assertEquals(GolfCamSpeedConfig.MAX_LEVEL, style.speedLevel, 0.0)
    }

    @Test
    fun workflowStateStoreRestoresMissingCurveControlsWithIosSuggestedDefaults() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.10f), "y" to JsonPrimitive(0.80f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.90f),
                            "normalizedY" to JsonPrimitive(0.62f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(emptyMap()),
                ),
            ),
        )
        val expected = GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(0.10f, 0.80f),
            end = NormalizedPoint(0.90f, 0.62f),
        )

        assertTrue(restored.hasCompletedFlightCurve)
        assertEquals(expected.first, restored.flightCurve?.controlPoint1)
        assertEquals(expected.second, restored.flightCurve?.controlPoint2)
    }

    @Test
    fun workflowStateStoreClampsCachedPresetAndRenderTuningLikeCloudConfig() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.25f), "y" to JsonPrimitive(0.75f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.80f),
                            "normalizedY" to JsonPrimitive(0.45f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.30f), "y" to JsonPrimitive(0.20f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.62f), "y" to JsonPrimitive(0.16f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "colorPreset" to JsonObject(
                                mapOf(
                                    "name" to JsonPrimitive("坏缓存色"),
                                    "gradientColors" to JsonArray(listOf(JsonPrimitive(0xFF55F3D8))),
                                    "glowBlur" to JsonPrimitive(-12.0),
                                    "glowAlpha" to JsonPrimitive(4.0),
                                    "bandAlpha" to JsonPrimitive(-2.0),
                                ),
                            ),
                            "renderTuning" to JsonObject(
                                mapOf(
                                    "bandAlphaMultiplier" to JsonPrimitive(9.0),
                                    "bodyAlphaBase" to JsonPrimitive(-1.0),
                                    "bodyAlphaScale" to JsonPrimitive(0.1),
                                    "glowBlurMultiplier" to JsonPrimitive(9.0),
                                    "outerGlowMultiplier" to JsonPrimitive(0.1),
                                    "fillGlowMultiplier" to JsonPrimitive(9.0),
                                    "highlightWidthRatio" to JsonPrimitive(0.1),
                                    "highlightAlphaMax" to JsonPrimitive(3.0),
                                    "highlightWhiteLiftBase" to JsonPrimitive(0.8),
                                    "highlightWhiteLiftScale" to JsonPrimitive(0.8),
                                    "highlightStart" to JsonPrimitive(0.95),
                                    "highlightEnd" to JsonPrimitive(0.01),
                                    "highlightPeak" to JsonPrimitive(2.0),
                                    "highlightStrength" to JsonPrimitive(9.0),
                                    "widthTaperPower" to JsonPrimitive(9.0),
                                    "apexThinStrength" to JsonPrimitive(9.0),
                                    "apexThinRadius" to JsonPrimitive(0.01),
                                    "highlightFalloffPower" to JsonPrimitive(9.0),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val style = restored.trackStyle?.style ?: error("missing restored style")
        val tuning = style.renderTuning

        assertTrue(restored.isComplete)
        assertEquals(0f, style.colorPreset.glowBlur, 0.0f)
        assertEquals(1f, style.colorPreset.glowAlpha, 0.0f)
        assertEquals(0f, style.colorPreset.bandAlpha, 0.0f)
        assertEquals(1.40f, tuning.bandAlphaMultiplier, 0.0f)
        assertEquals(0.00f, tuning.bodyAlphaBase, 0.0f)
        assertEquals(0.50f, tuning.bodyAlphaScale, 0.0f)
        assertEquals(1.20f, tuning.glowBlurMultiplier, 0.0f)
        assertEquals(0.20f, tuning.outerGlowMultiplier, 0.0f)
        assertEquals(0.45f, tuning.fillGlowMultiplier, 0.0f)
        assertEquals(0.30f, tuning.highlightWidthRatio, 0.0f)
        assertEquals(0.90f, tuning.highlightAlphaMax, 0.0f)
        assertEquals(0.00f, tuning.highlightWhiteLiftBase, 0.0f)
        assertEquals(0.00f, tuning.highlightWhiteLiftScale, 0.0f)
        assertEquals(0.90f, tuning.highlightStart, 0.0f)
        assertEquals(0.95f, tuning.highlightEnd, 0.0f)
        assertEquals(0.95f, tuning.highlightPeak, 0.0f)
        assertEquals(1.60f, tuning.highlightStrength, 0.0f)
        assertEquals(2.20f, tuning.widthTaperPower, 0.0f)
        assertEquals(0.85f, tuning.apexThinStrength, 0.0f)
        assertEquals(0.04f, tuning.apexThinRadius, 0.0f)
        assertEquals(1.80f, tuning.highlightFalloffPower, 0.0f)
    }

    @Test
    fun workflowStateStoreForcesProductionV5AlgorithmWhenRestoringCachedStyle() {
        val restored = GolfCamWorkflowStateStore.decode(
            JsonObject(
                mapOf(
                    "trimResult" to JsonObject(
                        mapOf(
                            "startMs" to JsonPrimitive(1_000L),
                            "endMs" to JsonPrimitive(7_000L),
                        ),
                    ),
                    "impactFrame" to JsonObject(
                        mapOf(
                            "frameIndex" to JsonPrimitive(60),
                            "timeMs" to JsonPrimitive(2_000L),
                        ),
                    ),
                    "impactPosition" to JsonObject(
                        mapOf("x" to JsonPrimitive(0.25f), "y" to JsonPrimitive(0.75f)),
                    ),
                    "landing" to JsonObject(
                        mapOf(
                            "normalizedX" to JsonPrimitive(0.80f),
                            "normalizedY" to JsonPrimitive(0.45f),
                            "flightTimeSec" to JsonPrimitive(4.5),
                        ),
                    ),
                    "flightCurve" to JsonObject(
                        mapOf(
                            "controlPoint1" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.30f), "y" to JsonPrimitive(0.20f)),
                            ),
                            "controlPoint2" to JsonObject(
                                mapOf("x" to JsonPrimitive(0.62f), "y" to JsonPrimitive(0.16f)),
                            ),
                        ),
                    ),
                    "trackStyle" to JsonObject(
                        mapOf(
                            "activeTrajectoryAlgorithm" to JsonPrimitive("CURRENT_V5"),
                            "compareTrajectoryAlgorithms" to JsonPrimitive(true),
                            "trajectoryPreset" to JsonPrimitive("LOW_PIERCING"),
                            "motionLaunchBias" to JsonPrimitive(99.0),
                            "motionDrag" to JsonPrimitive(-99.0),
                            "motionLiftHold" to JsonPrimitive(99.0),
                            "motionDescentAcceleration" to JsonPrimitive(-99.0),
                            "motionTailBoost" to JsonPrimitive(99.0),
                            "motionAnchorTime" to JsonPrimitive(99.0),
                            "v4VMin" to JsonPrimitive(-99.0),
                            "v4VMax" to JsonPrimitive(99.0),
                            "v4PLeft" to JsonPrimitive(-99.0),
                            "v4PRight" to JsonPrimitive(99.0),
                            "v4CustomSPeak" to JsonPrimitive(99.0),
                            "effectType" to JsonPrimitive("FIRE"),
                        ),
                    ),
                ),
            ),
        )
        val style = restored.trackStyle?.style ?: error("missing restored style")

        assertTrue(restored.isComplete)
        assertEquals(TrajectoryAlgorithmKind.GOLF_CAM_LIKE, style.activeTrajectoryAlgorithm)
        assertFalse(style.compareTrajectoryAlgorithms)
        assertEquals(TrajectoryPresetKind.GOLF_CAM_LIKE, style.trajectoryPreset)
        assertEquals(TrajectoryMotionParameters.standard.launchBias, style.motionLaunchBias, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.drag, style.motionDrag, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.liftHold, style.motionLiftHold, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.descentAcceleration, style.motionDescentAcceleration, 0.0)
        assertEquals(TrajectoryMotionParameters.standard.tailBoost, style.motionTailBoost, 0.0)
        assertEquals(null, style.motionAnchorTime)
        assertEquals(null, style.v4VMin)
        assertEquals(null, style.v4VMax)
        assertEquals(null, style.v4PLeft)
        assertEquals(null, style.v4PRight)
        assertEquals(null, style.v4CustomSPeak)
        assertEquals(TrackEffectType.GRADIENT, style.effectType)
    }

    @Test
    fun workflowStateStoreKeysAreScopedToVideoUriForReselectClearing() {
        val first = "content://media/external/video/media/10"
        val second = "content://media/external/video/media/11"

        assertEquals(
            "flight_path:content://media/external/video/media/10",
            GolfCamWorkflowStateStore.storageKey(first),
        )
        assertTrue(GolfCamWorkflowStateStore.storageKey(first) != GolfCamWorkflowStateStore.storageKey(second))
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
