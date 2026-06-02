package com.mygolfswing.app.ui.common

import androidx.annotation.DrawableRes
import com.mygolfswing.app.R

/** 注册 / 资料预设头像（对齐 iOS `DefaultAvatars` 与 `RegisterPage.avatarOptions`）。 */
data class V2PresetAvatar(
    /** 云端存储 key，如 `Avatar_XiaoQie` */
    val assetKey: String,
    @DrawableRes val drawableRes: Int,
    val displayName: String,
)

object V2PresetAvatars {
    val all: List<V2PresetAvatar> = listOf(
        V2PresetAvatar("Avatar_XiaoQie", R.drawable.avatar_xiaoqie, "小切"),
        V2PresetAvatar("Avatar_XiaoKaola", R.drawable.avatar_xiaokaola, "考拉"),
        V2PresetAvatar("Avatar_XiaoHuli", R.drawable.avatar_xiaohuli, "狐狸"),
        V2PresetAvatar("Avatar_XiaoXiong", R.drawable.avatar_xiaoxiong, "小熊"),
        V2PresetAvatar("Avatar_Konglong", R.drawable.avatar_konglong, "恐龙"),
    )

    private val drawableByKey: Map<String, Int> = buildMap {
        all.forEach { put(it.assetKey, it.drawableRes) }
        // 兼容旧版 Android 注册写入的短 key
        put("xiaoqie", R.drawable.avatar_xiaoqie)
        put("koala", R.drawable.avatar_xiaokaola)
        put("fox", R.drawable.avatar_xiaohuli)
        put("bear", R.drawable.avatar_xiaoxiong)
        put("dino", R.drawable.avatar_konglong)
    }

    fun drawableFor(avatar: String?): Int? = avatar?.trim()?.takeIf { it.isNotEmpty() }?.let { drawableByKey[it] }

    fun isPresetKey(avatar: String?): Boolean = drawableFor(avatar) != null
}
