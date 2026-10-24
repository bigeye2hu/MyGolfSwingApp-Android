package com.mygolfswing.app.ui.coach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachLessonMode
import com.mygolfswing.app.data.coach.V2CoachProfileInput
import com.mygolfswing.app.data.coach.V2CoachStudentAge
import com.mygolfswing.app.data.coach.V2CoachTrainingGoal
import com.mygolfswing.app.data.coach.coachAccountFacts
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val V2_COACH_STAGE_PRESETS = listOf("初学入门", "动作建立", "稳定击球", "下场提升", "专项突破")
internal val V2_COACH_GOAL_PRESETS = listOf("稳定击球", "降低杆数", "纠正右曲", "增加距离", "短杆提升", "推杆提升")
internal val V2_COACH_FOCUS_PRESETS = listOf(
    "开球稳定性", "上杆平面", "下杆节奏", "右曲修正", "短杆提升", "推杆稳定性",
    "增加距离", "身体旋转", "重心转移", "击球一致性", "下场策略", "其他",
)
internal val V2_COACH_COMMUNICATION_PRESETS = listOf("图文+视频", "视频为主", "文字为主", "语音为主")
internal val V2_COACH_CATEGORY_PRESETS = listOf("入门", "进阶", "青少年", "短杆", "长期跟进")

private enum class V2CoachProfileEditorRoute(val title: String) {
    GOAL("训练目标 · 选填"),
    LESSON_MODE("授课方式 · 选填"),
    TRAINING_FOCUS("训练重点 · 选填"),
    BASICS("当前阶段 · 必填"),
    COACH_MANAGEMENT("教练管理"),
}

@Composable
fun V2CoachStudentProfileEditor(
    connection: V2CoachConnection,
    isOnboarding: Boolean = false,
    onBack: () -> Unit,
    save: suspend (V2CoachProfileInput) -> Result<V2CoachConnection>,
    onSaved: (V2CoachConnection) -> Unit,
    onUpdated: (V2CoachConnection) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var stage by rememberSaveable(connection.connectionId) { mutableStateOf(connection.trainingStage.orEmpty()) }
    var primaryGoal by rememberSaveable(connection.connectionId) { mutableStateOf(connection.primaryGoal.orEmpty()) }
    var focus by rememberSaveable(connection.connectionId) { mutableStateOf(connection.trainingFocus.orEmpty()) }
    var focusTags by rememberSaveable(connection.connectionId) { mutableStateOf(connection.focusTags.joinToString("，")) }
    var lessonModeName by rememberSaveable(connection.connectionId) { mutableStateOf(connection.resolvedLessonMode.name) }
    var trainingGoalNames by rememberSaveable(connection.connectionId) {
        mutableStateOf(connection.trainingGoalTags.mapNotNull { value ->
            V2CoachTrainingGoal.entries.firstOrNull { it.wireValue == value }?.name
        }.toSet())
    }
    var ageName by rememberSaveable(connection.connectionId) {
        mutableStateOf(V2CoachStudentAge.entries.firstOrNull { it.wireValue == connection.ageTag }?.name)
    }
    var weeklyGoal by rememberSaveable(connection.connectionId, isOnboarding) {
        mutableStateOf(connection.weeklyPracticeGoal?.toString() ?: "2")
    }
    var hasWeeklyGoal by rememberSaveable(connection.connectionId, isOnboarding) {
        mutableStateOf(connection.weeklyPracticeGoal != null || isOnboarding)
    }
    var cadence by rememberSaveable(connection.connectionId, isOnboarding) {
        mutableStateOf(connection.cadence ?: if (isOnboarding) "每周" else "")
    }
    var communication by rememberSaveable(connection.connectionId, isOnboarding) {
        mutableStateOf(connection.communicationPreference ?: if (isOnboarding) "图文+视频" else "")
    }
    var category by rememberSaveable(connection.connectionId) { mutableStateOf(connection.category.orEmpty()) }
    var note by rememberSaveable(connection.connectionId) { mutableStateOf(connection.privateNote.orEmpty()) }
    var pinned by rememberSaveable(connection.connectionId) { mutableStateOf(connection.pinned) }
    var hasFollowUp by rememberSaveable(connection.connectionId) { mutableStateOf(!connection.followUpAt.isNullOrBlank()) }
    var followUpAt by rememberSaveable(connection.connectionId) {
        mutableStateOf(connection.followUpAt ?: Instant.now().plus(1, ChronoUnit.DAYS).toString())
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editorRouteName by rememberSaveable(connection.connectionId) { mutableStateOf<String?>(null) }

    val lessonMode = V2CoachLessonMode.entries.firstOrNull { it.name == lessonModeName }
        ?: V2CoachLessonMode.FIXED
    val selectedGoals = trainingGoalNames.mapNotNull { name ->
        V2CoachTrainingGoal.entries.firstOrNull { it.name == name }
    }
    val selectedAge = V2CoachStudentAge.entries.firstOrNull { it.name == ageName }
    val accountFacts = connection.student?.coachAccountFacts(selectedAge?.title).orEmpty()
    val selectedFocusTagValues = focusTags.split(Regex("[，,\\s]+"))
        .map(String::trim).filter(String::isNotEmpty).toSet()
    val focusTagOptions = V2_COACH_FOCUS_PRESETS + selectedFocusTagValues
        .filterNot(V2_COACH_FOCUS_PRESETS::contains).sorted()
    // Current iOS contract: only the current training stage is required.
    val profileCompletedLocally = stage.isNotBlank()
    val profileScrollState = rememberScrollState()
    val editorRoute = V2CoachProfileEditorRoute.entries.firstOrNull { it.name == editorRouteName }
    val firstIncompleteRoute = V2CoachProfileEditorRoute.BASICS

    fun submit(closeAfterSave: Boolean = true) {
        if (stage.isBlank()) {
            error = "请先选择当前阶段。这一项必填，没选不能保存。"
            return
        }
        val weekly = if (hasWeeklyGoal) weeklyGoal.trim().takeIf { it.isNotEmpty() }?.toIntOrNull() else null
        when {
            hasWeeklyGoal && weekly == null -> {
                error = "每周练习次数请输入数字"
                return
            }
            weekly != null && weekly !in 1..14 -> {
                error = "每周练习目标需要在1到14次之间"
                return
            }
        }
        val input = V2CoachProfileInput(
            privateNote = note.trim(),
            pinned = pinned,
            category = category.normalizedOrNull(),
            followUpAt = followUpAt.takeIf { hasFollowUp },
            trainingStage = stage.trim(),
            primaryGoal = primaryGoal.trim(),
            trainingFocus = focus.trim(),
            focusTags = focusTags.split(Regex("[，,\\s]+"))
                .map(String::trim).filter(String::isNotEmpty).distinct().take(8),
            lessonMode = lessonMode,
            trainingGoals = selectedGoals,
            studentAge = selectedAge,
            weeklyPracticeGoal = weekly,
            cadence = cadence.normalizedOrNull(),
            communicationPreference = communication.normalizedOrNull(),
        )
        saving = true
        error = null
        scope.launch {
            save(input)
                .onSuccess { updated ->
                    if (closeAfterSave) {
                        onSaved(updated)
                    } else {
                        onUpdated(updated)
                        editorRouteName = null
                    }
                }
                .onFailure { error = coachThrowableError("教学档案暂时无法保存，请稍后重试", it) }
            saving = false
        }
    }

    V2BackHandler {
        if (saving) return@V2BackHandler
        if (editorRouteName != null) editorRouteName = null else onBack()
    }

    if (editorRoute != null) {
        V2CoachProfileEditorSubpage(
            title = editorRoute.title,
            saving = saving,
            error = error,
            canFinish = editorRoute != V2CoachProfileEditorRoute.BASICS || stage.isNotBlank(),
            onBack = { if (!saving) editorRouteName = null },
            onDone = { submit(closeAfterSave = false) },
        ) {
            when (editorRoute) {
                V2CoachProfileEditorRoute.GOAL -> {
                    V2PageArtworkBackground(
                        artwork = V2PageArtwork.FAIRWAY,
                        tone = V2PageArtworkTone.BOTTOM_GREEN,
                        modifier = Modifier.fillMaxWidth().height(142.dp).clip(RoundedCornerShape(20.dp)),
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text("“ 设定一个目标", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("让每一次练习更有方向 ”", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    V2CoachTrainingGoal.entries.forEach { goal ->
                        ProfileGoalChoiceCard(
                            goal = goal,
                            selected = goal.name in trainingGoalNames,
                            onClick = {
                                trainingGoalNames = setOf(goal.name)
                                if (primaryGoal.isBlank()) primaryGoal = goal.title
                            },
                        )
                    }
                    ProfileGoalDetailEditor(
                        value = primaryGoal,
                        onValueChange = { primaryGoal = it.take(100) },
                    )
                }
                V2CoachProfileEditorRoute.LESSON_MODE -> {
                    ProfileLessonModeEditor(
                        lessonMode = lessonMode,
                        onLessonModeSelected = { lessonModeName = it.name },
                        hasWeeklyGoal = hasWeeklyGoal,
                        onHasWeeklyGoalChanged = { hasWeeklyGoal = it },
                        weeklyGoal = weeklyGoal.toIntOrNull() ?: 2,
                        onWeeklyGoalChanged = { weeklyGoal = it.coerceIn(1, 14).toString() },
                        cadence = cadence,
                        onCadenceSelected = { cadence = it },
                        communication = communication,
                        onCommunicationSelected = { communication = it },
                    )
                }
                V2CoachProfileEditorRoute.TRAINING_FOCUS -> {
                    ProfileTrainingFocusEditor(
                        focus = focus,
                        onFocusChanged = { focus = it.take(120) },
                        options = focusTagOptions,
                        selected = selectedFocusTagValues,
                        onToggle = { value ->
                            val next = when {
                                value in selectedFocusTagValues -> selectedFocusTagValues - value
                                selectedFocusTagValues.size < 8 -> selectedFocusTagValues + value
                                else -> selectedFocusTagValues
                            }
                            focusTags = next.sorted().joinToString("，")
                        },
                    )
                }
                V2CoachProfileEditorRoute.BASICS -> {
                    ProfileAccountCard(
                        connection = connection,
                        accountFacts = accountFacts,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp)).padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("当前阶段", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("只有这一项是必填。选好并保存后，首页不再提示补齐", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        if (stage.isBlank()) {
                            Text(
                                "请先点选下面一项。没选不能保存。",
                                color = Color(0xFF8C3A32),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFE4E0)).padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("当前阶段", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("必填", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF8C3A32))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        ProfileChoiceGrid(
                            choices = V2_COACH_STAGE_PRESETS,
                            selected = stage,
                            columns = 4,
                            onSelected = { stage = it },
                        )
                        Text("年龄标签（选填）", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        ProfileChoiceGrid(
                            choices = V2CoachStudentAge.entries.map(V2CoachStudentAge::title),
                            selected = selectedAge?.title.orEmpty(),
                            columns = 3,
                            allowEmpty = true,
                            onSelected = { title -> ageName = V2CoachStudentAge.entries.firstOrNull { it.title == title }?.name },
                        )
                        Text("沟通方式", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        ProfileChoiceGrid(
                            choices = V2_COACH_COMMUNICATION_PRESETS,
                            selected = communication,
                            columns = 4,
                            allowEmpty = true,
                            onSelected = { communication = it },
                        )
                    }
                }
                V2CoachProfileEditorRoute.COACH_MANAGEMENT -> {
                    ProfilePrivateManagementEditor(
                        category = category,
                        onCategoryChanged = { category = it.take(30) },
                        pinned = pinned,
                        onPinnedChanged = { pinned = it },
                        hasFollowUp = hasFollowUp,
                        onHasFollowUpChanged = { hasFollowUp = it },
                        followUpAt = followUpAt,
                        onFollowUpAtChanged = { followUpAt = it },
                        note = note,
                        onNoteChanged = { note = it.take(300) },
                    )
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).background(Color.White).padding(horizontal = 10.dp),
        ) {
            Text(
                if (isOnboarding) "稍后" else "取消",
                color = DSColors.deepGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterStart).testTag("coach-profile-cancel").clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !saving, onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
            )
            Text(
                if (isOnboarding) "新学员接入" else "教学档案",
                color = DSColors.deepGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                if (saving) "保存中…" else "保存",
                color = if (profileCompletedLocally) DSColors.fairway else DSColors.shadowGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).testTag("coach-profile-save").clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !saving && profileCompletedLocally) { submit() }
                    .padding(horizontal = 8.dp, vertical = 9.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(profileScrollState)
                .padding(horizontal = 14.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            V2PageArtworkBackground(
                artwork = V2PageArtwork.GOLFER,
                tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(208.dp)
                    .clip(RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        V2UserAvatar(
                            avatar = connection.student?.avatar,
                            modifier = Modifier.size(62.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .border(2.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                            fallbackFontSize = 24.sp,
                            placeholderIconSize = 36.dp,
                            fallbackText = connection.student?.nickname ?: "挥呗学员",
                            fallbackColor = DSColors.sandBeige,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                connection.student?.nickname ?: "挥呗学员",
                                color = DSColors.deepGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isOnboarding) "新加入的学员" else "一对一学员",
                                    color = DSColors.shadowGray,
                                    fontSize = 11.sp,
                                )
                                Text(
                                    if (isOnboarding) "刚刚接入" else "已连接",
                                    color = DSColors.fairway,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    if (accountFacts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            accountFacts.take(4).forEachIndexed { index, fact ->
                                Text(
                                    fact,
                                    color = DSColors.deepGreen.copy(alpha = 0.82f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                if (index < accountFacts.take(4).lastIndex) {
                                    Box(Modifier.size(width = 1.dp, height = 18.dp).background(Color.White.copy(alpha = 0.24f)))
                                }
                            }
                        }
                    }
                }
            }

            Text("教学信息", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            ProfileCompletionCard(
                isOnboarding = isOnboarding,
                complete = profileCompletedLocally,
                onStart = { editorRouteName = firstIncompleteRoute.name },
            )

            if (profileCompletedLocally) {
                ProfileOverviewGoalCard(
                    title = primaryGoal.ifBlank { selectedGoals.firstOrNull()?.title ?: "待设置" },
                    detail = selectedGoals.firstOrNull()?.title,
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.GOAL.name },
                )
                ProfileOverviewRow(
                    icon = when (lessonMode) {
                        V2CoachLessonMode.FIXED -> Icons.Filled.CalendarMonth
                        V2CoachLessonMode.FLEXIBLE -> Icons.Filled.Sync
                        V2CoachLessonMode.SINGLE -> Icons.Filled.Person
                    },
                    title = "授课方式",
                    value = lessonMode.title,
                    detail = if (hasWeeklyGoal) "每周 $weeklyGoal 次 · ${cadence.ifBlank { "按需" }}" else cadence.ifBlank { "按需安排" },
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.LESSON_MODE.name },
                )
                ProfileOverviewFocusCard(
                    focus = focus,
                    tags = selectedFocusTagValues,
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.TRAINING_FOCUS.name },
                )
            }

            Text("教学目标", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                if (profileCompletedLocally) "当前阶段已保存，其余可以以后再填"
                else "还不能保存。请先点「当前阶段」，从初学入门、动作建立、稳定击球、下场提升、专项突破里选一项。",
                color = if (profileCompletedLocally) DSColors.shadowGray else Color(0xFF8C3A32),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileEntryCard(
                    icon = Icons.Filled.Person,
                    title = "当前阶段",
                    detail = listOfNotNull(stage.takeIf(String::isNotBlank), selectedAge?.title)
                        .joinToString(" / ").ifBlank { "还没选。点这里选择一项" },
                    requirement = "必填",
                    missing = stage.isBlank(),
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.BASICS.name },
                    modifier = Modifier.weight(1f),
                )
                ProfileEntryCard(
                    icon = Icons.Filled.Flag,
                    title = "训练目标",
                    detail = selectedGoals.firstOrNull()?.title ?: primaryGoal.ifBlank { "可以以后再填" },
                    requirement = "选填",
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.GOAL.name },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileEntryCard(
                    icon = Icons.Filled.SportsGolf,
                    title = "训练重点",
                    detail = focus.ifBlank { "可以以后再填" },
                    requirement = "选填",
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.TRAINING_FOCUS.name },
                    modifier = Modifier.weight(1f),
                )
                ProfileEntryCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = "授课方式",
                    detail = lessonMode.title,
                    requirement = "选填",
                    onClick = { editorRouteName = V2CoachProfileEditorRoute.LESSON_MODE.name },
                    modifier = Modifier.weight(1f),
                )
            }

            Text("教学备注", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            ProfileManagementEntry(
                detail = listOfNotNull(
                    category.takeIf(String::isNotBlank),
                    if (pinned) "重点学员" else null,
                    followUpAt?.let { "已设置跟进" },
                ).joinToString(" · ").ifBlank { "重点学员、跟进提醒、分类和私密备注" },
                onClick = { editorRouteName = V2CoachProfileEditorRoute.COACH_MANAGEMENT.name },
            )

            error?.let {
                Text(
                    it,
                    color = Color(0xFF8C3A32),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE4E0)).padding(12.dp),
                )
            }
        }

    }
    }
}

@Composable
private fun ProfileOverviewGoalCard(
    title: String,
    detail: String?,
    onClick: () -> Unit,
) {
    V2PageArtworkBackground(
        artwork = V2PageArtwork.GOLFER,
        tone = V2PageArtworkTone.DARK_GREEN,
        modifier = Modifier.fillMaxWidth().height(106.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("当前目标", color = Color.White.copy(alpha = 0.76f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            detail?.takeIf { it != title }?.let {
                Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProfileOverviewRow(
    icon: ImageVector,
    title: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(DSColors.fairway),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DSColors.shadowGray, fontSize = 10.sp)
            Text(value, color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(detail, color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
    }
}

@Composable
private fun ProfileOverviewFocusCard(
    focus: String,
    tags: Set<String>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("当前训练重点", color = DSColors.deepGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Edit, null, tint = DSColors.fairway, modifier = Modifier.size(10.dp))
            Text("编辑", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 3.dp))
        }
        Text(
            focus.ifBlank { " " },
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.5f)).padding(12.dp),
        )
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tags.sorted().forEach { tag ->
                    Text(
                        tag,
                        color = DSColors.fairway,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(25.dp).clip(RoundedCornerShape(50))
                            .background(DSColors.mistGreen).padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEntryCard(
    icon: ImageVector,
    title: String,
    detail: String,
    requirement: String,
    missing: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("coach-profile-entry-$title").height(132.dp)
            .clip(RoundedCornerShape(18.dp)).background(if (missing) Color(0xFFFFF6F4) else Color.White)
            .border(if (missing) 1.5.dp else 1.dp, if (missing) Color(0xFF8C3A32) else DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(21.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                requirement,
                color = if (requirement == "必填") Color.White else DSColors.fairway,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(18.dp).clip(RoundedCornerShape(50))
                    .background(if (requirement == "必填") DSColors.fairway else DSColors.mistGreen)
                    .padding(horizontal = 7.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 2)
        }
    }
}

@Composable
private fun ProfileManagementEntry(
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("coach-profile-management-entry").clip(RoundedCornerShape(18.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.4f)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFFE9DCC2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, null, tint = Color(0xFFE0A82E), modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("教练管理", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
    }
}

@Composable
private fun V2CoachProfileEditorSubpage(
    title: String,
    saving: Boolean,
    error: String?,
    canFinish: Boolean = true,
    onBack: () -> Unit,
    onDone: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).background(Color.White).padding(horizontal = 10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回教学档案",
                tint = DSColors.deepGreen,
                modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !saving, onClick = onBack).padding(10.dp),
            )
            Text(
                title,
                color = DSColors.deepGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                if (saving) "保存中…" else "完成",
                color = if (canFinish) DSColors.fairway else DSColors.shadowGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !saving && canFinish, onClick = onDone)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        error?.let {
            Text(
                it,
                color = Color(0xFF8C3A32),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFE4E0)).padding(12.dp),
            )
        }
    }
    }
}

@Composable
private fun ProfileCompletionCard(
    isOnboarding: Boolean,
    complete: Boolean,
    onStart: () -> Unit,
) {
    if (complete) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.58f)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Verified, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            Text("教学档案已完善", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("可随时调整", color = DSColors.shadowGray, fontSize = 10.sp)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(androidx.compose.foundation.shape.CircleShape).background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isOnboarding) Icons.Filled.PersonAdd else Icons.Filled.FindInPage,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(27.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                if (isOnboarding) "有新学员接入" else "还没有完整教学档案",
                color = DSColors.deepGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (isOnboarding) "必填只有当前阶段。目标和训练重点可以以后再填。"
                else "还差当前阶段。选好并保存后就不再提示；目标和训练重点可以以后再填。",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Text(if (isOnboarding) "为这位学员建档" else "开始完善档案", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.padding(start = 8.dp).size(16.dp))
        }
    }
}

@Composable
private fun ProfileGoalChoiceCard(
    goal: V2CoachTrainingGoal,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("coach-profile-goal-${goal.name}")
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) DSColors.fairway else Color.White)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp)),
            )
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) DSColors.fairway else DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                coachTrainingGoalIcon(goal),
                contentDescription = null,
                tint = if (selected) Color.White else DSColors.fairway,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                goal.title,
                color = if (selected) Color.White else DSColors.deepGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                coachTrainingGoalDescription(goal),
                color = if (selected) Color.White.copy(alpha = 0.8f) else DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (selected) "已选择" else "未选择",
            tint = if (selected) Color(0xFFE0A82E) else DSColors.borderLine,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun ProfileGoalDetailEditor(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.ChatBubble, null, tint = DSColors.deepGreen, modifier = Modifier.size(13.dp))
            Text("补充说明（可选）", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = DSColors.deepGreen, fontSize = 13.sp),
            cursorBrush = SolidColor(DSColors.fairway),
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).testTag("coach-profile-goal-detail")
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp)).padding(10.dp),
        )
        Text(
            "${value.length}/100",
            color = DSColors.shadowGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

private fun coachTrainingGoalIcon(goal: V2CoachTrainingGoal): ImageVector = when (goal) {
    V2CoachTrainingGoal.BEGINNER -> Icons.Filled.Eco
    V2CoachTrainingGoal.BREAK_100_90 -> Icons.Filled.Flag
    V2CoachTrainingGoal.BREAK_80 -> Icons.Filled.EmojiEvents
}

private fun coachTrainingGoalDescription(goal: V2CoachTrainingGoal): String = when (goal) {
    V2CoachTrainingGoal.BEGINNER -> "建立基础动作与击球理解"
    V2CoachTrainingGoal.BREAK_100_90 -> "以降低下场杆数为目标"
    V2CoachTrainingGoal.BREAK_80 -> "向更高水平和稳定表现迈进"
}

@Composable
private fun ProfileLessonModeEditor(
    lessonMode: V2CoachLessonMode,
    onLessonModeSelected: (V2CoachLessonMode) -> Unit,
    hasWeeklyGoal: Boolean,
    onHasWeeklyGoalChanged: (Boolean) -> Unit,
    weeklyGoal: Int,
    onWeeklyGoalChanged: (Int) -> Unit,
    cadence: String,
    onCadenceSelected: (String) -> Unit,
    communication: String,
    onCommunicationSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileEditorSectionTitle(
            title = "选择授课方式",
            subtitle = "用于教练首页和学员列表的查看范围，不会移动历史内容",
        )
        V2CoachLessonMode.entries.forEach { mode ->
            val selected = lessonMode == mode
            Row(
                modifier = Modifier.fillMaxWidth().testTag("coach-profile-lesson-mode-${mode.name}")
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) DSColors.fairway else Color.White)
                    .clickable { onLessonModeSelected(mode) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (selected) DSColors.fairway else DSColors.mistGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        coachLessonModeIcon(mode),
                        contentDescription = null,
                        tint = if (selected) Color.White else DSColors.fairway,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        mode.title,
                        color = if (selected) Color.White else DSColors.deepGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        coachLessonModeDescription(mode),
                        color = if (selected) Color.White.copy(alpha = 0.78f) else DSColors.shadowGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (selected) "已选择授课方式" else "未选择授课方式",
                    tint = if (selected) Color.White else DSColors.borderLine,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            ProfileEditorSectionTitle(
                title = "教学节奏",
                subtitle = "目标和行为数据会自动组合成卡片进度",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("设置每周练习目标", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("只统计学员在教学空间提交的真实练习", color = DSColors.shadowGray, fontSize = 9.sp)
                }
                Switch(
                    checked = hasWeeklyGoal,
                    onCheckedChange = onHasWeeklyGoalChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DSColors.fairway,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DSColors.mistGreen,
                        uncheckedBorderColor = DSColors.borderLine,
                    ),
                    modifier = Modifier.testTag("coach-profile-weekly-toggle"),
                )
            }
            if (hasWeeklyGoal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("每周 $weeklyGoal 次", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    ProfileStepperButton(
                        icon = Icons.Filled.Remove,
                        contentDescription = "减少每周练习次数",
                        enabled = weeklyGoal > 1,
                        testTag = "coach-profile-weekly-decrement",
                    ) { onWeeklyGoalChanged(weeklyGoal - 1) }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    ProfileStepperButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "增加每周练习次数",
                        enabled = weeklyGoal < 14,
                        testTag = "coach-profile-weekly-increment",
                    ) { onWeeklyGoalChanged(weeklyGoal + 1) }
                }
            }
            Text("教学频率", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ProfileChoiceGrid(
                choices = listOf("每周", "双周", "每月", "按需"),
                selected = cadence,
                columns = 4,
                allowEmpty = true,
                onSelected = onCadenceSelected,
            )
            Text("沟通方式", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ProfileChoiceGrid(
                choices = V2_COACH_COMMUNICATION_PRESETS,
                selected = communication,
                columns = 4,
                allowEmpty = true,
                onSelected = onCommunicationSelected,
            )
        }
    }
}

@Composable
private fun ProfileStepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(34.dp).testTag(testTag).clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (enabled) DSColors.mistGreen else DSColors.mistGreen.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (enabled) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun ProfileTrainingFocusEditor(
    focus: String,
    onFocusChanged: (String) -> Unit,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Filled.Edit, null, tint = DSColors.deepGreen, modifier = Modifier.size(17.dp))
                Text("当前训练重点", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("告诉教练你最近主要在练什么", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            BasicTextField(
                value = focus,
                onValueChange = onFocusChanged,
                textStyle = TextStyle(color = DSColors.deepGreen, fontSize = 14.sp),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).testTag("coach-profile-focus-detail")
                    .clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp)).padding(12.dp),
            )
            Text(
                "${focus.length}/120",
                color = DSColors.shadowGray,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("常用主题（点击快速添加）", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ProfileFocusChoiceGrid(options = options, selected = selected, onToggle = onToggle)
        }
    }
}

@Composable
private fun ProfileFocusChoiceGrid(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        options.chunked(4).forEach { rowOptions ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    val isSelected = option in selected
                    Box(
                        modifier = Modifier.weight(1f).height(34.dp).testTag("coach-profile-focus-tag-$option")
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) DSColors.fairway else Color(0xFFE9DCC2))
                            .clickable { onToggle(option) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            option,
                            color = if (isSelected) Color.White else DSColors.fairway,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                repeat(4 - rowOptions.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ProfileEditorSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private fun coachLessonModeIcon(mode: V2CoachLessonMode): ImageVector = when (mode) {
    V2CoachLessonMode.FIXED -> Icons.Filled.CalendarMonth
    V2CoachLessonMode.FLEXIBLE -> Icons.Filled.Sync
    V2CoachLessonMode.SINGLE -> Icons.Filled.Person
}

private fun coachLessonModeDescription(mode: V2CoachLessonMode): String = when (mode) {
    V2CoachLessonMode.FIXED -> "有固定时间安排，持续跟进训练"
    V2CoachLessonMode.FLEXIBLE -> "长期但不固定时间，按练习节奏沟通"
    V2CoachLessonMode.SINGLE -> "围绕一次课程或具体问题完成指导"
}

@Composable
private fun ProfilePrivateManagementEditor(
    category: String,
    onCategoryChanged: (String) -> Unit,
    pinned: Boolean,
    onPinnedChanged: (Boolean) -> Unit,
    hasFollowUp: Boolean,
    onHasFollowUpChanged: (Boolean) -> Unit,
    followUpAt: String,
    onFollowUpAtChanged: (String) -> Unit,
    note: String,
    onNoteChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val normalizedCategory = category.trim()
    val categoryOptions = V2_COACH_CATEGORY_PRESETS + listOfNotNull(
        normalizedCategory.takeIf { it.isNotEmpty() && it !in V2_COACH_CATEGORY_PRESETS },
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.38f)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileEditorSectionTitle(title = "教练管理", subtitle = "以下信息仅教练本人可见")

        Text("学员分类", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ProfileChoiceGrid(
            choices = categoryOptions,
            selected = category,
            columns = 5,
            allowEmpty = true,
            onSelected = onCategoryChanged,
        )
        BasicTextField(
            value = category,
            onValueChange = onCategoryChanged,
            singleLine = true,
            textStyle = TextStyle(color = DSColors.deepGreen, fontSize = 12.sp),
            cursorBrush = SolidColor(DSColors.fairway),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (category.isEmpty()) {
                        Text("或输入自定义分类", color = DSColors.shadowGray, fontSize = 12.sp)
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("coach-profile-category-input")
                .clip(RoundedCornerShape(13.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(13.dp)).padding(12.dp),
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("设为重点学员", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(
                checked = pinned,
                onCheckedChange = onPinnedChanged,
                colors = coachProfileSwitchColors(),
                modifier = Modifier.testTag("coach-profile-pinned-toggle"),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("提醒我跟进", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(
                checked = hasFollowUp,
                onCheckedChange = onHasFollowUpChanged,
                colors = coachProfileSwitchColors(),
                modifier = Modifier.testTag("coach-profile-follow-up-toggle"),
            )
        }
        if (hasFollowUp) {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("coach-profile-follow-up-picker")
                    .clip(RoundedCornerShape(13.dp)).background(Color.White)
                    .border(1.dp, DSColors.borderLine, RoundedCornerShape(13.dp))
                    .clickable {
                        showCoachFollowUpDateTimePicker(
                            context = context,
                            currentValue = followUpAt,
                            onSelected = onFollowUpAtChanged,
                        )
                    }.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("跟进时间", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Text(coachFollowUpDisplay(followUpAt), color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.CalendarMonth, null, tint = DSColors.fairway, modifier = Modifier.padding(start = 7.dp).size(17.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("私密备注", color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("${note.length}/300", color = DSColors.shadowGray, fontSize = 9.sp)
        }
        BasicTextField(
            value = note,
            onValueChange = onNoteChanged,
            textStyle = TextStyle(color = DSColors.deepGreen, fontSize = 13.sp),
            cursorBrush = SolidColor(DSColors.fairway),
            modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp).testTag("coach-profile-private-note")
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp)).padding(9.dp),
        )
    }
}

@Composable
private fun coachProfileSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = DSColors.fairway,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = DSColors.mistGreen,
    uncheckedBorderColor = DSColors.borderLine,
)

private fun coachFollowUpInstant(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { Instant.now().plus(1, ChronoUnit.DAYS) }

private fun coachFollowUpDisplay(value: String): String =
    DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
        .withZone(ZoneId.systemDefault())
        .format(coachFollowUpInstant(value))

private fun showCoachFollowUpDateTimePicker(
    context: Context,
    currentValue: String,
    onSelected: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val now = ZonedDateTime.now(zone)
    val current = coachFollowUpInstant(currentValue).atZone(zone)
    val lower = if (current.isBefore(now)) current else now
    val upper = now.plusYears(1)
    val dateDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day)
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selected = date.atTime(hour, minute).atZone(zone).toInstant()
                    val bounded = when {
                        selected.isBefore(lower.toInstant()) -> lower.toInstant()
                        selected.isAfter(upper.toInstant()) -> upper.toInstant()
                        else -> selected
                    }
                    onSelected(bounded.toString())
                },
                current.hour,
                current.minute,
                DateFormat.is24HourFormat(context),
            ).show()
        },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth,
    )
    dateDialog.datePicker.minDate = lower.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    dateDialog.datePicker.maxDate = upper.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    dateDialog.show()
}

@Composable
private fun ProfileAccountCard(
    connection: V2CoachConnection,
    accountFacts: List<String>,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            V2UserAvatar(
                avatar = connection.student?.avatar,
                modifier = Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .border(1.5.dp, Color(0xFFE0A82E), androidx.compose.foundation.shape.CircleShape),
                fallbackFontSize = 20.sp,
                placeholderIconSize = 30.dp,
                fallbackText = connection.student?.nickname ?: "挥呗学员",
                fallbackColor = DSColors.sandBeige,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(connection.student?.nickname ?: "挥呗学员", color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("账号资料自动同步 · 无需重复填写", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Filled.CloudDone, null, tint = DSColors.fairway, modifier = Modifier.size(20.dp))
        }
        if (accountFacts.isEmpty()) {
            Text("学员还没有填写高尔夫资料，昵称和头像仍会保持同步。", color = Color(0xFF6C757D), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                accountFacts.chunked(3).forEach { rowFacts ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        rowFacts.forEach { fact ->
                            Box(
                                modifier = Modifier.weight(1f).height(25.dp).clip(RoundedCornerShape(50))
                                    .background(DSColors.mistGreen).padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(fact, color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                        repeat(3 - rowFacts.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileChoiceGrid(
    choices: List<String>,
    selected: String,
    columns: Int,
    allowEmpty: Boolean = false,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.chunked(columns).forEach { rowChoices ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowChoices.forEach { choice ->
                    val isSelected = selected == choice
                    Box(
                        modifier = Modifier.weight(1f).height(33.dp).testTag("coach-profile-choice-$choice")
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) DSColors.fairway else DSColors.mistGreen)
                            .clickable { onSelected(if (allowEmpty && isSelected) "" else choice) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            choice,
                            color = if (isSelected) Color.White else DSColors.fairway,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                repeat(columns - rowChoices.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun String.normalizedOrNull(): String? = trim().takeIf(String::isNotEmpty)
