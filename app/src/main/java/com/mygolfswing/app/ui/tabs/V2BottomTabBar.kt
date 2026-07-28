package com.mygolfswing.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Person as PersonOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme

/**
 * V2 底部导航（对齐 iOS `V2BottomTabBar.swift`）。
 *
 * - 5 项：home / memo / [中间 AI 凸起] / plaza / me
 * - 选中色 [DSColors.deepGreen]，未选中 [DSColors.shadowGray]
 * - 中间 AI 按钮：[DSColors.fairway] 圆形，白色边框，浮起阴影，向上偏移
 * - 顶部 0.5dp 淡描边，底部留出 navigation bar 安全区
 */
@Composable
fun V2BottomTabBar(
    selected: V2Tab,
    onSelect: (V2Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemNavInsets = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        // 底部白条
        Surface(
            color = DSColors.cardWhite,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            Column {
                // 顶部 0.5dp 淡描边
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(DSColors.borderLine),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp,
                            bottom = 8.dp,
                            start = DSSpacing.sm,
                            end = DSSpacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabItem(
                        tab = V2Tab.HOME,
                        selected = selected == V2Tab.HOME,
                        iconSelected = Icons.Filled.Home,
                        iconUnselected = Icons.Filled.Home,
                        label = "首页",
                        onClick = { onSelect(V2Tab.HOME) },
                        modifier = Modifier.weight(1f),
                    )
                    TabItem(
                        tab = V2Tab.MEMO,
                        selected = selected == V2Tab.MEMO,
                        iconSelected = Icons.Filled.Layers,
                        iconUnselected = Icons.Filled.Layers,
                        label = "Memo",
                        onClick = { onSelect(V2Tab.MEMO) },
                        modifier = Modifier.weight(1f),
                    )
                    // 中间 AI 让位
                    Spacer(modifier = Modifier.width(64.dp))
                    TabItem(
                        tab = V2Tab.PLAZA,
                        selected = selected == V2Tab.PLAZA,
                        iconSelected = Icons.Filled.Explore,
                        iconUnselected = Icons.Filled.Explore,
                        label = "广场",
                        onClick = { onSelect(V2Tab.PLAZA) },
                        modifier = Modifier.weight(1f),
                    )
                    TabItem(
                        tab = V2Tab.ME,
                        selected = selected == V2Tab.ME,
                        iconSelected = Icons.Outlined.PersonOutlined,
                        iconUnselected = Icons.Outlined.PersonOutlined,
                        label = "我",
                        onClick = { onSelect(V2Tab.ME) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // 留出系统导航栏安全区
                Spacer(modifier = Modifier.height(systemNavInsets.calculateBottomPadding()))
            }
        }

        // 中间 AI 凸起按钮
        AIFab(
            onClick = { onSelect(V2Tab.AI) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp),
        )
    }
}

@Composable
private fun TabItem(
    tab: V2Tab,
    selected: Boolean,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) DSColors.deepGreen else DSColors.shadowGray
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (selected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            style = DSTypography.tab,
            color = tint,
        )
    }
}

@Composable
private fun AIFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 外圈白色描边 + 内圈深绿
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(elevation = DSSpacing.Shadow.elevated.elevation, shape = CircleShape)
            .clip(CircleShape)
            .background(DSColors.cardWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(DSColors.fairway),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "AI",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                ),
                color = DSColors.cardWhite,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF7F0)
@Composable
private fun V2BottomTabBarPreview() {
    MyGolfSwingAppAndroidTheme {
        V2BottomTabBar(selected = V2Tab.HOME, onSelect = {})
    }
}
