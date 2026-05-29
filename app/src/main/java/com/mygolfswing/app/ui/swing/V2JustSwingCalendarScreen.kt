package com.mygolfswing.app.ui.swing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.memo.V2MemoSwingSessionCard
import java.time.LocalDate

/** 挥杆记录日历/列表（对齐 iOS `JustSwingCalendarView` 简化版） */
@Composable
fun V2JustSwingCalendarScreen(
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val dayItems by V2SwingSessionStore.allDayItems.collectAsState()

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(title = "挥杆记录", onBack = onBack)
        if (sessions.isEmpty()) {
            Text(
                "暂无挥杆记录。导入或录制视频后会自动归入当天。",
                modifier = Modifier.padding(DSSpacing.xl),
                color = DSColors.shadowGray,
            )
        } else {
            LazyColumn(Modifier.padding(vertical = 8.dp)) {
                items(dayItems, key = { it.id }) { item ->
                    V2MemoSwingSessionCard(
                        item = item,
                        onTap = { onOpenDay(item.day) },
                        modifier = Modifier.padding(horizontal = DSSpacing.lg, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
