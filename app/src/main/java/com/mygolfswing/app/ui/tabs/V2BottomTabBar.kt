package com.mygolfswing.app.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Person as PersonOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.filled.Forum
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag

/**
 * V2 底部导航（对齐 iOS `V2BottomTabBar.swift`）。
 *
 * - 5 项：首页 / Memo / [中间「挥呗」连接入口] / 工具 / 我
 * - 选中色 [DSColors.deepGreen]，未选中 [DSColors.shadowGray]
 * - 中间挥呗按钮：[DSColors.fairway] 圆形，白色边框，浮起阴影，向上偏移
 * - 顶部 0.5dp 淡描边，底部留出 navigation bar 安全区
 */
@Composable
fun V2BottomTabBar(
    selected: V2Tab,
    onSelect: (V2Tab) -> Unit,
    modifier: Modifier = Modifier,
    interactionBadge: Int = 0,
    hasCoachConnection: Boolean = false,
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
                            top = DSLayout.tabContentTop,
                            bottom = DSLayout.tabContentBottom,
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
                    // 中间挥呗入口让位
                    Spacer(modifier = Modifier.width(52.dp))
                    TabItem(
                        tab = V2Tab.PLAZA,
                        selected = selected == V2Tab.PLAZA,
                        iconSelected = Icons.Filled.GridView,
                        iconUnselected = Icons.Filled.GridView,
                        label = "工具",
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

        // 中间「挥呗」连接按钮
        InteractionFab(
            onClick = { onSelect(V2Tab.INTERACTION) },
            selected = selected == V2Tab.INTERACTION,
            badge = interactionBadge,
            hasCoachConnection = hasCoachConnection,
            modifier = Modifier
                .align(Alignment.TopCenter)
                // 和 iOS 一样只占白条高度，按钮用位移凸到页面上，不再在底栏上方留灰条。
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
            .testTag("bottom-tab-${tab.name.lowercase()}")
            .semantics { this.selected = selected }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (selected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(DSLayout.tabIcon),
        )
        Text(
            text = label,
            style = DSTypography.tab,
            color = tint,
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .testTag("bottom-tab-selection-${tab.name.lowercase()}")
                .background(
                    if (selected) DSColors.deepGreen else Color.Transparent,
                    CircleShape,
                ),
        )
    }
}

@Composable
private fun InteractionFab(
    onClick: () -> Unit,
    selected: Boolean,
    badge: Int,
    hasCoachConnection: Boolean,
    modifier: Modifier = Modifier,
) {
    // iOS 使用 54pt 深绿圆、4pt 白色内描边；选中时再向外增加 1.5pt 金色环。
    Box(
        modifier = modifier
            .size(50.dp)
            .testTag("bottom-tab-interaction")
            .semantics {
                contentDescription = if (hasCoachConnection) "教练互动" else "连接教练"
                stateDescription = if (selected) "当前页面" else ""
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(elevation = DSSpacing.Shadow.elevated.elevation, shape = CircleShape)
                .clip(CircleShape)
                .background(DSColors.fairway)
                .border(BorderStroke(4.dp, DSColors.cardWhite), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Icon(
                    if (hasCoachConnection) Icons.Filled.Forum else Icons.Filled.Link,
                    null,
                    tint = DSColors.cardWhite,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "挥呗",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    ),
                    color = DSColors.cardWhite,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .requiredSize(50.dp)
                    .testTag("bottom-tab-interaction-selection-ring")
                    .border(BorderStroke(1.5.dp, DSColors.goldBright), CircleShape),
            )
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-3).dp)
                    .testTag("bottom-tab-interaction-badge")
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.flagRed)
                    .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(50))
                    .padding(horizontal = if (badge > 9) 5.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
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
