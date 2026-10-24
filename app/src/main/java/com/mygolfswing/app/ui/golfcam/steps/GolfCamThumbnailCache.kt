package com.mygolfswing.app.ui.golfcam.steps

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.graphics.scale
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToLong

internal const val GOLF_CAM_THUMBNAIL_PRELOAD_COUNT = 120
internal const val GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT = 60
internal const val GOLF_CAM_THUMBNAIL_CACHE_COUNT =
    GOLF_CAM_THUMBNAIL_PRELOAD_COUNT + GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT + TRIM_THUMBNAIL_COUNT

internal fun golfCamThumbnailSampleTimesMs(
    startMs: Long,
    endMs: Long,
    maxThumbnails: Int = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
): List<Long> {
    val safeStartMs = startMs.coerceAtLeast(0L)
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(safeStartMs, endMs)
    val count = maxThumbnails.coerceIn(0, GOLF_CAM_THUMBNAIL_PRELOAD_COUNT)
    if (count == 0) return emptyList()
    if (count == 1) return listOf(safeStartMs)
    val spanMs = (safeEndMs - safeStartMs).coerceAtLeast(0L).toDouble()
    return (0 until count).map { index ->
        (safeStartMs + spanMs * index.toDouble() / (count - 1))
            .roundToLong()
            .coerceIn(safeStartMs, safeEndMs)
    }.distinct()
}

internal fun golfCamVideoPreloadSampleTimesMs(
    durationMs: Long,
    maxThumbnails: Int = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
): List<Long> {
    if (durationMs <= 0L) return emptyList()
    if (durationMs == 1L) return listOf(0L)
    return golfCamThumbnailSampleTimesMs(
        startMs = 0L,
        endMs = (durationMs - 1L).coerceAtLeast(0L),
        maxThumbnails = maxThumbnails,
    )
}

internal fun golfCamTrimImpactPreloadSampleTimesMs(
    trimStartMs: Long,
    trimEndMs: Long,
    frameRate: Double,
    maxThumbnails: Int = GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT,
): List<Long> {
    val safeStartMs = trimStartMs.coerceAtLeast(0L)
    val safeEndExclusiveMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(safeStartMs, trimEndMs)
    val totalFrames = GolfCamFrameTimeline.totalFramesForRange(safeStartMs, safeEndExclusiveMs, frameRate)
    val requestedCount = maxThumbnails.coerceIn(0, GOLF_CAM_TRIM_IMPACT_PRELOAD_COUNT)
    if (requestedCount == 0) return emptyList()
    val frameIndices = impactFrameThumbnailFrameIndices(totalFrames, requestedCount)
    val lastFrameTimeMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(safeStartMs, safeEndExclusiveMs, frameRate)
    return frameIndices.map { frameIndex ->
        GolfCamFrameTimeline.timeMsForFrame(safeStartMs, frameIndex, frameRate)
            .coerceIn(safeStartMs, lastFrameTimeMs)
    }.distinct()
}

internal fun golfCamTrimImpactPreloadProgressFraction(
    exactCachedCount: Int,
    totalCount: Int,
): Float {
    if (totalCount <= 0) return 1f
    return (exactCachedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}

internal fun shouldWaitForTrimImpactThumbnailPreload(
    exactCachedCount: Int,
    totalCount: Int,
): Boolean {
    return totalCount > 0 && exactCachedCount.coerceAtLeast(0) < totalCount
}

internal fun shouldStartVideoThumbnailPreload(
    exactCachedCount: Int,
    totalCount: Int,
    isPreloadActive: Boolean,
): Boolean {
    return totalCount > 0 &&
        !isPreloadActive &&
        exactCachedCount.coerceAtLeast(0) < totalCount
}

internal fun isVideoThumbnailPreloadActiveForCurrentVideo(
    loadingVideoKey: String,
    currentVideoKey: String?,
    isPreloadActiveForVideo: Boolean,
): Boolean {
    return isPreloadActiveForVideo && shouldContinueThumbnailLoadForVideo(loadingVideoKey, currentVideoKey)
}

internal fun shouldDropStaleVideoThumbnailPreload(
    loadingVideoKey: String,
    currentVideoKey: String?,
    isPreloadActiveForVideo: Boolean,
): Boolean {
    return isPreloadActiveForVideo && !shouldContinueThumbnailLoadForVideo(loadingVideoKey, currentVideoKey)
}

internal fun golfCamThumbnailLoadOrder(
    sampleTimesMs: List<Long>,
    priorityTimeMs: Long,
): List<Int> {
    if (sampleTimesMs.isEmpty()) return emptyList()
    val safePriority = priorityTimeMs.coerceAtLeast(0L)
    return sampleTimesMs.indices.sortedWith(
        compareBy<Int> { index ->
            golfCamThumbnailTimeDistanceMs(sampleTimesMs[index].coerceAtLeast(0L), safePriority)
        }
            .thenBy { index -> index },
    )
}

internal fun golfCamThumbnailPriorityIndex(
    sampleTimesMs: List<Long>,
    priorityTimeMs: Long,
): Int {
    if (sampleTimesMs.isEmpty()) return -1
    val safePriority = priorityTimeMs.coerceAtLeast(0L)
    return sampleTimesMs.indices.minWithOrNull(
        compareBy<Int> { index ->
            golfCamThumbnailTimeDistanceMs(sampleTimesMs[index].coerceAtLeast(0L), safePriority)
        }
            .thenBy { index -> index },
    ) ?: -1
}

internal fun golfCamThumbnailPrioritySampleTimeMs(
    sampleTimesMs: List<Long>,
    priorityTimeMs: Long,
): Long {
    val priorityIndex = golfCamThumbnailPriorityIndex(sampleTimesMs, priorityTimeMs)
    return sampleTimesMs.getOrNull(priorityIndex)?.coerceAtLeast(0L)
        ?: priorityTimeMs.coerceAtLeast(0L)
}

internal fun golfCamThumbnailTimeDistanceMs(firstMs: Long, secondMs: Long): Long {
    val distance = if (firstMs >= secondMs) firstMs - secondMs else secondMs - firstMs
    return if (distance >= 0L) distance else Long.MAX_VALUE
}

internal fun <T> golfCamFillMissingThumbnailSlotsFromNearest(slots: List<T?>): List<T?> {
    if (slots.isEmpty() || slots.all { it == null }) return slots
    val hitIndices = slots.indices.filter { slots[it] != null }
    return slots.mapIndexed { index, value ->
        value ?: hitIndices.minByOrNull { hitIndex -> abs(hitIndex - index) }?.let { slots[it] }
    }
}

internal fun golfCamThumbnailRetrieverTimeUs(sampleTimeMs: Long): Long {
    return sampleTimeMs.coerceIn(0L, Long.MAX_VALUE / 1000L) * 1000L
}

internal fun shouldEmitCachedThumbnailCallback(
    sampleTimeMs: Long,
    skipCachedSampleTimesMs: Set<Long>,
): Boolean {
    return sampleTimeMs.coerceAtLeast(0L) !in skipCachedSampleTimesMs
}

internal fun shouldContinueThumbnailLoadForVideo(
    loadingVideoKey: String,
    currentVideoKey: String?,
    loadingGeneration: Long? = null,
    currentGeneration: Long? = null,
): Boolean {
    val videoMatches = currentVideoKey == null || currentVideoKey == loadingVideoKey
    val generationMatches = loadingGeneration == null ||
        currentGeneration == null ||
        loadingGeneration == currentGeneration
    return videoMatches && generationMatches
}

internal fun nextThumbnailVideoGeneration(currentGeneration: Long): Long {
    return if (currentGeneration == Long.MAX_VALUE) 1L else currentGeneration + 1L
}

internal fun shouldClearActiveVideoThumbnailPreload(
    loadingGeneration: Long,
    activeGeneration: Long?,
): Boolean {
    return activeGeneration == loadingGeneration
}

private data class GolfCamThumbnailCacheKey(
    val videoKey: String,
    val sampleTimeMs: Long,
)

internal object GolfCamThumbnailCache {
    private const val DEFAULT_THUMBNAIL_HEIGHT_PX = 180

    private val lock = Any()
    private var currentVideoKey: String? = null
    private var currentVideoGeneration: Long = 0L
    private val activeVideoPreloads = mutableMapOf<String, Long>()
    private val cache = object : LinkedHashMap<GolfCamThumbnailCacheKey, Bitmap>(GOLF_CAM_THUMBNAIL_CACHE_COUNT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GolfCamThumbnailCacheKey, Bitmap>?): Boolean {
            return size > GOLF_CAM_THUMBNAIL_CACHE_COUNT
        }
    }

    fun videoKey(videoUri: Uri): String = videoUri.toString()

    fun cachedTimelineThumbnails(videoUri: Uri, sampleTimesMs: List<Long>): List<Bitmap?> {
        return cachedTimelineThumbnails(videoKey(videoUri), sampleTimesMs)
    }

    fun cachedExactSampleTimes(videoUri: Uri, sampleTimesMs: List<Long>): Set<Long> {
        return cachedExactSampleTimes(videoKey(videoUri), sampleTimesMs)
    }

    fun cachedExactSampleTimes(videoKey: String, sampleTimesMs: List<Long>): Set<Long> {
        return synchronized(lock) {
            sampleTimesMs
                .map { sampleTimeMs -> sampleTimeMs.coerceAtLeast(0L) }
                .filter { sampleTimeMs -> cache.containsKey(GolfCamThumbnailCacheKey(videoKey, sampleTimeMs)) }
                .toSet()
        }
    }

    fun cachedTimelineThumbnails(videoKey: String, sampleTimesMs: List<Long>): List<Bitmap?> {
        val exactOrNearest = synchronized(lock) {
            sampleTimesMs.map { sampleTimeMs ->
                val safeSampleTimeMs = sampleTimeMs.coerceAtLeast(0L)
                cache[GolfCamThumbnailCacheKey(videoKey, safeSampleTimeMs)]
                    ?: nearestBitmapLocked(videoKey, safeSampleTimeMs)
            }
        }
        return golfCamFillMissingThumbnailSlotsFromNearest(exactOrNearest)
    }

    suspend fun preloadVideo(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        maxThumbnails: Int = GOLF_CAM_THUMBNAIL_PRELOAD_COUNT,
    ) {
        if (durationMs <= 0L || maxThumbnails <= 0) return
        val sampleTimesMs = golfCamVideoPreloadSampleTimesMs(durationMs, maxThumbnails)
        if (sampleTimesMs.isEmpty()) return
        val key = videoKey(videoUri)
        val (shouldStart, loadGeneration) = synchronized(lock) {
            resetIfVideoChangedLocked(key)
            val activeGeneration = activeVideoPreloads[key]
            val isActiveForCurrentVideo = activeGeneration?.let { generation ->
                shouldContinueThumbnailLoadForVideo(
                    loadingVideoKey = key,
                    currentVideoKey = currentVideoKey,
                    loadingGeneration = generation,
                    currentGeneration = currentVideoGeneration,
                )
            } ?: false
            if (activeGeneration != null && !isActiveForCurrentVideo) {
                activeVideoPreloads.remove(key)
            }
            val exactHits = sampleTimesMs.count { sampleTimeMs ->
                cache.containsKey(GolfCamThumbnailCacheKey(key, sampleTimeMs.coerceAtLeast(0L)))
            }
            if (shouldStartVideoThumbnailPreload(
                    exactCachedCount = exactHits,
                    totalCount = sampleTimesMs.size,
                    isPreloadActive = isActiveForCurrentVideo,
                )
            ) {
                activeVideoPreloads[key] = currentVideoGeneration
                true to currentVideoGeneration
            } else {
                false to currentVideoGeneration
            }
        }
        if (!shouldStart) return
        try {
            loadTimelineThumbnails(
                context = context,
                videoUri = videoUri,
                sampleTimesMs = sampleTimesMs,
                priorityTimeMs = 0L,
                frameOption = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                expectedVideoGeneration = loadGeneration,
                onThumbnailLoaded = { _, _ -> },
            )
        } finally {
            synchronized(lock) {
                if (shouldClearActiveVideoThumbnailPreload(loadGeneration, activeVideoPreloads[key])) {
                    activeVideoPreloads.remove(key)
                }
            }
        }
    }

    suspend fun loadTimelineThumbnails(
        context: Context,
        videoUri: Uri,
        sampleTimesMs: List<Long>,
        priorityTimeMs: Long,
        skipCachedSampleTimesMs: Set<Long> = emptySet(),
        frameOption: Int = MediaMetadataRetriever.OPTION_CLOSEST,
        expectedVideoGeneration: Long? = null,
        onThumbnailLoaded: suspend (index: Int, bitmap: Bitmap) -> Unit,
    ) {
        if (sampleTimesMs.isEmpty()) return
        val key = videoKey(videoUri)
        val loadGeneration = expectedVideoGeneration ?: resetIfVideoChanged(key)
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val metadata = retriever.thumbnailVideoMetadata()
                for (index in golfCamThumbnailLoadOrder(sampleTimesMs, priorityTimeMs)) {
                    coroutineContext.ensureActive()
                    if (!isCurrentVideoLoad(key, loadGeneration)) break
                    val sampleTimeMs = sampleTimesMs[index].coerceAtLeast(0L)
                    val cacheKey = GolfCamThumbnailCacheKey(key, sampleTimeMs)
                    val cached = synchronized(lock) { cache[cacheKey] }
                    if (cached != null) {
                        if (shouldEmitCachedThumbnailCallback(sampleTimeMs, skipCachedSampleTimesMs)) {
                            withContext(Dispatchers.Main.immediate) {
                                if (isCurrentVideoLoad(key, loadGeneration)) onThumbnailLoaded(index, cached)
                            }
                        }
                        continue
                    }
                    val bitmap = retriever.getFrameAtTime(golfCamThumbnailRetrieverTimeUs(sampleTimeMs), frameOption)
                        ?.rotatedToVisualOrientation(metadata)
                        ?.scaledForTimelineCache()
                        ?: continue
                    if (!isCurrentVideoLoad(key, loadGeneration)) break
                    synchronized(lock) {
                        if (shouldContinueThumbnailLoadForVideo(
                                loadingVideoKey = key,
                                currentVideoKey = currentVideoKey,
                                loadingGeneration = loadGeneration,
                                currentGeneration = currentVideoGeneration,
                            )
                        ) {
                            cache[cacheKey] = bitmap
                        }
                    }
                    withContext(Dispatchers.Main.immediate) {
                        if (isCurrentVideoLoad(key, loadGeneration)) onThumbnailLoaded(index, bitmap)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Best effort cache: keep any frames already loaded, and let callers show placeholders.
            } finally {
                retriever.release()
            }
        }
    }

    private fun isCurrentVideoLoad(videoKey: String, videoGeneration: Long): Boolean {
        return synchronized(lock) {
            shouldContinueThumbnailLoadForVideo(
                loadingVideoKey = videoKey,
                currentVideoKey = currentVideoKey,
                loadingGeneration = videoGeneration,
                currentGeneration = currentVideoGeneration,
            )
        }
    }

    private fun resetIfVideoChanged(videoKey: String): Long {
        return synchronized(lock) {
            resetIfVideoChangedLocked(videoKey)
            currentVideoGeneration
        }
    }

    private fun resetIfVideoChangedLocked(videoKey: String) {
        if (currentVideoKey != videoKey) {
            cache.clear()
            currentVideoKey = videoKey
            currentVideoGeneration = nextThumbnailVideoGeneration(currentVideoGeneration)
        }
    }

    private fun nearestBitmapLocked(videoKey: String, sampleTimeMs: Long): Bitmap? {
        return cache.entries
            .asSequence()
            .filter { it.key.videoKey == videoKey }
            .minByOrNull { golfCamThumbnailTimeDistanceMs(it.key.sampleTimeMs, sampleTimeMs) }
            ?.value
    }

    private fun Bitmap.scaledForTimelineCache(): Bitmap {
        val safeHeight = height.coerceAtLeast(1)
        val safeWidth = width.coerceAtLeast(1)
        if (safeHeight <= DEFAULT_THUMBNAIL_HEIGHT_PX) return this
        val targetWidth = (safeWidth.toFloat() / safeHeight * DEFAULT_THUMBNAIL_HEIGHT_PX)
            .toInt()
            .coerceAtLeast(1)
        return scale(targetWidth, DEFAULT_THUMBNAIL_HEIGHT_PX, filter = false)
    }
}
