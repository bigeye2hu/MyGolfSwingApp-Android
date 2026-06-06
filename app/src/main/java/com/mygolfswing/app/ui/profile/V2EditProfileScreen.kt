package com.mygolfswing.app.ui.profile

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.Image as FoundationImage

/**
 * 编辑资料（对齐 iOS `V2ProfileEditView`）。
 *
 * 全部分区：
 * - 头像（点击 → 系统相册选 → 上传 `uploadAvatar`）
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname.takeIf { it != "未登录" } ?: "") }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var birthday by remember(profile.birthday) { mutableStateOf(profile.birthday ?: "") }
    var hasBirthday by remember(profile.birthday) { mutableStateOf(!profile.birthday.isNullOrBlank()) }
    var location by remember(profile.location) { mutableStateOf(profile.location ?: "") }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio ?: "") }
    var handicap by remember(profile.handicap) {
        mutableStateOf(profile.handicap.takeIf { it != "未设置" } ?: "")
    }
    var golfStartYear by remember(profile.golfYears) {
        mutableStateOf(
            extractStartYear(profile.golfYears)?.toString() ?: "",
        )
    }
    var dominantHand by remember(profile.dominantHand) { mutableStateOf(profile.dominantHand) }
    val selectedBrands = remember(profile.golfBrands) {
        mutableStateListOf<String>().apply { addAll(profile.golfBrands) }
    }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 系统相册选图
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                uploadingAvatar = true
                try {
                    val bm = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    if (bm != null) {
                        avatarBitmap = bm
                        ServiceLocator.authRepo.uploadAvatar(bm).onFailure {
                            error = "头像上传失败：${it.message}"
                        }
                    }
                } finally {
                    uploadingAvatar = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        V2NavTopBarCentered(title = "编辑资料", onBack = onBack)

        // 头像
        AvatarSection(
            avatarUrl = profile.avatar,
            localBitmap = avatarBitmap,
            uploading = uploadingAvatar,
            onPickPhoto = {
                picker.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
        )

        Spacer(Modifier.height(8.dp))

        // 基础信息
        Section(title = "基础信息") {
            FormItem(label = "昵称") {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = { Text("展示给球友的名字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            FormItem(label = "性别") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genderOptions.forEach { opt ->
                        PillChoice(text = opt, selected = gender == opt) { gender = opt }
                    }
                }
            }
            FormItem(label = "生日") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (hasBirthday) "已设置" else "不显示生日",
                        style = DSTypography.body,
                        color = if (hasBirthday) DSColors.inkGreen else DSColors.shadowGray,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = hasBirthday,
                        onCheckedChange = { checked ->
                            hasBirthday = checked
                            if (checked && birthday.isBlank()) birthday = LocalDate.now().toString()
                        },
                    )
                }
                if (hasBirthday) {
                    BirthdayPickerField(
                        birthday = birthday,
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
                }
            }
            FormItem(label = "地区") {
                LocationPickerField(
                    location = location,
                    onClick = { showLocationPicker = true },
                )
            }
            FormItem(label = "个人简介", trailing = "${bio.length}/100") {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 100) bio = it },
                    placeholder = { Text("和球友分享你的故事…") },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    colors = fieldColors(),
                )
            }
        }

        // 高尔夫信息
        Section(title = "高尔夫信息") {
            FormItem(label = "差点 Handicap") {
                OutlinedTextField(
                    value = handicap,
                    onValueChange = { handicap = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    placeholder = { Text("例：18.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                FieldHint(text = "当前：${handicap.ifBlank { "未设置" }}")
            }
            FormItem(label = "开始学球年份") {
                OutlinedTextField(
                    value = golfStartYear,
                    onValueChange = { golfStartYear = it.filter { c -> c.isDigit() }.take(4) },
                    placeholder = { Text("例：2018") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                FieldHint(text = golfAgeHint(golfStartYear))
            }
            FormItem(label = "惯用手") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    handOptions.forEach { opt ->
                        PillChoice(text = opt, selected = dominantHand == opt) { dominantHand = opt }
                    }
                }
            }
        }

        // 球具品牌
        Section(title = "使用的球具品牌") {
            Text(
                text = "选择您常用的品牌（可多选）",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                brandOptions.forEach { brand ->
                    val sel = brand in selectedBrands
                    PillChoice(text = brand, selected = sel) {
                        if (sel) selectedBrands.remove(brand) else selectedBrands.add(brand)
                    }
                }
            }
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

        Button(
            onClick = {
                if (nickname.isBlank()) {
                    error = "昵称不能为空"
                    return@Button
                }
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
                    val r = ServiceLocator.authRepo.updateUserInfo(
                        nickname = nickname.trim(),
                        gender = gender.takeUnless { it.isBlank() || it == "保密" },
                        birthday = if (hasBirthday) birthday.ifBlank { null } else null,
                        location = location.ifBlank { null },
                        bio = bio.ifBlank { null },
                        handicap = handicapValue?.toString(),
                        golfAge = startYear?.let { (LocalDate.now().year - it).coerceAtLeast(0) },
                        golfStartYear = startYear,
                        dominantHand = dominantHand.takeIf { it != "未设置" },
                        golfBrands = selectedBrands.toList(),
                    )
                    saving = false
                    r.onSuccess { onSaved() }
                        .onFailure { error = it.message ?: "保存失败" }
                }
            },
            enabled = !saving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DSColors.fairway,
                contentColor = DSColors.warmWhite,
            ),
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = DSColors.warmWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text("保存", style = DSTypography.bodyBold)
            }
        }

        Spacer(Modifier.height(40.dp))
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
            onClear = {
                location = ""
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false },
        )
    }
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
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(DSColors.mistGreen, CircleShape)
                .border(BorderStroke(2.dp, DSColors.goldBright), CircleShape)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center,
        ) {
            when {
                localBitmap != null -> FoundationImage(
                    bitmap = localBitmap.asImageBitmap(),
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).background(Color.Transparent, CircleShape),
                )
                !avatarUrl.isNullOrBlank() -> coil.compose.AsyncImage(
                    model = avatarUrl,
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp),
                )
                else -> Text("🐧", fontSize = 40.sp)
            }
            if (uploading) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(DSColors.fairway, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        tint = DSColors.warmWhite,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onPickPhoto) {
            Text("更换头像", color = DSColors.fairway, style = DSTypography.body)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = DSTypography.bodyBold, color = DSColors.inkGreen)
        content()
    }
}

@Composable
private fun FormItem(
    label: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = DSTypography.caption, color = DSColors.shadowGray)
            if (trailing != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(trailing, style = DSTypography.caption, color = DSColors.shadowGray)
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
            .background(DSColors.warmWhite, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, DSColors.borderLine), RoundedCornerShape(12.dp))
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
            .background(DSColors.warmWhite, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, DSColors.borderLine), RoundedCornerShape(12.dp))
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
        Text("选择", style = DSTypography.caption, color = DSColors.fairway)
    }
}

@Composable
private fun LocationPickerDialog(
    selectedLocation: String,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    val filteredCities = remember(searchText) {
        popularLocationCities.filter { searchText.isBlank() || it.contains(searchText) }
    }
    val filteredProvinces = remember(searchText) {
        locationProvinces.filter { searchText.isBlank() || it.contains(searchText) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择地区", style = DSTypography.titleMedium, color = DSColors.inkGreen) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜索城市") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                LocationPickerSection(
                    title = "热门城市",
                    options = filteredCities,
                    selectedLocation = selectedLocation,
                    emptyText = "没有匹配的城市",
                    onSelect = onSelect,
                )
                LocationPickerSection(
                    title = "选择省份 / 地区",
                    options = filteredProvinces,
                    selectedLocation = selectedLocation,
                    emptyText = "没有匹配的省份",
                    onSelect = onSelect,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DSColors.fairway)
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text("清除", color = DSColors.shadowGray)
            }
        },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LocationPickerSection(
    title: String,
    options: List<String>,
    selectedLocation: String,
    emptyText: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    PillChoice(
                        text = option,
                        selected = selectedLocation == option,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PillChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) DSColors.fairway else DSColors.warmWhite,
                RoundedCornerShape(20.dp),
            )
            .border(
                BorderStroke(1.dp, if (selected) DSColors.fairway else DSColors.borderLine),
                RoundedCornerShape(20.dp),
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

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    cursorColor = DSColors.fairway,
)

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
