package com.mygolfswing.app.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException

internal object AuthPalette {
    val goldAccent = Color(0xFFE0A82E)
    val inputBg = Color(0xFFF9F6EF)
    val labelText = Color(0xFF374151)
    val placeholder = Color(0xFF9CA3AF)
    val divider = Color(0xFFF3F0EA)
    val heroTop = Color(0xFF0D2B1F)
    val heroMid1 = Color(0xFF1B4332)
    val heroMid2 = Color(0xFF2D6A4F)
    val heroMid3 = Color(0xFF40916C)
    val heroBottom = Color(0xFF52B788)
}

internal val authHeroBrush: Brush
    get() = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to AuthPalette.heroTop,
            0.20f to AuthPalette.heroMid1,
            0.54f to AuthPalette.heroMid2,
            0.74f to AuthPalette.heroMid3,
            0.88f to AuthPalette.heroBottom,
            1.00f to DSColors.warmWhite.copy(alpha = 0.88f),
        ),
    )

internal fun authUserError(base: String, debugDetail: String?): String {
    return authUserError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun authUserError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    authSafeRecoveryMessage(debugDetail)?.let { return it }
    if (!isDebuggable) authSafeTransportRecoveryMessage(debugDetail)?.let { return it }
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

/** Only exposes backend messages that are curated, actionable, and free of implementation detail. */
internal fun authSafeRecoveryMessage(detail: String?): String? {
    val normalized = detail?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (Regex("""请\d+秒后再试""").matches(normalized)) return normalized
    return normalized.takeIf {
        it in setOf(
            "请输入正确的手机号",
            "请输入6位验证码",
            "验证码错误或已过期",
            "今日验证码发送次数已达上限，请明天再试",
            "用户名和密码不能为空",
            "用户名或密码错误",
            "用户不存在",
            "用户名已被注册",
            "邮箱已被注册",
            "用户名长度需要在3-20个字符之间",
            "密码长度至少6个字符",
            "密码至少需要8位字符",
            "密码必须包含字母",
            "密码必须包含数字",
            "当前密码错误",
            "手机号与账号不匹配",
            "当前账号已绑定其他手机号，请先换绑",
            "该手机号已绑定其他账号，请先完成账号合并",
            "该手机号未绑定任何账号",
            "绑定凭证已失效，请重新获取验证码",
            "登录已过期，请重新登录",
            "登录状态已失效，请重新登录",
            "请升级应用并重新登录",
            "系统初始化中，请稍后重试",
        )
    }
}

/** Converts common client/network diagnostics into actionable copy without exposing raw internals. */
internal fun authSafeTransportRecoveryMessage(detail: String?): String? {
    val normalized = detail?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
    return when {
        listOf("sockettimeoutexception", "timed out", "timeout", "deadline exceeded")
            .any(normalized::contains) -> "网络请求超时，请检查网络后重试"
        listOf("ssl", "certificate", "certpath", "trust anchor", "handshake")
            .any(normalized::contains) -> "安全连接失败，请检查系统时间或网络后重试"
        listOf(
            "unknownhostexception",
            "unable to resolve host",
            "failed to connect",
            "connectexception",
            "network is unreachable",
            "no route to host",
            "connection reset",
            "unexpected end of stream",
        ).any(normalized::contains) -> "网络连接失败，请检查网络后重试"
        Regex("""\bhttp 5\d{2}\b""").containsMatchIn(normalized) ->
            "登录服务暂时不可用，请稍后重试"
        listOf(
            "serializationexception",
            "jsondecodingerror",
            "jsonexception",
            "malformed json",
            "expected a json",
        ).any(normalized::contains) -> "登录服务返回异常，请稍后重试"
        else -> null
    }
}

internal fun authThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return authUserError(base, error.message)
}

private val rainbowRingBrush: Brush
    get() = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFFF6EC7),
            Color(0xFF5B8FFF),
            Color(0xFF00E5C3),
            Color(0xFFFFB347),
            Color(0xFFFF6EC7),
        ),
    )

@Composable
internal fun AuthHeroBackground(
    height: Dp,
    modifier: Modifier = Modifier,
    bottomFadeHeight: Dp = 96.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to AuthPalette.heroTop.copy(alpha = 0.86f),
                            0.34f to AuthPalette.heroMid1.copy(alpha = 0.70f),
                            0.62f to AuthPalette.heroMid2.copy(alpha = 0.44f),
                            0.82f to AuthPalette.heroBottom.copy(alpha = 0.20f),
                            1.00f to DSColors.warmWhite.copy(alpha = 0.84f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.16f),
                            0.50f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = 0.12f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomFadeHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DSColors.warmWhite.copy(alpha = 0f),
                            DSColors.warmWhite.copy(alpha = 0.52f),
                            DSColors.warmWhite.copy(alpha = 0.84f),
                            DSColors.warmWhite,
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
internal fun AuthLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    imageSize: Dp = 78.dp,
    @DrawableRes imageRes: Int = R.drawable.auth_logo,
    rainbow: Boolean = true,
) {
    Box(
        // iOS 的光晕绘制在 80pt 布局框之外；不能让它把 Hero 纵向节奏撑到 94dp。
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (rainbow) {
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .blur(3.dp)
                    .border(BorderStroke(4.dp, rainbowRingBrush), CircleShape),
            )
        }
        val ringStroke = if (rainbow) {
            BorderStroke(2.dp, rainbowRingBrush)
        } else {
            BorderStroke(2.dp, AuthPalette.goldAccent)
        }
        Box(
            modifier = Modifier
                .size(size)
                .shadow(12.dp, CircleShape, clip = false)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AuthPalette.heroMid2, AuthPalette.heroMid3, AuthPalette.heroBottom),
                    ),
                    CircleShape,
                )
                .border(ringStroke, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(imageSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
internal fun AuthCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = AuthPalette.heroMid2.copy(alpha = 0.12f),
                spotColor = AuthPalette.heroMid2.copy(alpha = 0.12f),
            )
            .background(DSColors.cardWhite, RoundedCornerShape(24.dp))
            .border(
                BorderStroke(1.dp, AuthPalette.heroMid2.copy(alpha = 0.12f)),
                RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
internal fun AuthFieldLabel(text: String, required: Boolean = false, trailing: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = DSTypography.bodyBold, color = AuthPalette.labelText)
        if (required) Text("*", color = DSColors.flagRed, style = DSTypography.bodyBold)
        if (trailing != null) Text(trailing, color = DSColors.fairway.copy(alpha = 0.8f), style = DSTypography.caption)
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = AuthPalette.inputBg,
    unfocusedContainerColor = AuthPalette.inputBg,
    cursorColor = DSColors.fairway,
    focusedTextColor = AuthPalette.labelText,
    unfocusedTextColor = AuthPalette.labelText,
    focusedPlaceholderColor = AuthPalette.placeholder,
    unfocusedPlaceholderColor = AuthPalette.placeholder,
)

@Composable
internal fun AuthPrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualAlpha: Float = 1f,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DSColors.fairway,
            contentColor = DSColors.warmWhite,
            disabledContainerColor = DSColors.fairway.copy(alpha = 0.55f),
            disabledContentColor = DSColors.warmWhite,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).alpha(visualAlpha),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = DSColors.warmWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text, style = DSTypography.bodyBold.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
internal fun AuthAgreementRow(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = DSColors.fairway,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        ),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .background(if (agreed) DSColors.fairway else Color.Transparent, RoundedCornerShape(4.dp))
                .border(
                    BorderStroke(1.5.dp, if (agreed) DSColors.fairway else DSColors.fairway.copy(alpha = 0.4f)),
                    RoundedCornerShape(4.dp),
                )
                .clickable { onAgreedChange(!agreed) },
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = DSColors.warmWhite,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            modifier = Modifier.weight(1f),
            style = DSTypography.caption.copy(color = AuthPalette.labelText),
            text = buildAnnotatedString {
                append("我已阅读并同意 ")
                withLink(
                    LinkAnnotation.Clickable(tag = "terms", styles = linkStyle) { onOpenTerms() },
                ) {
                    append("《用户协议》")
                }
                append(" 和 ")
                withLink(
                    LinkAnnotation.Clickable(tag = "privacy", styles = linkStyle) { onOpenPrivacy() },
                ) {
                    append("《隐私政策》")
                }
            },
        )
    }
}
