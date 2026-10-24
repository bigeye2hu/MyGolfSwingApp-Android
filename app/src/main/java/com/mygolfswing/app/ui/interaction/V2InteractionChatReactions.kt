package com.mygolfswing.app.ui.interaction

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.friend.V2IMMessage
import com.mygolfswing.app.data.friend.V2IMReaction
import com.mygolfswing.app.data.friend.V2IMReactionUser
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors

internal data class V2ChatReactionEmoji(
    val reactionId: String,
    val glyph: String,
    val assetIndex: Int = 0,
    val localizedName: String = reactionId,
)

/** Simplified-Chinese values copied from iOS EmojiFace.bundle/Localizable.strings. */
private val v2ChatEmojiLocalizedNames = listOf(
    "[微笑]", "[期待]", "[眨眼]", "[大笑]", "[姨母笑]", "[哈哈哈]", "[愉快]", "[无语]",
    "[惊讶]", "[悲伤]", "[得意]", "[傻了]", "[色]", "[憨笑]", "[亲亲]", "[大哭]",
    "[哭笑]", "[困]", "[口罩]", "[恐惧]", "[龇牙]", "[发怒]", "[打哈欠]", "[机智]",
    "[星星眼]", "[闭嘴]", "[叹气]", "[呵呵]", "[收声]", "[惊喜]", "[白眼]", "[OK]",
    "[便便]", "[怪兽]", "[恶魔]", "[恶魔怒]", "[衰]", "[猪]", "[牛]", "[AI]",
    "[骷髅]", "[炸弹]", "[咖啡]", "[蛋糕]", "[啤酒]", "[花]", "[瓜]", "[壕]",
    "[爱心]", "[月亮]", "[太阳]", "[星星]", "[红包]", "[庆祝]", "[福]", "[发]",
    "[服]", "[禁]", "[666]", "[857]", "[刀]", "[赞]",
)

/** IDs deliberately match iOS EmojiFace/emoji.plist and are sent unchanged to Tencent IM. */
internal val v2ChatReactionEmojis = listOf(
    V2ChatReactionEmoji("[TUIEmoji_Smile]", "🙂"),
    V2ChatReactionEmoji("[TUIEmoji_Expect]", "🤗"),
    V2ChatReactionEmoji("[TUIEmoji_Blink]", "😉"),
    V2ChatReactionEmoji("[TUIEmoji_Guffaw]", "😆"),
    V2ChatReactionEmoji("[TUIEmoji_KindSmile]", "😊"),
    V2ChatReactionEmoji("[TUIEmoji_Haha]", "😄"),
    V2ChatReactionEmoji("[TUIEmoji_Cheerful]", "😁"),
    V2ChatReactionEmoji("[TUIEmoji_Speechless]", "😶"),
    V2ChatReactionEmoji("[TUIEmoji_Amazed]", "😲"),
    V2ChatReactionEmoji("[TUIEmoji_Sorrow]", "😢"),
    V2ChatReactionEmoji("[TUIEmoji_Complacent]", "😏"),
    V2ChatReactionEmoji("[TUIEmoji_Silly]", "😜"),
    V2ChatReactionEmoji("[TUIEmoji_Lustful]", "😍"),
    V2ChatReactionEmoji("[TUIEmoji_Giggle]", "🤭"),
    V2ChatReactionEmoji("[TUIEmoji_Kiss]", "😘"),
    V2ChatReactionEmoji("[TUIEmoji_Wail]", "😭"),
    V2ChatReactionEmoji("[TUIEmoji_TearsLaugh]", "😂"),
    V2ChatReactionEmoji("[TUIEmoji_Trapped]", "😖"),
    V2ChatReactionEmoji("[TUIEmoji_Mask]", "😷"),
    V2ChatReactionEmoji("[TUIEmoji_Fear]", "😨"),
    V2ChatReactionEmoji("[TUIEmoji_BareTeeth]", "😬"),
    V2ChatReactionEmoji("[TUIEmoji_FlareUp]", "😡"),
    V2ChatReactionEmoji("[TUIEmoji_Yawn]", "🥱"),
    V2ChatReactionEmoji("[TUIEmoji_Tact]", "🤫"),
    V2ChatReactionEmoji("[TUIEmoji_Stareyes]", "🤩"),
    V2ChatReactionEmoji("[TUIEmoji_ShutUp]", "🤐"),
    V2ChatReactionEmoji("[TUIEmoji_Sigh]", "😮‍💨"),
    V2ChatReactionEmoji("[TUIEmoji_Hehe]", "🤓"),
    V2ChatReactionEmoji("[TUIEmoji_Silent]", "😑"),
    V2ChatReactionEmoji("[TUIEmoji_Surprised]", "😮"),
    V2ChatReactionEmoji("[TUIEmoji_Askance]", "🙄"),
    V2ChatReactionEmoji("[TUIEmoji_Ok]", "👌"),
    V2ChatReactionEmoji("[TUIEmoji_Shit]", "💩"),
    V2ChatReactionEmoji("[TUIEmoji_Monster]", "👾"),
    V2ChatReactionEmoji("[TUIEmoji_Daemon]", "👿"),
    V2ChatReactionEmoji("[TUIEmoji_Rage]", "🤬"),
    V2ChatReactionEmoji("[TUIEmoji_Fool]", "🤡"),
    V2ChatReactionEmoji("[TUIEmoji_Pig]", "🐷"),
    V2ChatReactionEmoji("[TUIEmoji_Cow]", "🐮"),
    V2ChatReactionEmoji("[TUIEmoji_Ai]", "🤖"),
    V2ChatReactionEmoji("[TUIEmoji_Skull]", "💀"),
    V2ChatReactionEmoji("[TUIEmoji_Bombs]", "💣"),
    V2ChatReactionEmoji("[TUIEmoji_Coffee]", "☕"),
    V2ChatReactionEmoji("[TUIEmoji_Cake]", "🎂"),
    V2ChatReactionEmoji("[TUIEmoji_Beer]", "🍺"),
    V2ChatReactionEmoji("[TUIEmoji_Flower]", "🌹"),
    V2ChatReactionEmoji("[TUIEmoji_Watermelon]", "🍉"),
    V2ChatReactionEmoji("[TUIEmoji_Rich]", "🤑"),
    V2ChatReactionEmoji("[TUIEmoji_Heart]", "❤️"),
    V2ChatReactionEmoji("[TUIEmoji_Moon]", "🌙"),
    V2ChatReactionEmoji("[TUIEmoji_Sun]", "☀️"),
    V2ChatReactionEmoji("[TUIEmoji_Star]", "⭐"),
    V2ChatReactionEmoji("[TUIEmoji_RedPacket]", "🧧"),
    V2ChatReactionEmoji("[TUIEmoji_Celebrate]", "🎉"),
    V2ChatReactionEmoji("[TUIEmoji_Bless]", "🙏"),
    V2ChatReactionEmoji("[TUIEmoji_Fortune]", "🪙"),
    V2ChatReactionEmoji("[TUIEmoji_Convinced]", "😎"),
    V2ChatReactionEmoji("[TUIEmoji_Prohibit]", "🚫"),
    V2ChatReactionEmoji("[TUIEmoji_666]", "🤙"),
    V2ChatReactionEmoji("[TUIEmoji_857]", "🤘"),
    V2ChatReactionEmoji("[TUIEmoji_Knife]", "🔪"),
    V2ChatReactionEmoji("[TUIEmoji_Like]", "👍"),
).mapIndexed { index, emoji ->
    emoji.copy(
        assetIndex = index,
        localizedName = v2ChatEmojiLocalizedNames[index],
    )
}

internal val v2ChatReactionDrawables = intArrayOf(
    R.drawable.tui_emoji_0,
    R.drawable.tui_emoji_1,
    R.drawable.tui_emoji_2,
    R.drawable.tui_emoji_3,
    R.drawable.tui_emoji_4,
    R.drawable.tui_emoji_5,
    R.drawable.tui_emoji_6,
    R.drawable.tui_emoji_7,
    R.drawable.tui_emoji_8,
    R.drawable.tui_emoji_9,
    R.drawable.tui_emoji_10,
    R.drawable.tui_emoji_11,
    R.drawable.tui_emoji_12,
    R.drawable.tui_emoji_13,
    R.drawable.tui_emoji_14,
    R.drawable.tui_emoji_15,
    R.drawable.tui_emoji_16,
    R.drawable.tui_emoji_17,
    R.drawable.tui_emoji_18,
    R.drawable.tui_emoji_19,
    R.drawable.tui_emoji_20,
    R.drawable.tui_emoji_21,
    R.drawable.tui_emoji_22,
    R.drawable.tui_emoji_23,
    R.drawable.tui_emoji_24,
    R.drawable.tui_emoji_25,
    R.drawable.tui_emoji_26,
    R.drawable.tui_emoji_27,
    R.drawable.tui_emoji_28,
    R.drawable.tui_emoji_29,
    R.drawable.tui_emoji_30,
    R.drawable.tui_emoji_31,
    R.drawable.tui_emoji_32,
    R.drawable.tui_emoji_33,
    R.drawable.tui_emoji_34,
    R.drawable.tui_emoji_35,
    R.drawable.tui_emoji_36,
    R.drawable.tui_emoji_37,
    R.drawable.tui_emoji_38,
    R.drawable.tui_emoji_39,
    R.drawable.tui_emoji_40,
    R.drawable.tui_emoji_41,
    R.drawable.tui_emoji_42,
    R.drawable.tui_emoji_43,
    R.drawable.tui_emoji_44,
    R.drawable.tui_emoji_45,
    R.drawable.tui_emoji_46,
    R.drawable.tui_emoji_47,
    R.drawable.tui_emoji_48,
    R.drawable.tui_emoji_49,
    R.drawable.tui_emoji_50,
    R.drawable.tui_emoji_51,
    R.drawable.tui_emoji_52,
    R.drawable.tui_emoji_53,
    R.drawable.tui_emoji_54,
    R.drawable.tui_emoji_55,
    R.drawable.tui_emoji_56,
    R.drawable.tui_emoji_57,
    R.drawable.tui_emoji_58,
    R.drawable.tui_emoji_59,
    R.drawable.tui_emoji_60,
    R.drawable.tui_emoji_61,
)

internal fun toggleChatReaction(
    reactions: List<V2IMReaction>,
    reactionId: String,
    shouldReact: Boolean,
): List<V2IMReaction> {
    val current = reactions.firstOrNull { it.reactionId == reactionId }
    val replacement = when {
        shouldReact && current == null -> V2IMReaction(reactionId, 1, true)
        shouldReact -> current?.copy(totalUserCount = current.totalUserCount + if (current.reactedByMe) 0 else 1, reactedByMe = true)
        current == null -> null
        !current.reactedByMe -> current
        current.totalUserCount <= 1 -> null
        else -> current.copy(totalUserCount = current.totalUserCount - 1, reactedByMe = false)
    }
    return buildList {
        reactions.forEach { reaction ->
            if (reaction.reactionId != reactionId) add(reaction)
            else if (replacement != null) add(replacement)
        }
        if (current == null && replacement != null) add(replacement)
    }
}

private const val REACTION_RECENTS_PREFERENCES = "v2_chat_reaction_recents"
private const val REACTION_RECENTS_KEY = "ids"

private fun quickReactionEmojis(context: Context): List<V2ChatReactionEmoji> {
    val recentIds = context.getSharedPreferences(REACTION_RECENTS_PREFERENCES, Context.MODE_PRIVATE)
        .getString(REACTION_RECENTS_KEY, null)
        ?.split('|')
        .orEmpty()
    val byId = v2ChatReactionEmojis.associateBy(V2ChatReactionEmoji::reactionId)
    return (recentIds.mapNotNull(byId::get) + v2ChatReactionEmojis)
        .distinctBy(V2ChatReactionEmoji::reactionId)
        .take(6)
}

private fun rememberReactionEmoji(context: Context, reactionId: String) {
    val preferences = context.getSharedPreferences(REACTION_RECENTS_PREFERENCES, Context.MODE_PRIVATE)
    val previous = preferences.getString(REACTION_RECENTS_KEY, null)?.split('|').orEmpty()
    preferences.edit().putString(
        REACTION_RECENTS_KEY,
        (listOf(reactionId) + previous).distinct().take(8).joinToString("|"),
    ).apply()
}

@Composable
internal fun V2ChatReactionQuickPicker(
    refreshKey: Any?,
    onSelect: (String) -> Unit,
    onExpand: () -> Unit,
) {
    val context = LocalContext.current
    val quickEmojis = remember(refreshKey) { quickReactionEmojis(context) }
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        quickEmojis.forEach { emoji ->
            V2ReactionEmojiButton(emoji, 28.dp) {
                rememberReactionEmoji(context, emoji.reactionId)
                onSelect(emoji.reactionId)
            }
        }
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.04f))
                .clickable(onClick = onExpand).testTag("chat-reaction-expand"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "更多表情", tint = DSColors.shadowGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun V2ReactionEmojiButton(
    emoji: V2ChatReactionEmoji,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
            .testTag("chat-reaction-${emoji.reactionId}"),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(v2ChatReactionDrawables[emoji.assetIndex]),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (size <= 30.dp) 24.dp else 38.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2ChatReactionPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DSColors.warmWhite) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("chat-reaction-picker-grid"),
        ) {
            items(v2ChatReactionEmojis, key = V2ChatReactionEmoji::reactionId) { emoji ->
                V2ReactionEmojiButton(emoji, 44.dp) {
                    rememberReactionEmoji(context, emoji.reactionId)
                    onSelect(emoji.reactionId)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun V2ChatReactionBar(
    reactions: List<V2IMReaction>,
    onClick: () -> Unit,
) {
    if (reactions.isEmpty()) return
    val shown = reactions.take(5)
    val hasMore = reactions.size > 5
    Row(
        modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(51.dp)).background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(51.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("chat-reaction-bar"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { reaction ->
            ReactionEmojiImage(reactionEmoji(reaction.reactionId), 18.dp)
            if (shown.size == 1 && reaction.totalUserCount > 0) {
                Text(reaction.totalUserCount.toString(), color = DSColors.shadowGray, fontSize = 14.sp)
            }
        }
        if (hasMore) Text("…", color = DSColors.shadowGray, fontSize = 14.sp)
        if (shown.size > 1 || hasMore) {
            Text(reactions.sumOf(V2IMReaction::totalUserCount).toString(), color = DSColors.shadowGray, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2ChatReactionDetailSheet(
    message: V2IMMessage,
    currentUserId: String?,
    onDismiss: () -> Unit,
    loadUsers: suspend (String) -> Result<List<V2IMReactionUser>>,
    onRemoveMine: (String) -> Unit,
) {
    var selectedId by remember(message.messageId) { mutableStateOf(message.reactions.firstOrNull()?.reactionId.orEmpty()) }
    var users by remember(message.messageId, selectedId) { mutableStateOf<List<V2IMReactionUser>>(emptyList()) }
    var loading by remember(message.messageId, selectedId) { mutableStateOf(false) }
    var error by remember(message.messageId, selectedId) { mutableStateOf<String?>(null) }
    val selectedReaction = message.reactions.firstOrNull { it.reactionId == selectedId }
    val displayedUsers = if (selectedReaction?.reactedByMe == true && currentUserId != null) {
        users.sortedBy { if (it.userId == currentUserId) 0 else 1 }
    } else users

    LaunchedEffect(message.messageId, selectedId) {
        if (selectedId.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        loadUsers(selectedId).onSuccess { users = it }.onFailure { error = "回应成员暂时无法加载" }
        loading = false
    }
    LaunchedEffect(message.reactions) {
        if (message.reactions.none { it.reactionId == selectedId }) {
            selectedId = message.reactions.firstOrNull()?.reactionId.orEmpty()
            if (selectedId.isBlank()) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DSColors.warmWhite) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            message.reactions.forEach { reaction ->
                val selected = reaction.reactionId == selectedId
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (selected) DSColors.fairway.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.04f))
                        .then(if (selected) Modifier.border(1.dp, DSColors.fairway, RoundedCornerShape(16.dp)) else Modifier)
                        .clickable { selectedId = reaction.reactionId }
                        .testTag("chat-reaction-detail-tab-${reaction.reactionId}")
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReactionEmojiImage(reactionEmoji(reaction.reactionId), 18.dp)
                    Text(
                        reaction.totalUserCount.toString(),
                        color = if (selected) DSColors.fairway else DSColors.shadowGray,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 560.dp).padding(horizontal = 16.dp)) {
            when {
                loading -> CircularProgressIndicator(Modifier.size(26.dp).align(Alignment.TopCenter), color = DSColors.fairway)
                error != null -> Text(error.orEmpty(), color = DSColors.shadowGray, modifier = Modifier.align(Alignment.TopCenter))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayedUsers, key = V2IMReactionUser::userId) { user ->
                        val isMe = selectedReaction?.reactedByMe == true && user.userId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = isMe) {
                                onRemoveMine(selectedId)
                            }.testTag("chat-reaction-user-${user.userId}").padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            V2UserAvatar(
                                avatar = user.avatar,
                                modifier = Modifier.size(36.dp),
                                fallbackText = user.nickname ?: user.userId,
                                fallbackColor = DSColors.mistGreen,
                            )
                            Column {
                                Text(user.nickname ?: user.userId, color = DSColors.deepGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (isMe) Text("点击移除", color = DSColors.shadowGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun reactionEmoji(reactionId: String): V2ChatReactionEmoji =
    v2ChatReactionEmojis.firstOrNull { it.reactionId == reactionId }
        ?: V2ChatReactionEmoji(reactionId, "🙂")

@Composable
private fun ReactionEmojiImage(
    emoji: V2ChatReactionEmoji,
    size: androidx.compose.ui.unit.Dp,
) {
    Image(
        painter = painterResource(v2ChatReactionDrawables[emoji.assetIndex]),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size),
    )
}
