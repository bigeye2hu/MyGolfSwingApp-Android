package com.mygolfswing.app.data.friend

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.net.URI

private const val HUIBEI_QR_SCHEME = "huibei"
private const val HUIBEI_QR_USER_HOST = "user"

fun v2FriendQrContent(userId: String): String =
    "$HUIBEI_QR_SCHEME://$HUIBEI_QR_USER_HOST/${userId.trim()}"

fun parseV2FriendQrUserId(content: String): String? {
    val uri = runCatching { URI(content.trim()) }.getOrNull() ?: return null
    if (!uri.scheme.equals(HUIBEI_QR_SCHEME, ignoreCase = true)) return null
    if (!uri.host.equals(HUIBEI_QR_USER_HOST, ignoreCase = true)) return null
    val userId = uri.path.orEmpty().removePrefix("/").trim()
    return userId.takeIf { it.isNotEmpty() && it.length <= 128 && '/' !in it }
}

internal fun decodeV2QrArgb(width: Int, height: Int, pixels: IntArray): String? {
    val required = requiredPixelCount(width, height) ?: return null
    if (pixels.size < required) return null
    return decodeQr(RGBLuminanceSource(width, height, pixels))
}

internal fun decodeV2QrLuma(width: Int, height: Int, luma: ByteArray): String? {
    val required = requiredPixelCount(width, height) ?: return null
    if (luma.size < required) return null
    return decodeQr(
        PlanarYUVLuminanceSource(
            luma,
            width,
            height,
            0,
            0,
            width,
            height,
            false,
        ),
    )
}

private fun decodeQr(source: com.google.zxing.LuminanceSource): String? = runCatching {
    MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text?.trim()?.takeIf(String::isNotEmpty)
}.getOrNull()

private fun requiredPixelCount(width: Int, height: Int): Int? {
    if (width <= 0 || height <= 0) return null
    val value = width.toLong() * height.toLong()
    return value.takeIf { it <= Int.MAX_VALUE }?.toInt()
}
