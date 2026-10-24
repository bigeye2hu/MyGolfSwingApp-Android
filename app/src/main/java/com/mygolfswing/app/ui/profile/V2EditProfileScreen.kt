package com.mygolfswing.app.ui.profile

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image as FoundationImage

/**
 * 编辑资料（对齐 iOS `V2ProfileEditView`）。
 *
 * 全部分区：
 * - 头像（点击 → 拍照 / 系统相册选 → 上传 `uploadAvatar`）
 * - 基础信息：昵称 / 性别 / 生日 / 地区 / 个人简介
 * - 高尔夫信息：差点 / 球龄起始年 / 惯用手
 * - 球具品牌（10 个多选 chips）
 *
 * 用户点「保存」后一次性调用 [V2AuthRepository.updateUserInfo]。
 */
private val genderOptions = listOf("男", "女", "保密")
private val handOptions = listOf("右手", "左手")
private val brandOptions = listOf(
    "TaylorMade", "Callaway", "Titleist", "Ping", "Cobra",
    "Mizuno", "Srixon", "Cleveland", "Bridgestone", "Honma",
)
private val popularLocationCities = listOf(
    "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "武汉",
    "西安", "南京", "苏州", "天津", "青岛", "厦门", "三亚", "海口",
)
private val locationProvinces = listOf(
    "北京", "上海", "天津", "重庆",
    "广东", "浙江", "江苏", "山东", "四川", "湖北", "湖南", "河南",
    "福建", "安徽", "河北", "陕西", "辽宁", "江西", "云南", "广西",
    "山西", "贵州", "黑龙江", "吉林", "甘肃", "内蒙古", "新疆", "海南",
    "宁夏", "青海", "西藏", "香港", "澳门", "台湾",
)

internal data class ProfileEditUpdate(
    val nickname: String?,
    val gender: String,
    val birthday: String,
    val location: String,
    val bio: String,
    val handicap: String?,
    val golfAge: Int?,
    val golfStartYear: Int?,
    val dominantHand: String,
    val golfBrands: List<String>?,
)

internal data class ProfileEditCameraOutput(
    val file: File,
    val uri: Uri,
)

internal data class ProfileEditOperations(
    val decodeAvatar: (Uri) -> Bitmap?,
    val uploadAvatar: suspend (Bitmap) -> Result<String>,
    val updateProfile: suspend (ProfileEditUpdate) -> Result<Unit>,
    val createCameraOutput: () -> ProfileEditCameraOutput,
    val launchGallery: (((Uri?) -> Unit) -> Unit)? = null,
    val launchCamera: ((ProfileEditCameraOutput, (Boolean) -> Unit) -> Unit)? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V2EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    title: String = "编辑资料",
    isCompletionFlow: Boolean = false,
    operations: ProfileEditOperations? = null,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentYear = LocalDate.now().year
    val defaultOperations = remember(context) {
        ProfileEditOperations(
            decodeAvatar = { uri ->
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            },
            uploadAvatar = ServiceLocator.authRepo::uploadAvatar,
            updateProfile = { update ->
                ServiceLocator.authRepo.updateUserInfo(
                    nickname = update.nickname,
                    gender = update.gender,
                    birthday = update.birthday,
                    location = update.location,
                    bio = update.bio,
                    handicap = update.handicap,
                    golfAge = update.golfAge,
                    golfStartYear = update.golfStartYear,
                    dominantHand = update.dominantHand,
                    golfBrands = update.golfBrands,
                ).map { Unit }
            },
            createCameraOutput = {
                createProfileEditCameraOutput(
                    createFile = { createAvatarCameraOutputFile(context) },
                    createUri = { outputFile ->
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            outputFile,
                        )
                    },
                )
            },
        )
    }
    val resolvedOperations = operations ?: defaultOperations

    var nickname by rememberSaveable(profile.nickname) {
        mutableStateOf(profile.nickname.takeIf { it != "未登录" } ?: "")
    }
    var gender by rememberSaveable(profile.gender) { mutableStateOf(profile.gender) }
    var birthday by rememberSaveable(profile.birthday) { mutableStateOf(profile.birthday ?: "") }
    var hasBirthday by rememberSaveable(profile.birthday) { mutableStateOf(!profile.birthday.isNullOrBlank()) }
    var location by rememberSaveable(profile.location) { mutableStateOf(profile.location ?: "") }
    var bio by rememberSaveable(profile.bio) { mutableStateOf(profile.bio ?: "") }
    var handicap by rememberSaveable(profile.handicap) {
        mutableStateOf(profile.handicap.takeIf { it != "未设置" }?.normalizedHandicapText() ?: "18.0")
    }
    var golfStartYear by rememberSaveable(profile.golfYears, currentYear) {
        mutableStateOf(
            (extractStartYear(profile.golfYears) ?: currentYear).coerceIn(1970, currentYear).toString(),
        )
    }
    var dominantHand by rememberSaveable(profile.dominantHand) {
        mutableStateOf(profile.dominantHand.takeUnless { it.isBlank() || it == "未设置" } ?: "右手")
    }
    var selectedBrands by rememberSaveable(profile.golfBrands) {
        mutableStateOf(ArrayList(profile.golfBrands))
    }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraAvatarUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraAvatarFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var showAvatarSourcePicker by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun uploadAvatarFromUri(uri: Uri, cleanupFile: File? = null) {
        scope.launch {
            uploadingAvatar = true
            try {
                val bm = resolvedOperations.decodeAvatar(uri)
                if (bm == null) {
                    saveError = "头像上传失败，请换一张图片或稍后重试"
                    return@launch
                }
                val previousBitmap = avatarBitmap
                // 与 iOS 一致：选中后立即预览；只有上传失败时才回退旧头像。
                avatarBitmap = bm
                resolvedOperations.uploadAvatar(bm)
                    .onFailure {
                        avatarBitmap = previousBitmap
                        saveError = profileEditError("头像上传失败，请换一张图片或稍后重试", it.message)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = profileEditThrowableError("头像上传失败，请换一张图片或稍后重试", e)
            } finally {
                cleanupFile?.delete()
                uploadingAvatar = false
            }
        }
    }

    // 系统相册选图
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            uploadAvatarFromUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = pendingCameraAvatarUriText?.let(Uri::parse)
        val file = pendingCameraAvatarFilePath?.let(::File)
        if (ok && uri != null) {
            uploadAvatarFromUri(uri, cleanupFile = file)
        } else {
            file?.delete()
        }
        pendingCameraAvatarUriText = null
        pendingCameraAvatarFilePath = null
    }

    fun openGalleryAvatarPicker() {
        runCatching {
            val override = resolvedOperations.launchGallery
            if (override != null) {
                override { uri -> if (uri != null) uploadAvatarFromUri(uri) }
            } else {
                galleryPicker.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            }
        }.onFailure {
            saveError = profileEditError("无法打开相册，请稍后重试或改用拍照", it.message)
        }
    }

    fun openCameraAvatarPicker() {
        var file: File? = null
        runCatching {
            val output = resolvedOperations.createCameraOutput()
            file = output.file
            val override = resolvedOperations.launchCamera
            if (override != null) {
                override(output) { ok ->
                    if (ok) uploadAvatarFromUri(output.uri, cleanupFile = output.file)
                    else output.file.delete()
                }
            } else {
                pendingCameraAvatarFilePath = output.file.absolutePath
                pendingCameraAvatarUriText = output.uri.toString()
                cameraLauncher.launch(output.uri)
            }
        }.onFailure {
            pendingCameraAvatarFilePath = null
            pendingCameraAvatarUriText = null
            file?.delete()
            saveError = profileEditError("无法打开相机，请检查权限或改从相册选择", it.message)
        }
    }

    fun showAvatarSourceOptions() {
        showAvatarSourcePicker = true
    }

    fun hideAvatarSourceOptions() {
        showAvatarSourcePicker = false
    }

    fun pickAvatarFromCamera() {
        hideAvatarSourceOptions()
        openCameraAvatarPicker()
    }

    fun pickAvatarFromGallery() {
        hideAvatarSourceOptions()
        openGalleryAvatarPicker()
    }

    BackHandler(enabled = saving) {}

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .v2NavigationBarPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileEditTopBar(title = title, enabled = !saving, onCancel = onBack)

        // 头像
        AvatarSection(
            avatarUrl = profile.avatar,
            localBitmap = avatarBitmap,
            uploading = uploadingAvatar,
            onPickPhoto = ::showAvatarSourceOptions,
        )

        // 基础信息
        Section(title = "基础信息") {
            FormItem(label = "昵称") {
                ProfileEditSingleLineField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "输入您的昵称",
                    modifier = Modifier.testTag("profile-edit-nickname"),
                )
            }
            FormItem(label = "性别") {
                GenderChoiceRow(
                    selected = gender,
                    expanded = isCompletionFlow,
                    onSelect = { option ->
                        gender = if (isCompletionFlow && gender == option) "" else option
                    },
                )
            }
            ProfileEditBirthdayField(
                hasBirthday = hasBirthday,
                birthday = birthday,
                onBirthdayEnabledChange = { checked ->
                    hasBirthday = checked
                    if (checked && birthday.isBlank()) birthday = LocalDate.now().toString()
                },
                onPick = {
                    val initial = parseBirthdayDate(birthday) ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            birthday = LocalDate.of(year, month + 1, day).toString()
                        },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth,
                    ).show()
                },
            )
            FormItem(label = "地区") {
                LocationPickerField(
                    location = location,
                    onClick = { showLocationPicker = true },
                )
            }
            FormItem(label = "个人简介", trailing = "${bio.length}/100") {
                ProfileEditBioField(
                    value = bio,
                    onValueChange = { if (it.length <= 100) bio = it },
                    modifier = Modifier.testTag("profile-edit-bio"),
                )
            }
        }

        // 高尔夫信息
        Section(title = "高尔夫信息") {
            FormItem(label = "差点 Handicap") {
                HandicapWheelField(
                    handicap = handicap,
                    onHandicapChange = { handicap = it },
                )
                FieldHint(text = "当前：${handicap.ifBlank { "未设置" }}")
            }
            FormItem(label = "开始学球年份") {
                YearWheelField(
                    yearText = golfStartYear,
                    currentYear = currentYear,
                    onYearChange = { golfStartYear = it.toString() },
                )
                FieldHint(text = golfAgeHint(golfStartYear))
            }
            FormItem(label = "惯用手") {
                EqualChoiceRow(
                    options = handOptions,
                    selected = dominantHand,
                    onSelect = { dominantHand = it },
                )
            }
        }

        // 球具品牌
        Section(title = "使用的球具品牌") {
            Text(
                text = "选择您常用的品牌（可多选）",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
            BrandChoiceGrid(
                brands = brandOptions,
                selectedBrands = selectedBrands,
                onToggle = { brand ->
                    selectedBrands = ArrayList(selectedBrands).apply {
                        if (brand in this) remove(brand) else add(brand)
                    }
                },
            )
        }

        if (!error.isNullOrBlank()) {
            Text(
                error!!,
                color = DSColors.flagRed,
                style = DSTypography.caption,
                modifier = Modifier
                    .padding(horizontal = DSSpacing.lg)
                    .padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        val saveButtonShape = RoundedCornerShape(16.dp)
        Button(
            onClick = {
                val handicapValue = handicap.ifBlank { null }?.toDoubleOrNull()
                if (handicap.isNotBlank() && (handicapValue == null || handicapValue !in 0.0..54.9)) {
                    error = "差点需在 0.0-54.9 之间"
                    return@Button
                }
                val currentYear = LocalDate.now().year
                val startYear = golfStartYear.toIntOrNull()
                if (golfStartYear.isNotBlank() && (startYear == null || startYear !in 1970..currentYear)) {
                    error = "开始学球年份需在 1970-$currentYear 之间"
                    return@Button
                }
                scope.launch {
                    saving = true
                    error = null
                    saveError = null
                    val r = try {
                        resolvedOperations.updateProfile(
                            ProfileEditUpdate(
                                nickname = profileEditNicknamePatch(nickname),
                                gender = gender.takeUnless { it.isBlank() || it == "保密" } ?: "",
                                birthday = if (hasBirthday) birthday else "",
                                location = location,
                                bio = bio,
                                handicap = handicapValue?.toString(),
                                golfAge = startYear?.let { (LocalDate.now().year - it).coerceAtLeast(0) },
                                golfStartYear = startYear,
                                dominantHand = dominantHand,
                                golfBrands = selectedBrands.toList().takeIf { it.isNotEmpty() },
                            ),
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Result.failure(e)
                    } finally {
                        saving = false
                    }
                    r.onSuccess { showSaveSuccess = true }
                        .onFailure { saveError = profileEditError("保存失败，请稍后重试", it.message) }
                }
            },
            enabled = !saving,
            shape = saveButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = DSColors.warmWhite,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = DSColors.warmWhite,
            ),
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .fillMaxWidth()
                .height(40.dp)
                .alpha(if (saving) 0.7f else 1f)
                .shadow(
                    elevation = 10.dp,
                    shape = saveButtonShape,
                    ambientColor = Color.Transparent,
                    spotColor = DSColors.fairway.copy(alpha = 0.3f),
                )
                .background(
                    Brush.horizontalGradient(listOf(DSColors.fairway, DSColors.deepGreen)),
                    saveButtonShape,
                ),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = DSColors.warmWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("保存中…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text("保存资料", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

            Spacer(Modifier.height(40.dp))
        }
    }

    LaunchedEffect(Unit) {
        // 进编辑页时刷新一遍最新用户信息
        V2ProfileStore.refresh(force = false)
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            selectedLocation = location,
            onSelect = {
                location = it
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false },
        )
    }

    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("保存成功") },
            text = { Text("您的资料已更新") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveSuccess = false
                        onSaved()
                    },
                ) {
                    Text("确定", color = DSColors.fairway)
                }
            },
        )
    }

    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("保存失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { saveError = null }) {
                    Text("确定", color = DSColors.fairway)
                }
            },
        )
    }

    if (showAvatarSourcePicker) {
        AvatarSourcePickerDialog(
            onCamera = ::pickAvatarFromCamera,
            onGallery = ::pickAvatarFromGallery,
            onDismiss = ::hideAvatarSourceOptions,
        )
    }
}

@Composable
private fun ProfileEditTopBar(
    title: String,
    enabled: Boolean,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .testTag("profile-edit-top-bar"),
    ) {
        Text(
            text = title,
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(72.dp)
                .height(44.dp)
                .clickable(enabled = enabled, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "取消",
                color = DSColors.fairway,
                fontSize = 17.sp,
            )
        }
    }
}

@Composable
private fun ProfileEditSingleLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = DSTypography.body.copy(color = DSColors.inkGreen),
        cursorBrush = SolidColor(DSColors.fairway),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        decorationBox = { innerField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = DSColors.shadowGray, fontSize = 15.sp)
                }
                innerField()
            }
        },
    )
}

@Composable
private fun ProfileEditBirthdayField(
    hasBirthday: Boolean,
    birthday: String,
    onBirthdayEnabledChange: (Boolean) -> Unit,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("生日", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = hasBirthday,
                onCheckedChange = onBirthdayEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DSColors.fairway,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = DSColors.shadowGray.copy(alpha = 0.35f),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        if (hasBirthday) BirthdayPickerField(birthday = birthday, onPick = onPick)
    }
}

@Composable
private fun ProfileEditBioField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = DSTypography.body.copy(color = DSColors.inkGreen),
        cursorBrush = SolidColor(DSColors.fairway),
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp))
            .padding(8.dp),
    )
}

@Composable
private fun AvatarSection(
    avatarUrl: String?,
    localBitmap: Bitmap?,
    uploading: Boolean,
    onPickPhoto: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFE0A82E), Color(0xFFF5C842)),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    localBitmap != null -> FoundationImage(
                        bitmap = localBitmap.asImageBitmap(),
                        contentDescription = "avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp),
                    )
                    !avatarUrl.isNullOrBlank() -> coil.compose.AsyncImage(
                        model = avatarUrl,
                        contentDescription = "avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp),
                    )
                    else -> Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "默认头像",
                        tint = DSColors.shadowGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(100.dp),
                    )
                }
                if (uploading) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (!uploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(DSColors.fairway, CircleShape)
                        .border(BorderStroke(2.dp, DSColors.warmWhite), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = DSColors.warmWhite,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点击更换头像",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onPickPhoto),
        )
    }
}

@Composable
private fun AvatarSourcePickerDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .background(DSColors.cardWhite, RoundedCornerShape(16.dp)),
        ) {
            AvatarSourceOption(
                icon = Icons.Filled.CameraAlt,
                title = "拍照",
                onClick = onCamera,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DSColors.borderLine),
            )
            AvatarSourceOption(
                icon = Icons.Filled.PhotoLibrary,
                title = "从相册选择",
                onClick = onGallery,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DSColors.borderLine),
            )
            AvatarSourceOption(
                icon = null,
                title = "取消",
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun AvatarSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = DSSpacing.lg)
            .padding(top = DSSpacing.lg)
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Transparent,
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .background(DSColors.cardWhite, shape)
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Text(title, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun FormItem(
    label: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (trailing != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(trailing, color = DSColors.shadowGray, fontSize = 11.sp)
            }
        }
        content()
    }
}

@Composable
private fun FieldHint(text: String) {
    Text(
        text = text,
        style = DSTypography.caption,
        color = DSColors.shadowGray,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun BirthdayPickerField(
    birthday: String,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp))
            .clickable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = birthday.ifBlank { "选择生日" },
            style = DSTypography.body,
            color = if (birthday.isBlank()) DSColors.shadowGray else DSColors.inkGreen,
            modifier = Modifier.weight(1f),
        )
        Text("选择", style = DSTypography.caption, color = DSColors.fairway)
    }
}

@Composable
private fun LocationPickerField(
    location: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = location.ifBlank { "选择您所在的地区" },
            style = DSTypography.body,
            color = if (location.isBlank()) DSColors.shadowGray else DSColors.inkGreen,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
internal fun LocationPickerDialog(
    selectedLocation: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val normalizedSearch = remember(searchText) { searchText.normalizedLocationSearch() }
    val filteredCities = remember(normalizedSearch) {
        popularLocationCities.filter { it.matchesLocationSearch(normalizedSearch) }
    }
    val filteredProvinces = remember(normalizedSearch) {
        locationProvinces.filter { it.matchesLocationSearch(normalizedSearch) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .v2StatusBarPadding()
                        .height(44.dp)
                        .testTag("profile-location-picker-top-bar"),
                ) {
                    Text(
                        "选择地区",
                        style = DSTypography.titleMedium,
                        color = DSColors.inkGreen,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = DSSpacing.sm),
                    ) {
                        Text("完成", color = DSColors.fairway, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .v2NavigationBarPadding()
                        .padding(DSSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DSColors.mistGreen)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = DSColors.shadowGray,
                            modifier = Modifier.size(14.dp),
                        )
                        Box(Modifier.weight(1f)) {
                            if (searchText.isEmpty()) {
                                Text("搜索城市", color = DSColors.shadowGray, fontSize = 15.sp)
                            }
                            BasicTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = DSColors.inkGreen,
                                    fontSize = 15.sp,
                                ),
                                cursorBrush = SolidColor(DSColors.fairway),
                            )
                        }
                        if (searchText.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = "清除搜索",
                                tint = DSColors.shadowGray.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp).clickable { searchText = "" },
                            )
                        }
                    }
                    LocationPickerSection(
                        title = "热门城市",
                        options = filteredCities,
                        selectedLocation = selectedLocation,
                        emptyText = "没有匹配的城市",
                        columns = 4,
                        onSelect = onSelect,
                    )
                    LocationPickerSection(
                        title = "选择省份 / 地区",
                        options = filteredProvinces,
                        selectedLocation = selectedLocation,
                        emptyText = "没有匹配的省份",
                        columns = 3,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPickerSection(
    title: String,
    options: List<String>,
    selectedLocation: String,
    emptyText: String,
    columns: Int,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(shape)
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            if (options.isNotEmpty()) {
                Text(options.size.toString(), style = DSTypography.caption, color = DSColors.shadowGray)
            }
        }
        if (options.isEmpty()) {
            Text(
                text = emptyText,
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.chunked(columns).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowOptions.forEach { option ->
                            LocationPickerChip(
                                text = option,
                                selected = selectedLocation == option,
                                onClick = { onSelect(option) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowOptions.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPickerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = if (selected) Color.White else DSColors.inkGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) DSColors.fairway else DSColors.mistGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun HandicapWheelField(
    handicap: String,
    onHandicapChange: (String) -> Unit,
) {
    val (integerPart, decimalPart) = remember(handicap) { parseHandicapParts(handicap) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NumberWheelColumn(
            value = integerPart,
            range = 0..54,
            label = { it.toString() },
            onValueChange = { onHandicapChange(formatHandicap(it, decimalPart)) },
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = ".",
            color = DSColors.inkGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        NumberWheelColumn(
            value = decimalPart,
            range = 0..9,
            label = { it.toString() },
            onValueChange = { onHandicapChange(formatHandicap(integerPart, it)) },
            modifier = Modifier.width(60.dp),
        )
    }
}

@Composable
private fun YearWheelField(
    yearText: String,
    currentYear: Int,
    onYearChange: (Int) -> Unit,
) {
    val selectedYear = (yearText.toIntOrNull() ?: currentYear).coerceIn(1970, currentYear)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(DSColors.mistGreen, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        NumberWheelColumn(
            value = selectedYear,
            range = 1970..currentYear,
            label = { "$it 年" },
            onValueChange = onYearChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NumberWheelColumn(
    value: Int,
    range: IntRange,
    label: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clampedValue = value.coerceIn(range.first, range.last)
    val previous = (clampedValue - 1).takeIf { it >= range.first }
    val next = (clampedValue + 1).takeIf { it <= range.last }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WheelOption(
            text = previous?.let(label).orEmpty(),
            selected = false,
            enabled = previous != null,
            onClick = { previous?.let(onValueChange) },
        )
        WheelOption(
            text = label(clampedValue),
            selected = true,
            enabled = false,
        )
        WheelOption(
            text = next?.let(label).orEmpty(),
            selected = false,
            enabled = next != null,
            onClick = { next?.let(onValueChange) },
        )
    }
}

@Composable
private fun WheelOption(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = if (enabled) 0.85f else 0f),
            fontSize = if (selected) 20.sp else 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EqualChoiceRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        options.forEach { option ->
            EqualChoiceButton(
                text = option,
                selected = selected == option,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EqualChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .background(
                if (selected) DSColors.fairway else DSColors.mistGreen,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) DSColors.warmWhite else DSColors.shadowGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GenderChoiceRow(
    selected: String,
    expanded: Boolean,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        GenderChoiceButton(
            text = genderOptions[0],
            drawableRes = R.drawable.complete_profile_gender_boy,
            selected = selected == genderOptions[0],
            expanded = expanded,
            onClick = { onSelect(genderOptions[0]) },
            modifier = Modifier.weight(1f),
        )
        GenderChoiceButton(
            text = genderOptions[1],
            drawableRes = R.drawable.complete_profile_gender_girl,
            selected = selected == genderOptions[1],
            expanded = expanded,
            onClick = { onSelect(genderOptions[1]) },
            modifier = Modifier.weight(1f),
        )
        GenderChoiceButton(
            text = genderOptions[2],
            selected = selected == genderOptions[2] || (!expanded && selected.isBlank()),
            expanded = expanded,
            onClick = { onSelect(genderOptions[2]) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GenderChoiceButton(
    text: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    drawableRes: Int? = null,
) {
    val foreground = if (selected) DSColors.warmWhite else DSColors.shadowGray
    val content: @Composable () -> Unit = {
        if (drawableRes != null) {
            FoundationImage(
                painter = painterResource(drawableRes),
                contentDescription = null,
                modifier = Modifier.size(if (expanded) 28.dp else 22.dp),
            )
        } else {
            Icon(
                Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(if (expanded) 21.dp else 15.dp),
            )
        }
        Text(
            text = text,
            color = foreground,
            fontSize = if (expanded) 13.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }

    if (expanded) {
        Column(
            modifier = modifier
                .height(64.dp)
                .background(
                    if (selected) DSColors.fairway else DSColors.mistGreen,
                    RoundedCornerShape(13.dp),
                )
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .height(42.dp)
                .background(
                    if (selected) DSColors.fairway else DSColors.mistGreen,
                    RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun BrandChoiceGrid(
    brands: List<String>,
    selectedBrands: List<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        brands.chunked(3).forEach { rowBrands ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowBrands.forEach { brand ->
                    BrandChoiceButton(
                        brand = brand,
                        selected = brand in selectedBrands,
                        onClick = { onToggle(brand) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowBrands.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BrandChoiceButton(
    brand: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) DSColors.fairway else DSColors.mistGreen,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = brand,
            color = if (selected) DSColors.warmWhite else DSColors.inkGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PillChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .background(
                if (selected) DSColors.fairway else DSColors.mistGreen,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) DSColors.warmWhite else DSColors.inkGreen,
            style = if (selected) DSTypography.bodyBold else DSTypography.body,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

internal fun profileEditNicknamePatch(value: String): String? = value.takeIf { it.isNotEmpty() }

internal fun profileEditError(base: String, debugDetail: String?): String {
    return profileEditError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun profileEditError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun profileEditThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return profileEditError(base, error.message)
}

private fun parseHandicapParts(text: String): Pair<Int, Int> {
    val value = text.toDoubleOrNull()?.coerceIn(0.0, 54.9) ?: 18.0
    val integerPart = value.toInt().coerceIn(0, 54)
    val decimalPart = ((value - integerPart) * 10 + 0.5).toInt().coerceIn(0, 9)
    return integerPart to decimalPart
}

private fun formatHandicap(integerPart: Int, decimalPart: Int): String {
    return "${integerPart.coerceIn(0, 54)}.${decimalPart.coerceIn(0, 9)}"
}

private fun String.normalizedHandicapText(): String {
    val (integerPart, decimalPart) = parseHandicapParts(this)
    return formatHandicap(integerPart, decimalPart)
}

/** "8 年（2018 起）" → 2018 */
private fun extractStartYear(text: String): Int? {
    val m = Regex("(\\d{4})").find(text) ?: return null
    return m.value.toIntOrNull()
}

private fun golfAgeHint(text: String): String {
    val year = text.toIntOrNull() ?: return "球龄：未设置"
    val currentYear = LocalDate.now().year
    if (year !in 1970..currentYear) return "球龄：请输入 1970-$currentYear 之间的年份"
    return "球龄：${currentYear - year} 年"
}

private fun parseBirthdayDate(text: String): LocalDate? =
    runCatching { LocalDate.parse(text.take(10)) }.getOrNull()

private fun String.normalizedLocationSearch(): String =
    trim()
        .lowercase(Locale.ROOT)
        .filterNot { it.isWhitespace() }

private fun String.matchesLocationSearch(normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return normalizedLocationSearch().contains(normalizedQuery)
}

private fun createAvatarCameraOutputFile(context: android.content.Context): File {
    val dir = File(context.cacheDir, "avatars").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File(dir, "avatar_$timestamp.jpg")
}

internal fun createProfileEditCameraOutput(
    createFile: () -> File,
    createUri: (File) -> Uri,
): ProfileEditCameraOutput {
    val file = createFile()
    return try {
        ProfileEditCameraOutput(file = file, uri = createUri(file))
    } catch (error: Throwable) {
        file.delete()
        throw error
    }
}
