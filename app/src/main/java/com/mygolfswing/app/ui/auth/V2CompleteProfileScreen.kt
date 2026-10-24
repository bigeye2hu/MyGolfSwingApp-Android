package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.content.V2ContentFilterService
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2PresetAvatar
import com.mygolfswing.app.ui.common.V2PresetAvatars
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * 首次登录后的两步资料完善页，对齐 iOS `CompleteProfilePage` 与
 * `CompleteProfileOptionalDetailsView`。它与日常的“编辑资料”是两个不同流程。
 */
@Composable
fun V2CompleteProfileScreen(onComplete: () -> Unit) {
    val session by ServiceLocator.session.state.collectAsState(initial = V2Session())
    if (!session.isLoggedIn) {
        V2BackHandler(onBack = {})
        CompleteProfileBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DSColors.fairway, strokeWidth = 2.dp)
            }
        }
        return
    }
    val initialStep = if (!session.nickname.isNullOrBlank() && !session.avatar.isNullOrBlank()) 2 else 1
    key(session.userId) {
        V2CompleteProfileContent(
            initialStep = initialStep,
            existingSession = session,
            onSaveBasic = { nickname, avatar ->
                ServiceLocator.authRepo.updateUserInfo(nickname = nickname, avatar = avatar).map { Unit }
            },
            onSaveOptional = { draft ->
                ServiceLocator.authRepo.updateCompleteProfileOptional(
                    gender = draft.gender,
                    handicap = draft.handicap?.let { String.format(Locale.US, "%.1f", it) },
                    golfAge = draft.golfAge,
                    golfStartYear = draft.golfAge?.let { LocalDate.now().year - it },
                    dominantHand = draft.dominantHand,
                    golfBrands = draft.brands,
                ).map { Unit }
            },
            onComplete = onComplete,
        )
    }
}

internal data class V2CompleteProfileOptionalDraft(
    val gender: String?,
    val golfAge: Int?,
    val handicap: Double?,
    val dominantHand: String?,
    val brands: List<String>,
)

private data class LearningOption(
    val id: String,
    val title: String,
    val representativeYears: Int,
    val handicap: Double?,
)

private val learningOptions = listOf(
    LearningOption("under_1", "1年内", 0, null),
    LearningOption("1_to_3", "1年～3年", 2, 20.0),
    LearningOption("3_to_5", "3年～5年", 4, 10.0),
    LearningOption("over_5", "5年以上", 5, 0.0),
)

private val completeProfileBrands = listOf(
    "TaylorMade", "Callaway", "Titleist", "Ping", "Cobra",
    "Mizuno", "Srixon", "Cleveland", "Bridgestone", "Honma",
)

@Composable
internal fun V2CompleteProfileContent(
    initialStep: Int,
    existingSession: V2Session,
    onSaveBasic: suspend (nickname: String, avatar: String) -> Result<Unit>,
    onSaveOptional: suspend (V2CompleteProfileOptionalDraft) -> Result<Unit>,
    onComplete: () -> Unit,
) {
    val avatars = V2PresetAvatars.all
    val existingAvatarIndex = avatars.indexOfFirst { it.assetKey == existingSession.avatar }.coerceAtLeast(0)
    val initialNickname = existingSession.nickname?.trim().orEmpty()
    var currentStep by remember { mutableIntStateOf(initialStep.coerceIn(1, 2)) }
    var selectedAvatarIndex by remember { mutableIntStateOf(existingAvatarIndex) }
    var nicknameIsAutoGenerated by remember { mutableStateOf(initialNickname.isEmpty()) }
    var nickname by remember {
        mutableStateOf(
            initialNickname.ifEmpty { generatedCompleteProfileNickname(avatars[existingAvatarIndex].displayName) },
        )
    }
    var basicSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    V2BackHandler(onBack = {})

    CompleteProfileBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .v2StatusBarPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CompleteProfileProgress(currentStep)
                if (currentStep == 1) {
                    CompleteProfileIntro()
                    CompleteProfileAvatarCard(
                        avatars = avatars,
                        selectedIndex = selectedAvatarIndex,
                        onSelect = { index ->
                            selectedAvatarIndex = index
                            if (nicknameIsAutoGenerated || nickname.isBlank()) {
                                nickname = generatedCompleteProfileNickname(avatars[index].displayName)
                                nicknameIsAutoGenerated = true
                            }
                        },
                    )
                    CompleteProfileNicknameCard(
                        nickname = nickname,
                        autoGenerated = nicknameIsAutoGenerated,
                        onNicknameChange = { value ->
                            nickname = value.take(15)
                            if (nickname.isNotEmpty() && !isGeneratedCompleteProfileNickname(nickname)) {
                                nicknameIsAutoGenerated = false
                            }
                            error = null
                        },
                        onRegenerate = {
                            nickname = generatedCompleteProfileNickname(avatars[selectedAvatarIndex].displayName)
                            nicknameIsAutoGenerated = true
                            error = null
                        },
                    )
                    CompleteProfileSaveNote(hasMoreSteps = true)
                    if (!error.isNullOrBlank()) CompleteProfileError(error.orEmpty())
                    CompleteProfileNextButton(
                        enabled = nickname.isNotBlank() && nickname.length <= 15,
                        loading = basicSaving,
                        onClick = {
                            if (basicSaving) return@CompleteProfileNextButton
                            val trimmedNickname = nickname.trim()
                            if (trimmedNickname.isEmpty()) {
                                error = "请输入昵称"
                                return@CompleteProfileNextButton
                            }
                            if (!V2ContentFilterService.check(context, trimmedNickname).isClean) {
                                error = "昵称包含不当内容，请修改后重试"
                                return@CompleteProfileNextButton
                            }
                            scope.launch {
                                basicSaving = true
                                error = null
                                try {
                                    onSaveBasic(trimmedNickname, avatars[selectedAvatarIndex].assetKey)
                                        .onSuccess { currentStep = 2 }
                                        .onFailure { error = authUserError("保存失败，请重试", it.message) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    error = authThrowableError("保存失败，请重试", e)
                                } finally {
                                    basicSaving = false
                                }
                            }
                        },
                    )
                } else {
                    CompleteProfileOptionalDetails(
                        session = existingSession,
                        onSave = onSaveOptional,
                        onComplete = onComplete,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteProfileBackground(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        val heroHeight = minOf(340.dp, maxHeight * 0.42f)
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(heroHeight).align(Alignment.TopCenter),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AuthPalette.heroTop.copy(alpha = 0.78f),
                            AuthPalette.heroMid1.copy(alpha = 0.46f),
                            DSColors.warmWhite,
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
private fun CompleteProfileProgress(step: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompleteProfileStepCircle(1, step == 1)
            Box(
                Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(if (step == 2) AuthPalette.heroMid2 else Color.White.copy(alpha = 0.28f)),
            )
            CompleteProfileStepCircle(2, step == 2)
        }
        Text(
            if (step == 1) "设置个人资料" else "让挥呗更了解你一些（可跳过）",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompleteProfileStepCircle(number: Int, active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (active) AuthPalette.heroMid2 else Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompleteProfileIntro() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(38.dp).background(AuthPalette.heroMid2, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Verified, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text("最后一步", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "选择头像并确认昵称，完成后直接进入首页。",
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CompleteProfileAvatarCard(
    avatars: List<V2PresetAvatar>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = DSColors.fairway.copy(alpha = 0.10f))
            .background(DSColors.cardWhite, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("选择你的头像", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Image(
            painter = painterResource(avatars[selectedIndex].drawableRes),
            contentDescription = "当前头像：${avatars[selectedIndex].displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(4.dp, Brush.linearGradient(listOf(AuthPalette.heroMid2, AuthPalette.heroMid1))),
                    CircleShape,
                )
                .shadow(20.dp, CircleShape, clip = false, spotColor = AuthPalette.heroMid2.copy(alpha = 0.36f)),
        )
        Text(avatars[selectedIndex].displayName, color = DSColors.shadowGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            avatars.forEachIndexed { index, avatar ->
                val selected = selectedIndex == index
                Image(
                    painter = painterResource(avatar.drawableRes),
                    contentDescription = avatar.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer(scaleX = if (selected) 1.1f else 1f, scaleY = if (selected) 1.1f else 1f)
                        .clip(CircleShape)
                        .border(if (selected) 3.dp else 0.dp, if (selected) AuthPalette.heroMid2 else Color.Transparent, CircleShape)
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun CompleteProfileNicknameCard(
    nickname: String,
    autoGenerated: Boolean,
    onNicknameChange: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(DSColors.cardWhite, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("设置昵称", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(" *", color = DSColors.flagRed, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${nickname.length}/15",
                color = if (nickname.length > 15) DSColors.flagRed else DSColors.shadowGray,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(AuthPalette.inputBg, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Person, null, tint = DSColors.shadowGray, modifier = Modifier.size(24.dp))
            BasicTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("complete-profile-nickname"),
                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 17.sp),
                cursorBrush = SolidColor(DSColors.fairway),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (nickname.isEmpty()) Text("给自己取个响亮的名字吧", color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 15.sp)
                        inner()
                    }
                },
            )
            if (nickname.isNotEmpty()) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "清空昵称",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(18.dp).clickable { onNicknameChange("") },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (autoGenerated) Icons.Filled.AutoAwesome else Icons.Filled.Verified,
                null,
                tint = if (autoGenerated) AuthPalette.goldAccent else AuthPalette.heroBottom,
                modifier = Modifier.size(12.dp),
            )
            Text(
                if (autoGenerated) "已自动生成昵称，可直接完成" else "昵称已更新，之后仍可修改",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (autoGenerated) {
                Text(
                    "换一个",
                    color = AuthPalette.heroBottom,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onRegenerate),
                )
            }
        }
    }
}

@Composable
private fun CompleteProfileSaveNote(hasMoreSteps: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DSColors.cardWhite, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(24.dp).background(DSColors.mistGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                if (hasMoreSteps) Icons.Filled.Sync else Icons.Filled.Home,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (hasMoreSteps) "资料会先保存" else "完成后直接进入首页", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (hasMoreSteps) "后续账号安全步骤可以继续补齐，昵称和头像会先写入正式账号。" else "昵称和头像以后可以在「我」里修改，不会影响你继续浏览和练习。",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CompleteProfileError(message: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(16.dp))
        Text(message, color = DSColors.flagRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CompleteProfileNextButton(enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 15.dp else 0.dp, shape, spotColor = AuthPalette.heroMid2.copy(alpha = 0.34f))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(AuthPalette.heroMid2, AuthPalette.heroMid1))
                else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f))),
                shape,
            )
            .testTag("complete-profile-next"),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一步", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CompleteProfileOptionalDetails(
    session: V2Session,
    onSave: suspend (V2CompleteProfileOptionalDraft) -> Result<Unit>,
    onComplete: () -> Unit,
) {
    val existingGolfAge = session.golfAge ?: session.golfStartYear?.let { (LocalDate.now().year - it).coerceAtLeast(0) }
    var gender by remember { mutableStateOf(session.gender.orEmpty()) }
    var dominantHand by remember { mutableStateOf(session.dominantHand ?: "右手") }
    var learningRange by remember { mutableStateOf(completeProfileLearningRangeFor(existingGolfAge)) }
    var selectedHandicap by remember { mutableStateOf(session.handicap?.toDoubleOrNull()) }
    var selectedBrands by remember { mutableStateOf(session.golfBrands.toSet()) }
    var handicapPickerVisible by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedLearningOption = learningOptions.firstOrNull { it.id == learningRange }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CompleteProfileCard(title = "关于你", subtitle = "选择性别，帮助内容推荐更贴近你") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompleteProfileGenderChoice("男", R.drawable.complete_profile_gender_boy, gender == "男", Modifier.weight(1f)) {
                    gender = if (gender == "男") "" else "男"
                }
                CompleteProfileGenderChoice("女", R.drawable.complete_profile_gender_girl, gender == "女", Modifier.weight(1f)) {
                    gender = if (gender == "女") "" else "女"
                }
                CompleteProfileGenderChoice("保密", null, gender == "保密", Modifier.weight(1f)) {
                    gender = if (gender == "保密") "" else "保密"
                }
            }
        }
        CompleteProfileCard(title = "高尔夫资料", subtitle = "只需选择最关键的信息") {
            CompleteProfileFieldTitle("学球年限")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                learningOptions.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            CompleteProfileChoiceButton(option.title, learningRange == option.id, Modifier.weight(1f)) {
                                if (learningRange == option.id) {
                                    learningRange = ""
                                    selectedHandicap = null
                                } else {
                                    learningRange = option.id
                                    selectedHandicap = option.handicap
                                }
                            }
                        }
                    }
                }
            }
            if (selectedLearningOption != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(DSColors.mistGreen, RoundedCornerShape(11.dp))
                        .clickable { handicapPickerVisible = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("建议差点", color = DSColors.shadowGray, fontSize = 12.sp)
                        Text("点击后可精确调整", color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        selectedHandicap?.let { String.format(Locale.US, "%.1f", it) } ?: "暂无",
                        color = DSColors.fairway,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
                }
            }
            CompleteProfileFieldTitle("惯用手")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("右手", "左手").forEach { hand ->
                    CompleteProfileChoiceButton(hand, dominantHand == hand, Modifier.weight(1f)) {
                        dominantHand = if (dominantHand == hand) "" else hand
                    }
                }
            }
            CompleteProfileFieldTitle("球具品牌（可多选）")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                completeProfileBrands.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { brand ->
                            CompleteProfileChoiceButton(brand, brand in selectedBrands, Modifier.weight(1f), fontSize = 12) {
                                selectedBrands = if (brand in selectedBrands) selectedBrands - brand else selectedBrands + brand
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error.orEmpty(), color = DSColors.flagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Button(
            onClick = {
                if (saving) return@Button
                val option = learningOptions.firstOrNull { it.id == learningRange }
                val draft = V2CompleteProfileOptionalDraft(
                    gender = gender.takeIf { it.isNotBlank() && it != "保密" },
                    golfAge = option?.representativeYears,
                    handicap = selectedHandicap,
                    dominantHand = dominantHand.takeIf(String::isNotBlank),
                    brands = selectedBrands.toList(),
                )
                scope.launch {
                    saving = true
                    error = null
                    try {
                        onSave(draft)
                            .onSuccess { onComplete() }
                            .onFailure { error = authUserError("资料保存失败，请重试或暂时跳过", it.message) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        error = authThrowableError("资料保存失败，请重试或暂时跳过", e)
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving,
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete-profile-save"),
        ) {
            if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Text("保存并进入首页", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable(enabled = !saving, onClick = onComplete)
                .testTag("complete-profile-skip"),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂时跳过，进入首页", color = DSColors.fairway, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (handicapPickerVisible) {
        CompleteProfileHandicapSheet(
            value = selectedHandicap ?: selectedLearningOption?.handicap ?: 18.0,
            onNone = { selectedHandicap = null; handicapPickerVisible = false },
            onConfirm = { selectedHandicap = it; handicapPickerVisible = false },
            onDismiss = { handicapPickerVisible = false },
        )
    }
}

@Composable
private fun CompleteProfileCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
            .border(1.dp, DSColors.fairway.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DSColors.shadowGray, fontSize = 11.sp)
        }
        content()
    }
}

@Composable
private fun CompleteProfileGenderChoice(
    title: String,
    drawableRes: Int?,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .background(if (selected) DSColors.fairway else DSColors.mistGreen, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (drawableRes != null) {
            Image(painterResource(drawableRes), null, contentScale = ContentScale.Fit, modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Filled.VisibilityOff, null, tint = if (selected) Color.White else DSColors.inkGreen, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = if (selected) Color.White else DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompleteProfileFieldTitle(title: String) {
    Text(title, color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CompleteProfileChoiceButton(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    fontSize: Int = 13,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(if (fontSize == 12) 34.dp else 40.dp)
            .background(if (selected) DSColors.fairway else DSColors.mistGreen, RoundedCornerShape(if (fontSize == 12) 10.dp else 11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = if (selected) Color.White else DSColors.inkGreen, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompleteProfileHandicapSheet(
    value: Double,
    onNone: () -> Unit,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var integer by remember(value) { mutableIntStateOf(value.toInt().coerceIn(0, 54)) }
    var decimal by remember(value) {
        mutableIntStateOf((((value - value.toInt()) * 10) + 0.5).toInt().coerceIn(0, 9))
    }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        contentColor = DSColors.inkGreen,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = Modifier.testTag("complete-profile-handicap-sheet"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.52f).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "取消",
                    color = DSColors.fairway,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                )
                Text("调整差点", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "确定",
                    color = DSColors.fairway,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .clickable { onConfirm(integer + decimal / 10.0) }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("complete-profile-handicap-confirm"),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine.copy(alpha = 0.72f)))
            Spacer(Modifier.height(18.dp))
            Text("选择你的差点", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CompleteProfileWheel(
                    value = integer,
                    range = 0..54,
                    label = "差点整数滚轮",
                    modifier = Modifier.width(100.dp),
                    onChange = { integer = it },
                )
                Text(".", color = DSColors.inkGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                CompleteProfileWheel(
                    value = decimal,
                    range = 0..9,
                    label = "差点小数滚轮",
                    modifier = Modifier.width(80.dp),
                    onChange = { decimal = it },
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "设为暂无",
                color = DSColors.shadowGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNone).padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun CompleteProfileWheel(
    value: Int,
    range: IntRange,
    label: String,
    modifier: Modifier,
    onChange: (Int) -> Unit,
) {
    val values = remember(range) { range.toList() }
    val initialIndex = remember(values, value) { values.indexOf(value).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()
    val centeredIndex by remember(listState, values) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - center)
            }?.index ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex, values) {
        values.getOrNull(centeredIndex)?.let(onChange)
    }

    Box(modifier = modifier.height(150.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.72f)),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "$label，当前 $value" }
                .testTag(if (range.last == 54) "complete-profile-handicap-integer-wheel" else "complete-profile-handicap-decimal-wheel"),
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(values, key = { it }) { option ->
                val selected = option == value
                Text(
                    option.toString(),
                    color = if (selected) DSColors.inkGreen else DSColors.shadowGray,
                    fontSize = if (selected) 22.sp else 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                        .alpha(if (selected) 1f else 0.55f)
                        .clickable {
                            scope.launch { listState.animateScrollToItem(values.indexOf(option)) }
                        }
                        .padding(top = if (selected) 6.dp else 10.dp),
                )
            }
        }
    }
}

internal fun completeProfileLearningRangeFor(years: Int?): String = when (years) {
    null -> ""
    in Int.MIN_VALUE..0 -> "under_1"
    in 1..2 -> "1_to_3"
    in 3..4 -> "3_to_5"
    else -> "over_5"
}

private fun generatedCompleteProfileNickname(avatarDisplayName: String): String =
    "$avatarDisplayName${(0..9999).random().toString().padStart(4, '0')}"

private fun isGeneratedCompleteProfileNickname(value: String): Boolean = V2PresetAvatars.all.any { avatar ->
    value.startsWith(avatar.displayName) && value.length <= avatar.displayName.length + 4
}
